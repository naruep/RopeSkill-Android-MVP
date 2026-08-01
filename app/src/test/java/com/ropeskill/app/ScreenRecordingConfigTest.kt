package com.ropeskill.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenRecordingConfigTest {
    @Test
    fun manifestDeclaresMediaProjectionForegroundServiceBoundary() {
        val manifest = sourceFile("src/main/AndroidManifest.xml").readText()

        assertTrue("permission.FOREGROUND_SERVICE\"" in manifest)
        assertTrue("permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" in manifest)
        assertTrue("android:foregroundServiceType=\"mediaProjection\"" in manifest)
        assertTrue("android:name=\".ScreenRecordingService\"" in manifest)
    }

    @Test
    fun android14ConsentTargetsDefaultDisplayInsteadOfOpeningAppPicker() {
        val mainActivity = sourceFile("src/main/java/com/ropeskill/app/MainActivity.kt").readText()

        assertTrue("Build.VERSION_CODES.UPSIDE_DOWN_CAKE" in mainActivity)
        assertTrue("MediaProjectionConfig.createConfigForDefaultDisplay()" in mainActivity)
        assertTrue("projectionManager.createScreenCaptureIntent()" in mainActivity)
    }

    @Test
    fun startDialogKeepsRecordingDisclosureUserInitiated() {
        val screens = sourceFile("src/main/java/com/ropeskill/app/Screens.kt").readText()

        assertTrue("Android will ask permission to capture the RopeSkill screen." in screens)
        assertTrue("Nothing is uploaded." in screens)
        assertTrue("RECORDING DETAILS" in screens)
        assertTrue("About screen recording" in screens)
    }

    @Test
    fun resultKeepsViewActionWithoutInAppSharing() {
        val screens = sourceFile("src/main/java/com/ropeskill/app/Screens.kt").readText()
        val mainActivity = sourceFile("src/main/java/com/ropeskill/app/MainActivity.kt").readText()

        assertTrue("VIEW VIDEO" in screens)
        assertFalse("SHARE VIDEO" in screens)
        assertFalse("onShareVideo" in screens)
        assertFalse("shareRecording" in mainActivity)
        assertFalse("Intent.ACTION_SEND" in mainActivity)
    }

    @Test
    fun smallerDisplay_keepsNativeEvenDimensions() {
        assertEquals(
            ScreenRecordingSize(width = 1_080, height = 1_920),
            calculateRecordingSize(sourceWidth = 1_080, sourceHeight = 1_920),
        )
    }

    @Test
    fun largePortraitDisplay_isBoundedAndPreservesPortraitShape() {
        assertEquals(
            ScreenRecordingSize(width = 894, height = 1_920),
            calculateRecordingSize(sourceWidth = 1_440, sourceHeight = 3_088),
        )
    }

    @Test
    fun oddDimensions_areRoundedDownForVideoEncoder() {
        assertEquals(
            ScreenRecordingSize(width = 1_078, height = 1_918),
            calculateRecordingSize(sourceWidth = 1_079, sourceHeight = 1_919),
        )
    }

    private fun sourceFile(relativePath: String): File {
        val candidates = listOf(File(relativePath), File("app", relativePath))
        return candidates.firstOrNull(File::isFile)
            ?: error("Cannot find source file $relativePath from ${File(".").absolutePath}")
    }
}
