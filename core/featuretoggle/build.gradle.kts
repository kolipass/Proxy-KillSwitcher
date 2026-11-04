plugins {
    id("mobitarantino.android.library")
    alias(libs.plugins.metro)
}

android {
    namespace = "mobi.tarantino.proxykillswitcher.core.featuretoggle"
}

dependencies {
    implementation(project(":core:di"))
    implementation(libs.findLibrary("kotlinx-coroutines-core").get())
}
