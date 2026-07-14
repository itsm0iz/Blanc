package app.blanc.data.prefs

/**
 * A durable reference to a launchable app: package + activity class + the user
 * profile serial it belongs to. Package and class names never contain the
 * delimiters used here, so encoding is lossless.
 */
data class AppKey(
    val packageName: String,
    val className: String,
    val userSerial: Long,
) {
    fun encode(): String = "$packageName|$className|$userSerial"

    companion object {
        fun decode(value: String): AppKey? {
            val parts = value.split("|")
            if (parts.size != 3) return null
            val serial = parts[2].toLongOrNull() ?: return null
            return AppKey(parts[0], parts[1], serial)
        }
    }
}

enum class HomeAlignment { START, CENTER, END }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** All user-facing launcher settings, as one immutable snapshot. */
data class BlancSettings(
    val homeApps: List<AppKey> = emptyList(),
    val alignment: HomeAlignment = HomeAlignment.START,
    val theme: ThemeMode = ThemeMode.DARK,
    val showStatusBar: Boolean = false,
    val animations: Boolean = true,
    val haptics: Boolean = true,
    /** 0 = off, 1 = low, 2 = medium, 3 = high. Dims the wallpaper behind content. */
    val wallpaperDim: Int = 0,
    val swipeLeftApp: AppKey? = null,
    val swipeRightApp: AppKey? = null,
    /** Occasional screen-time nudge notifications. */
    val nudgesEnabled: Boolean = true,
) {
    companion object {
        const val MAX_HOME_APPS = 6
        const val MAX_WALLPAPER_DIM = 3
        val DEFAULT = BlancSettings()
    }
}
