package com.datavite.eat.presentation.billing

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.datavite.eat.domain.model.DomainBilling
import com.datavite.eat.app.BottomNavigationBar
import com.datavite.eat.domain.model.DomainBillingPayment
import com.datavite.eat.presentation.components.TiqtaqTopBar
import com.datavite.eat.utils.BillPDFExporter
import com.datavite.eat.utils.TransactionBroker
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.BillingScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BillingScreen(
    navigator: DestinationsNavigator,
    viewModel: BillingViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val billingUiState by viewModel.billingUiState.collectAsState()
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val paymentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle snackbar messages
    LaunchedEffect(billingUiState.errorMessage) {
        billingUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(billingUiState.infoMessage) {
        billingUiState.infoMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearInfoMessage()
        }
    }

    // Show detail bottom sheet when billing is selected
    LaunchedEffect(billingUiState.selectedBilling) {
        if (billingUiState.selectedBilling != null) {
            detailSheetState.show()
        } else {
            detailSheetState.hide()
        }
    }

    // Show payment bottom sheet when triggered
    LaunchedEffect(billingUiState.isPaymentSheetVisible) {
        if (billingUiState.isPaymentSheetVisible) {
            paymentSheetState.show()
        } else {
            paymentSheetState.hide()
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
                onSearchQueryChanged = {},
                onSearchClosed = {},
                onRefresh = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://m.facebook.com/profile.php?id=61555380762150".toUri()
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(intent)
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = { BottomNavigationBar(route = BillingScreenDestination.route, destinationsNavigator = navigator) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (billingUiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (billingUiState.availableBillings.isEmpty()) {
                EmptyBillingState()
            } else {
                BillingList(
                    billings = billingUiState.availableBillings,
                    onBillingClick = { billing ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.selectBilling(billing)
                    }
                )
            }
        }

        // --- Billing Detail Bottom Sheet ---
        billingUiState.selectedBilling?.let { selectedBilling ->
            val billingPdfView = rememberBillPdfView(selectedBilling)

            ModalBottomSheet(
                sheetState = detailSheetState,
                onDismissRequest = { viewModel.unselectBilling() }
            ) {
                BillingDetailModal(
                    billing = selectedBilling,
                    onPrintBill = {
                        billingPdfView?.let {
                            BillPDFExporter.exportBillToPDF(context, it, selectedBilling.billNumber)
                        } ?: Toast.makeText(context, "Bill view not ready", Toast.LENGTH_SHORT).show()
                    },
                    onAddPayment = { viewModel.showPaymentSheet() },
                    onDeletePayment = { payment -> viewModel.deletePayment(payment) },
                    onDeleteBill = { viewModel.showDeleteDialog() },
                    onClose = { viewModel.unselectBilling() }
                )
            }
        }

        // --- Payment Bottom Sheet ---
        if (billingUiState.isPaymentSheetVisible) {
            ModalBottomSheet(
                sheetState = paymentSheetState,
                onDismissRequest = { viewModel.hidePaymentSheet() }
            ) {
                AddPaymentModal(
                    billing = billingUiState.selectedBilling,
                    onAddPayment = { amount, broker -> viewModel.addPayment(amount, broker) },
                    onCancel = { viewModel.hidePaymentSheet() }
                )
            }
        }

        // --- Delete Confirmation Dialog ---
        if (billingUiState.isDeleteDialogVisible) {
            DeleteConfirmationDialog(
                onConfirm = { viewModel.deleteSelectedBilling() },
                onDismiss = { viewModel.hideDeleteDialog() }
            )
        }
    }
}

@Composable
fun EmptyBillingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ReceiptLong,
            contentDescription = "No bills",
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Bills Found",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )
        Text(
            text = "Bills will appear here once created",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Composable
fun BillingList(
    billings: List<DomainBilling>,
    onBillingClick: (DomainBilling) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(billings) { billing ->
            BillingCard(
                billing = billing,
                onClick = { onBillingClick(billing) }
            )
        }
    }
}


@Composable
fun BillingDetailModal(
    billing: DomainBilling,
    onPrintBill: () -> Unit,
    onAddPayment: () -> Unit,
    onDeletePayment: (DomainBillingPayment) -> Unit,
    onDeleteBill: () -> Unit,
    onClose: () -> Unit
) {
    val totalPaid = billing.payments.sumOf { it.amount }
    val due = (billing.totalPrice - totalPaid).coerceAtLeast(0.0)
    val isFullyPaid = due <= 0.0

    Column(modifier = Modifier.padding(24.dp)) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    billing.customerName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Patient Info
            Text(
                "Tel: ${billing.customerPhoneNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                "Bill #${billing.billNumber}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Summary Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total:", fontWeight = FontWeight.Medium)
                    Text("${billing.totalPrice} FCFA", fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Paid:", fontWeight = FontWeight.Medium)
                    Text("$totalPaid FCFA", color = Color(0xFF4CAF50))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Due:", fontWeight = FontWeight.Medium)
                    Text(
                        "$due FCFA",
                        color = if (due > 0) Color.Red else Color.Gray,
                        fontWeight = if (due > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Items Section
        Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight(0.3f)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(billing.items) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.stockName, style = MaterialTheme.typography.bodyMedium)
                    Text("${item.quantity} x ${item.unitPrice} FCFA", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Payments Section with Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Payments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (!isFullyPaid) {
                FilledTonalButton(
                    onClick = onAddPayment,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Payment", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (billing.payments.isEmpty()) {
            Text(
                "No payments yet",
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxHeight(0.3f)
                    .padding(top = 12.dp)
            ) {
                items(billing.payments) { payment ->
                    PaymentItem(
                        domainBillingPayment = payment,
                        onDelete = { onDeletePayment(payment) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons - Just Smaller
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp) // Reduced spacing
        ) {
            OutlinedButton(
                onClick = onDeleteBill,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(vertical = 4.dp) // Tighter padding
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("Delete", fontSize = 11.sp) // Smaller text
            }

            OutlinedButton(
                onClick = onPrintBill,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("Print", fontSize = 11.sp)
            }

            Button(
                onClick = onClose,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text("Close", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun PaymentItem(
    domainBillingPayment: DomainBillingPayment,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    domainBillingPayment.transactionBroker.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Paid: ${domainBillingPayment.amount} FCFA",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete payment",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddPaymentModal(
    billing: DomainBilling?,
    onAddPayment: (Double, TransactionBroker) -> Unit,
    onCancel: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedBroker by remember { mutableStateOf<TransactionBroker?>(null) }
    var showBrokerDropdown by remember { mutableStateOf(false) }
    val due = remember(billing) {
        billing?.let { it.totalPrice - it.payments.sumOf { payment -> payment.amount } } ?: 0.0
    }

    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            "Add Payment",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Remaining due: $due FCFA",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Amount Input
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount (FCFA)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            ),
            trailingIcon = {
                if (amount.isNotEmpty()) {
                    IconButton(onClick = { amount = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Transaction Broker Dropdown
        Box {
            OutlinedTextField(
                value = selectedBroker?.name ?: "",
                onValueChange = { /* Read-only, handled by dropdown */ },
                label = { Text("Payment Method") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBrokerDropdown = true },
                readOnly = true,
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Select payment method",
                        modifier = Modifier.clickable { showBrokerDropdown = true }
                    )
                }
            )

            DropdownMenu(
                expanded = showBrokerDropdown,
                onDismissRequest = { showBrokerDropdown = false }
            ) {
                TransactionBroker.entries.forEach { broker ->
                    DropdownMenuItem(
                        text = { Text(broker.name) },
                        onClick = {
                            selectedBroker = broker
                            showBrokerDropdown = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    val paymentAmount = amount.toDoubleOrNull() ?: 0.0
                    val broker = selectedBroker
                    if (paymentAmount > 0 && broker != null) {
                        onAddPayment(paymentAmount, broker)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0 && selectedBroker != null
            ) {
                Text("Add Payment")
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Bill") },
        text = { Text("Are you sure you want to delete this bill? This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun safeProgress(current: Double, total: Double): Float {
    if (total <= 0.0) return 0f
    val value = current / total
    return if (value.isFinite()) value.toFloat().coerceIn(0f, 1f) else 0f
}