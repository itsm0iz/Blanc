package app.blanc.usage

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/** A chosen, fully-formatted nudge ready to post. */
data class SelectedNudge(val id: Int, val trigger: NudgeTrigger, val text: String)

/**
 * Turns a [NudgeContext] into an occasional, fitting, non-repeating message —
 * or decides to stay quiet. All thresholds live here as constants so tuning the
 * behaviour never touches the worker or the message library.
 */
class NudgeEngine {

    /**
     * Pick a nudge to show [now], or null to stay silent. [store] provides the
     * rate-limit timestamps and recent-id memory. The caller is responsible for
     * persisting the result (remember id, update timestamps).
     */
    fun pick(ctx: NudgeContext, store: NudgeStore, now: Long): SelectedNudge? {
        if (now - store.lastShownAt < MIN_INTERVAL_MS) return null

        val trigger = chooseTrigger(ctx, store, now) ?: return null
        val pool = nudgesFor(trigger)
        if (pool.isEmpty()) return null

        // Prefer messages not shown recently; fall back to the full pool once
        // every message of this trigger has been seen inside the memory window.
        val recent = store.recentIds()
        val fresh = pool.filterNot { it.id in recent }.ifEmpty { pool }
        val nudge = fresh[Random.nextInt(fresh.size)]

        return SelectedNudge(nudge.id, trigger, nudge.format(buildVars(ctx)))
    }

    private fun chooseTrigger(ctx: NudgeContext, store: NudgeStore, now: Long): NudgeTrigger? {
        // 1. A heavy social day is the strongest, most actionable signal.
        if (ctx.socialsTodayMs >= SOCIAL_WARNING_MS) return NudgeTrigger.SOCIAL_WARNING

        // 2. A genuinely lighter week earns an affirmation — but at most weekly.
        if (ctx.lastWeekMs > 0 && ctx.thisWeekMs < ctx.lastWeekMs) {
            val drop = (ctx.lastWeekMs - ctx.thisWeekMs) * 100.0 / ctx.lastWeekMs
            val affirmationDue = now - store.lastAffirmationAt >= AFFIRMATION_INTERVAL_MS
            if (drop >= AFFIRMATION_MIN_DROP_PERCENT && affirmationDue) return NudgeTrigger.AFFIRMATION
        }

        // 3. Otherwise an occasional fact. Projections need enough history to be
        //    meaningful; when they are, alternate with general awareness so it's
        //    not always a wall of numbers.
        return if (ctx.projectedYearMs >= PROJECTION_MIN_YEAR_MS && Random.nextBoolean()) {
            NudgeTrigger.PROJECTION
        } else {
            NudgeTrigger.GENERAL
        }
    }

    private fun buildVars(ctx: NudgeContext): Map<String, String> {
        val socialHours = ctx.socialsTodayMs.toDouble() / HOUR_MS
        val weekHours = ctx.thisWeekMs.toDouble() / HOUR_MS
        val yearHours = ctx.projectedYearMs.toDouble() / HOUR_MS
        val yearDays = ctx.projectedYearMs.toDouble() / DAY_MS
        val dropPercent = if (ctx.lastWeekMs > 0) {
            abs((ctx.lastWeekMs - ctx.thisWeekMs) * 100.0 / ctx.lastWeekMs)
        } else {
            0.0
        }
        return mapOf(
            "socialHours" to oneDecimal(socialHours),
            "weekHours" to weekHours.roundToInt().toString(),
            // Round yearly hours to the nearest 10 so it reads as the estimate it is.
            "yearHours" to (yearHours / 10.0).roundToInt().times(10).toString(),
            "yearDays" to yearDays.roundToInt().toString(),
            "percent" to dropPercent.roundToInt().toString(),
        )
    }

    private fun oneDecimal(value: Double): String {
        val rounded = (value * 10).roundToInt()
        return "${rounded / 10}.${rounded % 10}"
    }

    private companion object {
        const val HOUR_MS = 60.0 * 60 * 1000
        const val DAY_MS = 24 * HOUR_MS

        /** Never post two nudges closer than this — keeps them "occasional". */
        const val MIN_INTERVAL_MS = 20L * 60 * 60 * 1000 // 20 hours

        /** A day of social use at or above this triggers a social warning. */
        const val SOCIAL_WARNING_MS = 5L * 60 * 60 * 1000 // 5 hours

        /** Affirmations fire at most once per this window. */
        const val AFFIRMATION_INTERVAL_MS = 6L * 24 * 60 * 60 * 1000 // 6 days

        /** Minimum week-over-week drop (percent) worth celebrating. */
        const val AFFIRMATION_MIN_DROP_PERCENT = 10.0

        /** Only project a yearly figure once there's enough usage for it to matter. */
        const val PROJECTION_MIN_YEAR_MS = 100L * 60 * 60 * 1000 // ~100 hours/year
    }
}
