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
    fun startDialogUsesOneStartActionWithRecordingOffByDefault() {
        val screens = sourceFile("src/main/java/com/ropeskill/app/Screens.kt").readText()

        assertTrue("Start workout" in screens)
        assertTrue("Recording is optional. Videos stay on this device and are never uploaded." in screens)
        assertTrue("var recordSpeedWorkout by remember { mutableStateOf(false) }" in screens)
        assertTrue("Record this workout" in screens)
        assertTrue("Text(\"START WORKOUT\")" in screens)
        assertTrue("role = Role.Switch" in screens)
        assertTrue("onCheckedChange = null" in screens)
        assertTrue("if (recordSpeedWorkout && recordingSupported)" in screens)
        assertTrue("onRecordAndStartSpeed30()" in screens)
        assertTrue("onStartSpeed30()" in screens)
        assertTrue("RECORDING DETAILS" in screens)
        assertTrue("About screen recording" in screens)
        listOf(
            "camera preview, and diagnostics",
            "Does not record microphone or internal audio",
            "Saves locally to Movies/RopeSkill",
            "Nothing is uploaded or shared automatically",
            "Android asks for screen-capture permission each time",
            "View, delete, or share the video later from Gallery",
        ).forEach { disclosure ->
            assertTrue("Missing recording disclosure: $disclosure", disclosure in screens)
        }
        assertFalse("RECORD & START" in screens)
        assertFalse("Text(\"START\")" in screens)
        assertFalse("START WITHOUT RECORDING" in screens)
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
