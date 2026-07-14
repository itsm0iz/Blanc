package app.blanc.util

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens a web search for [query], falling back to a search URL. */
fun Context.webSearch(query: String) {
    val trimmed = query.trim()
    val webSearch = Intent(Intent.ACTION_WEB_SEARCH).apply {
        putExtra(SearchManager.QUERY, trimmed)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        startActivity(webSearch)
    } catch (e: Exception) {
        try {
            val url = "https://www.google.com/search?q=" + Uri.encode(trimmed)
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (inner: Exception) {
            inner.printStackTrace()
        }
    }
}

/** Opens a system settings screen by intent action. */
fun Context.openSettingsAction(action: String) {
    try {
        startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/** Opens a web URL in the browser. */
fun Context.openUrl(url: String) {
    if (url.isBlank()) return
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
