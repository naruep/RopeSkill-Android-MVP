package com.ropeskill.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyDisclosureTest {
    @Test
    fun publicPolicyUrl_isHttpsAndPointsToHtmlPage() {
        assertTrue(ROPESKILL_PRIVACY_POLICY_URL.startsWith("https://"))
        assertTrue(ROPESKILL_PRIVACY_POLICY_URL.endsWith("/privacy-policy.html"))
    }

    @Test
    fun inAppSummary_coversLocalDataDeletionBackupAndContact() {
        val summary = ROPESKILL_PRIVACY_SUMMARY.lowercase()

        listOf(
            "camera",
            "pose landmarks",
            "record this workout",
            "movies/ropeskill",
            "microphone",
            "share it yourself",
            "gallery",
            "nickname",
            "training history",
            "music",
            "backup",
            "delete",
            "uninstall",
            "naruep jukping",
            "naruep.j@gmail.com",
        ).forEach { disclosure ->
            assertTrue("Missing privacy disclosure: $disclosure", disclosure in summary)
        }
    }

    @Test
    fun manifest_doesNotRequestInternetPermission() {
        val manifest = sourceFile("src/main/AndroidManifest.xml").readText()

        assertFalse("Privacy baseline requires no INTERNET permission", "permission.INTERNET" in manifest)
    }

    private fun sourceFile(relativePath: String): File {
        val candidates =
            listOf(
                File(relativePath),
                File("app", relativePath),
            )

        return candidates.firstOrNull(File::isFile)
            ?: error("Cannot find source file $relativePath from ${File(".").absolutePath}")
    }
}
