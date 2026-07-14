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

/**
 * The Blanc app itself — a traditional, opaque app (its own launcher icon)
 * hosting the tabbed dashboard. Home-app slots are edited with an in-app
 * picker (reusing the app drawer) rather than the launcher's flow.
 */
@Composable
fun DashboardApp(viewModel: MainViewModel, onClose: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val usageGranted by viewModel.usageGranted.collectAsStateWithLifecycle()
    val usageState by viewModel.usageState.collectAsStateWithLifecycle()
    val motionEnabled = rememberMotionEnabled(settings.animations)

    var pickingSlot by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) { viewModel.loadUsage() }

    val homeAppNames = remember(apps, settings.homeApps) {
        settings.homeApps.map { key -> apps.firstOrNull { it.key == key }?.label ?: key.packageName }
    }

    BlancTheme(themeMode = settings.theme) {
        val view = LocalView.current
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                val slot = pickingSlot
                if (slot != null) {
                    BackHandler { pickingSlot = null }
                    AppDrawerScreen(
                        apps = apps,
                        mode = DrawerMode.AssignHome(slot),
                        homeAppsCount = settings.homeApps.size,
                        motionEnabled = motionEnabled,
                        onLaunch = {},
                        onAssign = { index, app ->
                            viewModel.assignHomeApp(index, app)
                            pickingSlot = null
                        },
                        onRemove = { index ->
                            viewModel.removeHomeApp(index)
                            pickingSlot = null
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
                            usageState = usageState,
                            labelFor = { pkg -> apps.firstOrNull { it.packageName == pkg }?.label ?: pkg },
                            onGrantUsage = { UsagePermission.requestAccess(view.context) },
                            onEditHomeApp = { pickingSlot = it },
                            onAddHomeApp = { pickingSlot = settings.homeApps.size },
                            onCycleAlignment = { viewModel.cycleAlignment() },
                            onCycleDim = { viewModel.cycleWallpaperDim() },
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
