package app.blanc.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import app.blanc.data.prefs.AppKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * Source of truth for the installed launchable apps. Enumerates activities
 * across every user profile (main + work + private) and exposes them as a
 * [Flow] that re-emits whenever packages are added, removed, or changed.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = appContext.getSystemService(Context.USER_SERVICE) as UserManager
    private val myPackage = appContext.packageName

    fun appsFlow(): Flow<List<AppInfo>> = callbackFlow {
        trySend(loadApps())

        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
                trySend(loadApps())
            }

            override fun onPackageAdded(packageName: String?, user: UserHandle?) {
                trySend(loadApps())
            }

            override fun onPackageChanged(packageName: String?, user: UserHandle?) {
                trySend(loadApps())
            }

            override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
                trySend(loadApps())
            }

            override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
                trySend(loadApps())
            }
        }

        // registerCallback builds a Handler on the calling thread, so it must
        // run against a Looper. This flow is on Dispatchers.IO (no Looper), so
        // hand it an explicit main-thread Handler for callback delivery.
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
    }.flowOn(Dispatchers.IO)

    private fun loadApps(): List<AppInfo> {
        val apps = ArrayList<AppInfo>()
        for (profile in userManager.userProfiles) {
            val serial = userManager.getSerialNumberForUser(profile)
            val activities = try {
                launcherApps.getActivityList(null, profile)
            } catch (e: Exception) {
                continue
            }
            for (activity in activities) {
                val packageName = activity.applicationInfo.packageName
                if (packageName == myPackage) continue
                apps.add(
                    AppInfo(
                        label = activity.label.toString(),
                        packageName = packageName,
                        className = activity.componentName.className,
                        user = profile,
                        userSerial = serial,
                    )
                )
            }
        }
        return apps.sortedBy { it.label.lowercase() }
    }

    fun launch(app: AppInfo) {
        try {
            launcherApps.startMainActivity(
                ComponentName(app.packageName, app.className),
                app.user,
                null,
                null,
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun userForSerial(serial: Long): UserHandle =
        userManager.getUserForSerialNumber(serial) ?: Process.myUserHandle()

    fun findByKey(apps: List<AppInfo>, key: AppKey): AppInfo? =
        apps.firstOrNull { it.key == key }
}
