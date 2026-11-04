@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "proxykillswitcher"

include(":app")

include(":core:designsystem")
include(":core:navigation")
include(":core:common")
include(":core:di")
include(":core:featuretoggle")
include(":core:preloading")
include(":core:tunnel")

include(":feature:killswitch")
