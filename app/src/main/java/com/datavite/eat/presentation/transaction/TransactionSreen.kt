package com.datavite.eat.presentation.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.datavite.eat.app.BottomNavigationBar
import com.datavite.eat.data.local.model.SyncStatus
import com.datavite.eat.domain.model.DomainTransaction
import com.datavite.eat.presentation.components.TiqtaqTopBar
import com.datavite.eat.utils.TransactionBroker
import com.datavite.eat.utils.TransactionType
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.TransactionScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Destination<RootGraph>
@Composable
fun TransactionScreen(
    navigator: DestinationsNavigator,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val transactionUiState by viewModel.transactionUiState.collectAsState()

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val snackbarHostState = remember { SnackbarHostState() }

    // Show bottom sheet when selectedTransaction changes
    LaunchedEffect(transactionUiState.selectedTransaction) {
        if (transactionUiState.selectedTransaction != null) bottomSheetState.show()
        else bottomSheetState.hide()
    }

    // Show snackbar for messages
    LaunchedEffect(transactionUiState.errorMessage) {
        transactionUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(transactionUiState.infoMessage) {
        transactionUiState.infoMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearInfoMessage()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TiqtaqTopBar(
                scrollBehavior = scrollBehavior,
                destinationsNavigator = navigator,
                onSearchQueryChanged = {  },
                onSearchClosed = {  },
                onRefresh = { viewModel.onRefresh() }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.showCreateTransactionForm()
                },
                icon = { Icon(Icons.Default.Add, "Add Transaction") },
                text = { Text("New Transaction") }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                route = TransactionScreenDestination.route,
                destinationsNavigator = navigator
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (transactionUiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {

                    // Transactions List
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(transactionUiState.transactions) { transaction ->
                            TransactionCard(
                                transaction = transaction,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.selectTransaction(transaction)
                                },
                            )
                        }
                    }
                }
            }
        }

        // Transaction Details Bottom Sheet
        transactionUiState.selectedTransaction?.let { selectedTransaction ->
            ModalBottomSheet(
                sheetState = bottomSheetState,
                onDismissRequest = { viewModel.unselectTransaction() }
            ) {
                TransactionDetailModal(
                    transaction = selectedTransaction,
                    onDelete = {
                        viewModel.deleteTransaction(selectedTransaction)
                        viewModel.unselectTransaction()
                    },
                    onClose = { viewModel.unselectTransaction() }
                )
            }
        }

        // Create Transaction Dialog
        if (transactionUiState.isCreatingTransaction) {
            CreateTransactionDialog(
                transactionUiState = transactionUiState,
                onDismiss = { viewModel.hideCreateTransactionForm() },
                onAmountChange = { viewModel.updateTransactionAmount(it) },
                onReasonChange = { viewModel.updateTransactionReason(it) },
                onParticipantChange = { viewModel.updateParticipant(it) },
                onTypeChange = { viewModel.updateTransactionType(it) },
                onBrokerChange = { viewModel.updateTransactionBroker(it) },
                onCreateTransaction = { viewModel.createTransaction() }
            )
        }
    }
}

@Composable
fun TransactionCard(
    transaction: DomainTransaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDeposit = transaction.transactionType == TransactionType.DEPOSIT

    // Material Design 3 color system
    val amountColor = if (isDeposit) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    val containerColor = if (isDeposit) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    val icon = if (isDeposit) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward

    // Accessible broker icons with proper content descriptions
    val (brokerIcon, brokerDescription) = when (transaction.transactionBroker) {
        TransactionBroker.CASHIER -> Icons.Outlined.AccountBalanceWallet to "Cash payment"
        TransactionBroker.ORANGE_MONEY -> Icons.Outlined.Payment to "Orange Money"
        TransactionBroker.MTN_MOBILE_MONEY -> Icons.Outlined.Smartphone to "MTN Mobile Money"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                role = Role.Button
            )
            .semantics {
                // Screen reader support
                contentDescription = buildString {
                    append("Transaction: ${transaction.reason}. ")
                    append("Amount: ${transaction.amount} FCFA. ")
                    append("Type: ${if (isDeposit) "deposit" else "withdrawal"}. ")
                    append("Participant: ${transaction.participant}. ")
                    append("Date: ${transaction.created.substring(0, 10)}")
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .heightIn(min = 72.dp), // Minimum touch target height
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading icon with semantic meaning
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (isDeposit) "Income" else "Expense",
                    tint = amountColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Main content - Flexible column that takes available space
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Reason with proper overflow handling
                Text(
                    text = transaction.reason,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics {
                        // Make reason focusable for screen readers
                        heading()
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Participant with improved contrast
                Text(
                    text = transaction.participant,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Metadata row with icons for better scannability
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Broker with icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = brokerIcon,
                            contentDescription = null, // Decorative, described in parent
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = transaction.transactionBroker.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, false)
                        )
                    }

                    // Separator dot
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Date
                    Text(
                        text = transaction.created.substring(0, 10),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }
            }

            // Amount section - Fixed width to prevent layout shifts
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                // Amount with proper formatting and color coding
                Text(
                    text = buildString {
                        append(if (isDeposit) "+" else "-")
                        append(transaction.amount.toInt()) // Remove decimals for cleaner display
                        append(" FCFA")
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(min = 80.dp) // Prevent text compression

                )

                // Status indicator for sync state
                if (transaction.syncStatus != SyncStatus.SYNCED) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (transaction.syncStatus) {
                                    SyncStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                                    SyncStatus.FAILED -> MaterialTheme.colorScheme.error
                                    else -> Color.Transparent
                                }
                            )
                    )
                }
            }
        }
    }
}

// Alternative version with expanded layout for better long text handling
@Composable
fun ExpandedTransactionCard(
    transaction: DomainTransaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDeposit = transaction.transactionType == TransactionType.DEPOSIT
    val amountColor = if (isDeposit) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.error

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                role = Role.Button
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row with amount and type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDeposit) Icons.Outlined.ArrowDownward
                        else Icons.Outlined.ArrowUpward,
                        contentDescription = if (isDeposit) "Income" else "Expense",
                        tint = amountColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDeposit) "Deposit" else "Withdrawal",
                        style = MaterialTheme.typography.labelLarge,
                        color = amountColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Amount
                Text(
                    text = "${transaction.amount.toInt()} FCFA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reason with more space
            Text(
                text = transaction.reason,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2, // Allow 2 lines for longer reasons
                overflow = TextOverflow.Ellipsis,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.1
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // Participant
                    Text(
                        text = transaction.participant,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Broker and date
                    Text(
                        text = "${transaction.transactionBroker.name.replace("_", " ")} • ${transaction.created.substring(0, 10)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Sync status indicator
                if (transaction.syncStatus != SyncStatus.SYNCED) {
                    val statusColor = when (transaction.syncStatus) {
                        SyncStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                        SyncStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> Color.Transparent
                    }
                    val statusText = when (transaction.syncStatus) {
                        SyncStatus.PENDING -> "Pending"
                        SyncStatus.FAILED -> "Failed"
                        else -> ""
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }
            }
        }
    }
}

// Usage in your LazyColumn with adaptive cards based on content
@Composable
fun TransactionCardAdaptive(
    transaction: DomainTransaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use expanded layout for transactions with long text
    val useExpandedLayout = transaction.reason.length > 40 ||
            transaction.participant.length > 25

    if (useExpandedLayout) {
        ExpandedTransactionCard(
            transaction = transaction,
            onClick = onClick,
            modifier = modifier
        )
    } else {
        TransactionCard(
            transaction = transaction,
            onClick = onClick,
            modifier = modifier
        )
    }
}
@Composable
fun TransactionDetailModal(
    transaction: DomainTransaction,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val isDeposit = transaction.transactionType == TransactionType.DEPOSIT
    val amountColor = if (isDeposit) Color(0xFF4CAF50) else Color(0xFFF44336)

    Column(modifier = Modifier.padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transaction Details",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Amount Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = amountColor.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${if (isDeposit) "Deposit" else "Withdrawal"} Amount",
                    style = MaterialTheme.typography.labelLarge,
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${transaction.amount} FCFA",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Details
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailRow("Reason", transaction.reason)
            DetailRow("Participant", transaction.participant)
            DetailRow("Type", transaction.transactionType.name)
            DetailRow("Broker", transaction.transactionBroker.name.replace("_", " "))
            DetailRow("Date", transaction.created)
            DetailRow("Status", transaction.syncStatus.name)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete")
            }

            Button(
                onClick = onClose,
                modifier = Modifier.weight(1f)
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun CreateTransactionDialog(
    transactionUiState: TransactionUiState,
    onDismiss: () -> Unit,
    onAmountChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onParticipantChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onBrokerChange: (TransactionBroker) -> Unit,
    onCreateTransaction: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Amount
                OutlinedTextField(
                    value = transactionUiState.transactionAmount,
                    onValueChange = onAmountChange,
                    label = { Text("Amount (FCFA)") },
                    placeholder = { Text("Enter amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    )
                )

                // Reason
                OutlinedTextField(
                    value = transactionUiState.transactionReason,
                    onValueChange = onReasonChange,
                    label = { Text("Reason") },
                    placeholder = { Text("Enter transaction reason") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Participant
                OutlinedTextField(
                    value = transactionUiState.participant,
                    onValueChange = onParticipantChange,
                    label = { Text("Participant") },
                    placeholder = { Text("Enter participant name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Transaction Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionType.entries.forEach { type ->
                        FilterChip(
                            selected = transactionUiState.selectedType == type,
                            onClick = { onTypeChange(type) },
                            label = { Text(type.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (type) {
                                    TransactionType.DEPOSIT -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                    TransactionType.WITHDRAWAL -> Color(0xFFF44336).copy(alpha = 0.1f)
                                },
                                selectedLabelColor = when (type) {
                                    TransactionType.DEPOSIT -> Color(0xFF4CAF50)
                                    TransactionType.WITHDRAWAL -> Color(0xFFF44336)
                                }
                            )
                        )
                    }
                }

                // Transaction Broker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionBroker.entries.forEach { broker ->
                        FilterChip(
                            selected = transactionUiState.selectedTransactionBroker == broker,
                            onClick = { onBrokerChange(broker) },
                            label = { Text(broker.name.replace("_", " ")) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCreateTransaction,
                enabled = transactionUiState.transactionAmount.isNotBlank() &&
                        transactionUiState.transactionReason.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}