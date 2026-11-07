package mobi.tarantino.proxykillswitcher.feature.update.domain

object ReleaseTag {
    private val TAG_PATTERN = Regex(pattern = """^v?(\d+)\.(\d+)\.(\d+)$""")

    fun parse(tagName: String): ReleaseVersion? {
        val matchResult = TAG_PATTERN.matchEntire(tagName) ?: return null
        val (majorText, minorText, patchText) = matchResult.destructured
        val major = majorText.toIntOrNull() ?: return null
        val minor = minorText.toIntOrNull() ?: return null
        val patch = patchText.toIntOrNull() ?: return null
        if (minor >= POSITION_BASE || patch >= POSITION_BASE) {
            return null
        }
        return ReleaseVersion(
            versionCode = major * MAJOR_WEIGHT + minor * MINOR_WEIGHT + patch,
            versionName = "$major.$minor.$patch",
        )
    }

    data class ReleaseVersion(
        val versionCode: Int,
        val versionName: String,
    )

    private const val POSITION_BASE = 100
    private const val MAJOR_WEIGHT = POSITION_BASE * POSITION_BASE
    private const val MINOR_WEIGHT = POSITION_BASE
}
