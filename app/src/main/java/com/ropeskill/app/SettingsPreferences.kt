package com.ropeskill.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class MeasurementUnits {
    METRIC,
    IMPERIAL,
}

data class UserSettings(
    val nickname: String = "",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val countdownSeconds: Int = 5,
    val measurementUnits: MeasurementUnits = MeasurementUnits.METRIC,
)

private val Context.settingsDataStore by preferencesDataStore(name = "user_settings")

class SettingsPreferences(private val context: Context) {
    val settings: Flow<UserSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            UserSettings(
                nickname = preferences[NICKNAME].orEmpty(),
                soundEnabled = preferences[SOUND_ENABLED] ?: true,
                vibrationEnabled = preferences[VIBRATION_ENABLED] ?: true,
                countdownSeconds = normalizedCountdown(preferences[COUNTDOWN_SECONDS]),
                measurementUnits = normalizedMeasurementUnits(preferences[MEASUREMENT_UNITS]),
            )
        }

    suspend fun setNickname(nickname: String) {
        context.settingsDataStore.edit { it[NICKNAME] = normalizedNickname(nickname) }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[VIBRATION_ENABLED] = enabled }
    }

    suspend fun setCountdownSeconds(seconds: Int) {
        require(seconds in SUPPORTED_COUNTDOWNS)
        context.settingsDataStore.edit { it[COUNTDOWN_SECONDS] = seconds }
    }

    suspend fun setMeasurementUnits(units: MeasurementUnits) {
        context.settingsDataStore.edit { it[MEASUREMENT_UNITS] = units.name }
    }

    suspend fun reset() {
        context.settingsDataStore.edit { it.clear() }
    }

    private companion object {
        val NICKNAME = stringPreferencesKey("nickname")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val COUNTDOWN_SECONDS = intPreferencesKey("countdown_seconds")
        val MEASUREMENT_UNITS = stringPreferencesKey("measurement_units")
        val SUPPORTED_COUNTDOWNS = SUPPORTED_COUNTDOWN_SECONDS
    }
}

internal val SUPPORTED_COUNTDOWN_SECONDS = setOf(3, 5, 10)

internal fun normalizedNickname(value: String): String = value.trim().take(30)

internal fun normalizedCountdown(value: Int?): Int =
    value?.takeIf { it in SUPPORTED_COUNTDOWN_SECONDS } ?: 5

internal fun normalizedMeasurementUnits(value: String?): MeasurementUnits =
    value?.let { storedValue ->
        MeasurementUnits.entries.firstOrNull { it.name == storedValue }
    } ?: MeasurementUnits.METRIC
