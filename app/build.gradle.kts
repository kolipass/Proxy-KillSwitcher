plugins {
    id("mobitarantino.android.application")
    id("mobitarantino.android.compose")
    alias(libs.plugins.metro)
}

val appVersionCode = providers.gradleProperty("appVersionCode").orNull?.toIntOrNull() ?: 1
val appVersionName = providers.gradleProperty("appVersionName").orNull?.takeIf(String::isNotBlank) ?: "1.0"

android {
    namespace = "mobi.tarantino.proxykillswitcher"

    defaultConfig {
        applicationId = "mobi.tarantino.proxykillswitcher"
        versionCode = appVersionCode
        versionName = appVersionName
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
    implementation(project(":feature:update"))

    implementation(libs.findLibrary("androidx-core-ktx").get())
    implementation(libs.findLibrary("androidx-activity-compose").get())
    implementation(libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
}
