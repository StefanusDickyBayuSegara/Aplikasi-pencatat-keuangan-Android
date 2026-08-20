package com.example.expensetracker.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.expensetracker.R
import com.example.expensetracker.data.AppPrefs
import com.example.expensetracker.data.Categories
import com.example.expensetracker.util.ThousandsTextWatcher
import com.example.expensetracker.viewmodel.TransactionViewModel

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var viewModel: TransactionViewModel
    private lateinit var spCategory: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPrefs.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
        val walletId = intent.getIntExtra("wallet_id", 1)

        val etAmount: EditText = findViewById(R.id.etAmount)
        etAmount.addTextChangedListener(ThousandsTextWatcher(etAmount))
        spCategory = findViewById(R.id.spCategory)
        val etNote: EditText = findViewById(R.id.etNote)
        val rgType: RadioGroup = findViewById(R.id.rgType)
        val btnSave: Button = findViewById(R.id.btnSave)

        // Default: kategori pengeluaran (karena radio default-nya "Pengeluaran")
        setCategoryAdapter(Categories.EXPENSE)

        rgType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbIncome) {
                setCategoryAdapter(Categories.INCOME)
            } else {
                setCategoryAdapter(Categories.EXPENSE)
            }
        }

        btnSave.setOnClickListener {
            val amountText = etAmount.text.toString()
            val category = spCategory.selectedItem?.toString() ?: ""
            val note = etNote.text.toString()

            if (amountText.isBlank()) {
                Toast.makeText(this, "Jumlah wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = ThousandsTextWatcher.parseRawNumber(amountText)
            if (amount <= 0.0) {
                Toast.makeText(this, "Jumlah harus berupa angka lebih dari 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedId = rgType.checkedRadioButtonId
            val type = if (selectedId == R.id.rbIncome) "income" else "expense"

            viewModel.addTransaction(walletId, amount, category, type, note.ifBlank { null })
            finish()
        }
    }

    private fun setCategoryAdapter(categories: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = adapter
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
