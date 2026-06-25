package com.faheem.hisabkitab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faheem.hisabkitab.data.LedgerRepository
import com.faheem.hisabkitab.domain.DateRangePreset
import com.faheem.hisabkitab.domain.LedgerSummary
import com.faheem.hisabkitab.domain.LedgerTransaction
import com.faheem.hisabkitab.domain.TransactionKind
import com.faheem.hisabkitab.util.DateUtils
import com.faheem.hisabkitab.util.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LedgerViewModel(private val repository: LedgerRepository) : ViewModel() {
    private val selectedRange = MutableStateFlow(DateRangePreset.MONTH)
    private val searchQuery = MutableStateFlow("")
    private val snackMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LedgerUiState> = combine(
        repository.observeTransactions(),
        selectedRange,
        searchQuery,
        snackMessage
    ) { transactions, range, query, message ->
        buildUiState(transactions, range, query, message)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LedgerUiState()
    )

    fun selectRange(range: DateRangePreset) {
        selectedRange.value = range
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun clearSnack() {
        snackMessage.value = null
    }

    fun addTransaction(
        kind: TransactionKind,
        amountText: String,
        occurredAt: Long,
        category: String,
        paymentMode: String,
        notes: String
    ) {
        val amountMinor = Money.parseToMinor(amountText)
        if (amountMinor == null || amountMinor <= 0) {
            snackMessage.value = "Enter a valid amount greater than zero."
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.addTransaction(
                    occurredAt = occurredAt,
                    kind = kind,
                    amountMinor = amountMinor,
                    category = category,
                    paymentMode = paymentMode,
                    notes = notes
                )
            }.onSuccess {
                snackMessage.value = "Entry saved."
            }.onFailure { error ->
                snackMessage.value = error.message ?: "Could not save entry."
            }
        }
    }

    suspend fun exportTransactions(): List<LedgerTransaction> = repository.exportTransactions()

    private fun buildUiState(
        allTransactions: List<LedgerTransaction>,
        range: DateRangePreset,
        query: String,
        message: String?
    ): LedgerUiState {
        val chronological = allTransactions.sortedWith(compareBy<LedgerTransaction> { it.occurredAt }.thenBy { it.id })
        val currentBalance = chronological.sumOf { it.impactMinor }
        val (start, end) = DateUtils.rangeFor(range)
        val rangeTransactions = chronological.filter { it.occurredAt in start until end }
        val opening = chronological.filter { it.occurredAt < start }.sumOf { it.impactMinor }
        val summary = LedgerSummary(
            openingBalanceMinor = opening,
            positiveImpactMinor = rangeTransactions.filter { it.impactMinor > 0 }.sumOf { it.impactMinor },
            negativeImpactMinor = rangeTransactions.filter { it.impactMinor < 0 }.sumOf { it.impactMinor },
            closingBalanceMinor = opening + rangeTransactions.sumOf { it.impactMinor },
            transactionCount = rangeTransactions.size
        )
        val displayTransactions = rangeTransactions
            .asReversed()
            .filter { tx ->
                if (query.isBlank()) true else {
                    val q = query.trim()
                    tx.kind.label.contains(q, ignoreCase = true) ||
                        tx.category.contains(q, ignoreCase = true) ||
                        tx.paymentMode.contains(q, ignoreCase = true) ||
                        tx.notes.contains(q, ignoreCase = true)
                }
            }
        return LedgerUiState(
            allTransactions = allTransactions,
            transactions = displayTransactions,
            recentTransactions = allTransactions.take(5),
            selectedRange = range,
            searchQuery = query,
            currentBalanceMinor = currentBalance,
            summary = summary,
            todaySummary = summaryFor(allTransactions, DateRangePreset.TODAY),
            monthSummary = summaryFor(allTransactions, DateRangePreset.MONTH),
            snackMessage = message
        )
    }

    private fun summaryFor(transactions: List<LedgerTransaction>, preset: DateRangePreset): LedgerSummary {
        val chronological = transactions.sortedWith(compareBy<LedgerTransaction> { it.occurredAt }.thenBy { it.id })
        val (start, end) = DateUtils.rangeFor(preset)
        val filtered = chronological.filter { it.occurredAt in start until end }
        val opening = chronological.filter { it.occurredAt < start }.sumOf { it.impactMinor }
        return LedgerSummary(
            openingBalanceMinor = opening,
            positiveImpactMinor = filtered.filter { it.impactMinor > 0 }.sumOf { it.impactMinor },
            negativeImpactMinor = filtered.filter { it.impactMinor < 0 }.sumOf { it.impactMinor },
            closingBalanceMinor = opening + filtered.sumOf { it.impactMinor },
            transactionCount = filtered.size
        )
    }
}

data class LedgerUiState(
    val allTransactions: List<LedgerTransaction> = emptyList(),
    val transactions: List<LedgerTransaction> = emptyList(),
    val recentTransactions: List<LedgerTransaction> = emptyList(),
    val selectedRange: DateRangePreset = DateRangePreset.MONTH,
    val searchQuery: String = "",
    val currentBalanceMinor: Long = 0,
    val summary: LedgerSummary = LedgerSummary(),
    val todaySummary: LedgerSummary = LedgerSummary(),
    val monthSummary: LedgerSummary = LedgerSummary(),
    val snackMessage: String? = null
)

class LedgerViewModelFactory(private val repository: LedgerRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LedgerViewModel::class.java)) {
            return LedgerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
