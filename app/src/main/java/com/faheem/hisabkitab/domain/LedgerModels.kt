package com.faheem.hisabkitab.domain

enum class TransactionKind(
    val label: String,
    val shortLabel: String,
    val defaultCategory: String,
    val helperText: String,
    val defaultImpactSign: Int
) {
    RECEIVED_FROM_BROTHER(
        label = "Received from Brother",
        shortLabel = "Received",
        defaultCategory = "Cash Handover",
        helperText = "You received shop money, so your payable increases.",
        defaultImpactSign = -1
    ),
    GIVEN_TO_BROTHER(
        label = "Given to Brother",
        shortLabel = "Given",
        defaultCategory = "Cash Returned",
        helperText = "You gave money back, so your payable reduces.",
        defaultImpactSign = 1
    ),
    EXPENSE_PAID_BY_ME(
        label = "Expense Paid by Me",
        shortLabel = "My Expense",
        defaultCategory = "Shop Expense",
        helperText = "You paid from your pocket, so brother/shop owes you more.",
        defaultImpactSign = 1
    ),
    EXPENSE_FROM_SHOP_CASH(
        label = "Expense from Shop Cash",
        shortLabel = "Shop Cash",
        defaultCategory = "Shop Expense",
        helperText = "You spent shop cash on shop work, so your payable reduces.",
        defaultImpactSign = 1
    ),
    SETTLEMENT_TO_BROTHER(
        label = "Settlement Paid",
        shortLabel = "Settlement Paid",
        defaultCategory = "Settlement",
        helperText = "Final/partial payment to brother/shop.",
        defaultImpactSign = 1
    ),
    SETTLEMENT_FROM_BROTHER(
        label = "Settlement Received",
        shortLabel = "Settlement Received",
        defaultCategory = "Settlement",
        helperText = "Brother/shop paid you back.",
        defaultImpactSign = -1
    ),
    ADJUSTMENT_PLUS(
        label = "Adjustment +",
        shortLabel = "Adjust +",
        defaultCategory = "Adjustment",
        helperText = "Correction that increases brother/shop payable to you.",
        defaultImpactSign = 1
    ),
    ADJUSTMENT_MINUS(
        label = "Adjustment -",
        shortLabel = "Adjust -",
        defaultCategory = "Adjustment",
        helperText = "Correction that increases your payable to brother/shop.",
        defaultImpactSign = -1
    );

    companion object {
        fun fromStoredValue(value: String): TransactionKind = entries.firstOrNull { it.name == value } ?: ADJUSTMENT_PLUS
    }
}

enum class DateRangePreset(val label: String) {
    TODAY("Today"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    ALL("All")
}

data class LedgerTransaction(
    val id: Long,
    val occurredAt: Long,
    val kind: TransactionKind,
    val amountMinor: Long,
    val impactMinor: Long,
    val category: String,
    val paymentMode: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class LedgerSummary(
    val openingBalanceMinor: Long = 0,
    val positiveImpactMinor: Long = 0,
    val negativeImpactMinor: Long = 0,
    val closingBalanceMinor: Long = 0,
    val transactionCount: Int = 0
) {
    val netMovementMinor: Long = positiveImpactMinor + negativeImpactMinor
}
