package app.blanc.search

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * Fully offline word lookup backed by a bundled SQLite dictionary. The asset DB
 * is copied to internal storage once, then opened read-only; lookups are
 * indexed primary-key hits, so they're effectively instant.
 */
class Dictionary(context: Context) {

    private val appContext = context.applicationContext

    @Volatile
    private var db: SQLiteDatabase? = null

    private fun database(): SQLiteDatabase? {
        db?.let { return it }
        return synchronized(this) {
            db ?: open().also { db = it }
        }
    }

    private fun open(): SQLiteDatabase? = try {
        val file = File(appContext.filesDir, ASSET_NAME)
        if (!file.exists() || file.length() == 0L) {
            appContext.assets.open(ASSET_NAME).use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    /** Returns compact definitions for [word], or null if it isn't in the dictionary. */
    fun lookup(word: String): String? {
        val key = word.trim().lowercase()
        if (key.isEmpty()) return null
        val database = database() ?: return null
        return try {
            database.rawQuery(
                "SELECT defs FROM entries WHERE word = ? LIMIT 1",
                arrayOf(key),
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val ASSET_NAME = "dictionary.db"
    }
}
