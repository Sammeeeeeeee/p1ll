package io.github.sammeeeeeeee.p1ll.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Prefs(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Index into [io.github.sammeeeeeeee.p1ll.ui.theme.Accents]. */
    val accent: Int = 0,
    /** Re-remind every N minutes until the dose is taken or snoozed; 0 = off. */
    val nagMinutes: Int = 0,
    val sounds: Boolean = true,
    val haptics: Boolean = true,
)

private val Context.store by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val theme = intPreferencesKey("theme")
        val accent = intPreferencesKey("accent")
        val nag = intPreferencesKey("nag")
        val sounds = booleanPreferencesKey("sounds")
        val haptics = booleanPreferencesKey("haptics")
    }

    val flow: Flow<Prefs> = context.store.data.map { it.toPrefs() }

    suspend fun current(): Prefs = flow.first()

    suspend fun update(transform: (Prefs) -> Prefs) {
        context.store.edit { p ->
            val next = transform(p.toPrefs())
            p[Keys.theme] = next.theme.ordinal
            p[Keys.accent] = next.accent
            p[Keys.nag] = next.nagMinutes
            p[Keys.sounds] = next.sounds
            p[Keys.haptics] = next.haptics
        }
    }

    private fun Preferences.toPrefs(): Prefs {
        val d = Prefs()
        return Prefs(
            theme = ThemeMode.entries.getOrElse(this[Keys.theme] ?: 0) { d.theme },
            accent = this[Keys.accent] ?: d.accent,
            nagMinutes = this[Keys.nag] ?: d.nagMinutes,
            sounds = this[Keys.sounds] ?: d.sounds,
            haptics = this[Keys.haptics] ?: d.haptics,
        )
    }
}
