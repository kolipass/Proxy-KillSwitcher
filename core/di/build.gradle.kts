plugins {
    id("mobitarantino.android.library")
    id("mobitarantino.android.compose")
    alias(libs.plugins.metro)
}

android {
    namespace = "mobi.tarantino.proxykillswitcher.core.di"
}

dependencies {
    implementation(libs.findLibrary("androidx-lifecycle-viewmodel-ktx").get())
    implementation(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
    api(libs.findLibrary("kotlinx-coroutines-core").get())
}
