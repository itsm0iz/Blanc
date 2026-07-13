package app.blanc.ui.motion

import android.content.Context
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay

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
