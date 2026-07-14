package app.blanc.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.blanc.MainViewModel
import app.blanc.ui.DrawerMode
import app.blanc.ui.drawer.AppDrawerScreen
import app.blanc.ui.motion.rememberMotionEnabled
import app.blanc.ui.theme.BlancTheme
import app.blanc.usage.UsagePermission
import app.blanc.util.DefaultLauncher
import app.blanc.util.openUrl

/** What the in-app picker is currently choosing an app for. */
private sealed interface PickTarget {
    data class HomeSlot(val index: Int) : PickTarget
    data object SwipeLeft : PickTarget
    data object SwipeRight : PickTarget
}

/**
 * The Blanc app itself — a traditional, opaque app (its own launcher icon)
 * hosting the tabbed dashboard, with an in-app picker for home slots and
 * swipe-gesture apps.
 */
@Composable
fun DashboardApp(viewModel: MainViewModel, onClose: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val usageGranted by viewModel.usageGranted.collectAsStateWithLifecycle()
    val usageReport by viewModel.usageReport.collectAsStateWithLifecycle()
    val usageRangeDays by viewModel.usageRangeDays.collectAsStateWithLifecycle()
    val motionEnabled = rememberMotionEnabled(settings.animations)

    var pickTarget by remember { mutableStateOf<PickTarget?>(null) }

    LaunchedEffect(Unit) { viewModel.loadUsage() }

    val homeAppNames = remember(apps, settings.homeApps) {
        settings.homeApps.map { key -> apps.firstOrNull { it.key == key }?.label ?: key.packageName }
    }
    fun nameOf(key: app.blanc.data.prefs.AppKey?): String =
        key?.let { apps.firstOrNull { app -> app.key == it }?.label ?: it.packageName } ?: "None"

    BlancTheme(themeMode = settings.theme) {
        val view = LocalView.current
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                val target = pickTarget
                if (target != null) {
                    BackHandler { pickTarget = null }
                    val slotIndex = (target as? PickTarget.HomeSlot)?.index ?: 0
                    AppDrawerScreen(
                        apps = apps,
                        mode = DrawerMode.AssignHome(slotIndex),
                        homeAppsCount = if (target is PickTarget.HomeSlot) settings.homeApps.size else 0,
                        motionEnabled = motionEnabled,
                        onLaunch = {},
                        onAssign = { index, app ->
                            when (target) {
                                is PickTarget.HomeSlot -> viewModel.assignHomeApp(index, app)
                                PickTarget.SwipeLeft -> viewModel.setSwipeLeftApp(app)
                                PickTarget.SwipeRight -> viewModel.setSwipeRightApp(app)
                            }
                            pickTarget = null
                        },
                        onRemove = { index ->
                            if (target is PickTarget.HomeSlot) viewModel.removeHomeApp(index)
                            pickTarget = null
                        },
                        onAddHome = {},
                    )
                } else {
                    BackHandler { onClose() }
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Blanc",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 8.dp),
                        )
                        DashboardScreen(
                            settings = settings,
                            homeAppNames = homeAppNames,
                            usageGranted = usageGranted,
                            usageReport = usageReport,
                            usageRangeDays = usageRangeDays,
                            labelFor = { pkg -> apps.firstOrNull { it.packageName == pkg }?.label ?: pkg },
                            onGrantUsage = { UsagePermission.requestAccess(view.context) },
                            onUsageRangeChange = { viewModel.setUsageRange(it) },
                            swipeLeftName = nameOf(settings.swipeLeftApp),
                            swipeRightName = nameOf(settings.swipeRightApp),
                            onEditHomeApp = { pickTarget = PickTarget.HomeSlot(it) },
                            onAddHomeApp = { pickTarget = PickTarget.HomeSlot(settings.homeApps.size) },
                            onCycleAlignment = { viewModel.cycleAlignment() },
                            onCycleDim = { viewModel.cycleWallpaperDim() },
                            onEditSwipeLeft = { pickTarget = PickTarget.SwipeLeft },
                            onEditSwipeRight = { pickTarget = PickTarget.SwipeRight },
                            onClearSwipeLeft = { viewModel.setSwipeLeftApp(null) },
                            onClearSwipeRight = { viewModel.setSwipeRightApp(null) },
                            onCycleTheme = { viewModel.cycleTheme() },
                            onToggleStatusBar = { viewModel.toggleStatusBar() },
                            onToggleAnimations = { viewModel.toggleAnimations() },
                            onToggleHaptics = { viewModel.toggleHaptics() },
                            onSetDefaultLauncher = { DefaultLauncher.request(view.context) },
                            onOpenUrl = { view.context.openUrl(it) },
                        )
                    }
                }
            }
        }
    }
}
