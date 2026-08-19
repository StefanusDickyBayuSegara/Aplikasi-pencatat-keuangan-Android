package com.example.expensetracker.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
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
    private lateinit var llTrendSection: LinearLayout
    private lateinit var barChartView: BarChartView
    private lateinit var spMonthFilter: Spinner
    private lateinit var etSearch: EditText
    private lateinit var recyclerView: RecyclerView

    private var selectedMonthKey: String = "ALL"
    private var searchQuery: String = ""
    private var fullTransactionList: List<Transaction> = emptyList()
    private var currentMonthKeys: List<String> = emptyList()

    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val monthLabelFormat = SimpleDateFormat("MMMM yyyy", Locale("in", "ID"))
    private val monthShortFormat = SimpleDateFormat("MMM", Locale("in", "ID"))

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
        llTrendSection = findViewById(R.id.llTrendSection)
        barChartView = findViewById(R.id.barChartView)
        spMonthFilter = findViewById(R.id.spMonthFilter)
        etSearch = findViewById(R.id.etSearch)
        recyclerView = findViewById(R.id.rvTransactions)
        val fab: FloatingActionButton = findViewById(R.id.fabAdd)
        val btnSettings: TextView = findViewById(R.id.btnSettings)

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

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                applyFilterAndRender()
            }
        })

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

        viewModel.allTransactions.observe(this) { transactions ->
            fullTransactionList = transactions
            updateMonthSpinner(transactions)
            updateTrendChart(transactions)
            applyFilterAndRender()
        }

        viewModel.balance.observe(this) { balance ->
            val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            tvBalance.text = format.format(balance ?: 0.0)
        }

        fab.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun updateMonthSpinner(transactions: List<Transaction>) {
        val newMonthKeys = transactions
            .map { monthKeyFormat.format(Date(it.date)) }
            .distinct()
            .sortedDescending()

        if (newMonthKeys == currentMonthKeys) return
        currentMonthKeys = newMonthKeys

        val labels = mutableListOf("Semua Bulan")
        labels.addAll(newMonthKeys.map { key ->
            val date = monthKeyFormat.parse(key) ?: Date()
            monthLabelFormat.format(date)
        })

        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spMonthFilter.adapter = monthAdapter

        val indexToSelect = if (selectedMonthKey == "ALL") 0
        else {
            val idx = newMonthKeys.indexOf(selectedMonthKey)
            if (idx >= 0) idx + 1 else 0
        }
        spMonthFilter.setSelection(indexToSelect)
    }

    /** Grafik tren: total pengeluaran per bulan, maksimal 6 bulan terakhir */
    private fun updateTrendChart(transactions: List<Transaction>) {
        val monthlyTotals = transactions
            .filter { it.type == "expense" }
            .groupBy { monthKeyFormat.format(Date(it.date)) }
            .mapValues { entry -> entry.value.sumOf { it.amount }.toFloat() }
            .toList()
            .sortedBy { it.first }
            .takeLast(6)

        if (monthlyTotals.size < 2) {
            llTrendSection.visibility = View.GONE
            return
        }

        llTrendSection.visibility = View.VISIBLE
        val chartData = monthlyTotals.map { (key, value) ->
            val date = monthKeyFormat.parse(key) ?: Date()
            monthShortFormat.format(date) to value
        }
        barChartView.setData(chartData)
    }

    private fun applyFilterAndRender() {
        var filtered = if (selectedMonthKey == "ALL") {
            fullTransactionList
        } else {
            fullTransactionList.filter { monthKeyFormat.format(Date(it.date)) == selectedMonthKey }
        }

        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.category.contains(searchQuery, ignoreCase = true) ||
                    (it.note?.contains(searchQuery, ignoreCase = true) == true)
            }
        }

        adapter.submitList(filtered) {
            // Animasi jatuh pas list ke-render ulang
            recyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down)
            recyclerView.scheduleLayoutAnimation()
        }

        if (filtered.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            tvEmptyTitle.text = when {
                searchQuery.isNotEmpty() -> "Nggak ketemu hasil pencarian"
                selectedMonthKey != "ALL" -> "Nggak ada transaksi di bulan ini"
                else -> "Belum ada transaksi"
            }
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

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
