package app.blanc.usage

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.compose.ui.graphics.Color

/** Coarse app categories for grouping usage, each with a chart color. */
enum class AppCategory(val displayName: String, val color: Color) {
    SOCIAL("Social", Color(0xFF5B8DEF)),
    GAMES("Games", Color(0xFFB06BE6)),
    VIDEO("Video", Color(0xFFEF5B7B)),
    MUSIC("Music", Color(0xFF3FBF8F)),
    PRODUCTIVITY("Productivity", Color(0xFFEFA13F)),
    NEWS("News", Color(0xFF3FBFBF)),
    OTHER("Other", Color(0xFF8A8A8A)),
}

/** Resolves a package's [AppCategory] from Android's declared app category (API 26+). */
class CategoryResolver(context: Context) {

    private val packageManager = context.applicationContext.packageManager
    private val cache = HashMap<String, AppCategory>()

    fun categoryOf(packageName: String): AppCategory =
        cache.getOrPut(packageName) { resolve(packageName) }

    private fun resolve(packageName: String): AppCategory {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return AppCategory.OTHER
        return try {
            when (packageManager.getApplicationInfo(packageName, 0).category) {
                ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
                ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
                ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
                ApplicationInfo.CATEGORY_AUDIO -> AppCategory.MUSIC
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
                ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
                else -> AppCategory.OTHER
            }
        } catch (e: Exception) {
            AppCategory.OTHER
        }
    }
}
