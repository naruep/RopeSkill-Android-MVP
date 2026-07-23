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
            RopeSkillTheme {
                RopeSkillApp()
            }
        }
    }
}

@Composable
fun RopeSkillApp(trainingViewModel: TrainingViewModel = viewModel()) {
    val uiState by trainingViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    RopeSkillNavHost(
        navController = navController,
        uiState = uiState,
        trainingViewModel = trainingViewModel,
    )
}

@Composable
private fun RopeSkillNavHost(
    navController: NavHostController,
    uiState: TrainingUiState,
    trainingViewModel: TrainingViewModel,
) {
    NavHost(navController = navController, startDestination = HOME_ROUTE) {
        composable(HOME_ROUTE) {
            HomeScreen(
                onStartTraining = {
                    trainingViewModel.resetWorkout()
                    trainingViewModel.startWorkout()
                    navController.navigate(TRAINING_ROUTE)
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
                onDone = {
                    trainingViewModel.resetWorkout()
                    navController.navigate(HOME_ROUTE) {
                        popUpTo(HOME_ROUTE)
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}

private const val HOME_ROUTE = "home"
private const val TRAINING_ROUTE = "training"
private const val RESULT_ROUTE = "result"
