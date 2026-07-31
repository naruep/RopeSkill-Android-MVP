package com.ropeskill.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseSigningBoundaryTest {
    @Test
    fun releaseSigning_usesExternalPropertiesAndRejectsRepositorySecrets() {
        val buildScript = projectFile("app/build.gradle.kts").readText()

        assertTrue(buildScript.contains("ROPESKILL_SIGNING_PROPERTIES"))
        assertTrue(buildScript.contains("must be stored outside the repository"))
        assertFalse(buildScript.contains("storePassword = \""))
        assertFalse(buildScript.contains("keyPassword = \""))
    }

    @Test
    fun gitIgnore_excludesKeystoresAndLocalSigningProperties() {
        val gitIgnore = projectFile(".gitignore").readText()

        assertTrue(gitIgnore.contains("*.jks"))
        assertTrue(gitIgnore.contains("*.keystore"))
        assertTrue(gitIgnore.contains("*.p12"))
        assertTrue(gitIgnore.contains("*.pfx"))
        assertTrue(gitIgnore.contains("keystore.properties"))
        assertTrue(gitIgnore.contains("release-signing.properties"))
    }

    @Test
    fun signingExample_containsPlaceholdersOnly() {
        val example = projectFile("config/release-signing.properties.example").readText()

        assertTrue(example.contains("STORE_PASSWORD_HERE"))
        assertTrue(example.contains("KEY_PASSWORD_HERE"))
        assertFalse(example.contains("storePassword=password"))
        assertFalse(example.contains("keyPassword=password"))
    }

    private fun projectFile(relativePath: String): File {
        val candidates =
            listOf(
                File(relativePath),
                File("..", relativePath),
                File("../..", relativePath),
            )

        return candidates.firstOrNull(File::isFile)
            ?: error("Cannot find $relativePath from ${File(".").absolutePath}")
    }
}
