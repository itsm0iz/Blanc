package app.blanc.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.blanc.data.AppInfo
import app.blanc.data.prefs.AppKey
import app.blanc.data.prefs.BlancSettings
import app.blanc.data.prefs.HomeAlignment
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    apps: List<AppInfo>,
    settings: BlancSettings,
    onLaunch: (AppInfo) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onAssignSlot: (Int) -> Unit,
    onAddSlot: () -> Unit,
) {
    val horizontalAlignment = when (settings.alignment) {
        HomeAlignment.START -> Alignment.Start
        HomeAlignment.CENTER -> Alignment.CenterHorizontally
        HomeAlignment.END -> Alignment.End
    }
    val textAlign = when (settings.alignment) {
        HomeAlignment.START -> TextAlign.Start
        HomeAlignment.CENTER -> TextAlign.Center
        HomeAlignment.END -> TextAlign.End
    }

    val resolved: List<Pair<AppInfo?, AppKey>> = remember(apps, settings.homeApps) {
        settings.homeApps.map { key -> apps.firstOrNull { it.key == key } to key }
    }

    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onOpenSettings() })
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragAccumulator < -140f) onOpenDrawer()
                        dragAccumulator = 0f
                    },
                    onVerticalDrag = { _, dragAmount -> dragAccumulator += dragAmount },
                )
            }
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = horizontalAlignment,
    ) {
        Clock(horizontalAlignment)

        Spacer(Modifier.height(28.dp))

        resolved.forEachIndexed { index, (info, _) ->
            if (info != null) {
                Text(
                    text = info.label,
                    fontSize = 22.sp,
                    textAlign = textAlign,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onLaunch(info) },
                            onLongClick = { onAssignSlot(index) },
                        )
                        .padding(vertical = 10.dp),
                )
            }
        }

        if (settings.homeApps.isEmpty()) {
            Text(
                text = "Swipe up for apps\nLong-press for settings",
                fontSize = 15.sp,
                textAlign = textAlign,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (settings.homeApps.size < BlancSettings.MAX_HOME_APPS) {
            Text(
                text = "+ add app",
                fontSize = 15.sp,
                textAlign = textAlign,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = { onAddSlot() })
                    .padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun Clock(horizontalAlignment: Alignment.Horizontal) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1_000)
        }
    }
    val timeFormat = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }

    Column(horizontalAlignment = horizontalAlignment) {
        Text(
            text = timeFormat.format(now),
            fontSize = 48.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = dateFormat.format(now),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )
    }
}
