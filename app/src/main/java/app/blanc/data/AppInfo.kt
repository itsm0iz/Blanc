package app.blanc.data

import android.os.UserHandle
import app.blanc.data.prefs.AppKey

/**
 * A launchable app activity, resolved for a specific user profile.
 *
 * [userSerial] is the stable serial number for [user], used to persist a
 * reference to this app across process restarts (a [UserHandle] itself is not
 * durable).
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val className: String,
    val user: UserHandle,
    val userSerial: Long,
) {
    val key: AppKey get() = AppKey(packageName, className, userSerial)
}
