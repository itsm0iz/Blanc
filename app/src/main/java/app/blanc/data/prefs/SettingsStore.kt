package app.blanc.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "blanc_settings")

/**
 * Persists [BlancSettings] via Jetpack DataStore. Home apps are stored as a
 * single newline-separated string of [AppKey.encode] values to preserve order.
 */
class SettingsStore(context: Context) {

    private val store = context.applicationContext.dataStore

    val settings: Flow<BlancSettings> = store.data.map { prefs -> prefs.toSettings() }

    /** A one-shot read of the current settings, for non-UI callers like workers. */
    suspend fun current(): BlancSettings = settings.first()

    private fun Preferences.toSettings(): BlancSettings {
        val homeApps = this[KEY_HOME_APPS]
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { AppKey.decode(it) }
            ?: emptyList()
        return BlancSettings(
            homeApps = homeApps,
            alignment = HomeAlignment.entries.getOrElse(this[KEY_ALIGNMENT] ?: 0) { HomeAlignment.START },
            theme = ThemeMode.entries.getOrElse(this[KEY_THEME] ?: ThemeMode.DARK.ordinal) { ThemeMode.DARK },
            showStatusBar = this[KEY_STATUS_BAR] ?: false,
            animations = this[KEY_ANIMATIONS] ?: true,
            haptics = this[KEY_HAPTICS] ?: true,
            wallpaperDim = this[KEY_WALLPAPER_DIM] ?: 0,
            swipeLeftApp = this[KEY_SWIPE_LEFT]?.let { AppKey.decode(it) },
            swipeRightApp = this[KEY_SWIPE_RIGHT]?.let { AppKey.decode(it) },
            nudgesEnabled = this[KEY_NUDGES] ?: true,
        )
    }

    suspend fun setHomeApps(apps: List<AppKey>) {
        store.edit { prefs ->
            prefs[KEY_HOME_APPS] = apps.joinToString("\n") { it.encode() }
        }
    }

    suspend fun assignHomeApp(index: Int, app: AppKey, current: List<AppKey>) {
        val updated = current.toMutableList()
        if (index in updated.indices) {
            updated[index] = app
        } else if (updated.size < BlancSettings.MAX_HOME_APPS) {
            updated.add(app)
        }
        setHomeApps(updated)
    }

    suspend fun addHomeApp(app: AppKey, current: List<AppKey>) {
        if (current.any { it == app } || current.size >= BlancSettings.MAX_HOME_APPS) return
        setHomeApps(current + app)
    }

    suspend fun removeHomeApp(index: Int, current: List<AppKey>) {
        if (index !in current.indices) return
        setHomeApps(current.toMutableList().also { it.removeAt(index) })
    }

    suspend fun setAlignment(alignment: HomeAlignment) {
        store.edit { it[KEY_ALIGNMENT] = alignment.ordinal }
    }

    suspend fun setTheme(theme: ThemeMode) {
        store.edit { it[KEY_THEME] = theme.ordinal }
    }

    suspend fun setShowStatusBar(show: Boolean) {
        store.edit { it[KEY_STATUS_BAR] = show }
    }

    suspend fun setAnimations(enabled: Boolean) {
        store.edit { it[KEY_ANIMATIONS] = enabled }
    }

    suspend fun setHaptics(enabled: Boolean) {
        store.edit { it[KEY_HAPTICS] = enabled }
    }

    suspend fun setWallpaperDim(level: Int) {
        store.edit { it[KEY_WALLPAPER_DIM] = level }
    }

    suspend fun setSwipeLeftApp(app: AppKey?) {
        store.edit { if (app == null) it.remove(KEY_SWIPE_LEFT) else it[KEY_SWIPE_LEFT] = app.encode() }
    }

    suspend fun setSwipeRightApp(app: AppKey?) {
        store.edit { if (app == null) it.remove(KEY_SWIPE_RIGHT) else it[KEY_SWIPE_RIGHT] = app.encode() }
    }

    suspend fun setNudgesEnabled(enabled: Boolean) {
        store.edit { it[KEY_NUDGES] = enabled }
    }

    private companion object {
        val KEY_HOME_APPS = stringPreferencesKey("home_apps")
        val KEY_ALIGNMENT = intPreferencesKey("alignment")
        val KEY_THEME = intPreferencesKey("theme")
        val KEY_STATUS_BAR = booleanPreferencesKey("status_bar")
        val KEY_ANIMATIONS = booleanPreferencesKey("animations")
        val KEY_HAPTICS = booleanPreferencesKey("haptics")
        val KEY_WALLPAPER_DIM = intPreferencesKey("wallpaper_dim")
        val KEY_SWIPE_LEFT = stringPreferencesKey("swipe_left_app")
        val KEY_SWIPE_RIGHT = stringPreferencesKey("swipe_right_app")
        val KEY_NUDGES = booleanPreferencesKey("nudges_enabled")
    }
}
