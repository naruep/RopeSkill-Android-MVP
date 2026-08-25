package com.ropeskill.app

import java.io.File
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
    fun t758Build_installsBesideProductionAndCondoDiagnostic() {
        val buildScript = sourceFile("build.gradle.kts").readText()

        assertTrue(buildScript.contains("create(\"t758\")"))
        assertTrue(buildScript.contains("initWith(getByName(\"debug\"))"))
        assertTrue(buildScript.contains("applicationIdSuffix = \".diagnostic.t758\""))
        assertTrue(buildScript.contains("versionNameSuffix = \"-t758\""))
        assertTrue(buildScript.contains("manifestPlaceholders[\"appLabel\"] = \"RopeSkill T758\""))
        assertTrue(buildScript.contains("matchingFallbacks += listOf(\"debug\")"))
    }

    @Test
    fun t759Build_installsBesideEarlierDiagnosticPackages() {
        val buildScript = sourceFile("build.gradle.kts").readText()

        assertTrue(buildScript.contains("create(\"t759\")"))
        assertTrue(buildScript.contains("applicationIdSuffix = \".diagnostic.t759\""))
        assertTrue(buildScript.contains("versionNameSuffix = \"-t759\""))
        assertTrue(buildScript.contains("manifestPlaceholders[\"appLabel\"] = \"RopeSkill T759\""))
    }

    @Test
    fun t757LiveBuild_isExplicitlyEnabledAndInstallsBesideProduction() {
        val buildScript = sourceFile("build.gradle.kts").readText()

        assertTrue(buildScript.contains("buildConfigField(\"boolean\", \"T757_LIVE_ENABLED\", \"false\")"))
        assertTrue(buildScript.contains("create(\"t757Live\")"))
        assertTrue(buildScript.contains("applicationIdSuffix = \".diagnostic.t757live\""))
        assertTrue(buildScript.contains("versionNameSuffix = \"-t757-live\""))
        assertTrue(
            buildScript.contains(
                "manifestPlaceholders[\"appLabel\"] = \"RopeSkill T757 Live\"",
            ),
        )
        assertTrue(buildScript.contains("buildConfigField(\"boolean\", \"T757_LIVE_ENABLED\", \"true\")"))
    }

    @Test
    fun t757LandingRearmProfile_isGuardedInLiveTraining() {
        val diagnostic = sourceFile(
            "src/main/java/com/ropeskill/app/BasicBounceVideoDiagnostic.kt",
        ).readText()
        val training = sourceFile(
            "src/main/java/com/ropeskill/app/TrainingViewModel.kt",
        ).readText()

        assertTrue(diagnostic.contains("T757DetectorProfiles.SHADOW_ONLY"))
        assertTrue(training.contains("trainingBasicBounceThresholds(BuildConfig.T757_LIVE_ENABLED)"))
        assertTrue(training.contains("if (t757LiveEnabled)"))
        assertTrue(training.contains("T757DetectorProfiles.SHADOW_ONLY"))
        assertTrue(training.contains("T738DetectorProfiles.PRODUCTION"))
    }

    private fun sourceFile(relativePath: String): File =
        sequenceOf(File(relativePath), File("app/$relativePath"))
            .first(File::isFile)
}
