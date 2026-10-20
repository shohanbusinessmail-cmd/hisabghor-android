package com.shohankhan.hisabghor.feature.transactions

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.AttachmentEntity
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.database.TransactionEntity
import com.shohankhan.hisabghor.core.database.TransactionItemEntity
import com.shohankhan.hisabghor.core.designsystem.ConfirmDialog
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.KvRow
import com.shohankhan.hisabghor.core.designsystem.LoadingState
import com.shohankhan.hisabghor.core.designsystem.bnLabel
import com.shohankhan.hisabghor.core.designsystem.formatAmount
import com.shohankhan.hisabghor.core.model.LedgerType
import com.shohankhan.hisabghor.core.model.TransactionKind
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.data.TransactionRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.shohankhan.hisabghor.core.designsystem.AmountText
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgAmountType
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgButtonStyle
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgDivider
import com.shohankhan.hisabghor.core.designsystem.HgListRow
import com.shohankhan.hisabghor.core.designsystem.HgNotice
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.HgTone
import com.shohankhan.hisabghor.core.designsystem.IconChip
import com.shohankhan.hisabghor.core.designsystem.Radius
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.Space
import com.shohankhan.hisabghor.core.designsystem.SummaryRow

data class PostingLine(val ledgerLabel: String, val signedAmountPoisha: Long)

data class TransactionDetailUiState(
    val loading: Boolean = true,
    val transaction: TransactionEntity? = null,
    val items: List<TransactionItemEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    val postingLines: List<PostingLine> = emptyList(),
    val accountName: String? = null,
    val counterAccountName: String? = null,
    val categoryName: String? = null,
    val shopName: String? = null,
    val partyName: String? = null,
    val loanName: String? = null
)

/** Where the edit button should go for each kind (null = not directly editable). */
fun TransactionEntity.editRoute(): String? = when (kind) {
    TransactionKind.INCOME -> "transaction_editor/INCOME?editId=$id"
    TransactionKind.EXPENSE -> "transaction_editor/EXPENSE?editId=$id"
    TransactionKind.EXPENSE_REFUND -> "transaction_editor/REFUND?editId=$id"
    TransactionKind.TRANSFER -> "transfer?editId=$id"
    TransactionKind.SHOP_PURCHASE_CREDIT -> shopId?.let { "shop_purchase/$it?editId=$id" }
    TransactionKind.SHOP_PAYMENT -> shopId?.let { "shop_payment/$it?editId=$id" }
    TransactionKind.LOAN_REPAYMENT, TransactionKind.LEND_RETURN ->
        loanId?.let { "loan_payment/$it?editId=$id" }
    TransactionKind.SAVINGS_DEPOSIT ->
        "savings_move?goalId=${savingsGoalId ?: -1}&accountId=${counterAccountId ?: -1}&withdraw=false&editId=$id"
    TransactionKind.SAVINGS_WITHDRAWAL ->
        "savings_move?goalId=${savingsGoalId ?: -1}&accountId=${accountId ?: -1}&withdraw=true&editId=$id"
    else -> null
}

class TransactionDetailViewModel(
    private val transactionId: Long,
    private val db: HisabGhorDatabase,
    private val transactions: TransactionRepository,
    private val receiptsDir: File
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionDetailUiState())
    val state: StateFlow<TransactionDetailUiState> = _state

    init {
        viewModelScope.launch {
            combine(
                transactions.observeById(transactionId),
                transactions.observeItems(transactionId),
                transactions.observeAttachments(transactionId)
            ) { txn, items, attachments -> Triple(txn, items, attachments) }
                .collect { (txn, items, attachments) ->
                    if (txn == null) {
                        _state.update { it.copy(loading = false) }
                        return@collect
                    }
                    _state.update {
                        it.copy(
                            loading = false,
                            transaction = txn,
                            items = items,
                            attachments = attachments,
                            accountName = txn.accountId?.let { id -> db.accountDao().getById(id)?.name },
                            counterAccountName = txn.counterAccountId?.let { id -> db.accountDao().getById(id)?.name },
                            categoryName = txn.categoryId?.let { id -> db.categoryDao().getById(id)?.name },
                            shopName = txn.shopId?.let { id -> db.shopDao().getById(id)?.name },
                            partyName = txn.partyId?.let { id -> db.partyDao().getById(id)?.name },
                            loanName = txn.loanId?.let { id -> db.loanDao().getById(id)?.organizationName },
                            postingLines = loadPostingLines(txn.id)
                        )
                    }
                }
        }
    }

    private suspend fun loadPostingLines(txnId: Long): List<PostingLine> =
        db.postingDao().getPostingsForTransaction(txnId).map { posting ->
            val label = when (posting.ledgerType) {
                LedgerType.ACCOUNT -> db.accountDao().getById(posting.ledgerId)?.name ?: "অ্যাকাউন্ট"
                LedgerType.INCOME_CATEGORY ->
                    "আয়: " + (db.categoryDao().getById(posting.ledgerId)?.name ?: "")
                LedgerType.EXPENSE_CATEGORY ->
                    "খরচ: " + (db.categoryDao().getById(posting.ledgerId)?.name ?: "")
                LedgerType.SHOP_LIABILITY ->
                    "দোকানের বকেয়া: " + (db.shopDao().getById(posting.ledgerId)?.name ?: "")
                LedgerType.LOAN_LIABILITY ->
                    "ঋণের বাকি: " + (db.loanDao().getById(posting.ledgerId)?.organizationName ?: "")
                LedgerType.RECEIVABLE ->
                    "পাওনা: " + (db.loanDao().getById(posting.ledgerId)?.organizationName ?: "")
                LedgerType.EQUITY -> "শুরুর মূলধন"
            }
            PostingLine(label, posting.signedAmountPoisha)
        }

    fun voidTransaction() {
        val txn = _state.value.transaction ?: return
        viewModelScope.launch {
            transactions.voidTransaction(txn.id, txn.title)
        }
    }

    suspend fun saveAttachment(fileName: String, mimeType: String?, sizeBytes: Long) {
        transactions.addAttachment(transactionId, fileName, mimeType, sizeBytes)
    }

    fun deleteAttachment(attachment: AttachmentEntity) {
        viewModelScope.launch {
            transactions.deleteAttachment(attachment.id)
            runCatching { File(receiptsDir, attachment.fileName).delete() }
        }
    }

    fun receiptFile(attachment: AttachmentEntity): File = File(receiptsDir, attachment.fileName)
}

@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val container = rememberAppContainer()
    val context = LocalContext.current
    val viewModel: TransactionDetailViewModel = viewModel(
        key = "txn_detail_$transactionId",
        factory = hisabViewModelFactory {
            TransactionDetailViewModel(
                transactionId, container.database,
                container.transactionRepository, container.receiptsDir
            )
        }
    )
    val state by viewModel.state.collectAsState()
    val txn = state.transaction
    var showVoidConfirm by remember { mutableStateOf(false) }
    var attachmentToDelete by remember { mutableStateOf<AttachmentEntity?>(null) }

    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            container.appScope.launch {
                try {
                    val resolver = context.contentResolver
                    val mime = resolver.getType(uri)
                    val safeName = "receipt_${System.currentTimeMillis()}"
                    val extension = when (mime) {
                        "image/png" -> ".png"
                        "image/jpeg" -> ".jpg"
                        "application/pdf" -> ".pdf"
                        else -> ""
                    }
                    val fileName = safeName + extension
                    val target = File(container.receiptsDir, fileName)
                    resolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (target.exists()) {
                        viewModel.saveAttachment(fileName, mime, target.length())
                    }
                } catch (e: Exception) {
                    // Picker copy failed; user can retry. Nothing was recorded.
                }
            }
        }
    }

    HgScreen(
        title = "লেনদেনের বিবরণ",
        onBack = onBack,
        actions = {
            val route = txn?.takeIf { it.voidedAt == null }?.editRoute()
            if (route != null) {
                IconButton(onClick = { onNavigate(route) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "সম্পাদনা করুন", tint = Hg.colors.ink)
                }
            }
        }
    ) { padding ->
        when {
            state.loading -> LoadingState(Modifier.padding(padding))
            txn == null -> EmptyDetail(Modifier.padding(padding))
            else -> androidx.compose.foundation.layout.Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenPadding)
            ) {
                Spacer(Modifier.height(Space.md))
                if (txn.voidedAt != null) {
                    HgNotice(
                        title = "এই লেনদেনটি বাতিল করা হয়েছে",
                        message = "বাতিল লেনদেন আর ব্যালান্স, বাজেট বা রিপোর্টে ধরা হয় না।",
                        tone = HgTone.Expense,
                        icon = Icons.Filled.Warning
                    )
                    Spacer(Modifier.height(Space.md))
                }

                // Hero: amount + identity
                Column(Modifier.fillMaxWidth().padding(vertical = Space.sm)) {
                    Eyebrow(txn.kind.bnLabel())
                    Spacer(Modifier.height(Space.xs))
                    AmountText(
                        kotlin.math.abs(txn.amountPoisha),
                        style = HgAmountType.hero,
                        color = if (txn.voidedAt != null) Hg.colors.inkTertiary else Hg.colors.ink
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        txn.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Hg.colors.ink
                    )
                    Text(
                        Dates.formatDateTime(txn.occurredAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkSecondary
                    )
                }

                HgCard(padding = PaddingValues(vertical = Space.xs)) {
                    SummaryLine("ধরন", txn.kind.bnLabel())
                    state.accountName?.let { SummaryLine("অ্যাকাউন্ট", it) }
                    state.counterAccountName?.let { SummaryLine("অপর অ্যাকাউন্ট", it) }
                    state.categoryName?.let { SummaryLine("ক্যাটাগরি", it) }
                    state.shopName?.let { SummaryLine("দোকান", it) }
                    state.partyName?.let { SummaryLine("ব্যক্তি", it) }
                    state.loanName?.let { SummaryLine("ঋণ/চুক্তি", it) }
                    if (txn.feePoisha > 0) SummaryLine("ফি/চার্জ", formatAmount(txn.feePoisha))
                    if (txn.discountPoisha > 0) SummaryLine("ছাড়", formatAmount(txn.discountPoisha))
                    if (txn.extraChargePoisha > 0) SummaryLine("অতিরিক্ত চার্জ", formatAmount(txn.extraChargePoisha))
                    txn.receiptNo?.let { SummaryLine("রসিদ নম্বর", BengaliNumber.toBengaliDigits(it)) }
                    txn.note?.takeIf { it.isNotBlank() }?.let { SummaryLine("মন্তব্য", it) }
                }

                if (state.items.isNotEmpty()) {
                    Spacer(Modifier.height(Space.md))
                    HgCard(padding = PaddingValues(vertical = Space.xs)) {
                        SectionLabel("পণ্যের তালিকা")
                        state.items.forEach { item ->
                            Column(Modifier.padding(horizontal = Space.lg, vertical = Space.sm)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        item.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Hg.colors.ink,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        formatAmount(item.lineTotalPoisha),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Hg.colors.ink
                                    )
                                }
                                Text(
                                    "${BengaliNumber.formatQuantityMilli(item.quantityMilli)} ${item.unit ?: ""} × " +
                                        formatAmount(item.unitPricePoisha) +
                                        if (item.discountPoisha > 0) " − ছাড় ${formatAmount(item.discountPoisha)}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Hg.colors.inkSecondary
                                )
                            }
                            HgDivider(startInset = 20)
                        }
                    }
                }

                Spacer(Modifier.height(Space.md))
                HgCard(padding = PaddingValues(vertical = Space.xs)) {
                    SectionLabel("হিসাবের খাতায় প্রভাব")
                    state.postingLines.forEach { line ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Space.lg, vertical = Space.xs),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                line.ledgerLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Hg.colors.inkSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                (if (line.signedAmountPoisha >= 0) "+" else "−") +
                                    formatAmount(kotlin.math.abs(line.signedAmountPoisha)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (line.signedAmountPoisha >= 0) Hg.colors.ink else Hg.colors.inkSecondary
                            )
                        }
                    }
                    Text(
                        "সব খাতার যোগফল সবসময় শূন্য — এটাই দ্বৈত হিসাব (double-entry) পদ্ধতির নিয়ম।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkTertiary,
                        modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.sm)
                    )
                }

                Spacer(Modifier.height(Space.md))
                HgCard(padding = PaddingValues(vertical = Space.xs)) {
                    SectionLabel("রসিদ ও সংযুক্তি")
                    state.attachments.forEach { attachment ->
                        HgListRow(
                            title = attachment.fileName,
                            subtitle = attachment.mimeType ?: "ফাইল",
                            leading = {
                                IconChip(
                                    Icons.Filled.AttachFile,
                                    tint = Hg.colors.brand,
                                    background = Hg.colors.brandTint
                                )
                            },
                            trailing = {
                                IconButton(onClick = { attachmentToDelete = attachment }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "সংযুক্তি মুছুন",
                                        tint = Hg.colors.inkSecondary
                                    )
                                }
                            },
                            onClick = {
                                val file = viewModel.receiptFile(attachment)
                                if (file.exists()) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, attachment.mimeType ?: "*/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    runCatching { context.startActivity(intent) }
                                }
                            }
                        )
                    }
                    if (state.attachments.isEmpty()) {
                        Text(
                            "এখনো কোনো রসিদ সংযুক্ত করা হয়নি।",
                            style = MaterialTheme.typography.bodySmall,
                            color = Hg.colors.inkSecondary,
                            modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.sm)
                        )
                    }
                    TextButton(
                        onClick = { documentPicker.launch(arrayOf("image/*", "application/pdf")) },
                        modifier = Modifier.padding(horizontal = Space.sm)
                    ) {
                        Icon(Icons.Filled.AttachFile, contentDescription = null, tint = Hg.colors.brand)
                        Spacer(Modifier.size(Space.xs))
                        Text("রসিদের ছবি বা PDF যোগ করুন", color = Hg.colors.brand)
                    }
                }

                if (txn.voidedAt == null) {
                    Spacer(Modifier.height(Space.lg))
                    val editRoute = txn.editRoute()
                    if (editRoute != null) {
                        HgButton(
                            text = "সম্পাদনা করুন",
                            onClick = { onNavigate(editRoute) },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Filled.Edit
                        )
                    } else {
                        HgNotice(
                            title = "সরাসরি সম্পাদনা করা যায় না",
                            message = "এই ধরনের লেনদেন সংশ্লিষ্ট স্ক্রিন থেকে বদলাতে হবে। ভুল থাকলে বাতিল করে নতুন করে লিখুন।",
                            tone = HgTone.Neutral
                        )
                    }
                    Spacer(Modifier.height(Space.sm))
                    HgButton(
                        text = "লেনদেন বাতিল করুন",
                        onClick = { showVoidConfirm = true },
                        style = HgButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(Space.xxl))
            }
        }
    }

    if (showVoidConfirm) {
        ConfirmDialog(
            title = "লেনদেন বাতিল করবেন?",
            message = "বাতিল করলে এই লেনদেন আর ব্যালান্স, বাজেট বা রিপোর্টে ধরা হবে না। " +
                "ইতিহাস হিসেবে এটি অ্যাপে থেকে যাবে (বাতিল হিসেবে চিহ্নিত)।",
            confirmText = "হ্যাঁ, বাতিল করুন",
            destructive = true,
            onConfirm = {
                viewModel.voidTransaction()
                showVoidConfirm = false
                onBack()
            },
            onDismiss = { showVoidConfirm = false }
        )
    }

    attachmentToDelete?.let { attachment ->
        ConfirmDialog(
            title = "সংযুক্তি মুছে ফেলবেন?",
            message = "রসিদের ফাইলটি স্থায়ীভাবে মুছে যাবে।",
            confirmText = "মুছে ফেলুন",
            destructive = true,
            onConfirm = {
                viewModel.deleteAttachment(attachment)
                attachmentToDelete = null
            },
            onDismiss = { attachmentToDelete = null }
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg, vertical = Space.sm)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = Hg.colors.inkSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = Hg.colors.ink,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = Hg.colors.inkSecondary,
        modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.sm)
    )
}

@Composable
private fun EmptyDetail(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.ReceiptLong,
                contentDescription = null,
                tint = Hg.colors.inkTertiary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(Space.sm))
            Text(
                "লেনদেনটি পাওয়া যায়নি।",
                style = MaterialTheme.typography.bodyMedium,
                color = Hg.colors.inkSecondary
            )
        }
    }
}
