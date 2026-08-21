package com.example.expensetracker.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Tempat nyimpen pengaturan aplikasi: budget limit (on/off + nilainya per kategori)
 * dan dark mode. Pakai SharedPreferences, jadi nggak perlu ubah struktur database.
 */
object AppPrefs {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_BUDGET_ENABLED = "budget_enabled"
    private const val KEY_DARK_MODE = "dark_mode_enabled"
    private const val KEY_BUDGET_PREFIX = "budget_limit_"
    private const val KEY_REMINDER_ENABLED = "reminder_enabled"
    private const val KEY_REMINDER_HOUR = "reminder_hour"
    private const val KEY_REMINDER_MINUTE = "reminder_minute"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---- Budget limit ----

    fun isBudgetEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BUDGET_ENABLED, false)

    fun setBudgetEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BUDGET_ENABLED, enabled).apply()
    }

    fun getBudgetLimit(context: Context, category: String): Float =
        prefs(context).getFloat(KEY_BUDGET_PREFIX + category, 0f)

    fun setBudgetLimit(context: Context, category: String, amount: Float) {
        prefs(context).edit().putFloat(KEY_BUDGET_PREFIX + category, amount).apply()
    }

    // ---- Dark mode ----

    fun isDarkModeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DARK_MODE, false)

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    // ---- Pengingat harian ----

    fun isReminderEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REMINDER_ENABLED, false)

    fun setReminderEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
    }

    /** Balikin pasangan (jam, menit), default jam 20:00 */
    fun getReminderTime(context: Context): Pair<Int, Int> {
        val hour = prefs(context).getInt(KEY_REMINDER_HOUR, 20)
        val minute = prefs(context).getInt(KEY_REMINDER_MINUTE, 0)
        return hour to minute
    }

    fun setReminderTime(context: Context, hour: Int, minute: Int) {
        prefs(context).edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
    }

    /** Panggil ini di paling awal onCreate() (SEBELUM super.onCreate/setContentView) di SETIAP Activity */
    fun applyTheme(context: Context) {
        val mode = if (isDarkModeEnabled(context)) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
