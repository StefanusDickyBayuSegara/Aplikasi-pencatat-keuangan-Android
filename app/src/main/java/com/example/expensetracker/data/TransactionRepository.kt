package com.example.expensetracker.data

import android.content.ContentValues
import android.content.Context

class TransactionRepository(context: Context) {

    private val dbHelper = DbHelper(context)

    // ---- Transaksi ----
    // CATATAN: sengaja TIDAK memanggil db.close() di tiap fungsi.
    // dbHelper.readableDatabase/writableDatabase itu koneksi yang dipakai bareng-bareng
    // (di-cache oleh SQLiteOpenHelper). Kalau ditutup manual tiap fungsi selesai,
    // sementara ada fungsi lain yang masih jalan bersamaan (misal loadWallets()
    // dan loadData() jalan hampir bersamaan), koneksinya bentrok dan bikin crash
    // "connection pool has been closed".

    fun insert(transaction: Transaction) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DbHelper.COL_WALLET_ID, transaction.walletId)
            put(DbHelper.COL_AMOUNT, transaction.amount)
            put(DbHelper.COL_CATEGORY, transaction.category)
            put(DbHelper.COL_TYPE, transaction.type)
            put(DbHelper.COL_DATE, transaction.date)
            put(DbHelper.COL_NOTE, transaction.note)
        }
        db.insert(DbHelper.TABLE_NAME, null, values)
    }

    fun delete(transaction: Transaction) {
        val db = dbHelper.writableDatabase
        db.delete(DbHelper.TABLE_NAME, "${DbHelper.COL_ID} = ?", arrayOf(transaction.id.toString()))
    }

    fun getAllTransactionsByWallet(walletId: Int): List<Transaction> {
        val list = mutableListOf<Transaction>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DbHelper.TABLE_NAME, null,
            "${DbHelper.COL_WALLET_ID} = ?", arrayOf(walletId.toString()),
            null, null,
            "${DbHelper.COL_DATE} DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Transaction(
                        id = it.getInt(it.getColumnIndexOrThrow(DbHelper.COL_ID)),
                        walletId = it.getInt(it.getColumnIndexOrThrow(DbHelper.COL_WALLET_ID)),
                        amount = it.getDouble(it.getColumnIndexOrThrow(DbHelper.COL_AMOUNT)),
                        category = it.getString(it.getColumnIndexOrThrow(DbHelper.COL_CATEGORY)),
                        type = it.getString(it.getColumnIndexOrThrow(DbHelper.COL_TYPE)),
                        date = it.getLong(it.getColumnIndexOrThrow(DbHelper.COL_DATE)),
                        note = it.getString(it.getColumnIndexOrThrow(DbHelper.COL_NOTE))
                    )
                )
            }
        }
        return list
    }

    fun getBalanceByWallet(walletId: Int): Double {
        val transactions = getAllTransactionsByWallet(walletId)
        var balance = 0.0
        for (t in transactions) {
            balance += if (t.type == "income") t.amount else -t.amount
        }
        return balance
    }

    // ---- Dompet ----

    fun getAllWallets(): List<Wallet> {
        val list = mutableListOf<Wallet>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DbHelper.TABLE_WALLETS, null, null, null, null, null,
            "${DbHelper.COL_WALLET_PK} ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Wallet(
                        id = it.getInt(it.getColumnIndexOrThrow(DbHelper.COL_WALLET_PK)),
                        name = it.getString(it.getColumnIndexOrThrow(DbHelper.COL_WALLET_NAME))
                    )
                )
            }
        }
        return list
    }

    fun insertWallet(name: String): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put(DbHelper.COL_WALLET_NAME, name) }
        return db.insert(DbHelper.TABLE_WALLETS, null, values)
    }

    /** Hapus dompet beserta SEMUA transaksi yang ada di dalamnya */
    fun deleteWallet(walletId: Int) {
        val db = dbHelper.writableDatabase
        db.delete(DbHelper.TABLE_NAME, "${DbHelper.COL_WALLET_ID} = ?", arrayOf(walletId.toString()))
        db.delete(DbHelper.TABLE_WALLETS, "${DbHelper.COL_WALLET_PK} = ?", arrayOf(walletId.toString()))
    }
}
