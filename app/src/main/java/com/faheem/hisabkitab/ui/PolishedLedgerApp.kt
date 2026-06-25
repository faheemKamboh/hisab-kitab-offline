package com.faheem.hisabkitab.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.faheem.hisabkitab.data.AppDatabase
import com.faheem.hisabkitab.data.LedgerRepository
import com.faheem.hisabkitab.domain.DateRangePreset
import com.faheem.hisabkitab.domain.LedgerSummary
import com.faheem.hisabkitab.domain.LedgerTransaction
import com.faheem.hisabkitab.domain.TransactionKind
import com.faheem.hisabkitab.util.CsvExporter
import com.faheem.hisabkitab.util.DateUtils
import com.faheem.hisabkitab.util.Money
import kotlinx.coroutines.launch
import kotlin.math.abs

private val BrandGreen = Color(0xFF00796B)
private val BrandMint = Color(0xFFE4F4F1)
private val SoftBackground = Color(0xFFF5FAF8)
private val DangerRed = Color(0xFFC62828)

private val PaymentModes = listOf("Cash", "Bank Transfer", "JazzCash", "EasyPaisa", "Card", "Other")
private val CoreCategories = listOf(
    "Cash Handover",
    "Cash Returned",
    "Shop Expense",
    "Stock Purchase",
    "Electricity",
    "Rent",
    "Transport",
    "Personal Withdrawal",
    "Settlement",
    "Adjustment",
    "Other"
)

@Composable
fun PolishedLedgerApp() {
    val context = LocalContext.current
    val viewModel = rememberPolishedLedgerViewModel(context)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    runCatching { CsvExporter.writeCsv(context, uri, viewModel.exportTransactions()) }
                        .onSuccess { snackbarHostState.showSnackbar("CSV exported.") }
                        .onFailure { snackbarHostState.showSnackbar(it.message ?: "Export failed.") }
                }
            }
        }
    )

    LaunchedEffect(uiState.snackMessage) {
        uiState.snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab != 1) {
                FloatingActionButton(onClick = { selectedTab = 1 }, containerColor = BrandGreen) {
                    Text("+", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf("Home", "Add", "Ledger", "Reports").forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Text(label.first().toString(), fontWeight = FontWeight.Bold) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Surface(
            color = SoftBackground,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(uiState, onAdd = { selectedTab = 1 }, onLedger = { selectedTab = 2 })
                1 -> AddTransactionScreen(viewModel, onSaved = { selectedTab = 0 })
                2 -> LedgerListScreen(uiState, viewModel)
                else -> ReportsScreen(uiState, onExportCsv = { csvLauncher.launch("hisab-kitab-ledger.csv") })
            }
        }
    }
}

@Composable
private fun rememberPolishedLedgerViewModel(context: Context): LedgerViewModel {
    return remember(context.applicationContext) {
        val database = AppDatabase.getInstance(context.applicationContext)
        LedgerViewModel(LedgerRepository(database.transactionDao()))
    }
}

@Composable
private fun HomeScreen(uiState: LedgerUiState, onAdd: () -> Unit, onLedger: () -> Unit) {
    val running = remember(uiState.allTransactions) { runningBalances(uiState.allTransactions) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BrandHeader("Hisab Kitab", "Offline settlement ledger") }
        item { BalanceCard(uiState.currentBalanceMinor) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricTile("Today Received", Money.format(abs(uiState.todaySummary.negativeImpactMinor)), "cash in", Modifier.weight(1f))
                MetricTile("Today Given", Money.format(uiState.todaySummary.positiveImpactMinor), "cash out", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricTile("Month Net", Money.format(uiState.monthSummary.netMovementMinor, showSign = true), "movement", Modifier.weight(1f))
                MetricTile("Total Entries", uiState.allTransactions.size.toString(), "records", Modifier.weight(1f))
            }
        }
        item { SectionTitle("Quick actions") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ActionCard("Add Entry", "record cash", "A", onAdd, Modifier.weight(1f))
                ActionCard("View Ledger", "running balance", "L", onLedger, Modifier.weight(1f))
            }
        }
        item { SectionHeader("Recent transactions", "latest 5") }
        if (uiState.recentTransactions.isEmpty()) {
            item { EmptyCard("No entries yet. Add your first transaction to start the ledger.") }
        } else {
            items(uiState.recentTransactions, key = { it.id }) { tx ->
                TransactionRow(tx, running[tx.id])
            }
        }
    }
}

@Composable
private fun AddTransactionScreen(viewModel: LedgerViewModel, onSaved: () -> Unit) {
    var selectedKind by rememberSaveable { mutableStateOf(TransactionKind.RECEIVED_FROM_BROTHER) }
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(TransactionKind.RECEIVED_FROM_BROTHER.defaultCategory) }
    var paymentMode by rememberSaveable { mutableStateOf("Cash") }
    var notes by rememberSaveable { mutableStateOf("") }
    val categoryOptions = remember(selectedKind) { categoryOptionsFor(selectedKind) }

    fun save(navigate: Boolean) {
        val valid = Money.parseToMinor(amount) != null
        viewModel.addTransaction(
            kind = selectedKind,
            amountText = amount,
            occurredAt = DateUtils.nowMillis(),
            category = category,
            paymentMode = paymentMode,
            notes = notes
        )
        if (valid) {
            amount = ""
            notes = ""
            if (navigate) onSaved()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BrandHeader("Add Transaction", "Guided entry to reduce mistakes") }
        item { SectionTitle("Transaction type") }
        item {
            ChoiceChips(
                options = TransactionKind.entries.map { it.shortLabel },
                selected = selectedKind.shortLabel,
                onSelected = { label ->
                    selectedKind = TransactionKind.entries.first { it.shortLabel == label }
                    category = selectedKind.defaultCategory
                }
            )
        }
        item { InfoCard(selectedKind.helperText) }
        item {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' || char == ',' } },
                label = { Text("Amount in PKR") },
                placeholder = { Text("15,600") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { SectionTitle("Category") }
        item { ChoiceChips(categoryOptions, category, onSelected = { category = it }) }
        item { SectionTitle("Payment mode") }
        item { ChoiceChips(PaymentModes, paymentMode, onSelected = { paymentMode = it }) }
        item { StaticField("Date & time", DateUtils.formatDateTime(DateUtils.nowMillis())) }
        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it.take(200) },
                label = { Text("Notes") },
                placeholder = { Text("Short reference, supplier, reason, or settlement note") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { save(true) }, modifier = Modifier.weight(1f)) { Text("Save") }
                OutlinedButton(onClick = { save(false) }, modifier = Modifier.weight(1f)) { Text("Save & Add") }
            }
        }
    }
}

@Composable
private fun LedgerListScreen(uiState: LedgerUiState, viewModel: LedgerViewModel) {
    val running = remember(uiState.allTransactions) { runningBalances(uiState.allTransactions) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BrandHeader("Ledger", "Running balance and filters") }
        item { PeriodChips(uiState.selectedRange, viewModel::selectRange) }
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = { Text("Search notes, category, payment mode") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { LedgerSummaryCard(uiState.summary) }
        if (uiState.transactions.isEmpty()) {
            item { EmptyCard("No entries match the selected filters.") }
        } else {
            items(uiState.transactions, key = { it.id }) { tx ->
                TransactionRow(tx, running[tx.id])
            }
        }
    }
}

@Composable
private fun ReportsScreen(uiState: LedgerUiState, onExportCsv: () -> Unit) {
    val categoryTotals = remember(uiState.transactions) { categoryTotals(uiState.transactions) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BrandHeader("Reports", "Summaries and export") }
        item { LedgerSummaryCard(uiState.summary) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricTile("Opening", Money.format(uiState.summary.openingBalanceMinor), "period", Modifier.weight(1f))
                MetricTile("Closing", Money.format(uiState.summary.closingBalanceMinor), "period", Modifier.weight(1f))
            }
        }
        item { SectionHeader("Category breakdown", "selected period") }
        if (categoryTotals.isEmpty()) {
            item { EmptyCard("No category data available for the selected period.") }
        } else {
            items(categoryTotals) { item -> CategoryBar(item) }
        }
        item { Button(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) { Text("Export CSV") } }
        item { InfoCard("CSV export is available now. PDF export, PIN lock, backup/restore, receipt attachments, and edit history are planned next.") }
    }
}

@Composable
private fun BrandHeader(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        BrandMark()
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF607D75))
        }
        StatusPill("Offline")
    }
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(BrandGreen, Color(0xFF26A69A)))),
        contentAlignment = Alignment.Center
    ) {
        Text("HK", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BalanceCard(balanceMinor: Long) {
    val status = when {
        balanceMinor > 0 -> "Brother/shop owes you"
        balanceMinor < 0 -> "You owe brother/shop"
        else -> "Settled"
    }
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(BrandGreen, Color(0xFF004D40))))
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Current Balance", color = Color.White.copy(alpha = 0.82f))
                Text(Money.format(balanceMinor), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                StatusPill(status, dark = true)
            }
        }
    }
}

@Composable
private fun MetricTile(title: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(20.dp), modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color(0xFF607D75))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(caption, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8EA39D))
        }
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, marker: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, shape = RoundedCornerShape(22.dp), modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(BrandMint), contentAlignment = Alignment.Center) {
                Text(marker, color = BrandGreen, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = Color(0xFF607D75))
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: LedgerTransaction, runningBalance: Long?) {
    val impactColor = if (transaction.impactMinor >= 0) BrandGreen else DangerRed
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(transaction.kind.shortLabel, fontWeight = FontWeight.Bold)
                    Text(DateUtils.formatShortDateTime(transaction.occurredAt), style = MaterialTheme.typography.labelMedium, color = Color(0xFF607D75))
                }
                Text(Money.format(transaction.impactMinor, showSign = true), color = impactColor, fontWeight = FontWeight.Black)
            }
            Text("${transaction.category} · ${transaction.paymentMode}", color = Color(0xFF455A64))
            if (transaction.notes.isNotBlank()) Text(transaction.notes, color = Color(0xFF607D75), maxLines = 2, overflow = TextOverflow.Ellipsis)
            runningBalance?.let { Text("Balance: ${Money.format(it)}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF607D75)) }
        }
    }
}

@Composable
private fun LedgerSummaryCard(summary: LedgerSummary) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryLine("Opening balance", Money.format(summary.openingBalanceMinor))
            SummaryLine("Positive movement", Money.format(summary.positiveImpactMinor, showSign = true), BrandGreen)
            SummaryLine("Negative movement", Money.format(summary.negativeImpactMinor, showSign = true), DangerRed)
            SummaryLine("Closing balance", Money.format(summary.closingBalanceMinor))
            SummaryLine("Entries", summary.transactionCount.toString())
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, valueColor: Color = Color(0xFF17201D)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF607D75))
        Text(value, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChoiceChips(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { option ->
            FilterChip(selected = selected == option, onClick = { onSelected(option) }, label = { Text(option) })
        }
    }
}

@Composable
private fun PeriodChips(selected: DateRangePreset, onSelect: (DateRangePreset) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(DateRangePreset.entries.toList()) { range ->
            FilterChip(selected = selected == range, onClick = { onSelect(range) }, label = { Text(range.label) })
        }
    }
}

@Composable
private fun StaticField(label: String, value: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF607D75))
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = BrandMint), modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(16.dp), color = Color(0xFF004D40))
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(18.dp), color = Color(0xFF607D75))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun SectionHeader(title: String, trailing: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        SectionTitle(title)
        Text(trailing, style = MaterialTheme.typography.labelMedium, color = Color(0xFF607D75))
    }
}

@Composable
private fun StatusPill(text: String, dark: Boolean = false) {
    val background = if (dark) Color.White.copy(alpha = 0.18f) else BrandMint
    val foreground = if (dark) Color.White else BrandGreen
    Surface(color = background, shape = CircleShape) {
        Text(text, color = foreground, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CategoryBar(item: CategoryTotal) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.category, fontWeight = FontWeight.SemiBold)
                Text(Money.format(item.amountMinor), fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = { item.fraction }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = BrandGreen, trackColor = BrandMint)
        }
    }
}

private fun categoryOptionsFor(kind: TransactionKind): List<String> {
    return (listOf(kind.defaultCategory) + CoreCategories).distinct()
}

private fun runningBalances(transactions: List<LedgerTransaction>): Map<Long, Long> {
    var running = 0L
    return transactions.sortedWith(compareBy<LedgerTransaction> { it.occurredAt }.thenBy { it.id }).associate { tx ->
        running += tx.impactMinor
        tx.id to running
    }
}

private data class CategoryTotal(val category: String, val amountMinor: Long, val fraction: Float)

private fun categoryTotals(transactions: List<LedgerTransaction>): List<CategoryTotal> {
    val totals = transactions
        .groupBy { it.category.ifBlank { "Other" } }
        .mapValues { entry -> entry.value.sumOf { abs(it.impactMinor) } }
        .filterValues { it > 0 }
        .toList()
        .sortedByDescending { it.second }
    val max = totals.firstOrNull()?.second ?: 0L
    return totals.map { (category, total) ->
        CategoryTotal(
            category = category,
            amountMinor = total,
            fraction = if (max == 0L) 0f else (total.toFloat() / max.toFloat()).coerceIn(0.05f, 1f)
        )
    }
}
