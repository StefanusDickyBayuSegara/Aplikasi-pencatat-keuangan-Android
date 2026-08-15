package com.example.expensetracker.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.R
import com.example.expensetracker.data.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val onItemLongClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(DiffCallback()) {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: TextView = itemView.findViewById(R.id.tvIcon)
        val category: TextView = itemView.findViewById(R.id.tvCategory)
        val amount: TextView = itemView.findViewById(R.id.tvAmount)
        val date: TextView = itemView.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)

        holder.icon.text = getCategoryIcon(transaction.category)
        holder.category.text = transaction.category

        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        val isIncome = transaction.type == "income"
        val sign = if (isIncome) "+" else "-"
        holder.amount.text = "$sign ${format.format(transaction.amount)}"

        // Warna beda: hijau buat pemasukan, merah buat pengeluaran
        val color = if (isIncome) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
        holder.amount.setTextColor(color)

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.date.text = sdf.format(Date(transaction.date))

        holder.itemView.setOnLongClickListener {
            onItemLongClick(transaction)
            true
        }
    }

    // Mapping kategori ke emoji. Kalau kategori nggak ada di list, pakai icon default.
    private fun getCategoryIcon(category: String): String {
        return when (category.trim().lowercase()) {
            "makan", "makanan", "makan siang", "makan malam" -> "🍔"
            "transport", "transportasi", "bensin", "ojek" -> "🚗"
            "belanja", "shopping" -> "🛍️"
            "gaji", "salary" -> "💰"
            "hiburan", "nonton", "game" -> "🎮"
            "tagihan", "listrik", "air", "internet" -> "🧾"
            "kesehatan", "obat", "dokter" -> "💊"
            "pendidikan", "sekolah", "kuliah" -> "📚"
            "hadiah", "gift" -> "🎁"
            "investasi" -> "📈"
            else -> "💵"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction) =
            oldItem == newItem
    }
}
