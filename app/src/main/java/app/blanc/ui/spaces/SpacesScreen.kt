package app.blanc.ui.spaces

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.blanc.data.AppInfo
import app.blanc.spaces.BlancSpace
import app.blanc.spaces.SpaceSlot
import app.blanc.spaces.SpacesConfig

@Composable
fun SpacesScreen(
    config: SpacesConfig,
    apps: List<AppInfo>,
    motionEnabled: Boolean,
    onLaunch: (AppInfo) -> Unit,
    onClose: () -> Unit,
    onOpenSetup: () -> Unit,
) {
    val appsByKey = remember(apps) { apps.associateBy { it.key } }
    val visibleSpaces = remember(config.spaces) {
        config.spaces.filter { space -> space.slots.any { it != null } }
    }
    val closeThreshold = with(LocalDensity.current) { 64.dp.toPx() }
    val entranceDistance = with(LocalDensity.current) { 18.dp.toPx() }
    val entrance = remember(motionEnabled) { Animatable(if (motionEnabled) 0f else 1f) }
    LaunchedEffect(motionEnabled) {
        if (motionEnabled) {
            entrance.snapTo(0f)
            entrance.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 155, easing = LinearOutSlowInEasing),
            )
        } else {
            entrance.snapTo(1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(onClose, closeThreshold) {
                var closeDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { closeDrag = 0f },
                    onHorizontalDrag = { change, amount ->
                        closeDrag += amount
                        change.consume()
                    },
                    onDragCancel = { closeDrag = 0f },
                    onDragEnd = {
                        if (closeDrag > closeThreshold) onClose()
                        closeDrag = 0f
                    },
                )
            },
    ) {
        LazyColumn(
            // The list is measured once. Animation touches only this hardware
            // layer, avoiding AnimatedVisibility's extra layout work.
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val progress = entrance.value
                    translationX = (1f - progress) * entranceDistance
                },
            contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "title") {
                Column(modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)) {
                    Text(
                        text = "Spaces",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Swipe right to return home",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.48f),
                    )
                }
            }

            if (visibleSpaces.isEmpty()) {
                item(key = "empty") {
                    EmptySpaces(onOpenSetup)
                }
            } else {
                items(visibleSpaces, key = { it.id }) { space ->
                    GlassSpace(
                        space = space,
                        appsByKey = appsByKey,
                        onLaunch = onLaunch,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySpaces(onOpenSetup: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Your Spaces are empty",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Set up in Blanc  →  Spaces",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onOpenSetup)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun GlassSpace(
    space: BlancSpace,
    appsByKey: Map<app.blanc.data.prefs.AppKey, AppInfo>,
    onLaunch: (AppInfo) -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shape = RoundedCornerShape(26.dp)
    val fill = if (dark) Color.Black.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.76f)
    val topFill = if (dark) Color.Black.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.84f)
    val border = if (dark) Color.White.copy(alpha = 0.13f) else Color.Black.copy(alpha = 0.09f)
    val panelBrush = remember(dark) { Brush.verticalGradient(listOf(topFill, fill)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(panelBrush, shape)
            .border(1.dp, border, shape)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Text(
            text = space.name.uppercase(),
            fontSize = 13.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            modifier = Modifier
                .padding(start = 6.dp, bottom = 7.dp)
                .semantics { heading() },
        )

        repeat(space.visibleRowCount()) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(BlancSpace.COLUMNS) { column ->
                    val slot = space.slots[row * BlancSpace.COLUMNS + column]
                    val app = slot?.let { appsByKey[it.appKey] }
                    SpaceAppCell(slot = slot, app = app, onLaunch = onLaunch)
                }
            }
        }
    }
}

@Composable
private fun RowScope.SpaceAppCell(
    slot: SpaceSlot?,
    app: AppInfo?,
    onLaunch: (AppInfo) -> Unit,
) {
    val interaction = if (app != null) remember { MutableInteractionSource() } else null
    val pressed = if (interaction != null) {
        interaction.collectIsPressedAsState().value
    } else {
        false
    }
    val baseModifier = Modifier
        .weight(1f)
        .heightIn(min = 52.dp)
        .clip(RoundedCornerShape(14.dp))
    val actionModifier = when {
        app != null -> baseModifier.clickable(
            interactionSource = requireNotNull(interaction),
            indication = null,
            role = Role.Button,
            onClick = { onLaunch(app) },
        )
        slot != null -> baseModifier.semantics {
            contentDescription = "${slot.savedLabel}, unavailable"
            disabled()
        }
        else -> baseModifier
    }

    Box(
        modifier = actionModifier.padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (slot != null) {
            Text(
                text = app?.label ?: slot.savedLabel,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = when {
                        app == null -> 0.30f
                        pressed -> 0.58f
                        else -> 0.92f
                    },
                ),
            )
        }
    }
}
