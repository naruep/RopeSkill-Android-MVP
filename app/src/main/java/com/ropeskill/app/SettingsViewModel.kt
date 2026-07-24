package com.ropeskill.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = SettingsPreferences(application)

    val uiState: StateFlow<UserSettings> = preferences.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings(),
    )

    fun setNickname(nickname: String) = update { preferences.setNickname(nickname) }

    fun setSoundEnabled(enabled: Boolean) = update {
        preferences.setSoundEnabled(enabled)
    }

    fun setVibrationEnabled(enabled: Boolean) = update {
        preferences.setVibrationEnabled(enabled)
    }

    fun setCountdownSeconds(seconds: Int) = update {
        preferences.setCountdownSeconds(seconds)
    }

    fun setMeasurementUnits(units: MeasurementUnits) = update {
        preferences.setMeasurementUnits(units)
    }

    fun setAppTheme(theme: AppTheme) = update {
        preferences.setAppTheme(theme)
    }

    fun resetSettings() = update { preferences.reset() }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
