package com.ropeskill.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SpeedAudioCueSchedulerTest {
    @Test
    fun manifestDeclaresTextToSpeechServiceQuery() {
        val manifest = sourceFile("src/main/AndroidManifest.xml").readText()

        assertEquals(
            1,
            Regex(Regex.escape("android.intent.action.TTS_SERVICE")).findAll(manifest).count(),
        )
    }

    @Test
    fun emitsExpectedSpeed30Schedule() {
        val scheduler = SpeedAudioCueScheduler()

        assertEquals(emptyList<SpeedAudioCue>(), scheduler.advanceTo(9_999L))
        assertEquals(listOf(SpeedAudioCue.TEN_SECONDS), scheduler.advanceTo(10_001L))
        assertEquals(listOf(SpeedAudioCue.TWENTY_SECONDS), scheduler.advanceTo(20_000L))
        assertEquals(
            listOf(
                SpeedAudioCue.FIVE_SECONDS_LEFT,
                SpeedAudioCue.FOUR_SECONDS_LEFT,
                SpeedAudioCue.THREE_SECONDS_LEFT,
                SpeedAudioCue.TWO_SECONDS_LEFT,
                SpeedAudioCue.ONE_SECOND_LEFT,
                SpeedAudioCue.COMPLETE,
            ),
            scheduler.advanceTo(30_000L),
        )
    }

    @Test
    fun eachCueIsEmittedOnlyOnce() {
        val scheduler = SpeedAudioCueScheduler()

        assertEquals(listOf(SpeedAudioCue.TEN_SECONDS), scheduler.advanceTo(10_000L))
        assertEquals(emptyList<SpeedAudioCue>(), scheduler.advanceTo(10_000L))
        assertEquals(emptyList<SpeedAudioCue>(), scheduler.advanceTo(19_999L))
    }

    @Test
    fun crossingMultipleThresholdsDoesNotLoseCues() {
        val scheduler = SpeedAudioCueScheduler()

        assertEquals(
            listOf(SpeedAudioCue.TEN_SECONDS, SpeedAudioCue.TWENTY_SECONDS),
            scheduler.advanceTo(24_999L),
        )
    }

    @Test
    fun resetAllowsNewSessionToEmitScheduleAgain() {
        val scheduler = SpeedAudioCueScheduler()
        scheduler.advanceTo(30_000L)

        scheduler.reset()

        assertEquals(listOf(SpeedAudioCue.TEN_SECONDS), scheduler.advanceTo(10_000L))
    }

    @Test
    fun rejectsBackwardTimeToPreventAccidentalReplay() {
        val scheduler = SpeedAudioCueScheduler()
        scheduler.advanceTo(10_000L)

        assertThrows(IllegalArgumentException::class.java) {
            scheduler.advanceTo(9_999L)
        }
    }

    private fun sourceFile(relativePath: String): File {
        val candidates = listOf(File(relativePath), File("app", relativePath))
        return candidates.firstOrNull(File::isFile)
            ?: error("Cannot find source file $relativePath from ${File(".").absolutePath}")
    }
}
