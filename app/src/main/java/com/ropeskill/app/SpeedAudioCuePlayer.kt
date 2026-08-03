package com.ropeskill.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Plays foreground-only Speed 30 time cues without retaining microphone or camera data. */
internal class SpeedAudioCuePlayer(
    context: Context,
    private val onCueActivityChanged: (Boolean) -> Unit,
) : TextToSpeech.OnInitListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val cueAudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val audioFocusRequest = AudioFocusRequest.Builder(
        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
    ).setAudioAttributes(cueAudioAttributes).build()
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, CUE_VOLUME_PERCENT)
    private val textToSpeech = TextToSpeech(context.applicationContext, this)

    private var enabled = true
    private var speechReady = false
    private var playbackGeneration = 0
    private var activeUtteranceId: String? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            speechReady = false
            return
        }
        val languageResult = textToSpeech.setLanguage(Locale.US)
        speechReady = languageResult != TextToSpeech.LANG_MISSING_DATA &&
            languageResult != TextToSpeech.LANG_NOT_SUPPORTED
        textToSpeech.setSpeechRate(SPEECH_RATE)
        textToSpeech.setAudioAttributes(cueAudioAttributes)
        textToSpeech.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {
                    if (utteranceId == activeUtteranceId) beginCueActivity()
                }

                override fun onDone(utteranceId: String) {
                    completeSpeechCue(utteranceId)
                }

                @Suppress("DEPRECATION")
                override fun onError(utteranceId: String) {
                    completeSpeechCue(utteranceId)
                }

                override fun onError(utteranceId: String, errorCode: Int) {
                    completeSpeechCue(utteranceId)
                }
            },
        )
    }

    fun configure(isEnabled: Boolean) {
        enabled = isEnabled
        if (!enabled) stop()
    }

    fun play(cue: SpeedAudioCue) {
        if (!enabled) return
        playbackGeneration += 1
        val generation = playbackGeneration

        if (cue == SpeedAudioCue.COMPLETE) {
            activeUtteranceId = null
            textToSpeech.stop()
            beginCueActivity()
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, COMPLETION_TONE_MILLIS.toInt())
            mainHandler.postDelayed(
                {
                    if (generation == playbackGeneration) endCueActivity()
                },
                COMPLETION_TONE_MILLIS,
            )
            return
        }

        val spokenText = cue.spokenText ?: return
        if (!speechReady) {
            playFallbackTone(generation)
            return
        }

        val utteranceId = "speed-${cue.name}-$generation"
        activeUtteranceId = utteranceId
        val result = textToSpeech.speak(
            spokenText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId,
        )
        if (result == TextToSpeech.ERROR) {
            activeUtteranceId = null
            playFallbackTone(generation)
        }
    }

    fun stop() {
        playbackGeneration += 1
        activeUtteranceId = null
        mainHandler.removeCallbacksAndMessages(null)
        textToSpeech.stop()
        toneGenerator.stopTone()
        endCueActivity()
    }

    fun release() {
        stop()
        textToSpeech.shutdown()
        toneGenerator.release()
    }

    private fun playFallbackTone(generation: Int) {
        beginCueActivity()
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, FALLBACK_TONE_MILLIS.toInt())
        mainHandler.postDelayed(
            {
                if (generation == playbackGeneration) endCueActivity()
            },
            FALLBACK_TONE_MILLIS,
        )
    }

    private fun completeSpeechCue(utteranceId: String) {
        if (utteranceId != activeUtteranceId) return
        activeUtteranceId = null
        endCueActivity()
    }

    private fun beginCueActivity() {
        audioManager.requestAudioFocus(audioFocusRequest)
        mainHandler.post { onCueActivityChanged(true) }
    }

    private fun endCueActivity() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        mainHandler.post { onCueActivityChanged(false) }
    }

    private companion object {
        const val CUE_VOLUME_PERCENT = 85
        const val SPEECH_RATE = 1.0f
        const val FALLBACK_TONE_MILLIS = 180L
        const val COMPLETION_TONE_MILLIS = 1_000L
    }
}
