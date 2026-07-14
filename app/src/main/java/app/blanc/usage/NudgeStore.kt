package app.blanc.usage

import android.content.Context

/**
 * Lightweight persistence for the nudge engine: when a nudge last fired, when an
 * affirmation last fired, and the ids shown recently (so we don't repeat them).
 * Backed by SharedPreferences — trivially small, no need for DataStore here.
 */
class NudgeStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("blanc_nudges", Context.MODE_PRIVATE)

    var lastShownAt: Long
        get() = prefs.getLong(KEY_LAST_SHOWN, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SHOWN, value).apply()

    var lastAffirmationAt: Long
        get() = prefs.getLong(KEY_LAST_AFFIRMATION, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_AFFIRMATION, value).apply()

    /** Ids shown within the recent-memory window, newest last. */
    fun recentIds(): Set<Int> =
        prefs.getString(KEY_RECENT, null)
            ?.split(',')
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()

    /** Record [id] as shown, trimming memory to the most recent [RECENT_MEMORY]. */
    fun remember(id: Int) {
        val current = prefs.getString(KEY_RECENT, null)
            ?.split(',')
            ?.mapNotNull { it.toIntOrNull() }
            ?: emptyList()
        val updated = (current + id).takeLast(RECENT_MEMORY)
        prefs.edit().putString(KEY_RECENT, updated.joinToString(",")).apply()
    }

    private companion object {
        const val KEY_LAST_SHOWN = "last_shown_at"
        const val KEY_LAST_AFFIRMATION = "last_affirmation_at"
        const val KEY_RECENT = "recent_ids"

        /** How many recent ids to avoid repeating. Well below the library size. */
        const val RECENT_MEMORY = 40
    }
}
