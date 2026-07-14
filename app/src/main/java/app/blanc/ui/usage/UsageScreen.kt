package app.blanc.ui.usage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.blanc.usage.CategorySlice
import app.blanc.usage.UsageReport
import app.blanc.util.formatDuration
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

private val RANGES = listOf(7, 14, 30, 60, 90)

@Composable
fun UsageScreen(
    granted: Boolean,
    report: UsageReport?,
    rangeDays: Int,
    labelFor: (String) -> String,
    onGrantAccess: () -> Unit,
    onRangeChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        when {
            !granted -> PermissionPrompt(onGrantAccess)
            report == null -> Text(
                text = "Loading…",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 24.dp),
            )

            else -> ReportContent(report, rangeDays, labelFor, onRangeChange)
        }
    }
}

@Composable
private fun PermissionPrompt(onGrantAccess: () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 24.dp)) {
        Text(
            text = "Blanc needs usage access to show your screen time. Everything stays on your device.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onGrantAccess) { Text("Grant usage access") }
    }
}

@Composable
private fun ReportContent(
    report: UsageReport,
    rangeDays: Int,
    labelFor: (String) -> String,
    onRangeChange: (Int) -> Unit,
) {
    val onBg = MaterialTheme.colorScheme.onBackground

    Text(
        text = formatDuration(report.todayMs),
        fontSize = 46.sp,
        fontWeight = FontWeight.Light,
        color = onBg,
    )
    Text("today", fontSize = 14.sp, color = onBg.copy(alpha = 0.5f))

    // Range slider
    Spacer(Modifier.height(20.dp))
    val startIndex = RANGES.indexOf(rangeDays).let { if (it < 0) 2 else it }
    var sliderPos by remember(rangeDays) { mutableFloatStateOf(startIndex.toFloat()) }
    val shownRange = RANGES[sliderPos.roundToInt().coerceIn(0, RANGES.lastIndex)]
    Text("Last $shownRange days", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = onBg)
    Slider(
        value = sliderPos,
        onValueChange = { sliderPos = it },
        valueRange = 0f..RANGES.lastIndex.toFloat(),
        steps = RANGES.size - 2,
        onValueChangeFinished = {
            onRangeChange(RANGES[sliderPos.roundToInt().coerceIn(0, RANGES.lastIndex)])
        },
    )

    // Daily trend line
    LineChart(
        values = report.dailyTotals.map { it.totalMs },
        lineColor = onBg,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(top = 4.dp),
    )
    Text(
        text = "Averaging ${formatDuration(report.avgPerDayMs)} a day",
        fontSize = 13.sp,
        color = onBg.copy(alpha = 0.5f),
    )

    // Week-over-week compare card
    Spacer(Modifier.height(24.dp))
    CompareCard(report)

    // Categories
    if (report.categories.isNotEmpty()) {
        Spacer(Modifier.height(28.dp))
        Text("By category", fontSize = 13.sp, color = onBg.copy(alpha = 0.5f))
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(
                slices = report.categories,
                modifier = Modifier.size(120.dp),
            )
            Spacer(Modifier.width(20.dp))
            Column {
                report.categories.take(5).forEach { slice ->
                    CategoryLegendRow(slice)
                }
            }
        }
    }

    // Top apps
    if (report.topApps.isNotEmpty()) {
        Spacer(Modifier.height(28.dp))
        Text("Most used", fontSize = 13.sp, color = onBg.copy(alpha = 0.5f))
        Spacer(Modifier.height(8.dp))
        report.topApps.forEach { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(labelFor(app.packageName), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onBg)
                Text(formatDuration(app.totalMs), fontSize = 16.sp, color = onBg.copy(alpha = 0.6f))
            }
        }
    }

    Spacer(Modifier.height(24.dp))
}

@Composable
private fun CompareCard(report: UsageReport) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val change = report.weekChangePercent
    val changeText = when {
        change > 0 -> "↑ $change% more than last week"
        change < 0 -> "↓ ${abs(change)}% less than last week"
        else -> "About the same as last week"
    }
    val trend = when {
        change > 5 -> "Trending up — next week is likely higher."
        change < -5 -> "Trending down — nice work."
        else -> "Holding steady."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(onBg.copy(alpha = 0.06f))
            .padding(16.dp),
    ) {
        Text("This week", fontSize = 13.sp, color = onBg.copy(alpha = 0.5f))
        Text(formatDuration(report.thisWeekMs), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = onBg)
        Spacer(Modifier.height(4.dp))
        Text(changeText, fontSize = 14.sp, color = onBg.copy(alpha = 0.8f))
        Text(trend, fontSize = 14.sp, color = onBg.copy(alpha = 0.6f))
        Spacer(Modifier.height(8.dp))
        Text(
            text = "At this pace, about ${projectedText(report.projectedYearMs)} this year.",
            fontSize = 14.sp,
            color = onBg.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun CategoryLegendRow(slice: CategorySlice) {
    val onBg = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(slice.category.color),
        )
        Spacer(Modifier.width(8.dp))
        Text(slice.category.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = onBg)
        Spacer(Modifier.width(8.dp))
        Text(formatDuration(slice.totalMs), fontSize = 13.sp, color = onBg.copy(alpha = 0.5f))
    }
}

private fun projectedText(projectedYearMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(projectedYearMs)
    if (hours < 48) return "${hours}h"
    val days = (hours / 24.0).roundToInt()
    return "$hours hours ($days days)"
}
