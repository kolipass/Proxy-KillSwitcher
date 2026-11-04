dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        // Named differently from the root build's auto-detected "libs" catalog: Gradle
        // generates both as a class named `LibrariesFor<CatalogName>`, and calling this one
        // "libs" too collided with the root catalog's accessors in any project applying a
        // build-logic convention plugin.
        create("buildLogicLibs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
