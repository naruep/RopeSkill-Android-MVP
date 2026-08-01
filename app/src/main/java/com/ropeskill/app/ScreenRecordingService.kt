package com.ropeskill.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScreenRecordingService : Service() {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var outputDescriptor: ParcelFileDescriptor? = null
    private var outputUri: Uri? = null
    private var recordingStarted = false
    private var finishing = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            finishRecording(projectionAlreadyStopped = true)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(intent)
            ACTION_STOP -> finishRecording(projectionAlreadyStopped = false)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        finishRecording(projectionAlreadyStopped = false)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        if (!finishing && (recordingStarted || outputUri != null)) {
            finishRecording(projectionAlreadyStopped = false)
        }
        super.onDestroy()
    }

    private fun startRecording(intent: Intent) {
        if (recordingStarted || outputUri != null) return
        finishing = false
        startAsForeground()
        try {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
            val resultData = intent.intentExtra(EXTRA_RESULT_DATA)
                ?: error("Screen-capture consent data is missing.")
            check(resultCode != Int.MIN_VALUE) { "Screen-capture result code is missing." }

            val bounds = maximumWindowBounds()
            val size = calculateRecordingSize(bounds.width(), bounds.height())
            val uri = createOutputUri()
            outputUri = uri
            val descriptor = contentResolver.openFileDescriptor(uri, "rw")
                ?: error("The recording file could not be opened.")
            outputDescriptor = descriptor

            val recorder = createMediaRecorder().apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(size.width, size.height)
                setVideoFrameRate(VIDEO_FRAME_RATE)
                setVideoEncodingBitRate(VIDEO_BIT_RATE)
                setOutputFile(descriptor.fileDescriptor)
                prepare()
            }
            mediaRecorder = recorder

            val projectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = projectionManager.getMediaProjection(resultCode, resultData)
                ?: error("Screen-capture projection could not be created.")
            mediaProjection = projection
            projection.registerCallback(projectionCallback, null)
            virtualDisplay = projection.createVirtualDisplay(
                "RopeSkillRecording",
                size.width,
                size.height,
                resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.surface,
                null,
                null,
            )
            recorder.start()
            recordingStarted = true
            ScreenRecordingController.markRecording()
        } catch (error: Exception) {
            finishing = true
            discardIncompleteRecording()
            ScreenRecordingController.markError(
                "Screen recording could not start (${error.javaClass.simpleName}).",
            )
            stopSelf()
        }
    }

    private fun finishRecording(projectionAlreadyStopped: Boolean) {
        if (finishing) return
        finishing = true
        val completedUri = outputUri
        var validRecording = recordingStarted

        if (recordingStarted) {
            try {
                mediaRecorder?.stop()
            } catch (_: RuntimeException) {
                validRecording = false
            }
        }
        recordingStarted = false
        runCatching { mediaRecorder?.reset() }
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        if (!projectionAlreadyStopped) {
            runCatching { mediaProjection?.stop() }
        }
        mediaProjection = null
        runCatching { outputDescriptor?.close() }
        outputDescriptor = null

        if (validRecording && completedUri != null) {
            runCatching { publishRecording(completedUri) }
                .onSuccess {
                    ScreenRecordingController.markSaved(completedUri)
                }
                .onFailure {
                    contentResolver.deleteSafely(completedUri)
                    ScreenRecordingController.markError("The recording could not be finalized.")
                }
        } else {
            completedUri?.let { uri -> contentResolver.deleteSafely(uri) }
            ScreenRecordingController.markError("The recording ended before a valid video was saved.")
        }
        outputUri = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun discardIncompleteRecording() {
        recordingStarted = false
        runCatching { mediaRecorder?.reset() }
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { mediaProjection?.stop() }
        mediaProjection = null
        runCatching { outputDescriptor?.close() }
        outputDescriptor = null
        outputUri?.let { uri -> contentResolver.deleteSafely(uri) }
        outputUri = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createOutputUri(): Uri {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "RopeSkill_Speed30_$timestamp.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, VIDEO_MIME_TYPE)
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_MOVIES}/RopeSkill",
            )
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        return checkNotNull(
            contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values),
        ) { "A MediaStore video could not be created." }
    }

    private fun publishRecording(uri: Uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.IS_PENDING, 0)
        }
        check(contentResolver.update(uri, values, null, null) == 1) {
            "The recording could not be published."
        }
    }

    private fun maximumWindowBounds(): Rect {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.maximumWindowMetrics.bounds
        } else {
            @Suppress("DEPRECATION")
            val metrics = DisplayMetrics().also(windowManager.defaultDisplay::getRealMetrics)
            Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
        }
    }

    private fun createMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    private fun startAsForeground() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(): Notification {
        val stopPendingIntent = PendingIntent.getService(
            this,
            STOP_REQUEST_CODE,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("RopeSkill is recording")
            .setContentText("Speed 30 screen recording is active")
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Screen recording",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows when RopeSkill records a Speed 30 session"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val ACTION_START = "com.ropeskill.app.action.START_RECORDING"
        private const val ACTION_STOP = "com.ropeskill.app.action.STOP_RECORDING"
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_RESULT_DATA = "result_data"
        private const val NOTIFICATION_CHANNEL_ID = "screen_recording"
        private const val NOTIFICATION_ID = 752
        private const val STOP_REQUEST_CODE = 753
        private const val VIDEO_FRAME_RATE = 30
        private const val VIDEO_BIT_RATE = 8_000_000
        private const val VIDEO_MIME_TYPE = "video/mp4"
        private val FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

        fun startIntent(context: Context, resultCode: Int, resultData: Intent): Intent =
            Intent(context, ScreenRecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, ScreenRecordingService::class.java).apply {
                action = ACTION_STOP
            }
    }
}

private fun Intent.intentExtra(key: String): Intent? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, Intent::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }

private fun android.content.ContentResolver.deleteSafely(uri: Uri) {
    runCatching { delete(uri, null, null) }
}
