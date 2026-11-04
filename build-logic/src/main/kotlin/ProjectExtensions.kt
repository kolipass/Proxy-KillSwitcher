import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** Access to the root `gradle/libs.versions.toml` catalog from a convention plugin. */
val Project.libs
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
