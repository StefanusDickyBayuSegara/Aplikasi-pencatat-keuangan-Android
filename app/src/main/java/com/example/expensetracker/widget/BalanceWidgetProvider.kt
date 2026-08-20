package com.example.expensetracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.expensetracker.R
import com.example.expensetracker.data.DbHelper
import com.example.expensetracker.ui.MainActivity
import java.text.NumberFormat
import java.util.Locale

/**
 * Widget home screen yang nampilin total saldo (dari semua dompet).
 * Pakai android.appwidget.AppWidgetProvider bawaan Android, nggak butuh library luar.
 */
class BalanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val balance = getTotalBalance(context)
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

        val views = RemoteViews(context.packageName, R.layout.widget_balance)
        views.setTextViewText(R.id.tvWidgetBalance, format.format(balance))

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getTotalBalance(context: Context): Double {
        val dbHelper = DbHelper(context)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM(CASE WHEN ${DbHelper.COL_TYPE} = 'income' THEN ${DbHelper.COL_AMOUNT} ELSE -${DbHelper.COL_AMOUNT} END) FROM ${DbHelper.TABLE_NAME}",
            null
        )
        var balance = 0.0
        if (cursor.moveToFirst()) {
            balance = cursor.getDouble(0)
        }
        cursor.close()
        db.close()
        return balance
    }

    companion object {
        /** Panggil ini tiap kali ada transaksi ditambah/dihapus, biar widget ikut ke-update */
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, BalanceWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(ComponentName(context, BalanceWidgetProvider::class.java))
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
