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
import app.blanc.search.Dictionary
import app.blanc.search.SearchEngine
import app.blanc.search.SearchResult
import app.blanc.ui.DrawerMode
import app.blanc.ui.Screen
import app.blanc.usage.AppTotal
import app.blanc.usage.UsagePermission
import app.blanc.usage.UsageReport
import app.blanc.usage.UsageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val appContext: Context,
    private val repository: AppRepository,
    private val settingsStore: SettingsStore,
    private val usageRepository: UsageRepository,
) : ViewModel() {

    val apps: StateFlow<List<AppInfo>> = repository.appsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<BlancSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlancSettings.DEFAULT)

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _usageGranted = MutableStateFlow(false)
    val usageGranted: StateFlow<Boolean> = _usageGranted.asStateFlow()

    private val _usageReport = MutableStateFlow<UsageReport?>(null)
    val usageReport: StateFlow<UsageReport?> = _usageReport.asStateFlow()

    private val _usageRangeDays = MutableStateFlow(30)
    val usageRangeDays: StateFlow<Int> = _usageRangeDays.asStateFlow()

    // The "Most used" list has its own range (default: today) so per-app usage
    // can be inspected for a single day independently of the charts above.
    private val _topRangeDays = MutableStateFlow(1)
    val topRangeDays: StateFlow<Int> = _topRangeDays.asStateFlow()

    private val _topApps = MutableStateFlow<List<AppTotal>>(emptyList())
    val topApps: StateFlow<List<AppTotal>> = _topApps.asStateFlow()

    private val searchEngine = SearchEngine(Dictionary(appContext))
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()
    private var searchJob: Job? = null

    // --- Navigation ---

    fun openDrawer(mode: DrawerMode = DrawerMode.Launch) {
        _screen.value = Screen.Drawer(mode)
    }

    fun openQuickSettings() {
        _screen.value = Screen.QuickSettings
    }

    fun goHome() {
        _screen.value = Screen.Home
    }

    fun setUsageRange(days: Int) {
        if (_usageRangeDays.value == days) return
        _usageRangeDays.value = days
        viewModelScope.launch {
            if (UsagePermission.isGranted(appContext)) {
                _usageReport.value = usageRepository.report(days)
            }
        }
    }

    fun setTopRange(days: Int) {
        if (_topRangeDays.value == days) return
        _topRangeDays.value = days
        viewModelScope.launch {
            if (UsagePermission.isGranted(appContext)) {
                _topApps.value = usageRepository.appUsage(days)
            }
        }
    }

    fun loadUsage() {
        viewModelScope.launch {
            val granted = UsagePermission.isGranted(appContext)
            _usageGranted.value = granted
            if (granted) {
                usageRepository.record()
                _usageReport.value = usageRepository.report(_usageRangeDays.value)
                _topApps.value = usageRepository.appUsage(_topRangeDays.value)
            } else {
                _usageReport.value = null
                _topApps.value = emptyList()
            }
        }
    }

    // --- Actions ---

    fun onSearchQuery(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(60)
            _searchResults.value = searchEngine.search(query, apps.value)
        }
    }

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

    fun toggleAnimations() {
        viewModelScope.launch { settingsStore.setAnimations(!settings.value.animations) }
    }

    fun toggleHaptics() {
        viewModelScope.launch { settingsStore.setHaptics(!settings.value.haptics) }
    }

    fun cycleWallpaperDim() {
        val next = (settings.value.wallpaperDim + 1) % (BlancSettings.MAX_WALLPAPER_DIM + 1)
        viewModelScope.launch { settingsStore.setWallpaperDim(next) }
    }

    fun setSwipeLeftApp(app: AppInfo?) {
        viewModelScope.launch { settingsStore.setSwipeLeftApp(app?.key) }
    }

    fun setSwipeRightApp(app: AppInfo?) {
        viewModelScope.launch { settingsStore.setSwipeRightApp(app?.key) }
    }

    fun setNudges(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setNudgesEnabled(enabled) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    MainViewModel(
                        appContext = appContext,
                        repository = AppRepository(appContext),
                        settingsStore = SettingsStore(appContext),
                        usageRepository = UsageRepository(appContext),
                    )
                }
            }
        }
    }
}
