package io.github.yxonyx.feedcut

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.withTranslation

/**
 * The status, painted as a trail blaze: white, red, white.
 * Whole when Feedcut runs, unpainted bands when it is off, and broken in trail yellow
 * when it is switched on but Android stopped it. The status line next to it says the
 * same in words, so the state never depends on colour alone.
 */
class BlazeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var state: GateState = GateState.ON
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val radius = 3 * density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = ContextCompat.getColor(context, R.color.c_muted)
    }
    private val white = ContextCompat.getColor(context, R.color.blaze_white)
    private val red = ContextCompat.getColor(context, R.color.blaze_red)
    private val yellow = ContextCompat.getColor(context, R.color.trail_yellow)
    private val box = RectF()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val inset = edge.strokeWidth / 2
        box.set(inset, inset, w - inset, h - inset)
        when (state) {
            GateState.OFF -> {
                // The same three bands, unpainted: the shape stays, the paint is gone.
                canvas.drawRoundRect(box, radius, radius, edge)
                canvas.drawLine(box.left, h * 0.3f, box.right, h * 0.3f, edge)
                canvas.drawLine(box.left, h * 0.7f, box.right, h * 0.7f, edge)
            }
            GateState.ON -> {
                paintBands(canvas, red, 0f, w, 0f)
                canvas.drawRoundRect(box, radius, radius, edge)
            }
            GateState.STUCK -> {
                // The middle band snaps: two pieces, the right one knocked out of line.
                val gap = w * 0.06f
                val half = (w - gap) / 2
                val drop = h * 0.14f
                paintBands(canvas, yellow, 0f, half, 0f)
                paintBands(canvas, yellow, half + gap, w, drop)
            }
        }
    }

    private fun paintBands(canvas: Canvas, middle: Int, left: Float, right: Float, dy: Float) {
        val h = height.toFloat() - dy
        val band = h * 0.3f
        canvas.withTranslation(0f, dy) {
            box.set(left, 0f, right, h)
            clipRect(box)
            fill.color = white
            drawRoundRect(box, radius, radius, fill)
            fill.color = middle
            drawRect(left, band, right, h - band, fill)
        }
        if (state == GateState.STUCK) {
            box.set(left, dy, right, dy + h)
            box.inset(edge.strokeWidth / 2, edge.strokeWidth / 2)
            canvas.drawRoundRect(box, radius, radius, edge)
        }
    }
}
