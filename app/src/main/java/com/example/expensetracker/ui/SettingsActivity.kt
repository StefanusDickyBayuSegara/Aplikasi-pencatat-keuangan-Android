package com.example.expensetracker.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.expensetracker.R
import com.example.expensetracker.data.AppPrefs
import com.example.expensetracker.data.Categories
import com.example.expensetracker.util.AlarmScheduler
import com.example.expensetracker.util.FirebaseSyncManager
import com.example.expensetracker.util.ThousandsTextWatcher

class SettingsActivity : AppCompatActivity() {

    private lateinit var swBudgetEnabled: Switch
    private lateinit var llBudgetList: LinearLayout
    private lateinit var swDarkMode: Switch
    private lateinit var swReminderEnabled: Switch
    private lateinit var llReminderTimeRow: LinearLayout
    private lateinit var tvReminderTime: TextView
    private val categoryInputs = mutableMapOf<String, EditText>()

    private var reminderHour = 20
    private var reminderMinute = 0

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Izin notifikasi ditolak, pengingat nggak akan muncul", Toast.LENGTH_LONG).show()
                swReminderEnabled.isChecked = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPrefs.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        swBudgetEnabled = findViewById(R.id.swBudgetEnabled)
        llBudgetList = findViewById(R.id.llBudgetList)
        swDarkMode = findViewById(R.id.swDarkMode)
        swReminderEnabled = findViewById(R.id.swReminderEnabled)
        llReminderTimeRow = findViewById(R.id.llReminderTimeRow)
        tvReminderTime = findViewById(R.id.tvReminderTime)
        val btnPickTime: Button = findViewById(R.id.btnPickTime)
        val btnSave: Button = findViewById(R.id.btnSaveSettings)
        val tvAccountStatus: TextView = findViewById(R.id.tvAccountStatus)
        val btnLoginRegister: Button = findViewById(R.id.btnLoginRegister)
        val llAccountActions: LinearLayout = findViewById(R.id.llAccountActions)
        val btnBackup: Button = findViewById(R.id.btnBackup)
        val btnRestore: Button = findViewById(R.id.btnRestore)
        val btnLogout: Button = findViewById(R.id.btnLogout)

        updateAccountUi(tvAccountStatus, btnLoginRegister, llAccountActions)

        btnLoginRegister.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnBackup.setOnClickListener {
            btnBackup.isEnabled = false
            FirebaseSyncManager.backupToCloud(this) { success, message ->
                btnBackup.isEnabled = true
                if (success) {
                    Toast.makeText(this, "Berhasil backup ke cloud", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Gagal backup: $message", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnRestore.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Restore dari Cloud?")
                .setMessage("Semua data lokal di HP ini akan DIGANTI sama data dari cloud. Tindakan ini nggak bisa dibatalkan.")
                .setPositiveButton("Restore") { _, _ ->
                    btnRestore.isEnabled = false
                    FirebaseSyncManager.restoreFromCloud(this) { success, message ->
                        btnRestore.isEnabled = true
                        if (success) {
                            Toast.makeText(this, "Berhasil restore dari cloud", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Gagal restore: $message", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        btnLogout.setOnClickListener {
            FirebaseSyncManager.logout()
            updateAccountUi(tvAccountStatus, btnLoginRegister, llAccountActions)
            Toast.makeText(this, "Berhasil keluar", Toast.LENGTH_SHORT).show()
        }

        swBudgetEnabled.isChecked = AppPrefs.isBudgetEnabled(this)
        swDarkMode.isChecked = AppPrefs.isDarkModeEnabled(this)

        swReminderEnabled.isChecked = AppPrefs.isReminderEnabled(this)
        val (savedHour, savedMinute) = AppPrefs.getReminderTime(this)
        reminderHour = savedHour
        reminderMinute = savedMinute
        updateReminderTimeText()

        buildBudgetInputs()
        toggleBudgetListVisibility(swBudgetEnabled.isChecked)
        toggleReminderTimeVisibility(swReminderEnabled.isChecked)

        swBudgetEnabled.setOnCheckedChangeListener { _, isChecked ->
            toggleBudgetListVisibility(isChecked)
        }

        swReminderEnabled.setOnCheckedChangeListener { _, isChecked ->
            toggleReminderTimeVisibility(isChecked)
            if (isChecked) checkNotificationPermission()
        }

        btnPickTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                reminderHour = hour
                reminderMinute = minute
                updateReminderTimeText()
            }, reminderHour, reminderMinute, true).show()
        }

        btnSave.setOnClickListener {
            AppPrefs.setBudgetEnabled(this, swBudgetEnabled.isChecked)

            categoryInputs.forEach { (category, editText) ->
                val amount = ThousandsTextWatcher.parseRawNumber(editText.text.toString()).toFloat()
                AppPrefs.setBudgetLimit(this, category, amount)
            }

            val darkModeChanged = swDarkMode.isChecked != AppPrefs.isDarkModeEnabled(this)
            AppPrefs.setDarkModeEnabled(this, swDarkMode.isChecked)

            AppPrefs.setReminderEnabled(this, swReminderEnabled.isChecked)
            AppPrefs.setReminderTime(this, reminderHour, reminderMinute)

            if (swReminderEnabled.isChecked) {
                AlarmScheduler.schedule(this, reminderHour, reminderMinute)
            } else {
                AlarmScheduler.cancel(this)
            }

            Toast.makeText(this, "Pengaturan disimpan", Toast.LENGTH_SHORT).show()

            if (darkModeChanged) {
                AppPrefs.applyTheme(this)
            }
            finish()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateReminderTimeText() {
        tvReminderTime.text = "Jam: %02d:%02d".format(reminderHour, reminderMinute)
    }

    private fun toggleReminderTimeVisibility(visible: Boolean) {
        llReminderTimeRow.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun buildBudgetInputs() {
        llBudgetList.removeAllViews()
        categoryInputs.clear()

        for (category in Categories.EXPENSE) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 12, 0, 12)
                gravity = Gravity.CENTER_VERTICAL
            }

            val label = TextView(this).apply {
                text = category
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val input = EditText(this).apply {
                hint = "0"
                inputType = InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                val existing = AppPrefs.getBudgetLimit(this@SettingsActivity, category)
                if (existing > 0f) setText(ThousandsTextWatcher.formatWithDots(existing.toLong()))
                addTextChangedListener(ThousandsTextWatcher(this))
            }

            categoryInputs[category] = input
            row.addView(label)
            row.addView(input)
            llBudgetList.addView(row)
        }
    }

    private fun toggleBudgetListVisibility(visible: Boolean) {
        llBudgetList.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun updateAccountUi(
        tvStatus: TextView,
        btnLoginRegister: Button,
        llActions: LinearLayout
    ) {
        val user = FirebaseSyncManager.currentUser()
        if (user != null) {
            tvStatus.text = "Masuk sebagai: ${user.email}"
            btnLoginRegister.visibility = View.GONE
            llActions.visibility = View.VISIBLE
        } else {
            tvStatus.text = "Belum masuk. Login buat backup data ke cloud."
            btnLoginRegister.visibility = View.VISIBLE
            llActions.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh status akun tiap kali balik ke halaman ini (misal habis login/register)
        val tvAccountStatus: TextView? = findViewById(R.id.tvAccountStatus)
        val btnLoginRegister: Button? = findViewById(R.id.btnLoginRegister)
        val llAccountActions: LinearLayout? = findViewById(R.id.llAccountActions)
        if (tvAccountStatus != null && btnLoginRegister != null && llAccountActions != null) {
            updateAccountUi(tvAccountStatus, btnLoginRegister, llAccountActions)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
