package com.faheem.hisabkitab.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["occurredAt"]), Index(value = ["kind"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val occurredAt: Long,
    val kind: String,
    val amountMinor: Long,
    val impactMinor: Long,
    val category: String,
    val paymentMode: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)
