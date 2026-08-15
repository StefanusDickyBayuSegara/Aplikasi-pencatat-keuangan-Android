package com.example.expensetracker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Custom pie chart sederhana, digambar manual pakai Canvas.
 * Nggak butuh library luar sama sekali.
 */
class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    private var slices: List<Triple<String, Float, Int>> = emptyList() // label, value, color

    companion object {
        val COLORS = listOf(
            Color.parseColor("#E57373"),
            Color.parseColor("#64B5F6"),
            Color.parseColor("#81C784"),
            Color.parseColor("#FFD54F"),
            Color.parseColor("#BA68C8"),
            Color.parseColor("#4DB6AC"),
            Color.parseColor("#F06292"),
            Color.parseColor("#A1887F")
        )
    }

    /** data: list pasangan (nama kategori, total nilai) */
    fun setData(data: List<Pair<String, Float>>) {
        slices = data.mapIndexed { index, pair ->
            Triple(pair.first, pair.second, COLORS[index % COLORS.size])
        }
        invalidate()
    }

    fun getSlices(): List<Triple<String, Float, Int>> = slices

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (slices.isEmpty()) return

        val total = slices.sumOf { it.second.toDouble() }.toFloat()
        if (total <= 0f) return

        val size = minOf(width, height).toFloat()
        val padding = 4f
        rectF.set(padding, padding, size - padding, size - padding)

        var startAngle = -90f
        for ((_, value, color) in slices) {
            val sweep = (value / total) * 360f
            paint.color = color
            canvas.drawArc(rectF, startAngle, sweep, true, paint)
            startAngle += sweep
        }
    }
}
