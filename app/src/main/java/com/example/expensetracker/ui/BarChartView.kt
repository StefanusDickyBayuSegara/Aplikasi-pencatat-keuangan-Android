package com.example.expensetracker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Bar chart sederhana buat nampilin tren pengeluaran per bulan.
 * Digambar manual pakai Canvas, nggak butuh library luar.
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2E7D32")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#666666")
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }

    private var data: List<Pair<String, Float>> = emptyList()

    fun setData(newData: List<Pair<String, Float>>) {
        data = newData
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val maxValue = data.maxOf { it.second }.coerceAtLeast(1f)
        val labelSpace = 50f
        val chartHeight = height - labelSpace
        val slot = width / data.size.toFloat()
        val barWidth = slot * 0.5f

        data.forEachIndexed { index, (label, value) ->
            val barHeight = (value / maxValue) * (chartHeight - 10f)
            val centerX = slot * index + slot / 2f
            val left = centerX - barWidth / 2f
            val right = centerX + barWidth / 2f
            val top = chartHeight - barHeight
            val bottom = chartHeight

            canvas.drawRoundRect(left, top, right, bottom, 8f, 8f, barPaint)
            canvas.drawText(label, centerX, height.toFloat() - 8f, textPaint)
        }
    }
}
