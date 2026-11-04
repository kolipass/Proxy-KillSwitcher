plugins {
    id("mobitarantino.android.library")
    alias(libs.plugins.metro)
}

android {
    namespace = "mobi.tarantino.proxykillswitcher.core.preloading"
}

dependencies {
    implementation(project(":core:di"))
    api(libs.findLibrary("kotlinx-coroutines-core").get())
}
