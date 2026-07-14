package app.blanc.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.blanc.MainViewModel
import app.blanc.ui.drawer.AppDrawerScreen
import app.blanc.ui.home.HomeScreen
import app.blanc.ui.motion.rememberMotionEnabled
import app.blanc.ui.search.UniversalSearchScreen
import app.blanc.ui.settings.SettingsScreen
import app.blanc.ui.theme.BlancTheme
import app.blanc.ui.usage.UsageScreen
import app.blanc.usage.UsagePermission
import app.blanc.util.DefaultLauncher
import app.blanc.util.openSettingsAction
import app.blanc.util.webSearch

@Composable
fun BlancApp(viewModel: MainViewModel) {
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val usageGranted by viewModel.usageGranted.collectAsStateWithLifecycle()
    val usageState by viewModel.usageState.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val motionEnabled = rememberMotionEnabled(settings.animations)

    BlancTheme(themeMode = settings.theme) {
        val view = LocalView.current
        LaunchedEffect(settings.showStatusBar) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (settings.showStatusBar) {
                controller.show(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
        }

        // No solid fill — the transparent window shows the system wallpaper.
        Box(modifier = Modifier.fillMaxSize()) {
            BackHandler(enabled = screen !is Screen.Home) { viewModel.goHome() }

            when (val current = screen) {
                is Screen.Home -> HomeScreen(
                    apps = apps,
                    settings = settings,
                    motionEnabled = motionEnabled,
                    hapticsEnabled = settings.haptics,
                    onLaunch = { viewModel.launchApp(it) },
                    onOpenDrawer = { viewModel.openDrawer() },
                    onOpenSettings = { viewModel.openSettings() },
                    onAssignSlot = { viewModel.openDrawer(DrawerMode.AssignHome(it)) },
                    onAddSlot = { viewModel.openDrawer(DrawerMode.AssignHome(settings.homeApps.size)) },
                )

                is Screen.Drawer -> when (current.mode) {
                    is DrawerMode.Launch -> UniversalSearchScreen(
                        results = searchResults,
                        motionEnabled = motionEnabled,
                        hapticsEnabled = settings.haptics,
                        onQueryChange = { viewModel.onSearchQuery(it) },
                        onLaunchApp = {
                            viewModel.launchApp(it)
                            viewModel.goHome()
                        },
                        onAddAppToHome = {
                            viewModel.addHomeApp(it)
                            viewModel.goHome()
                        },
                        onOpenSetting = {
                            view.context.openSettingsAction(it)
                            viewModel.goHome()
                        },
                        onWebSearch = {
                            view.context.webSearch(it)
                            viewModel.goHome()
                        },
                        onCopy = { clipboard.setText(AnnotatedString(it)) },
                    )

                    is DrawerMode.AssignHome -> AppDrawerScreen(
                        apps = apps,
                        mode = current.mode,
                        homeAppsCount = settings.homeApps.size,
                        motionEnabled = motionEnabled,
                        onLaunch = {
                            viewModel.launchApp(it)
                            viewModel.goHome()
                        },
                        onAssign = { index, app ->
                            viewModel.assignHomeApp(index, app)
                            viewModel.goHome()
                        },
                        onRemove = {
                            viewModel.removeHomeApp(it)
                            viewModel.goHome()
                        },
                        onAddHome = {
                            viewModel.addHomeApp(it)
                            viewModel.goHome()
                        },
                    )
                }

                is Screen.Settings -> SettingsScreen(
                    settings = settings,
                    onCycleAlignment = { viewModel.cycleAlignment() },
                    onCycleTheme = { viewModel.cycleTheme() },
                    onToggleStatusBar = { viewModel.toggleStatusBar() },
                    onToggleAnimations = { viewModel.toggleAnimations() },
                    onToggleHaptics = { viewModel.toggleHaptics() },
                    onSetDefaultLauncher = { DefaultLauncher.request(view.context) },
                    onOpenUsage = { viewModel.openUsage() },
                )

                is Screen.Usage -> UsageScreen(
                    granted = usageGranted,
                    state = usageState,
                    labelFor = { pkg ->
                        apps.firstOrNull { it.packageName == pkg }?.label ?: pkg
                    },
                    onGrantAccess = { UsagePermission.requestAccess(view.context) },
                )
            }
        }
    }
}
