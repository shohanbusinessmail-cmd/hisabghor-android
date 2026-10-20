package com.shohankhan.hisabghor.feature.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.TransactionEntity
import com.shohankhan.hisabghor.core.designsystem.Dimens
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgAmountType
import com.shohankhan.hisabghor.core.designsystem.IconChip
import com.shohankhan.hisabghor.core.designsystem.SignedAmountText
import com.shohankhan.hisabghor.core.designsystem.Space
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

private fun toneFor(kind: TransactionKind): Triple<Color, Color, Color> {
    // iconTint, iconBackground, amountColor
    return when (kind.displaySign()) {
        1 -> Triple(Hg.colors.income, Hg.colors.incomeTint, Hg.colors.income)
        -1 -> Triple(Hg.colors.expense, Hg.colors.expenseTint, Hg.colors.expense)
        else -> Triple(Hg.colors.inkSecondary, Hg.colors.surfaceMuted, Hg.colors.ink)
    }
}

/**
 * One transaction row, used on the dashboard and in every transaction list.
 * Amounts always carry a direction sign so meaning never depends on colour.
 */
@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hideAmount: Boolean = false,
    accountName: String? = null
) {
    val (iconTint, iconBackground, _) = toneFor(transaction.kind)
    val meta = buildString {
        append(transaction.kind.bnLabel())
        append(" • ")
        append(Dates.formatShortDate(transaction.occurredAt))
        if (!accountName.isNullOrBlank()) {
            append(" • ")
            append(accountName)
        }
    }
    val direction = when (transaction.kind.displaySign()) {
        1 -> "আয়"
        -1 -> "খরচ"
        else -> "স্থানান্তর"
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.listRowMinHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(
            icon = transactionKindIcon(transaction.kind),
            tint = iconTint,
            background = iconBackground
        )
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(
                transaction.title,
                style = MaterialTheme.typography.bodyLarge,
                color = Hg.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                meta,
                style = MaterialTheme.typography.bodySmall,
                color = Hg.colors.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(Space.sm))
        Column(horizontalAlignment = Alignment.End) {
            SignedAmountText(
                poisha = transaction.amountPoisha * transaction.kind.displaySign(),
                style = HgAmountType.medium,
                hidden = hideAmount
            )
            Spacer(Modifier.height(2.dp))
            Text(
                direction,
                style = MaterialTheme.typography.labelSmall,
                color = Hg.colors.inkTertiary
            )
        }
    }
}
