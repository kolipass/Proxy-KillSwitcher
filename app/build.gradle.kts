plugins {
    id("mobitarantino.android.application")
    id("mobitarantino.android.compose")
    alias(libs.plugins.metro)
}

android {
    namespace = "mobi.tarantino.proxykillswitcher"

    defaultConfig {
        applicationId = "mobi.tarantino.proxykillswitcher"
        versionCode = 1
        versionName = "1.0"
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
