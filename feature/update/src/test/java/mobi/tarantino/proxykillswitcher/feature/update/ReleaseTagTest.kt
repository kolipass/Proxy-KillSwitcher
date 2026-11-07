package mobi.tarantino.proxykillswitcher.feature.update

import mobi.tarantino.proxykillswitcher.feature.update.domain.ReleaseTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReleaseTagTest {
    @Test
    fun `When tag starts with v, Then version code is derived from major minor patch`() {
        val actual = ReleaseTag.parse("v1.2.3")

        val expected = ReleaseTag.ReleaseVersion(versionCode = 10203, versionName = "1.2.3")
        assertEquals(expected, actual)
    }

    @Test
    fun `When tag has no v prefix, Then version is still parsed`() {
        val actual = ReleaseTag.parse("0.4.12")

        val expected = ReleaseTag.ReleaseVersion(versionCode = 412, versionName = "0.4.12")
        assertEquals(expected, actual)
    }

    @Test
    fun `When tag is not a semantic version, Then version is null`() {
        assertNull(ReleaseTag.parse("release-1.2.3"))
    }

    @Test
    fun `When minor or patch has three digits, Then version is null`() {
        assertNull(ReleaseTag.parse("v1.100.0"))
        assertNull(ReleaseTag.parse("v1.2.100"))
    }
}
