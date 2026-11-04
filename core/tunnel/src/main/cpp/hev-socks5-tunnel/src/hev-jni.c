/*
 ============================================================================
 Name        : hev-jni.c
 Author      : hev <r@hev.cc>
 Copyright   : Copyright (c) 2019 - 2023 hev
 Description : Jave Native Interface
 ============================================================================
 */

#ifdef ANDROID

#include <jni.h>
#include <pthread.h>
#include <stdatomic.h>

#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <string.h>

#include "hev-main.h"

#include "hev-jni.h"

/* clang-format off */
#ifndef PKGNAME
#define PKGNAME hev/htproxy
#endif
#ifndef CLSNAME
#define CLSNAME TProxyService
#endif
/* clang-format on */

#define STR(s) STR_ARG (s)
#define STR_ARG(c) #c
#define N_ELEMENTS(arr) (sizeof (arr) / sizeof ((arr)[0]))

typedef struct _ThreadData ThreadData;

struct _ThreadData
{
    char *path;
    int fd;
};

static atomic_int is_running;
static int thread_joinable;
static JavaVM *java_vm;
static jclass tunnel_class;
static jmethodID protect_socket_method;
static pthread_t work_thread;
static pthread_mutex_t mutex;
static pthread_key_t current_jni_env;

/* The tunnel runs its tasks on hev-task coroutine stacks. ART records a
 * thread's stack bounds when it attaches and validates the stack pointer
 * against them on every transition into managed code, so calling protect()
 * straight from a task either throws StackOverflowError or segfaults. All JVM
 * work therefore happens on this dedicated thread, which never leaves its own
 * pthread stack. */
enum
{
    PROTECT_IDLE,
    PROTECT_REQUEST,
    PROTECT_DONE,
    PROTECT_QUIT,
};

static pthread_t protect_thread;
static pthread_mutex_t protect_mutex;
static pthread_cond_t protect_request_cond;
static pthread_cond_t protect_result_cond;
static int protect_state = PROTECT_IDLE;
static int protect_thread_joinable;
static int protect_fd;
static int protect_result;

static jboolean native_start_service (JNIEnv *env, jobject thiz,
                                      jstring conig_path, jint fd);
static jboolean native_stop_service (JNIEnv *env, jobject thiz);
static jboolean native_is_running (JNIEnv *env, jobject thiz);
static jlongArray native_get_stats (JNIEnv *env, jobject thiz);

static JNINativeMethod native_methods[] = {
    { "TProxyStartService", "(Ljava/lang/String;I)Z",
      (void *)native_start_service },
    { "TProxyStopService", "()Z", (void *)native_stop_service },
    { "TProxyIsRunning", "()Z", (void *)native_is_running },
    { "TProxyGetStats", "()[J", (void *)native_get_stats },
};

static void
detach_current_thread (void *env)
{
    (*java_vm)->DetachCurrentThread (java_vm);
}

jint
JNI_OnLoad (JavaVM *vm, void *reserved)
{
    JNIEnv *env = NULL;
    jclass klass;
    jint res;

    java_vm = vm;
    res = (*vm)->GetEnv (vm, (void **)&env, JNI_VERSION_1_4);
    if (res != JNI_OK)
        return JNI_ERR;

    klass = (*env)->FindClass (env, STR (PKGNAME) "/" STR (CLSNAME));
    if (!klass)
        return JNI_ERR;
    res = (*env)->RegisterNatives (env, klass, native_methods,
                                   N_ELEMENTS (native_methods));
    if (res < 0)
        return JNI_ERR;

    tunnel_class = (*env)->NewGlobalRef (env, klass);
    protect_socket_method =
        (*env)->GetStaticMethodID (env, klass, "protectSocket", "(I)Z");
    (*env)->DeleteLocalRef (env, klass);
    if (!tunnel_class || !protect_socket_method)
        return JNI_ERR;

    pthread_key_create (&current_jni_env, detach_current_thread);
    pthread_mutex_init (&mutex, NULL);
    pthread_mutex_init (&protect_mutex, NULL);
    pthread_cond_init (&protect_request_cond, NULL);
    pthread_cond_init (&protect_result_cond, NULL);

    return JNI_VERSION_1_4;
}

static void *
protect_thread_handler (void *data)
{
    JNIEnv *env = NULL;
    jint res;

    res = (*java_vm)->AttachCurrentThread (java_vm, &env, NULL);
    if (res != JNI_OK)
        return NULL;

    pthread_mutex_lock (&protect_mutex);

    for (;;) {
        jboolean result;
        int fd;

        while (protect_state != PROTECT_REQUEST &&
               protect_state != PROTECT_QUIT)
            pthread_cond_wait (&protect_request_cond, &protect_mutex);

        if (protect_state == PROTECT_QUIT)
            break;

        fd = protect_fd;
        pthread_mutex_unlock (&protect_mutex);

        result = (*env)->CallStaticBooleanMethod (env, tunnel_class,
                                                  protect_socket_method, fd);
        if ((*env)->ExceptionCheck (env)) {
            (*env)->ExceptionClear (env);
            result = JNI_FALSE;
        }

        pthread_mutex_lock (&protect_mutex);
        protect_result = (result == JNI_TRUE);
        protect_state = PROTECT_DONE;
        pthread_cond_signal (&protect_result_cond);
    }

    pthread_mutex_unlock (&protect_mutex);
    (*java_vm)->DetachCurrentThread (java_vm);

    return NULL;
}

static int
protect_thread_start (void)
{
    int res;

    protect_state = PROTECT_IDLE;
    res = pthread_create (&protect_thread, NULL, protect_thread_handler, NULL);
    if (res != 0)
        return -1;

    protect_thread_joinable = 1;

    return 0;
}

static void
protect_thread_stop (void)
{
    if (!protect_thread_joinable)
        return;

    pthread_mutex_lock (&protect_mutex);
    protect_state = PROTECT_QUIT;
    pthread_cond_signal (&protect_request_cond);
    pthread_mutex_unlock (&protect_mutex);

    pthread_join (protect_thread, NULL);
    protect_thread_joinable = 0;
    protect_state = PROTECT_IDLE;
}

int
hev_android_protect_socket (int fd)
{
    int result;

    pthread_mutex_lock (&protect_mutex);

    if (protect_state == PROTECT_QUIT) {
        pthread_mutex_unlock (&protect_mutex);
        return 0;
    }

    protect_fd = fd;
    protect_state = PROTECT_REQUEST;
    pthread_cond_signal (&protect_request_cond);

    while (protect_state == PROTECT_REQUEST)
        pthread_cond_wait (&protect_result_cond, &protect_mutex);

    result = (protect_state == PROTECT_DONE) ? protect_result : 0;
    protect_state = PROTECT_IDLE;

    pthread_mutex_unlock (&protect_mutex);

    return result;
}

static void *
thread_handler (void *data)
{
    ThreadData *tdata = data;

    hev_socks5_tunnel_main (tdata->path, tdata->fd);

    atomic_store_explicit (&is_running, 0, memory_order_release);

    free (tdata->path);
    free (tdata);

    return NULL;
}

static jboolean
native_start_service (JNIEnv *env, jobject thiz, jstring config_path, jint fd)
{
    const jbyte *bytes;
    ThreadData *tdata;
    int res;
    jboolean result = JNI_FALSE;

    pthread_mutex_lock (&mutex);

    if (atomic_load_explicit (&is_running, memory_order_acquire))
        goto exit;

    if (thread_joinable) {
        pthread_join (work_thread, NULL);
        thread_joinable = 0;
    }

    tdata = malloc (sizeof (ThreadData));
    if (!tdata)
        goto exit;
    tdata->fd = fd;

    bytes = (const jbyte *)(*env)->GetStringUTFChars (env, config_path, NULL);
    if (!bytes) {
        free (tdata);
        goto exit;
    }
    tdata->path = strdup ((const char *)bytes);
    (*env)->ReleaseStringUTFChars (env, config_path, (const char *)bytes);
    if (!tdata->path) {
        free (tdata);
        goto exit;
    }

    res = protect_thread_start ();
    if (res < 0) {
        free (tdata->path);
        free (tdata);
        goto exit;
    }

    atomic_store_explicit (&is_running, 1, memory_order_release);
    res = pthread_create (&work_thread, NULL, thread_handler, tdata);
    if (res != 0) {
        atomic_store_explicit (&is_running, 0, memory_order_release);
        protect_thread_stop ();
        free (tdata->path);
        free (tdata);
        goto exit;
    }

    thread_joinable = 1;
    result = JNI_TRUE;
exit:
    pthread_mutex_unlock (&mutex);
    return result;
}

static jboolean
native_stop_service (JNIEnv *env, jobject thiz)
{
    int res = 0;

    pthread_mutex_lock (&mutex);

    if (!thread_joinable)
        goto exit;

    if (atomic_load_explicit (&is_running, memory_order_acquire))
        hev_socks5_tunnel_quit ();
    res = pthread_join (work_thread, NULL);
    protect_thread_stop ();

    thread_joinable = 0;
    atomic_store_explicit (&is_running, 0, memory_order_release);
exit:
    pthread_mutex_unlock (&mutex);
    return res == 0 ? JNI_TRUE : JNI_FALSE;
}

static jboolean
native_is_running (JNIEnv *env, jobject thiz)
{
    return atomic_load_explicit (&is_running, memory_order_acquire) ? JNI_TRUE :
                                                                      JNI_FALSE;
}

static jlongArray
native_get_stats (JNIEnv *env, jobject thiz)
{
    size_t tx_packets, rx_packets, tx_bytes, rx_bytes;
    jlongArray res;
    jlong array[4];

    hev_socks5_tunnel_stats (&tx_packets, &tx_bytes, &rx_packets, &rx_bytes);
    array[0] = tx_packets;
    array[1] = tx_bytes;
    array[2] = rx_packets;
    array[3] = rx_bytes;

    res = (*env)->NewLongArray (env, 4);
    (*env)->SetLongArrayRegion (env, res, 0, 4, array);

    return res;
}

#endif /* ANDROID */
