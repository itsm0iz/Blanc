package app.blanc.ui.spaces

import android.app.Activity
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import java.util.function.Consumer

/**
 * Applies one fixed, system-level wallpaper blur while Spaces is visible.
 * Android may disable cross-window blur at runtime (including in battery saver),
 * so callers receive whether the blur is actually active and render a stronger
 * glass fallback when it is not.
 */
@Composable
fun rememberSpacesWindowBlur(active: Boolean, requested: Boolean): Boolean {
    if (!active || !requested || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    return rememberSpacesWindowBlurApi31(active, requested)
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun rememberSpacesWindowBlurApi31(active: Boolean, requested: Boolean): Boolean {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: return false
    val windowManager = remember(view.context) {
        view.context.getSystemService(WindowManager::class.java)
    }
    var supported by remember(windowManager) {
        mutableStateOf(windowManager.isCrossWindowBlurEnabled)
    }

    DisposableEffect(windowManager) {
        val listener = Consumer<Boolean> { enabled ->
            // The listener normally runs on the registering thread; post keeps
            // the Compose state update safe on devices that dispatch elsewhere.
            view.post { supported = enabled }
        }
        windowManager.addCrossWindowBlurEnabledListener(listener)
        onDispose { windowManager.removeCrossWindowBlurEnabledListener(listener) }
    }

    val shouldBlur = active && requested && supported
    DisposableEffect(window, shouldBlur) {
        setWallpaperBlur(window, if (shouldBlur) BLUR_RADIUS else 0)
        onDispose { setWallpaperBlur(window, 0) }
    }
    return shouldBlur
}

@RequiresApi(Build.VERSION_CODES.S)
private fun setWallpaperBlur(window: Window, radius: Int) {
    val attributes = window.attributes
    attributes.setBlurBehindRadius(radius)
    window.attributes = attributes
    if (radius > 0) {
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    }
}

private const val BLUR_RADIUS = 24
