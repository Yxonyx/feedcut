package io.github.yxonyx.feedcut

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * One row per day: the day, a bar, the estimated minutes. Every bar uses the same fixed
 * scale, so a bar's length is its minutes and two weeks can be compared by eye.
 */
class WeekView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var days: List<Pair<LocalDate, Int>> = emptyList()

    fun show(days: List<Pair<LocalDate, Int>>) {
        this.days = days
        contentDescription = days.joinToString(". ") { (day, minutes) ->
            "${day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}: $minutes minutes"
        }
        requestLayout()
        invalidate()
    }

    private val density = resources.displayMetrics.density
    private val rowHeight = 26 * density
    private val barHeight = 8 * density
    private val labelWidth = 44 * density
    private val valueWidth = 64 * density
    private val barfont = ResourcesCompat.getFont(context, R.font.barlow_condensed_semibold)
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = barfont
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 15f, resources.displayMetrics)
        color = ContextCompat.getColor(context, R.color.c_muted)
    }
    private val value = Paint(label).apply { textAlign = Paint.Align.RIGHT }
    private val today = Paint(label).apply { color = ContextCompat.getColor(context, R.color.c_ink) }
    private val todayValue = Paint(today).apply { textAlign = Paint.Align.RIGHT }
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.c_bar) }
    private val barToday = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.c_primary) }
    private val stub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.c_line) }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = (rowHeight * days.size.coerceAtLeast(1)).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), h)
    }

    override fun onDraw(canvas: Canvas) {
        val barLeft = labelWidth
        val barMax = width - labelWidth - valueWidth - 8 * density
        days.forEachIndexed { i, (day, cuts) ->
            val isToday = i == days.lastIndex
            val minutes = minutesKept(cuts)
            val mid = rowHeight * i + rowHeight / 2
            val baseline = mid + label.textSize * 0.35f
            val name = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            canvas.drawText(name, 0f, baseline, if (isToday) today else label)
            val length = barMax * (minutes.coerceAtMost(SCALE_MINUTES) / SCALE_MINUTES.toFloat())
            if (minutes == 0) {
                canvas.drawRect(barLeft, mid - density, barLeft + 3 * density, mid + density, stub)
            } else {
                canvas.drawRect(barLeft, mid - barHeight / 2, barLeft + length, mid + barHeight / 2, if (isToday) barToday else bar)
            }
            val text = if (minutes > SCALE_MINUTES) "$minutes min+" else "$minutes min"
            canvas.drawText(text, width.toFloat(), baseline, if (isToday) todayValue else value)
        }
    }

    private companion object {
        /** Full bar width. Longer days are drawn full and marked with "+". */
        const val SCALE_MINUTES = 120
    }
}
