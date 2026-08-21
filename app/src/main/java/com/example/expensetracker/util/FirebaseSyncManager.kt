package com.example.expensetracker.util

import android.content.Context
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.data.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Backup & restore data (wallets + transactions) ke/dari Firestore,
 * per akun (dikelompokkan berdasarkan UID user yang login).
 * Pakai listener callback (bukan coroutine) biar nggak perlu nambah
 * dependency kotlinx-coroutines-play-services segala.
 */
object FirebaseSyncManager {

    private fun db() = FirebaseFirestore.getInstance()
    private fun auth() = FirebaseAuth.getInstance()

    fun currentUser() = auth().currentUser

    fun logout() {
        auth().signOut()
    }

    fun backupToCloud(context: Context, onResult: (success: Boolean, message: String?) -> Unit) {
        val uid = auth().currentUser?.uid
        if (uid == null) {
            onResult(false, "Belum login")
            return
        }

        val repository = TransactionRepository(context)

        val wallets = repository.getAllWallets().map { w ->
            mapOf("id" to w.id, "name" to w.name)
        }
        val transactions = repository.getAllTransactionsAllWallets().map { t ->
            mapOf(
                "id" to t.id,
                "walletId" to t.walletId,
                "amount" to t.amount,
                "category" to t.category,
                "type" to t.type,
                "date" to t.date,
                "note" to (t.note ?: "")
            )
        }

        val data = mapOf(
            "wallets" to wallets,
            "transactions" to transactions,
            "backupAt" to System.currentTimeMillis()
        )

        db().collection("users").document(uid).set(data)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    @Suppress("UNCHECKED_CAST")
    fun restoreFromCloud(context: Context, onResult: (success: Boolean, message: String?) -> Unit) {
        val uid = auth().currentUser?.uid
        if (uid == null) {
            onResult(false, "Belum login")
            return
        }

        val repository = TransactionRepository(context)

        db().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onResult(false, "Belum ada backup buat akun ini")
                    return@addOnSuccessListener
                }
                try {
                    val walletsRaw = doc.get("wallets") as? List<Map<String, Any>> ?: emptyList()
                    val transactionsRaw = doc.get("transactions") as? List<Map<String, Any>> ?: emptyList()

                    repository.clearAllData()

                    for (w in walletsRaw) {
                        val id = (w["id"] as? Long)?.toInt() ?: continue
                        val name = w["name"] as? String ?: "Dompet"
                        repository.insertWalletWithId(id, name)
                    }

                    for (t in transactionsRaw) {
                        val id = (t["id"] as? Long)?.toInt() ?: 0
                        val walletId = (t["walletId"] as? Long)?.toInt() ?: 1
                        val amount = (t["amount"] as? Double) ?: 0.0
                        val category = t["category"] as? String ?: "Lainnya"
                        val type = t["type"] as? String ?: "expense"
                        val date = (t["date"] as? Long) ?: System.currentTimeMillis()
                        val note = (t["note"] as? String)?.ifBlank { null }

                        repository.insertTransactionWithId(
                            Transaction(
                                id = id, walletId = walletId, amount = amount,
                                category = category, type = type, date = date, note = note
                            )
                        )
                    }

                    onResult(true, null)
                } catch (e: Exception) {
                    onResult(false, e.message)
                }
            }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }
}
