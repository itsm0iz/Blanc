package app.blanc.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.blanc.MainViewModel
import app.blanc.ui.drawer.AppDrawerScreen
import app.blanc.ui.home.HomeScreen
import app.blanc.ui.settings.SettingsScreen
import app.blanc.ui.theme.BlancTheme
import app.blanc.util.DefaultLauncher

@Composable
fun BlancApp(viewModel: MainViewModel) {
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            BackHandler(enabled = screen !is Screen.Home) { viewModel.goHome() }

            when (val current = screen) {
                is Screen.Home -> HomeScreen(
                    apps = apps,
                    settings = settings,
                    onLaunch = { viewModel.launchApp(it) },
                    onOpenDrawer = { viewModel.openDrawer() },
                    onOpenSettings = { viewModel.openSettings() },
                    onAssignSlot = { viewModel.openDrawer(DrawerMode.AssignHome(it)) },
                    onAddSlot = { viewModel.openDrawer(DrawerMode.AssignHome(settings.homeApps.size)) },
                )

                is Screen.Drawer -> AppDrawerScreen(
                    apps = apps,
                    mode = current.mode,
                    homeAppsCount = settings.homeApps.size,
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

                is Screen.Settings -> SettingsScreen(
                    settings = settings,
                    onCycleAlignment = { viewModel.cycleAlignment() },
                    onCycleTheme = { viewModel.cycleTheme() },
                    onToggleStatusBar = { viewModel.toggleStatusBar() },
                    onSetDefaultLauncher = { DefaultLauncher.request(view.context) },
                )
            }
        }
    }
}
