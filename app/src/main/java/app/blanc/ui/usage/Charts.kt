package app.blanc.ui.usage

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.blanc.usage.CategorySlice
import kotlin.math.min

/** A filled line chart of daily values. */
@Composable
fun LineChart(values: List<Long>, lineColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val maxValue = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
        val width = size.width
        val height = size.height
        val stepX = width / (values.size - 1)

        val line = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - (value.toFloat() / maxValue) * height
            if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }

        val area = Path().apply {
            addPath(line)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
            ),
        )
        drawPath(
            path = line,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

/** A donut chart of category proportions. */
@Composable
fun DonutChart(slices: List<CategorySlice>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val total = slices.sumOf { it.totalMs }.coerceAtLeast(1L)
        val strokeWidth = 22.dp.toPx()
        val diameter = min(size.width, size.height) - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = (slice.totalMs.toFloat() / total) * 360f
            drawArc(
                color = slice.category.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            startAngle += sweep
        }
    }
}
