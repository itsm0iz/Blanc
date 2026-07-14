package app.blanc.ui.motion

import android.content.Context
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Whether Blanc should animate right now. False when the user turned animations
 * off, when the system "remove animations" accessibility setting is on, or when
 * the display is low-refresh / e-ink (where motion reads as lag).
 */
@Composable
fun rememberMotionEnabled(userEnabled: Boolean): Boolean {
    val context = LocalContext.current
    return remember(userEnabled) {
        if (!userEnabled) return@remember false
        val animatorScale = try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        } catch (e: Exception) {
            1f
        }
        if (animatorScale == 0f) return@remember false
        val refreshRate = try {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .defaultDisplay.refreshRate
        } catch (e: Exception) {
            60f
        }
        refreshRate >= 30f
    }
}

/**
 * Returns a crisp "clock tick" haptic callback, or a no-op when haptics are
 * off. Respects the system haptic setting (performHapticFeedback honors it).
 */
@Composable
fun rememberHapticTick(enabled: Boolean): () -> Unit {
    val view = LocalView.current
    return remember(enabled, view) {
        {
            if (enabled) {
                try {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                } catch (e: Exception) {
                    // ignore; haptics are non-essential
                }
            }
        }
    }
}

/**
 * Staggered fade-and-slide-up entrance for a list line. Cheap: only a
 * graphicsLayer alpha + translation, so nothing relayouts. [index] drives the
 * stagger; only the first [BlancMotion.MAX_CASCADE_ITEMS] lines animate.
 */
fun Modifier.cascadeEnter(index: Int, enabled: Boolean): Modifier = composed {
    val active = enabled && index < BlancMotion.MAX_CASCADE_ITEMS
    val progress = remember { Animatable(if (active) 0f else 1f) }
    val slidePx = with(LocalDensity.current) { BlancMotion.SlideDistance.toPx() }
    LaunchedEffect(active) {
        if (active) {
            progress.snapTo(0f)
            delay(index.toLong() * BlancMotion.STAGGER_MS)
            progress.animateTo(1f, BlancMotion.ContentSpring)
        } else {
            progress.snapTo(1f)
        }
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * slidePx
    }
}

/**
 * Emphasizes the row nearest the list's vertical center — full size and opaque
 * in the middle, gently smaller and dimmer toward the edges — so scrolling feels
 * alive. Pure graphicsLayer (scale + alpha), recomputed from scroll state each
 * frame; no relayout. Scales from the left edge so left-aligned labels stay put.
 */
fun Modifier.centerEmphasis(listState: LazyListState, index: Int, enabled: Boolean): Modifier {
    if (!enabled) return this
    return graphicsLayer {
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return@graphicsLayer
        val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
        val itemCenter = item.offset + item.size / 2f
        val maxDistance = ((info.viewportEndOffset - info.viewportStartOffset) / 2f).coerceAtLeast(1f)
        val proximity = (1f - abs(itemCenter - viewportCenter) / maxDistance).coerceIn(0f, 1f)
        val scale = 0.93f + 0.07f * proximity
        scaleX = scale
        scaleY = scale
        alpha = 0.72f + 0.28f * proximity
        transformOrigin = TransformOrigin(0f, 0.5f)
    }
}

/**
 * A crisp spring "pop" from ~94% to full size with a slight overshoot, centered.
 * Used when the search field appears alongside the keyboard.
 */
fun Modifier.overshootEnter(enabled: Boolean): Modifier = composed {
    val progress = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(enabled) {
        if (enabled) {
            progress.snapTo(0f)
            progress.animateTo(1f, BlancMotion.OvershootSpring)
        } else {
            progress.snapTo(1f)
        }
    }
    graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        val scale = 0.94f + 0.06f * p
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin.Center
    }
}
