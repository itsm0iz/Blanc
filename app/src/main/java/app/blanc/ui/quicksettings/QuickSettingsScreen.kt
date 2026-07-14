package app.blanc.ui.quicksettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.blanc.data.prefs.BlancSettings
import app.blanc.data.prefs.HomeAlignment
import app.blanc.ui.components.HomeAppsSection
import app.blanc.ui.components.SectionHeader
import app.blanc.ui.components.SettingRow

@Composable
fun QuickSettingsScreen(
    settings: BlancSettings,
    homeAppNames: List<String>,
    onEditHomeApp: (Int) -> Unit,
    onAddHomeApp: () -> Unit,
    onCycleAlignment: () -> Unit,
    onCycleDim: () -> Unit,
    onOpenDashboard: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Text(
            text = "Blanc",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        SectionHeader("Home apps")
        HomeAppsSection(homeAppNames, onEditHomeApp, onAddHomeApp)

        SectionHeader("Layout")
        SettingRow("Alignment", alignmentLabel(settings.alignment), onCycleAlignment)
        SettingRow("Dim wallpaper", dimLabel(settings.wallpaperDim), onCycleDim)

        Spacer(Modifier.height(28.dp))
        SettingRow("All settings & screen time  →", onClick = onOpenDashboard)
    }
}

internal fun alignmentLabel(alignment: HomeAlignment): String = when (alignment) {
    HomeAlignment.START -> "Left"
    HomeAlignment.CENTER -> "Center"
    HomeAlignment.END -> "Right"
}

internal fun dimLabel(level: Int): String = when (level) {
    0 -> "Off"
    1 -> "Low"
    2 -> "Medium"
    else -> "High"
}
