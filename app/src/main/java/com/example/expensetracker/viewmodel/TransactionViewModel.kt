package com.example.expensetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.data.TransactionRepository
import com.example.expensetracker.data.Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository(application)

    val allTransactions = MutableLiveData<List<Transaction>>()
    val balance = MutableLiveData<Double>()
    val wallets = MutableLiveData<List<Wallet>>()

    var currentWalletId: Int = 1
        private set

    init {
        loadWallets()
        loadData()
    }

    private suspend fun fetchWallets(): List<Wallet> =
        withContext(Dispatchers.IO) { repository.getAllWallets() }

    private fun loadWallets() {
        viewModelScope.launch {
            wallets.value = fetchWallets()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val transactions = withContext(Dispatchers.IO) {
                repository.getAllTransactionsByWallet(currentWalletId)
            }
            val bal = withContext(Dispatchers.IO) {
                repository.getBalanceByWallet(currentWalletId)
            }
            allTransactions.value = transactions
            balance.value = bal
        }
    }

    fun switchWallet(walletId: Int) {
        if (walletId == currentWalletId) return
        currentWalletId = walletId
        loadData()
    }

    fun addWalletAndSwitch(name: String) {
        viewModelScope.launch {
            val newId = withContext(Dispatchers.IO) { repository.insertWallet(name).toInt() }
            wallets.value = fetchWallets()
            currentWalletId = newId
            loadData()
        }
    }

    fun deleteWallet(walletId: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.deleteWallet(walletId) }
            val updatedWallets = fetchWallets()
            wallets.value = updatedWallets
            if (currentWalletId == walletId) {
                currentWalletId = updatedWallets.firstOrNull()?.id ?: 1
            }
            loadData()
        }
    }

    fun addTransaction(walletId: Int, amount: Double, category: String, type: String, note: String?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insert(
                    Transaction(
                        walletId = walletId,
                        amount = amount,
                        category = category,
                        type = type,
                        date = System.currentTimeMillis(),
                        note = note
                    )
                )
            }
            if (walletId == currentWalletId) loadData()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.delete(transaction) }
            loadData()
        }
    }
}
