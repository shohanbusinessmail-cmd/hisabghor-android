package com.shohankhan.hisabghor.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.TransactionEntity
import com.shohankhan.hisabghor.core.designsystem.AmountText
import com.shohankhan.hisabghor.core.designsystem.Danger600
import com.shohankhan.hisabghor.core.designsystem.Emerald600
import com.shohankhan.hisabghor.core.designsystem.bnLabel
import com.shohankhan.hisabghor.core.designsystem.displaySign
import com.shohankhan.hisabghor.core.model.TransactionKind

fun transactionKindIcon(kind: TransactionKind): ImageVector = when (kind) {
    TransactionKind.INCOME, TransactionKind.LEND_RETURN -> Icons.AutoMirrored.Filled.TrendingUp
    TransactionKind.EXPENSE, TransactionKind.EXPENSE_REFUND -> Icons.AutoMirrored.Filled.TrendingDown
    TransactionKind.TRANSFER -> Icons.Filled.SwapHoriz
    TransactionKind.SHOP_PURCHASE_CREDIT, TransactionKind.SHOP_PAYMENT -> Icons.Filled.Storefront
    TransactionKind.LOAN_RECEIVE, TransactionKind.LOAN_REPAYMENT -> Icons.Filled.AccountBalance
    TransactionKind.LEND_OUT -> Icons.Filled.Handshake
    TransactionKind.SAVINGS_DEPOSIT, TransactionKind.SAVINGS_WITHDRAWAL -> Icons.Filled.Savings
    TransactionKind.OPENING_BALANCE -> Icons.Filled.ReceiptLong
    TransactionKind.ADJUSTMENT -> Icons.Filled.Tune
}

/** One transaction in any list. Amount sign/color follows the money direction. */
@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hideAmount: Boolean = false
) {
    val sign = transaction.kind.displaySign()
    val amountColor = when {
        sign > 0 -> Emerald600
        sign < 0 -> Danger600
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                transactionKindIcon(transaction.kind),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                transaction.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${transaction.kind.bnLabel()} • ${Dates.formatShortDate(transaction.occurredAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        AmountText(
            poisha = kotlin.math.abs(transaction.amountPoisha),
            style = MaterialTheme.typography.titleSmall,
            color = amountColor,
            hidden = hideAmount
        )
    }
}
