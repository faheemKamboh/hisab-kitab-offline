package com.faheem.hisabkitab.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

@Composable
fun LedgerApp() {
    val context = LocalContext.current
    val viewModel = rememberLedgerViewModel(context)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    runCatching {
                        CsvExporter.writeCsv(context, uri, viewModel.exportTransactions())
                    }.onSuccess {
                        snackbarHostState.showSnackbar("CSV exported.")
                    }.onFailure { error ->
                        snackbarHostState.showSnackbar(error.message ?: "Export failed.")
                    }
                }
            }
        }
    )

    LaunchedEffect(uiState.snackMessage) {
        uiState.snackMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab != 1) {
                FloatingActionButton(onClick = { selectedTab = 1 }) {
                    Text("+")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                listOf("Home", "Add", "Ledger", "Reports").forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Text(label.first().toString()) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(uiState = uiState, onAdd = { selectedTab = 1 }, onLedger = { selectedTab = 2 })
                1 -> AddEntryScreen(viewModel = viewModel, onSaved = { selectedTab = 0 })
                2 -> LedgerScreen(uiState = uiState, viewModel = viewModel)
                else -> ReportsScreen(uiState = uiState, onExportCsv = { csvLauncher.launch("hisab-kitab-ledger.csv") })
            }
        }
    }
}

@Composable
private fun rememberLedgerViewModel(context: Context): LedgerViewModel {
    return remember(context.applicationContext) {
        val database = AppDatabase.getInstance(context.applicationContext)
        LedgerViewModel(LedgerRepository(database.transactionDao()))
    }
}

@Composable
private fun DashboardScreen(uiState: LedgerUiState, onAdd: () -> Unit, onLedger: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { AppHeader(title = "Hisab Kitab", subtitle = "Offline settlement ledger") }
        item { BalanceCard(balanceMinor = uiState.currentBalanceMinor) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryTile("Today In", Money.format(uiState.todaySummary.positiveImpactMinor), Modifier.weight(1f))
                SummaryTile("Today Out", Money.format(uiState.todaySummary.negativeImpactMinor), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryTile("Month Net", Money.format(uiState.monthSummary.netMovementMinor, showSign = true), Modifier.weight(1f))
                SummaryTile("Entries", uiState.allTransactions.size.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onAdd, modifier = Modifier.weight(1f)) { Text("Add Entry") }
                OutlinedButton(onClick = onLedger, modifier = Modifier.weight(1f)) { Text("View Ledger") }
            }
        }
        item { SectionTitle("Recent entries") }
        if (uiState.recentTransactions.isEmpty()) {
            item { EmptyCard("No entries yet. Add your first received, given, expense, or settlement entry.") }
        } else {
            items(uiState.recentTransactions) { tx -> TransactionCard(transaction = tx) }
        }
    }
}

@Composable
private fun AddEntryScreen(viewModel: LedgerViewModel, onSaved: () -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var paymentMode by rememberSaveable { mutableStateOf("Cash") }
    var notes by rememberSaveable { mutableStateOf("") }
    var selectedKind by rememberSaveable { mutableStateOf(TransactionKind.RECEIVED_FROM_BROTHER) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { AppHeader(title = "Add Transaction", subtitle = "Record a new ledger entry") }
        item { SectionTitle("Transaction type") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TransactionKind.entries) { kind ->
                    FilterChip(
                        selected = selectedKind == kind,
                        onClick = { selectedKind = kind },
                        label = { Text(kind.shortLabel) }
                    )
                }
            }
        }
        item { HelpCard(selectedKind.helperText) }
        item {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount PKR") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                placeholder = { Text(selectedKind.defaultCategory) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = paymentMode,
                onValueChange = { paymentMode = it },
                label = { Text("Payment mode") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
        item {
            Button(
                onClick = {
                    viewModel.addTransaction(
                        kind = selectedKind,
                        amountText = amount,
                        occurredAt = DateUtils.nowMillis(),
                        category = category,
                        paymentMode = paymentMode,
                        notes = notes
                    )
                    amount = ""
                    category = ""
                    notes = ""
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Entry")
            }
        }
    }
}

@Composable
private fun LedgerScreen(uiState: LedgerUiState, viewModel: LedgerViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { AppHeader(title = "Ledger", subtitle = "Running balance") }
        item { PeriodChips(selected = uiState.selectedRange, onSelect = viewModel::selectRange) }
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = { Text("Search notes, category, or type") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item { SummaryCard(uiState.summary) }
        if (uiState.transactions.isEmpty()) {
            item { EmptyCard("No entries match this period or search.") }
        } else {
            items(uiState.transactions) { tx -> TransactionCard(transaction = tx) }
        }
    }
}

@Composable
private fun ReportsScreen(uiState: LedgerUiState, onExportCsv: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { AppHeader(title = "Reports", subtitle = "Period totals and export") }
        item { SummaryCard(uiState.summary) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryTile("Opening", Money.format(uiState.summary.openingBalanceMinor), Modifier.weight(1f))
                SummaryTile("Closing", Money.format(uiState.summary.closingBalanceMinor), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryTile("Positive", Money.format(uiState.summary.positiveImpactMinor), Modifier.weight(1f))
                SummaryTile("Negative", Money.format(uiState.summary.negativeImpactMinor), Modifier.weight(1f))
            }
        }
        item { Button(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) { Text("Export CSV") } }
        item { EmptyCard("PDF export, PIN lock, and full backup/restore are planned for the next hardened version.") }
    }
}

@Composable
private fun AppHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Current Balance", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f))
            Text(Money.format(balanceMinor), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            Text(status, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun PeriodChips(selected: DateRangePreset, onSelect: (DateRangePreset) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(DateRangePreset.entries) { range ->
            FilterChip(selected = selected == range, onClick = { onSelect(range) }, label = { Text(range.label) })
        }
    }
}

@Composable
private fun SummaryCard(summary: LedgerSummary) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryLine("Opening", Money.format(summary.openingBalanceMinor))
            SummaryLine("Positive movement", Money.format(summary.positiveImpactMinor, showSign = true))
            SummaryLine("Negative movement", Money.format(summary.negativeImpactMinor, showSign = true))
            SummaryLine("Closing", Money.format(summary.closingBalanceMinor))
            SummaryLine("Entries", summary.transactionCount.toString())
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(18.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TransactionCard(transaction: LedgerTransaction) {
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(transaction.kind.shortLabel, fontWeight = FontWeight.SemiBold)
                Text(Money.format(transaction.impactMinor, showSign = true), fontWeight = FontWeight.Bold)
            }
            Text(DateUtils.formatShortDateTime(transaction.occurredAt), style = MaterialTheme.typography.labelMedium)
            Text("${transaction.category} · ${transaction.paymentMode}")
            if (transaction.notes.isNotBlank()) Text(transaction.notes, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun HelpCard(text: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f))) {
        Text(text, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text)
            Spacer(Modifier.height(4.dp))
        }
    }
}
