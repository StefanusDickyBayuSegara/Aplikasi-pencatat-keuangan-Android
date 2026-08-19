package com.example.expensetracker.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.expensetracker.data.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bikin laporan PDF dari list transaksi, pakai android.graphics.pdf.PdfDocument
 * (bawaan Android, nggak butuh library luar), lalu langsung buka menu Share.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595  // ukuran A4 kira-kira, dalam satuan "point"
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun generateAndShare(context: Context, transactions: List<Transaction>, periodLabel: String) {
        val pdfDocument = PdfDocument()
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("in", "ID"))

        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val subtitlePaint = Paint().apply { textSize = 11f; color = Color.DKGRAY }
        val headerPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val textPaint = Paint().apply { textSize = 10f }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        var pageNumber = 1
        var y: Float
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeader(): Float {
            var yy = MARGIN
            canvas.drawText("Laporan Keuangan - Expense Tracker", MARGIN, yy, titlePaint)
            yy += 20f
            canvas.drawText("Periode: $periodLabel", MARGIN, yy, subtitlePaint)
            yy += 25f
            canvas.drawLine(MARGIN, yy, PAGE_WIDTH - MARGIN, yy, linePaint)
            yy += 20f
            canvas.drawText("Tanggal", MARGIN, yy, headerPaint)
            canvas.drawText("Kategori", MARGIN + 90f, yy, headerPaint)
            canvas.drawText("Tipe", MARGIN + 250f, yy, headerPaint)
            canvas.drawText("Jumlah", PAGE_WIDTH - MARGIN - 100f, yy, headerPaint)
            yy += 8f
            canvas.drawLine(MARGIN, yy, PAGE_WIDTH - MARGIN, yy, linePaint)
            yy += 15f
            return yy
        }

        y = drawHeader()

        var totalIncome = 0.0
        var totalExpense = 0.0
        val sorted = transactions.sortedBy { it.date }

        for (t in sorted) {
            if (t.type == "income") totalIncome += t.amount else totalExpense += t.amount

            if (y > PAGE_HEIGHT - MARGIN - 60f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = drawHeader()
            }

            canvas.drawText(dateFormat.format(Date(t.date)), MARGIN, y, textPaint)
            canvas.drawText(t.category, MARGIN + 90f, y, textPaint)
            canvas.drawText(if (t.type == "income") "Masuk" else "Keluar", MARGIN + 250f, y, textPaint)
            val amountText = (if (t.type == "income") "+" else "-") + format.format(t.amount)
            canvas.drawText(amountText, PAGE_WIDTH - MARGIN - 100f, y, textPaint)
            y += 18f
        }

        y += 15f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 20f
        canvas.drawText("Total Pemasukan: ${format.format(totalIncome)}", MARGIN, y, headerPaint)
        y += 18f
        canvas.drawText("Total Pengeluaran: ${format.format(totalExpense)}", MARGIN, y, headerPaint)
        y += 18f
        canvas.drawText("Saldo: ${format.format(totalIncome - totalExpense)}", MARGIN, y, headerPaint)

        pdfDocument.finishPage(page)

        val fileName = "Laporan_ExpenseTracker_${System.currentTimeMillis()}.pdf"
        val dir = File(context.getExternalFilesDir(null), "reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)

        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        pdfDocument.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan PDF"))
    }
}
