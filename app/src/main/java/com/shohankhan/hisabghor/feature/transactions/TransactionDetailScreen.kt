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

    Scaffold(
        topBar = {
            HisabTopBar(
                title = "লেনদেনের বিবরণ",
                onBack = onBack,
                actions = {
                    val route = txn?.takeIf { it.voidedAt == null }?.editRoute()
                    if (route != null) {
                        IconButton(onClick = { onNavigate(route) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "সম্পাদনা করুন")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.loading -> LoadingState(Modifier.padding(padding))
            txn == null -> Column(Modifier.padding(padding).padding(16.dp)) {
                Text("লেনদেনটি পাওয়া যায়নি।")
            }
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (txn.voidedAt != null) {
                    Card(Modifier.fillMaxWidth()) {
                        Text(
                            "এই লেনদেনটি বাতিল করা হয়েছে — এটি আর কোনো হিসাবে ধরা হয় না।",
                            Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(txn.title, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatAmount(kotlin.math.abs(txn.amountPoisha)),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        KvRow("ধরন", txn.kind.bnLabel())
                        KvRow("তারিখ", Dates.formatDateTime(txn.occurredAt))
                        state.accountName?.let { KvRow("অ্যাকাউন্ট", it) }
                        state.counterAccountName?.let { KvRow("অপর অ্যাকাউন্ট", it) }
                        state.categoryName?.let { KvRow("ক্যাটাগরি", it) }
                        state.shopName?.let { KvRow("দোকান", it) }
                        state.partyName?.let { KvRow("ব্যক্তি", it) }
                        state.loanName?.let { KvRow("ঋণ/চুক্তি", it) }
                        if (txn.feePoisha > 0) KvRow("ফি/চার্জ", formatAmount(txn.feePoisha))
                        if (txn.discountPoisha > 0) KvRow("ছাড়", formatAmount(txn.discountPoisha))
                        if (txn.extraChargePoisha > 0) KvRow("অতিরিক্ত চার্জ", formatAmount(txn.extraChargePoisha))
                        txn.receiptNo?.let { KvRow("রসিদ নম্বর", BengaliNumber.toBengaliDigits(it)) }
                        txn.note?.let { KvRow("মন্তব্য", it) }
                    }
                }

                if (state.items.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("পণ্যের তালিকা", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            state.items.forEach { item ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name)
                                        Text(
                                            "${BengaliNumber.formatQuantityMilli(item.quantityMilli)} ${item.unit ?: ""} × ${formatAmount(item.unitPricePoisha)}" +
                                                if (item.discountPoisha > 0) " − ছাড় ${formatAmount(item.discountPoisha)}" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(formatAmount(item.lineTotalPoisha))
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("হিসাবের খাতায় প্রভাব", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        state.postingLines.forEach { line ->
                            KvRow(
                                line.ledgerLabel,
                                (if (line.signedAmountPoisha >= 0) "+" else "−") +
                                    formatAmount(kotlin.math.abs(line.signedAmountPoisha))
                            )
                        }
                        Text(
                            "সব খাতার যোগফল সবসময় শূন্য — এটাই দ্বৈত হিসাব (double-entry) পদ্ধতির নিয়ম।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("রসিদ/সংযুক্তি", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        state.attachments.forEach { attachment ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
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
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row {
                                    Icon(Icons.Filled.AttachFile, contentDescription = null)
                                    Spacer(Modifier.height(0.dp))
                                    Text(
                                        "  ${attachment.fileName}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                IconButton(onClick = { attachmentToDelete = attachment }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "সংযুক্তি মুছুন")
                                }
                            }
                        }
                        OutlinedButton(onClick = {
                            documentPicker.launch(arrayOf("image/*", "application/pdf"))
                        }) {
                            Text("রসিদের ছবি/PDF যোগ করুন")
                        }
                    }
                }

                if (txn.voidedAt == null) {
                    val editRoute = txn.editRoute()
                    if (editRoute != null) {
                        Button(
                            onClick = { onNavigate(editRoute) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("সম্পাদনা করুন") }
                    } else {
                        Text(
                            "এই ধরনের লেনদেন সরাসরি সম্পাদনা করা যায় না। ভুল থাকলে বাতিল করে নতুন করে লিখুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { showVoidConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("লেনদেন বাতিল করুন") }
                }
                Spacer(Modifier.height(16.dp))
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
