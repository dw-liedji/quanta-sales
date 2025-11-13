package com.datavite.eat.presentation.shopping

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.datavite.eat.domain.model.DomainStock

// ================================
// SELECTABLE STOCK CARD
// ================================
@Composable
fun SelectableDomainStockCard(
    domainStock: DomainStock,
    selectedStock: SelectedDomainStock?,
    onToggle: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onPriceChange: (Double) -> Unit,
    onLockToggle: () -> Unit
) {
    val isSelected = selectedStock != null
    val quantity = selectedStock?.quantity ?: 1
    val price = selectedStock?.price ?: domainStock.billingPrice
    val isLocked = selectedStock?.isPriceLocked ?: true

    val borderColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outlineVariant

    Card(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product image
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(domainStock.imageUrl)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = "Image de ${domainStock.itemName}",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp),
                contentScale = ContentScale.Crop
            )

            // Product info + actions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = domainStock.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                if (isSelected) {
                    EditablePriceField(
                        price = price.toString(),
                        isLocked = isLocked,
                        onPriceChange = {
                            val value = it.toDoubleOrNull()
                            if (value != null) onPriceChange(value)
                        },
                        onLockToggle = onLockToggle
                    )

                    QuantitySelector(
                        quantity = quantity,
                        onQuantityChange = onQuantityChange
                    )
                } else {
                    Text(
                        text = "${domainStock.billingPrice} FCFA",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ================================
// EDITABLE PRICE FIELD (IMPROVED)
// ================================
@Composable
fun EditablePriceField(
    price: String,
    isLocked: Boolean,
    onPriceChange: (String) -> Unit,
    onLockToggle: () -> Unit
) {
    OutlinedTextField(
        value = price,
        onValueChange = onPriceChange,
        enabled = !isLocked,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        trailingIcon = {
            IconButton(onClick = onLockToggle) {
                Icon(
                    imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = if (isLocked) "Déverrouiller le prix" else "Verrouiller le prix",
                    tint = if (isLocked)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        },
        label = { Text("Prix") },
        modifier = Modifier
            .width(130.dp)
            .heightIn(min = 56.dp)
    )
}

