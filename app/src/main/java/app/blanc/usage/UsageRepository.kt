package app.blanc.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/** A snapshot of usage data for the usage-monitor screen. */
data class UsageState(
    val todayMs: Long,
    val avgPerDayMs: Long,
    val projectedYearMs: Long,
    val last7Days: List<DayTotal>,
    val topApps: List<AppTotal>,
)

/**
 * Records daily foreground-time rollups into [UsageDatabase] and derives the
 * [UsageState] shown to the user. Recording pulls from the system's recent
 * window (a handful of days); the local DB is what accumulates long-term.
 */
class UsageRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = UsageDatabase.get(appContext).usageDao()
    private val usageStatsManager =
        appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val myPackage = appContext.packageName

    /** Backfill the last [days] days of per-app usage into the local DB. */
    suspend fun record(days: Int = 7) = withContext(Dispatchers.IO) {
        if (!UsagePermission.isGranted(appContext)) return@withContext
        val now = System.currentTimeMillis()
        val midnight = startOfToday()
        val rows = ArrayList<DailyUsage>()

        for (offset in 0 until days) {
            val dayStart = midnight - offset * ONE_DAY_MS
            val dayEnd = min(dayStart + ONE_DAY_MS, now)
            if (dayEnd <= dayStart) continue

            val aggregated = try {
                usageStatsManager.queryAndAggregateUsageStats(dayStart, dayEnd)
            } catch (e: Exception) {
                continue
            } ?: continue

            val epochDay = epochDayOf(dayStart)
            for ((packageName, stats) in aggregated) {
                val foreground = stats.totalTimeInForeground
                if (foreground <= 0 || packageName == myPackage) continue
                rows.add(DailyUsage(epochDay, packageName, foreground))
            }
        }

        if (rows.isNotEmpty()) dao.upsertAll(rows)
    }

    /** Compute the current [UsageState] from the local DB. */
    suspend fun state(): UsageState = withContext(Dispatchers.IO) {
        val todayEpoch = epochDayOf(startOfToday())
        val weekStart = todayEpoch - 6

        val dailyTotals = dao.dailyTotals(weekStart, todayEpoch)
        val byDay = dailyTotals.associateBy { it.epochDay }

        // Fill every day in the window so the chart has 7 bars.
        val last7 = (weekStart..todayEpoch).map { day ->
            byDay[day] ?: DayTotal(day, 0L)
        }

        val daysWithData = dailyTotals.count { it.totalMs > 0 }
        val weekSum = dailyTotals.sumOf { it.totalMs }
        val avgPerDay = weekSum / max(1, daysWithData)

        UsageState(
            todayMs = byDay[todayEpoch]?.totalMs ?: 0L,
            avgPerDayMs = avgPerDay,
            projectedYearMs = avgPerDay * 365,
            last7Days = last7,
            topApps = dao.topApps(weekStart, todayEpoch, 8),
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
    }
}
