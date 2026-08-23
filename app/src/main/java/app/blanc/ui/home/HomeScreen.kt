package app.blanc.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.blanc.data.AppInfo
import app.blanc.data.prefs.AppKey
import app.blanc.data.prefs.BlancSettings
import app.blanc.data.prefs.HomeAlignment
import app.blanc.ui.motion.BlancMotion
import app.blanc.ui.motion.rememberHapticTick
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    apps: List<AppInfo>,
    settings: BlancSettings,
    motionEnabled: Boolean,
    hapticsEnabled: Boolean,
    swipeRightApp: AppInfo?,
    onLaunch: (AppInfo) -> Unit,
    onSwipeLeft: () -> Unit,
    swipeLeftActionLabel: String?,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwipeDown: () -> Unit,
    onAssignSlot: (Int) -> Unit,
) {
    val tick = rememberHapticTick(hapticsEnabled)
    // Preserve the original gesture's ~40–48dp travel on modern phones.
    val swipeThreshold = with(LocalDensity.current) { 48.dp.toPx() }
    val latestOnLaunch by rememberUpdatedState(onLaunch)
    val latestOnSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val latestOpenDrawer by rememberUpdatedState(onOpenDrawer)
    val latestOpenSettings by rememberUpdatedState(onOpenSettings)
    val latestSwipeDown by rememberUpdatedState(onSwipeDown)
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

    var dragX by remember { mutableFloatStateOf(0f) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                customActions = swipeLeftActionLabel?.let { label ->
                    listOf(
                        CustomAccessibilityAction(label) {
                            latestOnSwipeLeft()
                            true
                        },
                    )
                } ?: emptyList()
            }
            .pointerInput(tick) {
                detectTapGestures(onLongPress = {
                    tick()
                    latestOpenSettings()
                })
            }
            .pointerInput(swipeThreshold, swipeRightApp) {
                var dragY = 0f
                detectDragGestures(
                    onDragStart = {
                        dragX = 0f
                        dragY = 0f
                    },
                    onDragEnd = {
                        if (kotlin.math.abs(dragX) > kotlin.math.abs(dragY)) {
                            if (dragX > swipeThreshold) swipeRightApp?.let(latestOnLaunch)
                            else if (dragX < -swipeThreshold) latestOnSwipeLeft()
                        } else {
                            if (dragY < -swipeThreshold) latestOpenDrawer()
                            else if (dragY > swipeThreshold) latestSwipeDown()
                        }
                        dragX = 0f
                        dragY = 0f
                    },
                    onDrag = { _, dragAmount ->
                        dragX += dragAmount.x
                        dragY += dragAmount.y
                    },
                )
            }
            // Immediate finger feedback with no layout/recomposition: only the
            // existing Home layer shifts a few pixels while dragging.
            .graphicsLayer {
                translationX = if (motionEnabled) {
                    dragX.coerceIn(-swipeThreshold, swipeThreshold) * 0.12f
                } else {
                    0f
                }
            }
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = horizontalAlignment,
    ) {
        Clock(horizontalAlignment)

        Spacer(Modifier.height(28.dp))

        resolved.forEachIndexed { index, (info, _) ->
            if (info != null) {
                HomeAppText(
                    label = info.label,
                    textAlign = textAlign,
                    motionEnabled = motionEnabled,
                    onClick = { onLaunch(info) },
                    onLongClick = {
                        tick()
                        onAssignSlot(index)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeAppText(
    label: String,
    textAlign: TextAlign,
    motionEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // The "whisper": letters spread ~1.5sp while pressed, springing back on release.
    val letterSpacing by animateFloatAsState(
        targetValue = if (pressed && motionEnabled) 1.5f else 0f,
        animationSpec = BlancMotion.WhisperSpring,
        label = "whisper",
    )
    Text(
        text = label,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = textAlign,
        letterSpacing = letterSpacing.sp,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(vertical = 10.dp),
    )
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
