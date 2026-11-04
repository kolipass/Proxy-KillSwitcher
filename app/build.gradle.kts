plugins {
    id("mobitarantino.android.application")
    id("mobitarantino.android.compose")
    alias(libs.plugins.metro)
}

android {
    namespace = "mobi.tarantino.proxykillswitcher"

    val releaseVersionCode =
        providers
            .environmentVariable("VERSION_CODE")
            .orElse(providers.gradleProperty("versionCode"))
            .map { it.toInt() }
            .getOrElse(1)

    val releaseVersionName =
        providers
            .environmentVariable("VERSION_NAME")
            .orElse(providers.gradleProperty("versionName"))
            .getOrElse("0.1.0")

    defaultConfig {
        applicationId = "mobi.tarantino.proxykillswitcher"
        versionCode = releaseVersionCode
        versionName = releaseVersionName
    }

    signingConfigs {
        val defaultDebugKeystore = rootProject.file(".gradle/debug.keystore")
        if (defaultDebugKeystore.exists()) {
            getByName("debug") {
                storeFile = defaultDebugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }

        val keystorePath =
            providers
                .environmentVariable("RELEASE_KEYSTORE_PATH")
                .orElse(providers.gradleProperty("releaseKeystorePath"))
                .orNull
        val keystorePassword =
            providers
                .environmentVariable("RELEASE_KEYSTORE_PASSWORD")
                .orElse(providers.gradleProperty("releaseKeystorePassword"))
                .orNull
        val keyAlias =
            providers
                .environmentVariable("RELEASE_KEY_ALIAS")
                .orElse(providers.gradleProperty("releaseKeyAlias"))
                .orNull
        val keyPassword =
            providers
                .environmentVariable("RELEASE_KEY_PASSWORD")
                .orElse(providers.gradleProperty("releaseKeyPassword"))
                .orNull

        val hasReleaseSigning =
            keystorePath != null &&
                keystorePassword != null &&
                keyAlias != null &&
                keyPassword != null &&
                file(keystorePath).exists()

        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val releaseSigning = signingConfigs.findByName("release")
            signingConfig = releaseSigning ?: signingConfigs.getByName("debug")
        }
        debug {
            isDebuggable = true
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:common"))
    implementation(project(":core:di"))
    implementation(project(":core:featuretoggle"))
    implementation(project(":core:preloading"))
    implementation(project(":core:tunnel"))
    implementation(project(":feature:killswitch"))

    implementation(libs.findLibrary("androidx-core-ktx").get())
    implementation(libs.findLibrary("androidx-activity-compose").get())
    implementation(libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
}
