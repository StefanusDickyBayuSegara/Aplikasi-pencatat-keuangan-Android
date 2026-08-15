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
import com.example.expensetracker.viewmodel.TransactionViewModel

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var viewModel: TransactionViewModel
    private lateinit var spCategory: Spinner

    // Daftar kategori. Bisa ditambah/diedit sesuka kamu.
    private val expenseCategories = listOf(
        "Makan", "Transport", "Belanja", "Hiburan",
        "Tagihan", "Kesehatan", "Pendidikan", "Lainnya"
    )
    private val incomeCategories = listOf(
        "Gaji", "Hadiah", "Investasi", "Lainnya"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

        val etAmount: EditText = findViewById(R.id.etAmount)
        spCategory = findViewById(R.id.spCategory)
        val etNote: EditText = findViewById(R.id.etNote)
        val rgType: RadioGroup = findViewById(R.id.rgType)
        val btnSave: Button = findViewById(R.id.btnSave)

        // Default: kategori pengeluaran (karena radio default-nya "Pengeluaran")
        setCategoryAdapter(expenseCategories)

        // Ganti isi dropdown kategori sesuai tipe yang dipilih
        rgType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbIncome) {
                setCategoryAdapter(incomeCategories)
            } else {
                setCategoryAdapter(expenseCategories)
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

            val amount = amountText.toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(this, "Jumlah harus berupa angka", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedId = rgType.checkedRadioButtonId
            val type = if (selectedId == R.id.rbIncome) "income" else "expense"

            viewModel.addTransaction(amount, category, type, note.ifBlank { null })
            finish()
        }
    }

    private fun setCategoryAdapter(categories: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = adapter
    }
}
