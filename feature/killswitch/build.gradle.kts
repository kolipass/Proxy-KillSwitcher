plugins {
    id("mobitarantino.android.feature")
}

android {
    namespace = "mobi.tarantino.proxykillswitcher.feature.killswitch"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:tunnel"))
    implementation(libs.findLibrary("androidx-activity-compose").get())
    implementation(libs.findLibrary("compose-material-icons-extended").get())
    testImplementation(libs.findLibrary("robolectric").get())
}
