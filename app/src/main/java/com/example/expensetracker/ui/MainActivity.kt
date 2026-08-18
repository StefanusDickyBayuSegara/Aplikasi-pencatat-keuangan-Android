package com.example.expensetracker.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.R
import com.example.expensetracker.data.AppPrefs
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.viewmodel.TransactionViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: TransactionViewModel
    private lateinit var adapter: TransactionAdapter
    private lateinit var tvBalance: TextView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmptyTitle: TextView
    private lateinit var llChartSection: LinearLayout
    private lateinit var llLegend: LinearLayout
    private lateinit var pieChartView: PieChartView
    private lateinit var spMonthFilter: Spinner

    // key format "yyyy-MM", "ALL" berarti semua bulan
    private var selectedMonthKey: String = "ALL"
    private var fullTransactionList: List<Transaction> = emptyList()
    private var currentMonthKeys: List<String> = emptyList()

    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val monthLabelFormat = SimpleDateFormat("MMMM yyyy", Locale("in", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPrefs.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvBalance = findViewById(R.id.tvBalance)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        llChartSection = findViewById(R.id.llChartSection)
        llLegend = findViewById(R.id.llLegend)
        pieChartView = findViewById(R.id.pieChartView)
        spMonthFilter = findViewById(R.id.spMonthFilter)
        val recyclerView: RecyclerView = findViewById(R.id.rvTransactions)
        val fab: FloatingActionButton = findViewById(R.id.fabAdd)
        val btnSettings: TextView = findViewById(R.id.btnSettings)

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        adapter = TransactionAdapter { transaction ->
            viewModel.deleteTransaction(transaction)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        spMonthFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMonthKey = if (position == 0) "ALL" else currentMonthKeys[position - 1]
                applyFilterAndRender()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

        viewModel.allTransactions.observe(this) { transactions ->
            fullTransactionList = transactions
            updateMonthSpinner(transactions)
            applyFilterAndRender()
        }

        viewModel.balance.observe(this) { balance ->
            val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            tvBalance.text = format.format(balance ?: 0.0)
        }

        fab.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    /** Bangun ulang isi dropdown bulan berdasarkan bulan-bulan yang ada di data transaksi */
    private fun updateMonthSpinner(transactions: List<Transaction>) {
        val newMonthKeys = transactions
            .map { monthKeyFormat.format(Date(it.date)) }
            .distinct()
            .sortedDescending()

        // Kalau daftar bulan nggak berubah, nggak usah bangun ulang adapter (biar posisi pilihan nggak reset)
        if (newMonthKeys == currentMonthKeys) return
        currentMonthKeys = newMonthKeys

        val labels = mutableListOf("Semua Bulan")
        labels.addAll(newMonthKeys.map { key ->
            val date = monthKeyFormat.parse(key) ?: Date()
            monthLabelFormat.format(date)
        })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spMonthFilter.adapter = adapter

        // Kalau bulan yang lagi dipilih masih ada di daftar baru, pertahankan pilihannya
        val indexToSelect = if (selectedMonthKey == "ALL") 0
        else {
            val idx = newMonthKeys.indexOf(selectedMonthKey)
            if (idx >= 0) idx + 1 else 0
        }
        spMonthFilter.setSelection(indexToSelect)
    }

    /** Filter list transaksi sesuai bulan terpilih, lalu render list, empty state, dan pie chart */
    private fun applyFilterAndRender() {
        val filtered = if (selectedMonthKey == "ALL") {
            fullTransactionList
        } else {
            fullTransactionList.filter { monthKeyFormat.format(Date(it.date)) == selectedMonthKey }
        }

        adapter.submitList(filtered)

        if (filtered.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            tvEmptyTitle.text = if (selectedMonthKey == "ALL") "Belum ada transaksi" else "Nggak ada transaksi di bulan ini"
        } else {
            layoutEmpty.visibility = View.GONE
        }

        val expenseByCategory = filtered
            .filter { it.type == "expense" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount }.toFloat() }
            .toList()
            .sortedByDescending { it.second }

        if (expenseByCategory.isEmpty()) {
            llChartSection.visibility = View.GONE
        } else {
            llChartSection.visibility = View.VISIBLE
            pieChartView.setData(expenseByCategory)
            buildLegend(expenseByCategory)
        }
    }

    private fun buildLegend(data: List<Pair<String, Float>>) {
        llLegend.removeAllViews()
        val total = data.sumOf { it.second.toDouble() }.toFloat()
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

        val budgetEnabled = AppPrefs.isBudgetEnabled(this)

        data.forEachIndexed { index, (category, value) ->
            val color = PieChartView.COLORS[index % PieChartView.COLORS.size]
            val percent = if (total > 0) (value / total) * 100 else 0f

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 4, 0, 4)
            }

            val colorBox = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(24, 24).apply {
                    marginEnd = 12
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(color)
                    cornerRadius = 4f
                }
            }

            // Cek budget limit (kalau fiturnya diaktifkan)
            val limit = if (budgetEnabled) AppPrefs.getBudgetLimit(this, category) else 0f
            val overBudget = budgetEnabled && limit > 0f && value > limit

            val label = TextView(this).apply {
                text = if (overBudget) {
                    "$category — ${format.format(value)} (${percent.toInt()}%) ⚠️ Lewat limit ${format.format(limit)}"
                } else {
                    "$category — ${format.format(value)} (${percent.toInt()}%)"
                }
                textSize = 12f
                setTextColor(if (overBudget) Color.parseColor("#C62828") else Color.parseColor("#333333"))
            }

            row.addView(colorBox)
            row.addView(label)
            llLegend.addView(row)
        }
    }
}
