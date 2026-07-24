package com.ropeskill.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ropeskill.app.ui.theme.RopeSkillTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RopeSkillApp()
        }
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

@Composable
private fun RopeSkillNavHost(
    navController: NavHostController,
    uiState: TrainingUiState,
    savedSessions: List<TrainingSession>,
    trainingViewModel: TrainingViewModel,
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
) {
    NavHost(navController = navController, startDestination = HOME_ROUTE) {
        composable(HOME_ROUTE) {
            HomeScreen(
                nickname = settings.nickname,
                savedSessions = savedSessions,
                onStartTraining = {
                    trainingViewModel.configureCountdownSeconds(settings.countdownSeconds)
                    trainingViewModel.resetWorkout()
                    trainingViewModel.startWorkout()
                    navController.navigate(TRAINING_ROUTE)
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
            LifecycleStartEffect(trainingViewModel) {
                onStopOrDispose {
                    trainingViewModel.pauseWorkout()
                }
            }

            TrainingScreen(
                uiState = uiState,
                settings = settings,
                onAddJump = trainingViewModel::addJump,
                onStart = trainingViewModel::startWorkout,
                onPause = trainingViewModel::pauseWorkout,
                onFinish = {
                    trainingViewModel.finishWorkout()
                    navController.navigate(RESULT_ROUTE) {
                        popUpTo(TRAINING_ROUTE) { inclusive = true }
                    }
                },
                onReset = {
                    trainingViewModel.resetWorkout()
                    trainingViewModel.startWorkout()
                },
                onPoseFrame = trainingViewModel::processPoseFrame,
            )
        }
        composable(RESULT_ROUTE) {
            ResultScreen(
                uiState = uiState,
                onViewHistory = {
                    trainingViewModel.resetWorkout()
                    navController.navigateToMainDestination(MainDestination.HISTORY)
                },
                onDone = {
                    trainingViewModel.resetWorkout()
                    navController.navigateToMainDestination(MainDestination.HOME)
                },
            )
        }
    }
}

private fun NavHostController.navigateToMainDestination(destination: MainDestination) {
    navigate(destination.route) {
        popUpTo(HOME_ROUTE) {
            inclusive = destination == MainDestination.HOME
        }
        launchSingleTop = true
    }
}

private const val HOME_ROUTE = "home"
private const val SETTINGS_ROUTE = "settings"
private const val HISTORY_ROUTE = "history"
private const val TRAINING_ROUTE = "training"
private const val RESULT_ROUTE = "result"
