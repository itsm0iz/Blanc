package app.blanc.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.blanc.data.prefs.BlancSettings
import app.blanc.data.prefs.HomeAlignment
import app.blanc.data.prefs.ThemeMode

@Composable
fun SettingsScreen(
    settings: BlancSettings,
    onCycleAlignment: () -> Unit,
    onCycleTheme: () -> Unit,
    onToggleStatusBar: () -> Unit,
    onToggleAnimations: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onOpenUsage: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(20.dp))

        SettingRow(
            label = "Alignment",
            value = when (settings.alignment) {
                HomeAlignment.START -> "Left"
                HomeAlignment.CENTER -> "Center"
                HomeAlignment.END -> "Right"
            },
            onClick = onCycleAlignment,
        )

        SettingRow(
            label = "Theme",
            value = when (settings.theme) {
                ThemeMode.SYSTEM -> "System"
                ThemeMode.LIGHT -> "Light"
                ThemeMode.DARK -> "Dark"
            },
            onClick = onCycleTheme,
        )

        SettingRow(
            label = "Status bar",
            value = if (settings.showStatusBar) "On" else "Off",
            onClick = onToggleStatusBar,
        )

        SettingRow(
            label = "Animations",
            value = if (settings.animations) "On" else "Off",
            onClick = onToggleAnimations,
        )

        SettingRow(
            label = "Set Blanc as default",
            value = "",
            onClick = onSetDefaultLauncher,
        )

        SettingRow(
            label = "Usage monitor",
            value = "›",
            onClick = onOpenUsage,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Blanc v0.1.0",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
    }
}
