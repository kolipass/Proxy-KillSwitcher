import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class KoverConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            extensions.configure<KoverProjectExtension> {
                reports {
                    filters {
                        excludes {
                            annotatedBy("*Preview*", "*Generated*")
                            classes(
                                "*.di.*",
                                "*Module",
                                "*Module\$*",
                                "*_Factory",
                                "*ScreenKt",
                                "*ScreenKt\$*",
                                "*.BuildConfig",
                            )
                        }
                    }
                }
            }
        }
    }
}
