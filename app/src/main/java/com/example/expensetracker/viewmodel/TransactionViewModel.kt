package com.example.expensetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.data.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository(application)

    val allTransactions = MutableLiveData<List<Transaction>>()
    val balance = MutableLiveData<Double>()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val transactions = withContext(Dispatchers.IO) { repository.getAllTransactions() }
            val bal = withContext(Dispatchers.IO) { repository.getBalance() }
            allTransactions.value = transactions
            balance.value = bal
        }
    }

    fun addTransaction(amount: Double, category: String, type: String, note: String?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insert(
                    Transaction(
                        amount = amount,
                        category = category,
                        type = type,
                        date = System.currentTimeMillis(),
                        note = note
                    )
                )
            }
            loadData()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.delete(transaction) }
            loadData()
        }
    }
}
