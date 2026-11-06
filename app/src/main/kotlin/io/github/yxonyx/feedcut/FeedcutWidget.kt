package io.github.yxonyx.feedcut

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import java.text.NumberFormat
import kotlin.math.ceil

/**
 * Time left of the daily limits on the home screen. The system redraws it every 30 minutes
 * at most, so the service, a saved rule and the app itself ask for a redraw too.
 */
class FeedcutWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { draw(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        options: Bundle,
    ) {
        draw(context, manager, id)
    }

    companion object {
        private const val COMPACT_DP = 140
        private const val NUMBER_SP = 60f
        private const val UNIT_SP = 24f
        private const val ROW_SP = 17f

        private val ROWS = listOf(
            Triple(R.id.widget_row_2, R.id.widget_name_2, R.id.widget_left_2),
            Triple(R.id.widget_row_3, R.id.widget_name_3, R.id.widget_left_3),
        )

        fun refresh(context: Context) {
            // Null on devices without home screen widgets.
            val manager = AppWidgetManager.getInstance(context) ?: return
            manager.getAppWidgetIds(ComponentName(context, FeedcutWidget::class.java))
                .forEach { draw(context, manager, it) }
        }

        private fun draw(context: Context, manager: AppWidgetManager, id: Int) {
            val height = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            manager.updateAppWidget(id, views(context, compact = height in 1 until COMPACT_DP))
        }

        private fun views(context: Context, compact: Boolean): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget)
            val rules = RuleStore.readQuiet(context)?.apps.orEmpty()
            val installed = limitsToShow(rules) { BlockLog.installedVersion(context, it) != null }
            val shown = byTimeLeft(installed) { UsageLog.today(context, it) }
            val cuts = DayCount.today(context)
            val first = shown.firstOrNull()
            if (first == null) showCuts(context, views, cuts) else showLimit(context, views, first)
            ROWS.forEachIndexed { index, (row, name, left) ->
                val rule = shown.getOrNull(index + 1).takeUnless { compact }
                views.setViewVisibility(row, if (rule == null) View.GONE else View.VISIBLE)
                if (rule != null) {
                    views.setTextViewText(name, rule.label)
                    paint(context, views, left, leftText(context, rule), R.font.barlow_condensed_bold, ROW_SP)
                }
            }
            views.setViewVisibility(R.id.widget_cuts, if (first == null || compact) View.GONE else View.VISIBLE)
            // A lone limit stays centred; with rows the count sits at the foot of the board.
            views.setViewVisibility(R.id.widget_space, if (shown.size < 2 || compact) View.GONE else View.VISIBLE)
            views.setTextViewText(R.id.widget_cuts, context.getString(R.string.widget_cuts, cuts))
            val open = Intent(context, MainActivity::class.java)
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE),
            )
            return views
        }

        private fun showLimit(context: Context, views: RemoteViews, rule: AppRule) {
            val used = UsageLog.today(context, rule.packageName)
            val minutes = minutesLeft(rule, used)
            val unit = context.getString(if (minutes > 0) R.string.widget_min_left else R.string.widget_used_up)
            paintHead(context, views, number(minutes), unit)
            views.setTextViewText(R.id.widget_detail, rule.label)
            views.setViewVisibility(R.id.widget_bar, View.VISIBLE)
            views.setProgressBar(R.id.widget_bar, BAR_STEPS, stepsLeft(rule, used), false)
        }

        private fun showCuts(context: Context, views: RemoteViews, cuts: Int) {
            paintHead(context, views, number(cuts), context.resources.getQuantityString(R.plurals.count_label, cuts))
            views.setTextViewText(R.id.widget_detail, context.getString(R.string.kept, minutesKept(cuts)))
            views.setViewVisibility(R.id.widget_bar, View.GONE)
        }

        private fun paintHead(context: Context, views: RemoteViews, number: String, unit: String) {
            paint(context, views, R.id.widget_number, number, R.font.barlow_condensed_extrabold, NUMBER_SP, tight = true)
            paint(context, views, R.id.widget_unit, unit, R.font.barlow_condensed_bold, UNIT_SP)
        }

        /**
         * White letters: the tint in the layout colours them, so light and dark still follow the launcher.
         * [tight] drops the line's empty top and bottom, for digits that stand beside other text.
         */
        private fun paint(
            context: Context,
            views: RemoteViews,
            id: Int,
            text: String,
            @FontRes font: Int,
            sp: Float,
            tight: Boolean = false,
        ) {
            val metrics = context.resources.displayMetrics
            val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = ResourcesCompat.getFont(context, font)
                textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics)
            }
            val ink = Rect().also { pen.getTextBounds(text, 0, text.length, it) }
            val top = if (tight) ink.top.toFloat() else pen.fontMetrics.ascent
            val bottom = if (tight) ink.bottom.toFloat() else pen.fontMetrics.descent
            val bitmap = createBitmap(ceil(pen.measureText(text)).toInt(), ceil(bottom - top).toInt())
            Canvas(bitmap).drawText(text, 0f, -top, pen)
            views.setImageViewBitmap(id, bitmap)
            views.setContentDescription(id, text)
        }

        private fun leftText(context: Context, rule: AppRule): String {
            val minutes = minutesLeft(rule, UsageLog.today(context, rule.packageName))
            return if (minutes > 0) context.getString(R.string.widget_left, minutes)
            else context.getString(R.string.widget_used_up)
        }

        private fun number(value: Int) = NumberFormat.getIntegerInstance().format(value)
    }
}
