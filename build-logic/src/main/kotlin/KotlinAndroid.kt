import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** Compile SDK / Java / Kotlin target shared by every Android module, application or library. */
internal fun Project.configureKotlinAndroid(extension: CommonExtension<*, *, *, *, *, *>) {
    val libs = libs

    extension.apply {
        compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()

        defaultConfig {
            minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(
                org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(
                    libs.findVersion("jvmTarget").get().requiredVersion,
                ),
            )
        }
    }

    val localGradleHome = rootProject.layout.projectDirectory.dir(".gradle-home").asFile
    if (localGradleHome.exists()) {
        val localMavenRepo = java.io.File(localGradleHome, ".m2/repository")
        localMavenRepo.mkdirs()
        tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
            systemProperty("user.home", localGradleHome.absolutePath)
            systemProperty("maven.repo.local", localMavenRepo.absolutePath)
        }
    }
}
