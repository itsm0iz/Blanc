package app.blanc

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.blanc.data.AppInfo
import app.blanc.data.AppRepository
import app.blanc.data.prefs.AppKey
import app.blanc.data.prefs.BlancSettings
import app.blanc.data.prefs.HomeAlignment
import app.blanc.data.prefs.SettingsStore
import app.blanc.data.prefs.ThemeMode
import app.blanc.ui.DrawerMode
import app.blanc.ui.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: AppRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val apps: StateFlow<List<AppInfo>> = repository.appsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<BlancSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlancSettings.DEFAULT)

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    // --- Navigation ---

    fun openDrawer(mode: DrawerMode = DrawerMode.Launch) {
        _screen.value = Screen.Drawer(mode)
    }

    fun openSettings() {
        _screen.value = Screen.Settings
    }

    fun goHome() {
        _screen.value = Screen.Home
    }

    // --- Actions ---

    fun launchApp(app: AppInfo) {
        repository.launch(app)
    }

    fun assignHomeApp(index: Int, app: AppInfo) {
        viewModelScope.launch {
            settingsStore.assignHomeApp(index, app.key, settings.value.homeApps)
        }
    }

    fun addHomeApp(app: AppInfo) {
        viewModelScope.launch {
            settingsStore.addHomeApp(app.key, settings.value.homeApps)
        }
    }

    fun removeHomeApp(index: Int) {
        viewModelScope.launch {
            settingsStore.removeHomeApp(index, settings.value.homeApps)
        }
    }

    fun resolveHomeApp(key: AppKey): AppInfo? = repository.findByKey(apps.value, key)

    fun cycleAlignment() {
        val next = when (settings.value.alignment) {
            HomeAlignment.START -> HomeAlignment.CENTER
            HomeAlignment.CENTER -> HomeAlignment.END
            HomeAlignment.END -> HomeAlignment.START
        }
        viewModelScope.launch { settingsStore.setAlignment(next) }
    }

    fun cycleTheme() {
        val next = when (settings.value.theme) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        viewModelScope.launch { settingsStore.setTheme(next) }
    }

    fun toggleStatusBar() {
        viewModelScope.launch { settingsStore.setShowStatusBar(!settings.value.showStatusBar) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    MainViewModel(
                        repository = AppRepository(appContext),
                        settingsStore = SettingsStore(appContext),
                    )
                }
            }
        }
    }
}
