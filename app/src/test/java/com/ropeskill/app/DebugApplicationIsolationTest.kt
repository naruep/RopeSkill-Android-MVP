package com.ropeskill.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugApplicationIsolationTest {
    @Test
    fun debugBuild_installsBesideProductionWithoutChangingReleaseId() {
        val buildScript = sourceFile("build.gradle.kts").readText()
        val manifest = sourceFile("src/main/AndroidManifest.xml").readText()

        assertTrue(buildScript.contains("applicationId = \"com.ropeskill.app\""))
        assertTrue(buildScript.contains("applicationIdSuffix = \".diagnostic\""))
        assertTrue(buildScript.contains("manifestPlaceholders[\"appLabel\"] = \"RopeSkill Diagnostic\""))
        assertTrue(manifest.contains("android:label=\"${'$'}{appLabel}\""))
    }

    @Test
    fun t757LandingRearmProfile_isReferencedOnlyByOfflineVideoDiagnostic() {
        val diagnostic = sourceFile(
            "src/main/java/com/ropeskill/app/BasicBounceVideoDiagnostic.kt",
        ).readText()
        val training = sourceFile(
            "src/main/java/com/ropeskill/app/TrainingViewModel.kt",
        ).readText()

        assertTrue(diagnostic.contains("T757DetectorProfiles.SHADOW_ONLY"))
        assertFalse(training.contains("T757DetectorProfiles"))
    }

    private fun sourceFile(relativePath: String): File =
        sequenceOf(File(relativePath), File("app/$relativePath"))
            .first(File::isFile)
}
