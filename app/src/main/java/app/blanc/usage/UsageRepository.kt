package app.blanc.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** One category's total time over the report range. */
data class CategorySlice(val category: AppCategory, val totalMs: Long)

/** Minimal usage signals the nudge engine reasons about. */
data class NudgeContext(
    val socialsTodayMs: Long,
    val thisWeekMs: Long,
    val lastWeekMs: Long,
    val projectedYearMs: Long,
)

/** A full snapshot for the stats screen over a chosen range of days. */
data class UsageReport(
    val rangeDays: Int,
    val todayMs: Long,
    val rangeTotalMs: Long,
    val avgPerDayMs: Long,
    val dailyTotals: List<DayTotal>,
    val categories: List<CategorySlice>,
    val topApps: List<AppTotal>,
    val thisWeekMs: Long,
    val lastWeekMs: Long,
    val weekChangePercent: Int,
    val projectedYearMs: Long,
) {
    val socialsWeekMs: Long
        get() = categories.firstOrNull { it.category == AppCategory.SOCIAL }?.totalMs ?: 0L
}

/**
 * Records daily foreground-time rollups into [UsageDatabase] and derives a
 * [UsageReport]. Recording pulls from the system's recent window; the local DB
 * accumulates long-term history the system discards.
 */
class UsageRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = UsageDatabase.get(appContext).usageDao()
    private val usageStatsManager =
        appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val categoryResolver = CategoryResolver(appContext)

    /** Backfill the last [days] days of per-app usage into the local DB. */
    suspend fun record(days: Int = 7) = withContext(Dispatchers.IO) {
        if (!UsagePermission.isGranted(appContext)) return@withContext
        purgeLegacyOverCountOnce()

        val now = System.currentTimeMillis()
        val midnight = startOfToday()
        val rows = ArrayList<DailyUsage>()

        for (offset in 0 until days) {
            val dayStart = midnight - offset * ONE_DAY_MS
            val dayEnd = min(dayStart + ONE_DAY_MS, now)
            if (dayEnd <= dayStart) continue

            val epochDay = epochDayOf(dayStart)
            for ((packageName, foreground) in foregroundTimeByPackage(dayStart, dayEnd)) {
                if (foreground <= 0) continue
                rows.add(DailyUsage(epochDay, packageName, foreground))
            }
        }

        if (rows.isNotEmpty()) dao.upsertAll(rows)
    }

    /**
     * Foreground (on-screen) time per package within `[start, end)`, computed
     * from raw usage events and strictly clipped to the window.
     *
     * The model mirrors how the system's own Digital Wellbeing measures screen
     * time, and holds to two facts about a phone: only one app is visible at a
     * time, and nothing is visible while the screen is off. So the "current"
     * foreground app is closed out when another app comes forward, when it moves
     * to the background, or when the screen turns off / locks. Without the
     * screen-off and hand-off closes, a background app that briefly flashes an
     * activity (e.g. an alarm) but never logs a matching background event would
     * be counted as foreground for hours — the runaway this replaces.
     *
     * Every interval is bounded by the window, so a partial day can never report
     * more time than has actually elapsed.
     */
    @Suppress("DEPRECATION") // MOVE_TO_* share values with ACTIVITY_* and work on all API levels.
    private fun foregroundTimeByPackage(start: Long, end: Long): Map<String, Long> {
        val events = try {
            usageStatsManager.queryEvents(start, end)
        } catch (e: Exception) {
            return emptyMap()
        } ?: return emptyMap()

        val totals = HashMap<String, Long>()
        var currentPkg: String? = null
        var currentSince = start
        val event = UsageEvents.Event()

        fun close(at: Long) {
            val pkg = currentPkg ?: return
            val delta = at.coerceIn(start, end) - currentSince
            if (delta > 0) totals[pkg] = (totals[pkg] ?: 0L) + delta
            currentPkg = null
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    val pkg = event.packageName
                    if (pkg != null && pkg != currentPkg) {
                        close(event.timeStamp)          // previous app lost the screen
                        currentPkg = pkg
                        currentSince = event.timeStamp.coerceIn(start, end)
                    }
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND ->
                    if (event.packageName == currentPkg) close(event.timeStamp)

                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_SHOWN,
                UsageEvents.Event.DEVICE_SHUTDOWN -> close(event.timeStamp)
            }
        }

        // Whatever is still on screen at the window's edge (e.g. the app open
        // right now) is counted up to that edge.
        close(end)

        return totals
    }

    /**
     * One-time wipe of history recorded by the old, over-counting method so the
     * stats screen doesn't keep showing inflated pre-fix days. Recording then
     * rebuilds the recent window from the accurate event-based measurement.
     */
    private suspend fun purgeLegacyOverCountOnce() {
        val prefs = appContext.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_PURGED_LEGACY, false)) return
        dao.clearAll()
        prefs.edit().putBoolean(KEY_PURGED_LEGACY, true).apply()
    }

    suspend fun report(rangeDays: Int): UsageReport = withContext(Dispatchers.IO) {
        val todayEpoch = epochDayOf(startOfToday())
        val rangeStart = todayEpoch - (rangeDays - 1)

        val dailyByDay = dao.dailyTotals(rangeStart, todayEpoch).associateBy { it.epochDay }
        val daily = (rangeStart..todayEpoch).map { day -> dailyByDay[day] ?: DayTotal(day, 0L) }
        val rangeTotal = daily.sumOf { it.totalMs }
        val daysWithData = daily.count { it.totalMs > 0 }
        val avgPerDay = rangeTotal / max(1, daysWithData)

        val appTotals = dao.appTotals(rangeStart, todayEpoch)
        val categoryTotals = HashMap<AppCategory, Long>()
        for (app in appTotals) {
            val category = categoryResolver.categoryOf(app.packageName)
            categoryTotals[category] = (categoryTotals[category] ?: 0L) + app.totalMs
        }
        val categories = categoryTotals
            .map { CategorySlice(it.key, it.value) }
            .filter { it.totalMs > 0 }
            .sortedByDescending { it.totalMs }

        // Week-over-week using the last 14 days.
        val twoWeeks = dao.dailyTotals(todayEpoch - 13, todayEpoch).associateBy { it.epochDay }
        val thisWeek = (todayEpoch - 6..todayEpoch).sumOf { twoWeeks[it]?.totalMs ?: 0L }
        val lastWeek = (todayEpoch - 13..todayEpoch - 7).sumOf { twoWeeks[it]?.totalMs ?: 0L }
        val weekChange = when {
            lastWeek > 0 -> (((thisWeek - lastWeek).toDouble() / lastWeek) * 100).roundToInt()
            thisWeek > 0 -> 100
            else -> 0
        }

        UsageReport(
            rangeDays = rangeDays,
            todayMs = dailyByDay[todayEpoch]?.totalMs ?: 0L,
            rangeTotalMs = rangeTotal,
            avgPerDayMs = avgPerDay,
            dailyTotals = daily,
            categories = categories,
            topApps = appTotals.take(8),
            thisWeekMs = thisWeek,
            lastWeekMs = lastWeek,
            weekChangePercent = weekChange,
            projectedYearMs = avgPerDay * 365,
        )
    }

    /**
     * The minimal, self-contained signals the nudge engine reasons about. Kept
     * independent of the currently-selected report range so a background worker
     * can evaluate usage without touching the UI's state.
     */
    suspend fun nudgeContext(): NudgeContext = withContext(Dispatchers.IO) {
        val todayEpoch = epochDayOf(startOfToday())

        // Today's social-app foreground time.
        var socialsToday = 0L
        for (app in dao.appTotals(todayEpoch, todayEpoch)) {
            if (categoryResolver.categoryOf(app.packageName) == AppCategory.SOCIAL) {
                socialsToday += app.totalMs
            }
        }

        // This week vs. last week, from the last 14 days.
        val twoWeeks = dao.dailyTotals(todayEpoch - 13, todayEpoch).associateBy { it.epochDay }
        val thisWeek = (todayEpoch - 6..todayEpoch).sumOf { twoWeeks[it]?.totalMs ?: 0L }
        val lastWeek = (todayEpoch - 13..todayEpoch - 7).sumOf { twoWeeks[it]?.totalMs ?: 0L }

        // Yearly projection from a 30-day average over days that have data.
        val month = dao.dailyTotals(todayEpoch - 29, todayEpoch)
        val monthTotal = month.sumOf { it.totalMs }
        val daysWithData = month.count { it.totalMs > 0 }
        val avgPerDay = monthTotal / max(1, daysWithData)

        NudgeContext(
            socialsTodayMs = socialsToday,
            thisWeekMs = thisWeek,
            lastWeekMs = lastWeek,
            projectedYearMs = avgPerDay * 365,
        )
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun epochDayOf(millis: Long): Long {
        val offset = Calendar.getInstance().timeZone.getOffset(millis)
        return TimeUnit.MILLISECONDS.toDays(millis + offset)
    }

    private companion object {
        const val ONE_DAY_MS = 24L * 60 * 60 * 1000
        const val META_PREFS = "blanc_usage_meta"
        // Bumped when the measurement changes so stale, mis-measured history is
        // wiped once and rebuilt with the current method.
        const val KEY_PURGED_LEGACY = "purged_overcount_v2"
    }
}
