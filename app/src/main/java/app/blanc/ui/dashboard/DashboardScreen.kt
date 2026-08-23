package app.blanc.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.blanc.data.prefs.BlancSettings
import app.blanc.data.prefs.ThemeMode
import app.blanc.data.AppInfo
import app.blanc.ui.components.HomeAppsSection
import app.blanc.ui.components.SectionHeader
import app.blanc.ui.components.SettingRow
import app.blanc.ui.quicksettings.alignmentLabel
import app.blanc.ui.quicksettings.dimLabel
import app.blanc.ui.usage.UsageScreen
import app.blanc.usage.AppTotal
import app.blanc.usage.UsageReport

private const val GITHUB_URL = "https://github.com/itsm0iz/Blanc"

@Composable
fun DashboardScreen(
    initialTab: Int,
    settings: BlancSettings,
    apps: List<AppInfo>,
    homeAppNames: List<String>,
    usageGranted: Boolean,
    usageReport: UsageReport?,
    usageRangeDays: Int,
    topApps: List<AppTotal>,
    topRangeDays: Int,
    labelFor: (String) -> String,
    onGrantUsage: () -> Unit,
    onUsageRangeChange: (Int) -> Unit,
    onTopRangeChange: (Int) -> Unit,
    swipeLeftName: String,
    swipeRightName: String,
    onEditHomeApp: (Int) -> Unit,
    onAddHomeApp: () -> Unit,
    onCycleAlignment: () -> Unit,
    onCycleDim: () -> Unit,
    onEditSwipeRight: () -> Unit,
    onClearSwipeRight: () -> Unit,
    onToggleSpacesEnabled: () -> Unit,
    onToggleSpacesBlur: () -> Unit,
    onCreateSpace: (String) -> String,
    onRenameSpace: (String, String) -> Unit,
    onDeleteSpace: (String) -> Unit,
    onMoveSpace: (String, Int) -> Unit,
    onSetSpaceSlot: (String, Int, AppInfo?) -> Unit,
    onCycleTheme: () -> Unit,
    onToggleStatusBar: () -> Unit,
    onToggleAnimations: () -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleNudges: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    val tabs = listOf("Stats", "Home", "Spaces", "App")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            when (tab) {
                0 -> UsageScreen(
                    granted = usageGranted,
                    report = usageReport,
                    rangeDays = usageRangeDays,
                    topApps = topApps,
                    topRangeDays = topRangeDays,
                    labelFor = labelFor,
                    onGrantAccess = onGrantUsage,
                    onRangeChange = onUsageRangeChange,
                    onTopRangeChange = onTopRangeChange,
                )

                1 -> HomeTab(
                    settings = settings,
                    homeAppNames = homeAppNames,
                    swipeLeftName = swipeLeftName,
                    swipeRightName = swipeRightName,
                    onEditHomeApp = onEditHomeApp,
                    onAddHomeApp = onAddHomeApp,
                    onCycleAlignment = onCycleAlignment,
                    onCycleDim = onCycleDim,
                    onEditSwipeRight = onEditSwipeRight,
                    onClearSwipeRight = onClearSwipeRight,
                    onOpenSpacesTab = { tab = 2 },
                )

                2 -> SpacesTab(
                    config = settings.spaces,
                    apps = apps,
                    onToggleEnabled = onToggleSpacesEnabled,
                    onToggleBlur = onToggleSpacesBlur,
                    onCreate = onCreateSpace,
                    onRename = onRenameSpace,
                    onDelete = onDeleteSpace,
                    onMove = onMoveSpace,
                    onSetSlot = onSetSpaceSlot,
                )

                else -> AppTab(
                    settings = settings,
                    onCycleTheme = onCycleTheme,
                    onToggleStatusBar = onToggleStatusBar,
                    onToggleAnimations = onToggleAnimations,
                    onToggleHaptics = onToggleHaptics,
                    onToggleNudges = onToggleNudges,
                    onSetDefaultLauncher = onSetDefaultLauncher,
                    onOpenUrl = onOpenUrl,
                )
            }
        }
    }
}

@Composable
private fun HomeTab(
    settings: BlancSettings,
    homeAppNames: List<String>,
    swipeLeftName: String,
    swipeRightName: String,
    onEditHomeApp: (Int) -> Unit,
    onAddHomeApp: () -> Unit,
    onCycleAlignment: () -> Unit,
    onCycleDim: () -> Unit,
    onEditSwipeRight: () -> Unit,
    onClearSwipeRight: () -> Unit,
    onOpenSpacesTab: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        SectionHeader("Home apps")
        HomeAppsSection(homeAppNames, onEditHomeApp, onAddHomeApp)

        SectionHeader("Layout")
        SettingRow("Alignment", alignmentLabel(settings.alignment), onCycleAlignment)
        SettingRow("Dim wallpaper", dimLabel(settings.wallpaperDim), onCycleDim)

        SectionHeader("Gestures")
        SettingRow(
            "Swipe left",
            if (settings.spaces.enabled) "Spaces" else swipeLeftName,
            onClick = onOpenSpacesTab,
        )
        SettingRow("Swipe right app", swipeRightName, onClick = onEditSwipeRight, onLongClick = onClearSwipeRight)
        Text(
            text = "Manage Spaces in its tab. Long-press the right gesture to clear it. Swipe up opens search, down opens notifications.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun AppTab(
    settings: BlancSettings,
    onCycleTheme: () -> Unit,
    onToggleStatusBar: () -> Unit,
    onToggleAnimations: () -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleNudges: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        SectionHeader("Appearance")
        SettingRow("Theme", themeLabel(settings.theme), onCycleTheme)
        SettingRow("Status bar", if (settings.showStatusBar) "On" else "Off", onToggleStatusBar)
        SettingRow("Animations", if (settings.animations) "On" else "Off", onToggleAnimations)
        SettingRow("Haptics", if (settings.haptics) "On" else "Off", onToggleHaptics)

        SectionHeader("Screen time")
        SettingRow("Nudges", if (settings.nudgesEnabled) "On" else "Off", onToggleNudges)
        Text(
            text = "Occasional, gentle reminders about your usage — social warnings, weekly wins, and what all that time could become.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 6.dp),
        )

        SectionHeader("Launcher")
        SettingRow("Set Blanc as default", onClick = onSetDefaultLauncher)

        SectionHeader("About")
        SettingRow("Version", "0.2.0-spaces")
        SettingRow("Source code", "GitHub", onClick = { onOpenUrl(GITHUB_URL) })
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Dictionary: Wordset (CC BY-SA 4.0, from WordNet 3.0). Blanc is MIT licensed.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(24.dp))
    }
}

private fun themeLabel(theme: ThemeMode): String = when (theme) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
