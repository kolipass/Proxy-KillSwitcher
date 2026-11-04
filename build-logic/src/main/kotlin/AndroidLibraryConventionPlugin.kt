import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
                apply("mobitarantino.detekt")
                apply("mobitarantino.ktlint")
                apply("mobitarantino.kover")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                testOptions.unitTests.isIncludeAndroidResources = true
            }

            tasks.withType<Test>().configureEach {
                useJUnitPlatform()
                failOnNoDiscoveredTests.set(false)
            }
        }
    }
}
