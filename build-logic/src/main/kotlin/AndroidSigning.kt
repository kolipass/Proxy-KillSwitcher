import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.util.Base64

private const val DEBUG_SIGNING_CONFIG_NAME = "sharedDebug"
private const val DEBUG_KEYSTORE_RELATIVE_PATH = "config/signing/debug.keystore"
private const val DEBUG_KEY_ALIAS = "androiddebugkey"
private const val DEBUG_KEYSTORE_PASSWORD = "android"
private const val DEBUG_APPLICATION_ID_SUFFIX = ".debug"

private const val RELEASE_SIGNING_CONFIG_NAME = "release"
private const val RELEASE_KEYSTORE_PATH_ENV = "RELEASE_KEYSTORE_PATH"
private const val RELEASE_KEYSTORE_BASE64_ENV = "RELEASE_KEYSTORE_BASE64"
private const val RELEASE_KEYSTORE_PASSWORD_ENV = "RELEASE_KEYSTORE_PASSWORD"
private const val RELEASE_KEY_ALIAS_ENV = "RELEASE_KEY_ALIAS"
private const val RELEASE_KEY_PASSWORD_ENV = "RELEASE_KEY_PASSWORD"
private const val DECODED_RELEASE_KEYSTORE_PATH = "signing/release.keystore"

private val RELEASE_TASKS_REQUIRING_SIGNING = listOf(
    "packageRelease",
    "bundleRelease",
    "assembleRelease",
    "installRelease",
)

internal fun Project.configureSigning(extension: ApplicationExtension) {
    val debugKeystore = sharedDebugKeystoreFile()
    val debugSigningConfig = extension.signingConfigs.create(DEBUG_SIGNING_CONFIG_NAME) {
        storeFile = debugKeystore
        storePassword = DEBUG_KEYSTORE_PASSWORD
        keyAlias = DEBUG_KEY_ALIAS
        keyPassword = DEBUG_KEYSTORE_PASSWORD
    }
    extension.buildTypes.getByName("debug") {
        signingConfig = debugSigningConfig
        applicationIdSuffix = DEBUG_APPLICATION_ID_SUFFIX
    }

    val releaseSigningConfig = createReleaseSigningConfigOrNull(extension)
    if (releaseSigningConfig == null) {
        failReleaseTasksWithoutSigningConfig()
    } else {
        extension.buildTypes.getByName("release").signingConfig = releaseSigningConfig
    }
}

private fun Project.sharedDebugKeystoreFile(): File =
    rootProject.file(DEBUG_KEYSTORE_RELATIVE_PATH).also { keystore ->
        if (!keystore.isFile) {
            throw GradleException(
                "Shared debug keystore is missing at ${keystore.path}. " +
                    "It is versioned with the repository, restore it from git instead of regenerating.",
            )
        }
    }

private fun Project.createReleaseSigningConfigOrNull(extension: ApplicationExtension): ApkSigningConfig? {
    val releaseKeystore = releaseKeystoreFileOrNull() ?: return null
    val storePasswordValue = requiredReleaseSecret(RELEASE_KEYSTORE_PASSWORD_ENV)
    val keyAliasValue = requiredReleaseSecret(RELEASE_KEY_ALIAS_ENV)
    val keyPasswordValue = requiredReleaseSecret(RELEASE_KEY_PASSWORD_ENV)
    return extension.signingConfigs.create(RELEASE_SIGNING_CONFIG_NAME) {
        storeFile = releaseKeystore
        storePassword = storePasswordValue
        keyAlias = keyAliasValue
        keyPassword = keyPasswordValue
    }
}

private fun Project.releaseKeystoreFileOrNull(): File? {
    releaseSecret(RELEASE_KEYSTORE_PATH_ENV)?.let { configuredPath ->
        val keystore = file(configuredPath)
        if (!keystore.isFile) {
            throw GradleException("$RELEASE_KEYSTORE_PATH_ENV points to a missing file: ${keystore.path}")
        }
        return keystore
    }
    val encodedKeystore = releaseSecret(RELEASE_KEYSTORE_BASE64_ENV) ?: return null
    return layout.buildDirectory.file(DECODED_RELEASE_KEYSTORE_PATH).get().asFile.also { decodedKeystore ->
        decodedKeystore.parentFile.mkdirs()
        decodedKeystore.writeBytes(Base64.getDecoder().decode(encodedKeystore))
    }
}

private fun Project.releaseSecret(environmentVariableName: String): String? =
    providers.environmentVariable(environmentVariableName).orNull?.takeIf(String::isNotBlank)

private fun Project.requiredReleaseSecret(environmentVariableName: String): String =
    releaseSecret(environmentVariableName)
        ?: throw GradleException(
            "Release keystore is configured, but $environmentVariableName is missing.",
        )

private fun Project.failReleaseTasksWithoutSigningConfig() {
    val guardedTaskPaths = RELEASE_TASKS_REQUIRING_SIGNING.mapTo(mutableSetOf()) { "$path:$it" }
    gradle.taskGraph.whenReady {
        if (allTasks.any { task -> task.path in guardedTaskPaths }) {
            throw GradleException(
                "Release signing is not configured. Provide $RELEASE_KEYSTORE_PATH_ENV or $RELEASE_KEYSTORE_BASE64_ENV " +
                    "together with $RELEASE_KEYSTORE_PASSWORD_ENV, $RELEASE_KEY_ALIAS_ENV and $RELEASE_KEY_PASSWORD_ENV. " +
                    "Falling back to the debug keystore for release builds is forbidden.",
            )
        }
    }
}
