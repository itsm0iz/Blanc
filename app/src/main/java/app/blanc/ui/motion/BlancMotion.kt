package app.blanc.ui.motion

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp

/**
 * Central motion tokens for Blanc. Everything animated reads from here, so the
 * whole feel is tunable in one place. All springs are physics-based and
 * interruption-safe (an [androidx.compose.animation.core.Animatable] carries its
 * velocity when retargeted mid-flight).
 */
object BlancMotion {

    /** Delay between consecutive lines in the staggered cascade. */
    const val STAGGER_MS = 12L

    /** Beyond this many leading lines, cascade items share the final stagger delay. */
    const val CASCADE_STAGGER_CAP = 8

    /** How far each line slides up as it fades in. */
    val SlideDistance = 10.dp

    /** Crisp, critically damped spring for content settling (no overshoot). */
    val ContentSpring: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 1600f)

    /** Underdamped spring with a slight overshoot, for the search "pop". */
    val OvershootSpring: SpringSpec<Float> = spring(dampingRatio = 0.55f, stiffness = 1500f)

    /** Springy spring for the long-press letter-spacing whisper. */
    val WhisperSpring: SpringSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 1400f)
}
