package app.blanc.util

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/** Opens the system UI for choosing the default home app / launcher. */
object DefaultLauncher {

    fun request(context: Context) {
        val intent = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                } else {
                    Intent(Settings.ACTION_HOME_SETTINGS)
                }
            }

            else -> Intent(Settings.ACTION_HOME_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isDefault(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(intent, 0)
        return resolved?.activityInfo?.packageName == context.packageName
    }
}
