package com.example.expensetracker.data

/**
 * Daftar kategori terpusat, dipakai bareng-bareng sama AddTransactionActivity
 * dan SettingsActivity (buat budget limit), biar konsisten.
 */
object Categories {
    val EXPENSE = listOf(
        "Makan", "Transport", "Belanja", "Hiburan",
        "Tagihan", "Kesehatan", "Pendidikan", "Lainnya"
    )
    val INCOME = listOf(
        "Gaji", "Hadiah", "Investasi", "Lainnya"
    )
}
