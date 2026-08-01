package com.ropeskill.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ScreenRecordingState {
    data object Idle : ScreenRecordingState

    data object Starting : ScreenRecordingState

    data class Recording(
        val startedAtElapsedRealtime: Long,
    ) : ScreenRecordingState

    data object Stopping : ScreenRecordingState

    data class Saved(
        val uri: Uri,
    ) : ScreenRecordingState

    data class Error(
        val message: String,
    ) : ScreenRecordingState
}

/** Process-local recording state. Video content remains on-device in MediaStore. */
object ScreenRecordingController {
    private val _state = MutableStateFlow<ScreenRecordingState>(ScreenRecordingState.Idle)
    val state: StateFlow<ScreenRecordingState> = _state.asStateFlow()

    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun start(
        context: Context,
        resultCode: Int,
        resultData: Intent,
    ) {
        if (!isSupported) {
            markError("Integrated recording requires Android 10 or newer.")
            return
        }
        _state.value = ScreenRecordingState.Starting
        runCatching {
            val intent = ScreenRecordingService.startIntent(context, resultCode, resultData)
            ContextCompat.startForegroundService(context, intent)
        }.onFailure { error ->
            markError("Screen recording could not start (${error.javaClass.simpleName}).")
        }
    }

    fun stop(context: Context) {
        if (
            _state.value !is ScreenRecordingState.Starting &&
            _state.value !is ScreenRecordingState.Recording
        ) {
            return
        }
        _state.value = ScreenRecordingState.Stopping
        runCatching {
            context.startService(ScreenRecordingService.stopIntent(context))
        }.onFailure { error ->
            markError("Screen recording could not stop (${error.javaClass.simpleName}).")
        }
    }

    fun dismissMessage() {
        if (_state.value is ScreenRecordingState.Error) {
            _state.value = ScreenRecordingState.Idle
        }
    }

    internal fun markRecording() {
        _state.value = ScreenRecordingState.Recording(SystemClock.elapsedRealtime())
    }

    internal fun markSaved(uri: Uri) {
        _state.value = ScreenRecordingState.Saved(uri)
    }

    internal fun markError(message: String) {
        _state.value = ScreenRecordingState.Error(message)
    }
}

internal data class ScreenRecordingSize(
    val width: Int,
    val height: Int,
)

internal fun calculateRecordingSize(
    sourceWidth: Int,
    sourceHeight: Int,
    maximumLongEdge: Int = 1_920,
): ScreenRecordingSize {
    require(sourceWidth > 0 && sourceHeight > 0)
    require(maximumLongEdge > 0)
    val longEdge = maxOf(sourceWidth, sourceHeight)
    val scale = minOf(1.0, maximumLongEdge.toDouble() / longEdge.toDouble())
    val width = ((sourceWidth * scale).toInt().coerceAtLeast(2) / 2) * 2
    val height = ((sourceHeight * scale).toInt().coerceAtLeast(2) / 2) * 2
    return ScreenRecordingSize(width = width, height = height)
}
