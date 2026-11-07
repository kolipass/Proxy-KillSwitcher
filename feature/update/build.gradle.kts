plugins {
    id("mobitarantino.android.feature")
}

android {
    namespace = "mobi.tarantino.proxykillswitcher.feature.update"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(libs.findLibrary("androidx-core-ktx").get())
}
