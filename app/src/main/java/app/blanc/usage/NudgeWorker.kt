package app.blanc.usage

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.blanc.data.prefs.SettingsStore
import java.util.concurrent.TimeUnit

/**
 * Periodically evaluates usage and — occasionally — posts a fitting, non-
 * repeating nudge. Runs a few times a day, but [NudgeEngine]'s rate limit keeps
 * actual notifications to roughly one per day at most. Bails quietly whenever
 * nudges are disabled, usage access is missing, or notifications aren't allowed.
 */
class NudgeWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext

            if (!SettingsStore(context).current().nudgesEnabled) return Result.success()
            if (!UsagePermission.isGranted(context)) return Result.success()
            if (!NudgeNotifier.canPost(context)) return Result.success()

            val repository = UsageRepository(context)
            repository.record()

            val store = NudgeStore(context)
            val now = System.currentTimeMillis()
            val selected = NudgeEngine().pick(repository.nudgeContext(), store, now)
                ?: return Result.success()

            NudgeNotifier.show(context, selected.text)
            store.remember(selected.id)
            store.lastShownAt = now
            if (selected.trigger == NudgeTrigger.AFFIRMATION) store.lastAffirmationAt = now

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "blanc_nudges"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NudgeWorker>(12, TimeUnit.HOURS)
                .setInitialDelay(6, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
