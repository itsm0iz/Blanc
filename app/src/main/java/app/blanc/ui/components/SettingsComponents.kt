package app.blanc.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.blanc.data.prefs.BlancSettings

/** A small, grey, spaced-out section label. */
@Composable
fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        modifier = Modifier.padding(top = 22.dp, bottom = 4.dp),
    )
}

/** A tappable row: bold label on the left, optional grey value on the right. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingRow(
    label: String,
    value: String = "",
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
    }
}

/** The shared "manage home apps" list: one row per app + an add row. */
@Composable
fun HomeAppsSection(
    homeAppNames: List<String>,
    onEdit: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    homeAppNames.forEachIndexed { index, name ->
        SettingRow(
            label = name.ifBlank { "App ${index + 1}" },
            value = "Edit",
            onClick = { onEdit(index) },
        )
    }
    if (homeAppNames.size < BlancSettings.MAX_HOME_APPS) {
        SettingRow(label = "+ Add app", onClick = onAdd)
    }
}
