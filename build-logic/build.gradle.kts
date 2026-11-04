plugins {
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    compileOnly(buildLogicLibs.android.application.gradlePlugin)
    compileOnly(buildLogicLibs.android.library.gradlePlugin)
    compileOnly(buildLogicLibs.kotlin.android.gradlePlugin)
    compileOnly(buildLogicLibs.kotlin.compose.gradlePlugin)
    compileOnly(buildLogicLibs.kotlin.serialization.gradlePlugin)
    compileOnly(buildLogicLibs.metro.gradlePlugin)
    compileOnly(buildLogicLibs.detekt.gradlePlugin)
    compileOnly(buildLogicLibs.ktlint.gradlePlugin)
    compileOnly(buildLogicLibs.kover.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "mobitarantino.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "mobitarantino.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "mobitarantino.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidCompose") {
            id = "mobitarantino.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("detekt") {
            id = "mobitarantino.detekt"
            implementationClass = "DetektConventionPlugin"
        }
        register("ktlint") {
            id = "mobitarantino.ktlint"
            implementationClass = "KtlintConventionPlugin"
        }
        register("kover") {
            id = "mobitarantino.kover"
            implementationClass = "KoverConventionPlugin"
        }
    }
}
