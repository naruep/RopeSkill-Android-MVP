package com.ropeskill.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingCameraOverlayTest {
    @Test
    fun cameraPermissionMissing_hidesTrainingCameraOverlays() {
        assertFalse(shouldShowTrainingCameraOverlays(cameraPermissionGranted = false))
    }

    @Test
    fun cameraPermissionGranted_showsTrainingCameraOverlays() {
        assertTrue(shouldShowTrainingCameraOverlays(cameraPermissionGranted = true))
    }
}
