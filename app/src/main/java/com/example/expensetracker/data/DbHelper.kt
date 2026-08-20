package com.example.expensetracker.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "expense_tracker.db"
        // PENTING: versi dinaikkan dari 1 ke 2 karena nambah tabel wallets + kolom wallet_id.
        // Ini otomatis nge-trigger onUpgrade() di HP yang udah pernah install versi lama,
        // TANPA menghapus data transaksi yang udah ada.
        private const val DATABASE_VERSION = 2

        const val TABLE_NAME = "transactions"
        const val COL_ID = "id"
        const val COL_WALLET_ID = "wallet_id"
        const val COL_AMOUNT = "amount"
        const val COL_CATEGORY = "category"
        const val COL_TYPE = "type"
        const val COL_DATE = "date"
        const val COL_NOTE = "note"

        const val TABLE_WALLETS = "wallets"
        const val COL_WALLET_PK = "id"
        const val COL_WALLET_NAME = "name"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_WALLETS (
                $COL_WALLET_PK INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_WALLET_NAME TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_WALLET_ID INTEGER NOT NULL DEFAULT 1,
                $COL_AMOUNT REAL NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_TYPE TEXT NOT NULL,
                $COL_DATE INTEGER NOT NULL,
                $COL_NOTE TEXT
            )
            """.trimIndent()
        )

        // Bikin dompet default pertama kali app di-install
        val values = ContentValues().apply { put(COL_WALLET_NAME, "Pribadi") }
        db.insert(TABLE_WALLETS, null, values)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Tambah tabel wallets kalau belum ada (buat HP yang udah pernah install versi lama)
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_WALLETS (
                    $COL_WALLET_PK INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_WALLET_NAME TEXT NOT NULL
                )
                """.trimIndent()
            )

            // Kalau tabel wallets masih kosong, bikinin dompet default
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_WALLETS", null)
            cursor.moveToFirst()
            val count = cursor.getInt(0)
            cursor.close()
            if (count == 0) {
                val values = ContentValues().apply { put(COL_WALLET_NAME, "Pribadi") }
                db.insert(TABLE_WALLETS, null, values)
            }

            // Tambah kolom wallet_id ke transactions kalau belum ada.
            // Transaksi lama otomatis kepakai default 1 (dompet "Pribadi"), jadi data lama AMAN.
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_WALLET_ID INTEGER NOT NULL DEFAULT 1")
            } catch (e: Exception) {
                // Kolom udah ada (misal upgrade dijalankan 2x), aman diabaikan
            }
        }
    }
}
