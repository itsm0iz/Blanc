package app.blanc

import android.app.Application
import android.util.Log
import java.io.File

/**
 * Installs a process-wide crash recorder. On any uncaught exception (including
 * ones thrown during Compose composition), the stack trace is written to a file
 * that [MainActivity] shows on the next launch — turning a crash loop into a
 * readable, screenshottable error instead of a silent flicker.
 */
class BlancApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val report = buildString {
                    append("Blanc crashed\n\n")
                    append(Log.getStackTraceString(throwable))
                }
                File(filesDir, CRASH_FILE).writeText(report)
            } catch (_: Throwable) {
                // Best effort; fall through to the default handler regardless.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        const val CRASH_FILE = "last_crash.txt"
    }
}
