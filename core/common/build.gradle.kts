plugins {
    id("mobitarantino.android.library")
}

android {
    namespace = "mobi.tarantino.proxykillswitcher.core.common"
}

dependencies {
    api(libs.findLibrary("orbit-core").get())
    implementation(libs.findLibrary("kotlinx-coroutines-core").get())
}
