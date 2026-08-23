package app.blanc.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.blanc.spaces.BlancSpace
import app.blanc.spaces.SpaceSlot
import app.blanc.spaces.SpacesCodec
import app.blanc.spaces.SpacesConfig
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
            spaces = readSpaces(),
        )
    }

    /** Import the former swipe-left app into a first Space without erasing it. */
    private fun Preferences.readSpaces(): SpacesConfig {
        this[KEY_SPACES]?.let { return SpacesCodec.decode(it) }
        val legacy = this[KEY_SWIPE_LEFT]?.let { AppKey.decode(it) } ?: return SpacesConfig.DEFAULT
        val quick = BlancSpace.empty(LEGACY_QUICK_SPACE_ID, "Quick").withSlot(
            index = 0,
            slot = SpaceSlot(
                appKey = legacy,
                savedLabel = legacy.packageName.substringAfterLast('.'),
            ),
        )
        return SpacesConfig.DEFAULT.copy(spaces = listOf(quick))
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

    // --- Spaces ----------------------------------------------------------

    /**
     * Every Spaces mutation reads and writes inside the same DataStore edit.
     * This prevents two quick slot edits from overwriting one another with a
     * stale UI snapshot.
     */
    private suspend fun updateSpaces(transform: (SpacesConfig) -> SpacesConfig) {
        store.edit { prefs ->
            val current = prefs.readSpaces()
            prefs[KEY_SPACES] = SpacesCodec.encode(transform(current))
        }
    }

    suspend fun setSpacesEnabled(enabled: Boolean) {
        updateSpaces { it.copy(enabled = enabled) }
    }

    suspend fun setSpacesWallpaperBlur(enabled: Boolean) {
        updateSpaces { it.copy(wallpaperBlur = enabled) }
    }

    suspend fun createSpace(id: String, name: String) {
        updateSpaces { config ->
            if (config.spaces.size >= SpacesConfig.MAX_SPACES || config.spaces.any { it.id == id }) {
                config
            } else {
                config.copy(spaces = config.spaces + BlancSpace.empty(id, name))
            }
        }
    }

    suspend fun renameSpace(spaceId: String, name: String) {
        updateSpaces { config ->
            config.copy(
                spaces = config.spaces.map { space ->
                    if (space.id == spaceId) space.withName(name) else space
                },
            )
        }
    }

    suspend fun deleteSpace(spaceId: String) {
        updateSpaces { config -> config.copy(spaces = config.spaces.filterNot { it.id == spaceId }) }
    }

    suspend fun moveSpace(spaceId: String, offset: Int) {
        if (offset == 0) return
        updateSpaces { config ->
            val from = config.spaces.indexOfFirst { it.id == spaceId }
            if (from < 0) return@updateSpaces config
            val to = (from + offset).coerceIn(config.spaces.indices)
            if (from == to) return@updateSpaces config
            val reordered = config.spaces.toMutableList()
            val moving = reordered.removeAt(from)
            reordered.add(to, moving)
            config.copy(spaces = reordered)
        }
    }

    suspend fun setSpaceSlot(spaceId: String, index: Int, slot: SpaceSlot?) {
        if (index !in 0 until BlancSpace.SLOT_COUNT) return
        updateSpaces { config ->
            config.copy(
                spaces = config.spaces.map { space ->
                    if (space.id == spaceId) space.withSlot(index, slot) else space
                },
            )
        }
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
        val KEY_SPACES = stringPreferencesKey("spaces_config_v1")
        const val LEGACY_QUICK_SPACE_ID = "legacy-quick"
    }
}
