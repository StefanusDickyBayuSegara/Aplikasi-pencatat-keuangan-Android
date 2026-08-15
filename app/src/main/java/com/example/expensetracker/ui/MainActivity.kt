package com.example.expensetracker.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.R
import com.example.expensetracker.viewmodel.TransactionViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: TransactionViewModel
    private lateinit var adapter: TransactionAdapter
    private lateinit var tvBalance: TextView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var llChartSection: LinearLayout
    private lateinit var llLegend: LinearLayout
    private lateinit var pieChartView: PieChartView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvBalance = findViewById(R.id.tvBalance)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        llChartSection = findViewById(R.id.llChartSection)
        llLegend = findViewById(R.id.llLegend)
        pieChartView = findViewById(R.id.pieChartView)
        val recyclerView: RecyclerView = findViewById(R.id.rvTransactions)
        val fab: FloatingActionButton = findViewById(R.id.fabAdd)

        adapter = TransactionAdapter { transaction ->
            viewModel.deleteTransaction(transaction)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

        viewModel.allTransactions.observe(this) { transactions ->
            adapter.submitList(transactions)

            // Empty state
            if (transactions.isEmpty()) {
                layoutEmpty.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                layoutEmpty.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }

            // Hitung total pengeluaran per kategori buat pie chart
            val expenseByCategory = transactions
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

        viewModel.balance.observe(this) { balance ->
            val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            tvBalance.text = format.format(balance ?: 0.0)
        }

        fab.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    private fun buildLegend(data: List<Pair<String, Float>>) {
        llLegend.removeAllViews()
        val total = data.sumOf { it.second.toDouble() }.toFloat()
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

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

            val label = TextView(this).apply {
                text = "$category — ${format.format(value)} (${percent.toInt()}%)"
                textSize = 12f
                setTextColor(Color.parseColor("#333333"))
            }

            row.addView(colorBox)
            row.addView(label)
            llLegend.addView(row)
        }
    }
}
