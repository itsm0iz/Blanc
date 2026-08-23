package app.blanc.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.blanc.DashboardActivity
import app.blanc.MainViewModel
import app.blanc.ui.drawer.AppDrawerScreen
import app.blanc.ui.home.HomeScreen
import app.blanc.ui.motion.rememberMotionEnabled
import app.blanc.ui.quicksettings.QuickSettingsScreen
import app.blanc.ui.search.UniversalSearchScreen
import app.blanc.ui.spaces.SpacesScreen
import app.blanc.ui.spaces.rememberSpacesWindowBlur
import app.blanc.ui.theme.BlancTheme
import app.blanc.util.expandNotifications
import app.blanc.util.openSettingsAction
import app.blanc.util.webSearch

@Composable
fun BlancApp(viewModel: MainViewModel) {
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val motionEnabled = rememberMotionEnabled(settings.animations)

    val homeAppNames = remember(apps, settings.homeApps) {
        settings.homeApps.map { key -> apps.firstOrNull { it.key == key }?.label ?: key.packageName }
    }
    val swipeLeftApp = remember(apps, settings.swipeLeftApp) {
        settings.swipeLeftApp?.let { key -> apps.firstOrNull { it.key == key } }
    }
    val swipeRightApp = remember(apps, settings.swipeRightApp) {
        settings.swipeRightApp?.let { key -> apps.firstOrNull { it.key == key } }
    }
    val dimAlpha = when (settings.wallpaperDim) {
        1 -> 0.2f
        2 -> 0.4f
        3 -> 0.65f
        else -> 0f
    }

    BlancTheme(themeMode = settings.theme) {
        val view = LocalView.current
        val spacesVisible = screen is Screen.Spaces
        val blurActive = rememberSpacesWindowBlur(
            active = spacesVisible,
            requested = settings.spaces.wallpaperBlur,
        )
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

        // Transparent window shows the system wallpaper. An optional full-screen
        // scrim dims it for readability; content sits above the scrim and clears
        // the system bars.
        Box(modifier = Modifier.fillMaxSize()) {
            if (spacesVisible) {
                val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val veil = if (dark) Color.Black else Color.White
                val veilAlpha = when {
                    dark && blurActive -> 0.26f
                    dark -> 0.36f
                    blurActive -> 0.18f
                    else -> 0.28f
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(veil.copy(alpha = veilAlpha)),
                )
            } else if (dimAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = dimAlpha)),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
            ) {
                BackHandler(enabled = screen !is Screen.Home) { viewModel.goHome() }

                when (val current = screen) {
                    is Screen.Home -> HomeScreen(
                        apps = apps,
                        settings = settings,
                        motionEnabled = motionEnabled,
                        hapticsEnabled = settings.haptics,
                        swipeRightApp = swipeRightApp,
                        onLaunch = { viewModel.launchApp(it) },
                        onSwipeLeft = {
                            if (settings.spaces.enabled) {
                                viewModel.openSpaces()
                            } else {
                                swipeLeftApp?.let(viewModel::launchApp)
                            }
                        },
                        swipeLeftActionLabel = if (settings.spaces.enabled) {
                            "Open Spaces"
                        } else {
                            swipeLeftApp?.let { "Open ${it.label}" }
                        },
                        onOpenDrawer = { viewModel.openDrawer() },
                        onOpenSettings = { viewModel.openQuickSettings() },
                        onSwipeDown = { view.context.expandNotifications() },
                        onAssignSlot = { viewModel.openDrawer(DrawerMode.AssignHome(it)) },
                    )

                    is Screen.Spaces -> SpacesScreen(
                        config = settings.spaces,
                        apps = apps,
                        motionEnabled = motionEnabled,
                        onLaunch = {
                            viewModel.launchApp(it)
                            viewModel.goHome()
                        },
                        onClose = { viewModel.goHome() },
                        onOpenSetup = {
                            view.context.startActivity(
                                Intent(view.context, DashboardActivity::class.java)
                                    .putExtra(DashboardActivity.EXTRA_OPEN_SPACES, true),
                            )
                        },
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

                    is Screen.QuickSettings -> QuickSettingsScreen(
                        settings = settings,
                        homeAppNames = homeAppNames,
                        onEditHomeApp = { viewModel.openDrawer(DrawerMode.AssignHome(it)) },
                        onAddHomeApp = { viewModel.openDrawer(DrawerMode.AssignHome(settings.homeApps.size)) },
                        onCycleAlignment = { viewModel.cycleAlignment() },
                        onCycleDim = { viewModel.cycleWallpaperDim() },
                        onOpenDashboard = {
                            view.context.startActivity(
                                Intent(view.context, DashboardActivity::class.java),
                            )
                        },
                    )
                }
            }
        }
    }
}
