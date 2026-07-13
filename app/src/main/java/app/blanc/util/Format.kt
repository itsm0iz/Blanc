package app.blanc.util

/** Formats a millisecond duration as a compact screen-time label, e.g. "2h 15m". */
fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0m"
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}
