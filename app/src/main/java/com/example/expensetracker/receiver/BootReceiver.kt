package com.example.expensetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.expensetracker.data.AppPrefs
import com.example.expensetracker.util.AlarmScheduler

/**
 * Alarm otomatis ke-cancel tiap kali HP restart, jadi perlu di-set ulang.
 * Receiver ini dengerin sinyal "HP baru nyala" dan pasang lagi alarm-nya kalau reminder aktif.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (AppPrefs.isReminderEnabled(context)) {
                val (hour, minute) = AppPrefs.getReminderTime(context)
                AlarmScheduler.schedule(context, hour, minute)
            }
        }
    }
}
