import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention for a `:feature:*` module: Android library + Compose + Metro DI + Orbit MVI,
 * wired to the shared `core` modules every feature needs. Feature-specific dependencies still
 * go in that feature's own `build.gradle.kts`.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("mobitarantino.android.library")
                apply("mobitarantino.android.compose")
                apply("org.jetbrains.kotlin.plugin.serialization")
                apply("dev.zacsweers.metro")
            }

            dependencies {
                add("implementation", project(":core:di"))
                add("implementation", project(":core:common"))
                add("implementation", project(":core:navigation"))
                add("implementation", project(":core:featuretoggle"))

                add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())
                add("implementation", libs.findLibrary("kotlinx-serialization-json").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-ktx").get())
                add("implementation", libs.findLibrary("orbit-viewmodel").get())
                add("implementation", libs.findLibrary("orbit-compose").get())

                add("testImplementation", libs.findLibrary("junit").get())
                add("testImplementation", libs.findLibrary("junit-jupiter-api").get())
                add("testImplementation", libs.findLibrary("junit-jupiter-params").get())
                add("testRuntimeOnly", libs.findLibrary("junit-jupiter-engine").get())
                add("testRuntimeOnly", libs.findLibrary("junit-vintage-engine").get())
                add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
                add("testImplementation", libs.findLibrary("mockk").get())
                add("testImplementation", libs.findLibrary("turbine").get())
                add("testImplementation", libs.findLibrary("orbit-test").get())
                add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            }
        }
    }
}
