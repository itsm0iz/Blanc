package app.blanc.ui.usage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.blanc.usage.DayTotal
import app.blanc.usage.UsageState
import app.blanc.util.formatDuration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Composable
fun UsageScreen(
    granted: Boolean,
    state: UsageState?,
    labelFor: (String) -> String,
    onGrantAccess: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Text(
            text = "Screen time",
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(24.dp))

        when {
            !granted -> PermissionPrompt(onGrantAccess)
            state == null -> Text(
                text = "Loading…",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )

            else -> UsageContent(state, labelFor)
        }
    }
}

@Composable
private fun PermissionPrompt(onGrantAccess: () -> Unit) {
    Column {
        Text(
            text = "Blanc needs usage access to show your screen time. " +
                "Everything stays on your device.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onGrantAccess) {
            Text("Grant usage access")
        }
    }
}

@Composable
private fun UsageContent(state: UsageState, labelFor: (String) -> String) {
    Text(
        text = formatDuration(state.todayMs),
        fontSize = 48.sp,
        fontWeight = FontWeight.Light,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = "today",
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
    )

    Spacer(Modifier.height(28.dp))
    WeekChart(state.last7Days)

    Spacer(Modifier.height(24.dp))
    Text(
        text = "Averaging ${formatDuration(state.avgPerDayMs)} a day.",
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = "At this rate, about ${projectedHoursText(state.projectedYearMs)} this year.",
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )

    if (state.topApps.isNotEmpty()) {
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Most used this week",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(8.dp))
        state.topApps.forEach { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = labelFor(app.packageName),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = formatDuration(app.totalMs),
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun WeekChart(days: List<DayTotal>) {
    val maxMs = (days.maxOfOrNull { it.totalMs } ?: 0L).coerceAtLeast(1L)
    val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            val fraction = (day.totalMs.toDouble() / maxMs).toFloat().coerceIn(0f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                        .width(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = dayFormat.format(Date(epochDayToMillis(day.epochDay))),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
            }
        }
    }
}

private fun projectedHoursText(projectedYearMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(projectedYearMs)
    if (hours < 48) return "${hours}h"
    val days = (hours / 24.0).roundToInt()
    return "$hours hours (${days} days)"
}

private fun epochDayToMillis(epochDay: Long): Long {
    val utcMidnight = TimeUnit.DAYS.toMillis(epochDay)
    val offset = Calendar.getInstance().timeZone.getOffset(utcMidnight)
    return utcMidnight - offset
}
