package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsPreferencesTest {
    @Test
    fun nickname_isTrimmedAndLimitedToThirtyCharacters() {
        val nickname = "  123456789012345678901234567890EXTRA  "

        assertEquals("123456789012345678901234567890", normalizedNickname(nickname))
    }

    @Test
    fun countdown_acceptsSupportedValuesAndFallsBackToFive() {
        assertEquals(3, normalizedCountdown(3))
        assertEquals(5, normalizedCountdown(5))
        assertEquals(10, normalizedCountdown(10))
        assertEquals(5, normalizedCountdown(4))
        assertEquals(5, normalizedCountdown(null))
    }

    @Test
    fun units_acceptKnownValueAndFallBackToMetric() {
        assertEquals(
            MeasurementUnits.IMPERIAL,
            normalizedMeasurementUnits(MeasurementUnits.IMPERIAL.name),
        )
        assertEquals(MeasurementUnits.METRIC, normalizedMeasurementUnits("UNKNOWN"))
        assertEquals(MeasurementUnits.METRIC, normalizedMeasurementUnits(null))
    }

    @Test
    fun theme_acceptsKnownValueAndFallsBackToDark() {
        assertEquals(AppTheme.LIGHT, normalizedAppTheme(AppTheme.LIGHT.name))
        assertEquals(AppTheme.DARK, normalizedAppTheme("UNKNOWN"))
        assertEquals(AppTheme.DARK, normalizedAppTheme(null))
    }

    @Test
    fun trainingMusicVolume_acceptsValidValueAndFallsBackToSeventyPercent() {
        assertEquals(0.35f, normalizedTrainingMusicVolume(0.35f), 0f)
        assertEquals(
            DEFAULT_TRAINING_MUSIC_VOLUME,
            normalizedTrainingMusicVolume(null),
            0f,
        )
    }

    @Test
    fun trainingMusicVolume_isClampedToPlayerRange() {
        assertEquals(0f, normalizedTrainingMusicVolume(-0.25f), 0f)
        assertEquals(1f, normalizedTrainingMusicVolume(1.25f), 0f)
    }
}
