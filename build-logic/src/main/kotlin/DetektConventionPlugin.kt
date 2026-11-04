import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.gitlab.arturbosch.detekt")

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig = true
                allRules = false
                parallel = true
                config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
            }

            dependencies {
                add("detektPlugins", libs.findLibrary("detekt-formatting").get())
            }

            tasks.withType<Detekt>().configureEach {
                jvmTarget = libs.findVersion("jvmTarget").get().requiredVersion
            }
        }
    }
}
