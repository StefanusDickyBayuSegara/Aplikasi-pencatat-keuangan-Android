package com.example.expensetracker.data

import android.content.ContentValues
import android.content.Context

class TransactionRepository(context: Context) {

    private val dbHelper = DbHelper(context)

    fun insert(transaction: Transaction) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DbHelper.COL_AMOUNT, transaction.amount)
            put(DbHelper.COL_CATEGORY, transaction.category)
            put(DbHelper.COL_TYPE, transaction.type)
            put(DbHelper.COL_DATE, transaction.date)
            put(DbHelper.COL_NOTE, transaction.note)
        }
        db.insert(DbHelper.TABLE_NAME, null, values)
        db.close()
    }

    fun delete(transaction: Transaction) {
        val db = dbHelper.writableDatabase
        db.delete(DbHelper.TABLE_NAME, "${DbHelper.COL_ID} = ?", arrayOf(transaction.id.toString()))
        db.close()
    }

    fun getAllTransactions(): List<Transaction> {
        val list = mutableListOf<Transaction>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DbHelper.TABLE_NAME, null, null, null, null, null,
            "${DbHelper.COL_DATE} DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Transaction(
                        id = it.getInt(it.getColumnIndexOrThrow(DbHelper.COL_ID)),
                        amount = it.getDouble(it.getColumnIndexOrThrow(DbHelper.COL_AMOUNT)),
                        category = it.getString(it.getColumnIndexOrThrow(DbHelper.COL_CATEGORY)),
                        type = it.getString(it.getColumnIndexOrThrow(DbHelper.COL_TYPE)),
                        date = it.getLong(it.getColumnIndexOrThrow(DbHelper.COL_DATE)),
                        note = it.getString(it.getColumnIndexOrThrow(DbHelper.COL_NOTE))
                    )
                )
            }
        }
        db.close()
        return list
    }

    fun getBalance(): Double {
        val transactions = getAllTransactions()
        var balance = 0.0
        for (t in transactions) {
            balance += if (t.type == "income") t.amount else -t.amount
        }
        return balance
    }
}
