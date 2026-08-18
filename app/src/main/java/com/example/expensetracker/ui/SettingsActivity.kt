package com.example.expensetracker.ui

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
import androidx.appcompat.app.AppCompatActivity
import com.example.expensetracker.R
import com.example.expensetracker.data.AppPrefs
import com.example.expensetracker.data.Categories

class SettingsActivity : AppCompatActivity() {

    private lateinit var swBudgetEnabled: Switch
    private lateinit var llBudgetList: LinearLayout
    private lateinit var swDarkMode: Switch
    private val categoryInputs = mutableMapOf<String, EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPrefs.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        swBudgetEnabled = findViewById(R.id.swBudgetEnabled)
        llBudgetList = findViewById(R.id.llBudgetList)
        swDarkMode = findViewById(R.id.swDarkMode)
        val btnSave: Button = findViewById(R.id.btnSaveSettings)

        swBudgetEnabled.isChecked = AppPrefs.isBudgetEnabled(this)
        swDarkMode.isChecked = AppPrefs.isDarkModeEnabled(this)

        buildBudgetInputs()
        toggleBudgetListVisibility(swBudgetEnabled.isChecked)

        // Ini toggle utamanya: nyala/mati fitur budget limit
        swBudgetEnabled.setOnCheckedChangeListener { _, isChecked ->
            toggleBudgetListVisibility(isChecked)
        }

        btnSave.setOnClickListener {
            AppPrefs.setBudgetEnabled(this, swBudgetEnabled.isChecked)

            categoryInputs.forEach { (category, editText) ->
                val amount = editText.text.toString().toFloatOrNull() ?: 0f
                AppPrefs.setBudgetLimit(this, category, amount)
            }

            val darkModeChanged = swDarkMode.isChecked != AppPrefs.isDarkModeEnabled(this)
            AppPrefs.setDarkModeEnabled(this, swDarkMode.isChecked)

            Toast.makeText(this, "Pengaturan disimpan", Toast.LENGTH_SHORT).show()

            if (darkModeChanged) {
                AppPrefs.applyTheme(this)
            }
            finish()
        }
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
                if (existing > 0f) setText(existing.toInt().toString())
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
}
