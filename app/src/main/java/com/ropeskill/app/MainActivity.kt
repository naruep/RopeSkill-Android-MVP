package com.ropeskill.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ropeskill.app.ui.theme.RopeSkillTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RopeSkillApp()
        }
    }

    override fun onStop() {
        if (
            ScreenRecordingController.state.value is ScreenRecordingState.Starting ||
            ScreenRecordingController.state.value is ScreenRecordingState.Recording
        ) {
            ScreenRecordingController.stop(this)
        }
        super.onStop()
    }
}

@Composable
fun RopeSkillApp(trainingViewModel: TrainingViewModel = viewModel()) {
    val uiState by trainingViewModel.uiState.collectAsStateWithLifecycle()
    val savedSessions by trainingViewModel.savedSessions.collectAsStateWithLifecycle()
    val settingsViewModel: SettingsViewModel = viewModel()
    val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    RopeSkillTheme(appTheme = settings.appTheme) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize(),
        ) {
            RopeSkillNavHost(
                navController = navController,
                uiState = uiState,
                savedSessions = savedSessions,
                trainingViewModel = trainingViewModel,
                settings = settings,
                settingsViewModel = settingsViewModel,
            )
        }
    }
}

@Composable
private fun RopeSkillNavHost(
    navController: NavHostController,
    uiState: TrainingUiState,
    savedSessions: List<TrainingSession>,
    trainingViewModel: TrainingViewModel,
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val recordingState by ScreenRecordingController.state.collectAsStateWithLifecycle()
    var pendingRecordedSpeedStart by rememberSaveable { mutableStateOf(false) }
    var currentSessionWasRecorded by rememberSaveable { mutableStateOf(false) }
    val startTraining: (WorkoutMode) -> Unit = { mode ->
        trainingViewModel.resetWorkout(mode)
        trainingViewModel.configureCountdownSeconds(settings.countdownSeconds)
        trainingViewModel.configureTrainingMusic(settings)
        trainingViewModel.startWorkout()
        navController.navigate(TRAINING_ROUTE)
    }
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val resultData = result.data
        if (result.resultCode == Activity.RESULT_OK && resultData != null) {
            pendingRecordedSpeedStart = true
            ScreenRecordingController.start(context, result.resultCode, resultData)
        } else {
            pendingRecordedSpeedStart = false
            ScreenRecordingController.markError(
                "Screen recording permission was not granted. Training was not started.",
            )
        }
    }
    val launchScreenCaptureConsent = {
        val projectionManager = context.getSystemService(
            Context.MEDIA_PROJECTION_SERVICE,
        ) as MediaProjectionManager
        val captureIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            projectionManager.createScreenCaptureIntent(
                MediaProjectionConfig.createConfigForDefaultDisplay(),
            )
        } else {
            projectionManager.createScreenCaptureIntent()
        }
        screenCaptureLauncher.launch(captureIntent)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchScreenCaptureConsent()
        } else {
            ScreenRecordingController.markError(
                "Camera permission is required before a recorded Speed 30 session can start.",
            )
        }
    }

    LaunchedEffect(recordingState, pendingRecordedSpeedStart) {
        when (recordingState) {
            is ScreenRecordingState.Recording -> {
                if (pendingRecordedSpeedStart) {
                    pendingRecordedSpeedStart = false
                    currentSessionWasRecorded = true
                    startTraining(WorkoutMode.SPEED_30)
                }
            }
            is ScreenRecordingState.Error -> pendingRecordedSpeedStart = false
            else -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = HOME_ROUTE,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(HOME_ROUTE) {
            HomeScreen(
                nickname = settings.nickname,
                savedSessions = savedSessions,
                onStartBasicBounce = {
                    currentSessionWasRecorded = false
                    startTraining(WorkoutMode.BASIC_BOUNCE)
                },
                onStartSpeed30 = {
                    currentSessionWasRecorded = false
                    startTraining(WorkoutMode.SPEED_30)
                },
                recordingSupported = ScreenRecordingController.isSupported,
                onRecordAndStartSpeed30 = {
                    if (isCameraPermissionGranted(context)) {
                        launchScreenCaptureConsent()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                bottomBar = {
                    RopeSkillBottomBar(
                        selectedDestination = MainDestination.HOME,
                        onDestinationSelected = navController::navigateToMainDestination,
                    )
                },
            )
        }
        composable(SETTINGS_ROUTE) {
            SettingsScreen(
                settings = settings,
                onNicknameChange = settingsViewModel::setNickname,
                onSoundEnabledChange = settingsViewModel::setSoundEnabled,
                onVibrationEnabledChange = settingsViewModel::setVibrationEnabled,
                onCountdownChange = settingsViewModel::setCountdownSeconds,
                onMeasurementUnitsChange = settingsViewModel::setMeasurementUnits,
                onAppThemeChange = settingsViewModel::setAppTheme,
                onTrainingMusicEnabledChange =
                    settingsViewModel::setTrainingMusicEnabled,
                onTrainingMusicSelected = settingsViewModel::selectTrainingMusic,
                onTrainingMusicRemoved = settingsViewModel::clearTrainingMusic,
                onTrainingMusicVolumeChange =
                    settingsViewModel::setTrainingMusicVolume,
                onResetSettings = settingsViewModel::resetSettings,
                bottomBar = {
                    RopeSkillBottomBar(
                        selectedDestination = MainDestination.SETTINGS,
                        onDestinationSelected = navController::navigateToMainDestination,
                    )
                },
            )
        }
        composable(HISTORY_ROUTE) {
            TrainingHistoryScreen(
                sessions = savedSessions,
                onDeleteSession = trainingViewModel::deleteSession,
                bottomBar = {
                    RopeSkillBottomBar(
                        selectedDestination = MainDestination.HISTORY,
                        onDestinationSelected = navController::navigateToMainDestination,
                    )
                },
            )
        }
        composable(TRAINING_ROUTE) {
            BackHandler {
                ScreenRecordingController.stop(context)
                trainingViewModel.pauseWorkout()
                navController.popBackStack()
            }
            LifecycleStartEffect(trainingViewModel) {
                onStopOrDispose {
                    trainingViewModel.pauseWorkout()
                }
            }

            LaunchedEffect(uiState.status) {
                if (uiState.status == WorkoutStatus.FINISHED) {
                    navController.navigate(RESULT_ROUTE) {
                        popUpTo(TRAINING_ROUTE) { inclusive = true }
                    }
                }
            }

            TrainingScreen(
                uiState = uiState,
                settings = settings,
                recordingState = if (currentSessionWasRecorded) {
                    recordingState
                } else {
                    ScreenRecordingState.Idle
                },
                onAddJump = trainingViewModel::addJump,
                onStart = trainingViewModel::startWorkout,
                onPause = trainingViewModel::pauseWorkout,
                onToggleMusicMuted = trainingViewModel::toggleTrainingMusicMuted,
                onFinish = {
                    trainingViewModel.finishWorkout()
                },
                onReset = {
                    trainingViewModel.resetWorkout()
                    trainingViewModel.configureTrainingMusic(settings)
                    trainingViewModel.startWorkout()
                },
                onPoseFrame = trainingViewModel::processPoseFrame,
                onPerformanceSnapshot = trainingViewModel::updatePosePerformance,
            )
        }
        composable(RESULT_ROUTE) {
            BackHandler {
                ScreenRecordingController.stop(context)
                trainingViewModel.resetWorkout()
                navController.navigateToMainDestination(MainDestination.HOME)
            }
            LaunchedEffect(currentSessionWasRecorded) {
                if (currentSessionWasRecorded) {
                    delay(RESULT_RECORDING_TAIL_MILLIS)
                    ScreenRecordingController.stop(context)
                }
            }
            ResultScreen(
                uiState = uiState,
                recordingState = if (currentSessionWasRecorded) {
                    recordingState
                } else {
                    ScreenRecordingState.Idle
                },
                onViewVideo = { uri -> context.viewRecording(uri) },
                onShareVideo = { uri -> context.shareRecording(uri) },
                onViewHistory = {
                    ScreenRecordingController.stop(context)
                    trainingViewModel.resetWorkout()
                    navController.navigateToMainDestination(MainDestination.HISTORY)
                },
                onDone = {
                    ScreenRecordingController.stop(context)
                    trainingViewModel.resetWorkout()
                    navController.navigateToMainDestination(MainDestination.HOME)
                },
            )
        }
    }

    val recordingError = (recordingState as? ScreenRecordingState.Error)?.message
    if (recordingError != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = ScreenRecordingController::dismissMessage,
            title = { androidx.compose.material3.Text("Recording unavailable") },
            text = { androidx.compose.material3.Text(recordingError) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = ScreenRecordingController::dismissMessage,
                ) {
                    androidx.compose.material3.Text("OK")
                }
            },
        )
    }
}

private fun Context.viewRecording(uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/mp4")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { startActivity(intent) }
}

private fun Context.shareRecording(uri: Uri) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(shareIntent, "Share RopeSkill recording"))
}

private fun NavHostController.navigateToMainDestination(destination: MainDestination) {
    if (destination == MainDestination.HOME) {
        if (currentDestination?.route != HOME_ROUTE) {
            val returnedHome = popBackStack(HOME_ROUTE, inclusive = false)
            if (!returnedHome) {
                navigate(HOME_ROUTE) {
                    launchSingleTop = true
                }
            }
        }
        return
    }
    navigate(destination.route) {
        popUpTo(HOME_ROUTE) {
            inclusive = false
        }
        launchSingleTop = true
    }
}

private const val HOME_ROUTE = "home"
private const val SETTINGS_ROUTE = "settings"
private const val HISTORY_ROUTE = "history"
private const val TRAINING_ROUTE = "training"
private const val RESULT_ROUTE = "result"
private const val RESULT_RECORDING_TAIL_MILLIS = 1_500L
