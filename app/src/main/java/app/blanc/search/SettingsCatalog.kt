package app.blanc.search

import android.provider.Settings

/** A quick-launchable system settings screen. */
data class SettingEntry(val label: String, val action: String)

/** Static catalog of common settings screens, searchable by label. */
object SettingsCatalog {

    private val entries = listOf(
        SettingEntry("Wi-Fi", Settings.ACTION_WIFI_SETTINGS),
        SettingEntry("Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS),
        SettingEntry("Airplane mode", Settings.ACTION_AIRPLANE_MODE_SETTINGS),
        SettingEntry("Mobile data", Settings.ACTION_DATA_ROAMING_SETTINGS),
        SettingEntry("Display", Settings.ACTION_DISPLAY_SETTINGS),
        SettingEntry("Sound", Settings.ACTION_SOUND_SETTINGS),
        SettingEntry("Battery", Settings.ACTION_BATTERY_SAVER_SETTINGS),
        SettingEntry("Storage", Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        SettingEntry("Location", Settings.ACTION_LOCATION_SOURCE_SETTINGS),
        SettingEntry("Apps", Settings.ACTION_APPLICATION_SETTINGS),
        SettingEntry("Notifications", "android.settings.NOTIFICATION_SETTINGS"),
        SettingEntry("Date & time", Settings.ACTION_DATE_SETTINGS),
        SettingEntry("Language & input", Settings.ACTION_LOCALE_SETTINGS),
        SettingEntry("Accessibility", Settings.ACTION_ACCESSIBILITY_SETTINGS),
        SettingEntry("Security", Settings.ACTION_SECURITY_SETTINGS),
        SettingEntry("Developer options", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
        SettingEntry("Default apps", Settings.ACTION_HOME_SETTINGS),
        SettingEntry("All settings", Settings.ACTION_SETTINGS),
    )

    fun search(query: String): List<SettingEntry> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        return entries.filter { it.label.contains(q, ignoreCase = true) }
    }
}
