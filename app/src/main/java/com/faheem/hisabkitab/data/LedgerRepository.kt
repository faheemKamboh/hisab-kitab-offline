package com.faheem.hisabkitab.data

import com.faheem.hisabkitab.domain.LedgerTransaction
import com.faheem.hisabkitab.domain.TransactionKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LedgerRepository(private val dao: TransactionDao) {
    fun observeTransactions(): Flow<List<LedgerTransaction>> = dao.observeTransactions().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun addTransaction(
        occurredAt: Long,
        kind: TransactionKind,
        amountMinor: Long,
        category: String,
        paymentMode: String,
        notes: String
    ) {
        require(amountMinor > 0) { "Amount must be greater than zero." }
        val now = System.currentTimeMillis()
        val cleanCategory = category.trim().ifBlank { kind.defaultCategory }
        val cleanPaymentMode = paymentMode.trim().ifBlank { "Cash" }
        val impact = amountMinor * kind.defaultImpactSign
        dao.insert(
            TransactionEntity(
                occurredAt = occurredAt,
                kind = kind.name,
                amountMinor = amountMinor,
                impactMinor = impact,
                category = cleanCategory,
                paymentMode = cleanPaymentMode,
                notes = notes.trim(),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun exportTransactions(): List<LedgerTransaction> = dao.getTransactionsForExport().map { it.toDomain() }
}

private fun TransactionEntity.toDomain(): LedgerTransaction = LedgerTransaction(
    id = id,
    occurredAt = occurredAt,
    kind = TransactionKind.fromStoredValue(kind),
    amountMinor = amountMinor,
    impactMinor = impactMinor,
    category = category,
    paymentMode = paymentMode,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)
