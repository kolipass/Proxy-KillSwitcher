plugins {
    id("mobitarantino.android.library")
    id("mobitarantino.android.compose")
}

android {
    namespace = "mobi.tarantino.proxykillswitcher.core.navigation"
}

dependencies {
    api(libs.findLibrary("androidx-navigation-compose").get())
}
