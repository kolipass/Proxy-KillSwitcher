# hev-socks5-tunnel JNI bindings
# Native functions and protectSocket callback invoked from JNI in hev-jni.c
-keepclasseswithmembers class mobi.tarantino.proxykillswitcher.core.tunnel.Socks5Tunnel* {
    native <methods>;
    private static boolean protectSocket(int);
}

-keep class mobi.tarantino.proxykillswitcher.core.tunnel.Socks5Tunnel { *; }
-keep class mobi.tarantino.proxykillswitcher.core.tunnel.Socks5Tunnel$Companion { *; }
