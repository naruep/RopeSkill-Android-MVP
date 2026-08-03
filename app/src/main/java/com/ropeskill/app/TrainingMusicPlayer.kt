package com.ropeskill.app

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Foreground-only local music playback for a Training session.
 *
 * The selected content URI remains owned by its document provider. RopeSkill stores only the URI
 * permission and never copies or uploads the audio file.
 */
internal class TrainingMusicPlayer(
    context: Context,
    private val onPlaybackError: (String) -> Unit,
) {
    private val player = ExoPlayer.Builder(context)
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            repeatMode = Player.REPEAT_MODE_ONE
            addListener(
                object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        onPlaybackError(error.errorCodeName)
                    }
                },
            )
        }

    private var currentUri: String? = null
    private var configuredVolume = DEFAULT_TRAINING_MUSIC_VOLUME
    private var muted = false
    private var ducked = false

    fun configure(
        enabled: Boolean,
        uri: String,
        volume: Float,
    ) {
        configuredVolume = normalizedTrainingMusicVolume(volume)
        muted = false
        ducked = false
        applyVolume()
        player.pause()

        if (!enabled || uri.isBlank()) {
            currentUri = null
            player.clearMediaItems()
            return
        }
        if (uri == currentUri && player.mediaItemCount > 0) {
            player.seekTo(0L)
            return
        }

        currentUri = uri
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    fun play() {
        if (currentUri != null && player.mediaItemCount > 0) {
            player.play()
        }
    }

    fun pause() {
        player.pause()
    }

    fun stopAndRewind() {
        player.pause()
        if (player.mediaItemCount > 0) {
            player.seekTo(0L)
        }
    }

    fun setMuted(isMuted: Boolean) {
        muted = isMuted
        applyVolume()
    }

    fun setDucked(isDucked: Boolean) {
        ducked = isDucked
        applyVolume()
    }

    fun release() {
        player.release()
    }

    private fun applyVolume() {
        player.volume = resolvedTrainingMusicVolume(
            configuredVolume = configuredVolume,
            muted = muted,
            ducked = ducked,
        )
    }
}

internal fun resolvedTrainingMusicVolume(
    configuredVolume: Float,
    muted: Boolean,
    ducked: Boolean,
): Float = when {
    muted -> 0f
    ducked -> configuredVolume * TRAINING_MUSIC_DUCKED_MULTIPLIER
    else -> configuredVolume
}

private const val TRAINING_MUSIC_DUCKED_MULTIPLIER = 0.2f
