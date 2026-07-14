package app.blanc.usage

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/**
 * One row per (day, app): the foreground time that app accumulated on that day.
 *
 * We persist our own daily rollups because the Android usage-stats API only
 * retains a rolling window of recent history. Recording daily lets Blanc show
 * long-range trends and projections the system alone cannot.
 */
@Entity(tableName = "daily_usage", primaryKeys = ["epochDay", "packageName"])
data class DailyUsage(
    val epochDay: Long,
    val packageName: String,
    val totalMs: Long,
)

/** Aggregate of total foreground time on a single day, across all apps. */
data class DayTotal(
    val epochDay: Long,
    val totalMs: Long,
)

/** Aggregate of one app's foreground time over a range of days. */
data class AppTotal(
    val packageName: String,
    val totalMs: Long,
)

@Dao
interface UsageDao {

    @Upsert
    suspend fun upsertAll(rows: List<DailyUsage>)

    @Query(
        "SELECT epochDay, SUM(totalMs) AS totalMs FROM daily_usage " +
            "WHERE epochDay BETWEEN :startDay AND :endDay GROUP BY epochDay ORDER BY epochDay"
    )
    suspend fun dailyTotals(startDay: Long, endDay: Long): List<DayTotal>

    @Query(
        "SELECT packageName, SUM(totalMs) AS totalMs FROM daily_usage " +
            "WHERE epochDay BETWEEN :startDay AND :endDay " +
            "GROUP BY packageName ORDER BY totalMs DESC LIMIT :limit"
    )
    suspend fun topApps(startDay: Long, endDay: Long, limit: Int): List<AppTotal>

    @Query(
        "SELECT packageName, SUM(totalMs) AS totalMs FROM daily_usage " +
            "WHERE epochDay BETWEEN :startDay AND :endDay " +
            "GROUP BY packageName ORDER BY totalMs DESC"
    )
    suspend fun appTotals(startDay: Long, endDay: Long): List<AppTotal>
}

@Database(entities = [DailyUsage::class], version = 1, exportSchema = false)
abstract class UsageDatabase : RoomDatabase() {

    abstract fun usageDao(): UsageDao

    companion object {
        @Volatile
        private var instance: UsageDatabase? = null

        fun get(context: Context): UsageDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    UsageDatabase::class.java,
                    "blanc_usage.db",
                ).build().also { instance = it }
            }
    }
}
