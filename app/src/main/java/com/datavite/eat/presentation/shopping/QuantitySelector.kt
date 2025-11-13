package com.datavite.eat.presentation.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun QuantitySelector(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    range: IntRange = 1..50,
    modifier: Modifier = Modifier,
    wheelHeight: Dp = 50.dp,
    wheelWidth: Dp = 50.dp,
    enabled: Boolean = true
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = quantity - range.first)

    // Find the centered item instead of just the first visible item
    val centeredItemIndex = remember { derivedStateOf {
        val layoutInfo = listState.layoutInfo
        val viewportCenter = layoutInfo.viewportSize.height / 2

        layoutInfo.visibleItemsInfo
            .minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                abs(itemCenter - viewportCenter)
            }
            ?.index ?: (quantity - range.first)
    } }

    // Sync scroll position with external quantity changes
    LaunchedEffect(quantity) {
        val targetIndex = quantity - range.first
        listState.animateScrollToItem(targetIndex)
    }

    // Sync quantity with centered item changes
    LaunchedEffect(centeredItemIndex.value) {
        val newQuantity = range.first + centeredItemIndex.value
        if (newQuantity in range && newQuantity != quantity) {
            onQuantityChange(newQuantity)
        }
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Decrease button
            IconButton(
                onClick = {
                    if (quantity > range.first) onQuantityChange(quantity - 1)
                },
                enabled = enabled && quantity > range.first,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (enabled && quantity > range.first) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease quantity",
                    tint = if (enabled && quantity > range.first) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Quantity Wheel
            Box(
                modifier = Modifier
                    .height(wheelHeight)
                    .width(wheelWidth),
                contentAlignment = Alignment.Center
            ) {
                // Selection highlight
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .height(36.dp)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        )
                )

                LazyColumn(
                    state = listState,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(vertical = (wheelHeight - 36.dp) / 2), // Center items properly
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
                ) {
                    items(range.count()) { index ->
                        val value = range.first + index
                        val isSelected = value == quantity

                        Text(
                            text = value.toString(),
                            style = if (isSelected) {
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // Increase button
            IconButton(
                onClick = {
                    if (quantity < range.last) onQuantityChange(quantity + 1)
                },
                enabled = enabled && quantity < range.last,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (enabled && quantity < range.last) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase quantity",
                    tint = if (enabled && quantity < range.last) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
