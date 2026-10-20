package com.shohankhan.hisabghor.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.backup.BackupException
import com.shohankhan.hisabghor.core.backup.BackupPreview
import com.shohankhan.hisabghor.core.backup.BackupRepository
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.designsystem.Danger600
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.KvRow
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.core.security.AppLockManager
import com.shohankhan.hisabghor.core.security.AppSettings
import com.shohankhan.hisabghor.core.security.BiometricAuth
import com.shohankhan.hisabghor.core.security.PinVerifyResult
import com.shohankhan.hisabghor.core.security.SecurityRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class ExportKind { ENCRYPTED_BACKUP, PLAIN_JSON, CSV }

data class PendingExport(
    val kind: ExportKind,
    val bytes: ByteArray,
    val fileName: String
)

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val biometricEnabled: Boolean = false,
    val autoLockMinutes: Int = 5,
    val busy: Boolean = false,
    val message: String? = null,
    val pendingExport: PendingExport? = null,
    val restorePreview: BackupPreview? = null
)

class SettingsViewModel(
    private val security: SecurityRepository,
    private val lockManager: AppLockManager,
    private val backup: BackupRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    private var restoreBytes: ByteArray? = null
    private var restorePassword: CharArray? = null

    val settingsFlow: StateFlow<AppSettings> = security.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val biometricFlow: StateFlow<Boolean> = security.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val autoLockFlow: StateFlow<Int> = security.autoLockMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5)

    fun clearMessage() = _state.update { it.copy(message = null) }
    fun clearPendingExport() = _state.update { it.copy(pendingExport = null) }

    fun setHideAmounts(hide: Boolean) = viewModelScope.launch { security.setHideAmounts(hide) }
    fun setLeadDays(days: Int) = viewModelScope.launch { security.setReminderLeadDays(days) }
    fun setAutoLock(minutes: Int) = viewModelScope.launch { security.setAutoLockMinutes(minutes) }
    fun setBiometric(enabled: Boolean) =
        viewModelScope.launch { security.setBiometricEnabled(enabled) }

    fun lockNow() = lockManager.lockNow()

    fun changePin(oldPin: String, newPin: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            when (val result = security.verifyPin(oldPin)) {
                PinVerifyResult.Success -> {
                    security.setPin(newPin)
                    onResult(null)
                }
                is PinVerifyResult.Wrong -> onResult("বর্তমান পিন ভুল হয়েছে")
                is PinVerifyResult.LockedOut -> onResult("অনেকবার ভুল হয়েছে — কিছুক্ষণ অপেক্ষা করুন")
                PinVerifyResult.NotConfigured -> onResult("পিন সেট করা নেই")
            }
        }
    }

    private fun stamp(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))

    fun createEncryptedBackup(password: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val bytes = backup.createEncryptedBackup(password.toCharArray())
                _state.update {
                    it.copy(
                        busy = false,
                        pendingExport = PendingExport(
                            ExportKind.ENCRYPTED_BACKUP, bytes,
                            "hisabghor_backup_${stamp()}.hgbak"
                        )
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, message = "ব্যাকআপ তৈরি করা যায়নি") }
            }
        }
    }

    fun createJsonExport() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val bytes = backup.createPlainJsonExport()
                _state.update {
                    it.copy(
                        busy = false,
                        pendingExport = PendingExport(
                            ExportKind.PLAIN_JSON, bytes,
                            "hisabghor_export_${stamp()}.json"
                        )
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, message = "এক্সপোর্ট করা যায়নি") }
            }
        }
    }

    fun createCsvExport(allTime: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val (fromMs, toMs) = if (allTime) 0L to Long.MAX_VALUE
                else Dates.monthRange(Dates.nowMs())
                val bytes = backup.createTransactionsCsv(fromMs, toMs)
                _state.update {
                    it.copy(
                        busy = false,
                        pendingExport = PendingExport(
                            ExportKind.CSV, bytes,
                            "hisabghor_transactions_${stamp()}.csv"
                        )
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, message = "এক্সপোর্ট করা যায়নি") }
            }
        }
    }

    fun onExportWritten(kind: ExportKind) {
        viewModelScope.launch {
            if (kind == ExportKind.ENCRYPTED_BACKUP) {
                security.markBackupDone(Dates.nowMs())
            }
            _state.update {
                it.copy(
                    pendingExport = null,
                    message = when (kind) {
                        ExportKind.ENCRYPTED_BACKUP -> "এনক্রিপ্ট করা ব্যাকআপ সংরক্ষণ করা হয়েছে"
                        ExportKind.PLAIN_JSON -> "JSON এক্সপোর্ট সংরক্ষণ করা হয়েছে"
                        ExportKind.CSV -> "CSV ফাইল সংরক্ষণ করা হয়েছে"
                    }
                )
            }
        }
    }

    fun inspectRestore(bytes: ByteArray, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val preview = withContext(Dispatchers.IO) {
                    backup.inspectEncrypted(bytes, password.toCharArray())
                }
                restoreBytes = bytes
                restorePassword = password.toCharArray()
                _state.update { it.copy(busy = false, restorePreview = preview) }
            } catch (e: BackupException) {
                _state.update { it.copy(busy = false, message = e.message ?: "ব্যাকআপ পড়া যায়নি") }
            } catch (e: Exception) {
                _state.update {
                    it.copy(busy = false, message = "ব্যাকআপ পড়া যায়নি — পাসওয়ার্ড ঠিক আছে তো?")
                }
            }
        }
    }

    fun cancelRestore() {
        restoreBytes = null
        restorePassword = null
        _state.update { it.copy(restorePreview = null) }
    }

    fun confirmRestore() {
        val bytes = restoreBytes ?: return
        val password = restorePassword ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, restorePreview = null) }
            try {
                backup.restoreEncrypted(bytes, password)
                cancelRestore()
                _state.update {
                    it.copy(busy = false, message = "ব্যাকআপ সফলভাবে ফেরত আনা হয়েছে")
                }
            } catch (e: BackupException) {
                _state.update { it.copy(busy = false, message = e.message ?: "রিস্টোর করা যায়নি") }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, message = "রিস্টোর করা যায়নি") }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val container = rememberAppContainer()
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = hisabViewModelFactory {
            SettingsViewModel(
                container.securityRepository,
                container.appLockManager,
                container.backupRepository
            )
        }
    )
    val state by viewModel.state.collectAsState()
    val settings by viewModel.settingsFlow.collectAsState()
    val biometricEnabled by viewModel.biometricFlow.collectAsState()
    val autoLockMinutes by viewModel.autoLockFlow.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showChangePin by remember { mutableStateOf(false) }
    var showBackupPassword by remember { mutableStateOf(false) }
    var restoreFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showRestorePassword by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Storage Access Framework: file writers (one per export kind).
    fun writeTo(uri: Uri?, export: PendingExport?) {
        if (uri == null || export == null) {
            viewModel.clearPendingExport()
            return
        }
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(export.bytes) }
                }.isSuccess
            }
            if (ok) viewModel.onExportWritten(export.kind) else viewModel.clearPendingExport()
        }
    }

    var pendingRef by remember { mutableStateOf<PendingExport?>(null) }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> writeTo(uri, pendingRef) }
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> writeTo(uri, pendingRef) }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> writeTo(uri, pendingRef) }

    LaunchedEffect(state.pendingExport) {
        val export = state.pendingExport ?: return@LaunchedEffect
        pendingRef = export
        when (export.kind) {
            ExportKind.ENCRYPTED_BACKUP -> backupLauncher.launch(export.fileName)
            ExportKind.PLAIN_JSON -> jsonLauncher.launch(export.fileName)
            ExportKind.CSV -> csvLauncher.launch(export.fileName)
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }.getOrNull()
                }
                if (bytes != null) {
                    restoreFileBytes = bytes
                    showRestorePassword = true
                } else {
                    snackbarHost.showSnackbar("ফাইল পড়া যায়নি")
                }
            }
        }
    }

    Scaffold(
        topBar = { HisabTopBar(title = "সেটিংস", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.busy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.padding(end = 10.dp))
                    Text("কাজ চলছে…")
                }
            }

            SettingsCard(title = "নিরাপত্তা") {
                OutlinedButton(
                    onClick = { showChangePin = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("পিন বদলান") }
                Spacer(Modifier.height(8.dp))
                val canBiometric = remember { BiometricAuth.canAuthenticate(context) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("ফিঙ্গারপ্রিন্ট দিয়ে খুলুন")
                        Text(
                            if (canBiometric) "পিনের পাশাপাশি ফিঙ্গারপ্রিন্ট/মুখ দিয়েও লক খোলা যাবে"
                            else "এই ফোনে বায়োমেট্রিক সেট করা নেই",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = biometricEnabled,
                        enabled = canBiometric,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val activity = context as? FragmentActivity
                                if (activity != null) {
                                    BiometricAuth.showPrompt(
                                        activity = activity,
                                        onSuccess = { viewModel.setBiometric(true) },
                                        onError = { }
                                    )
                                }
                            } else {
                                viewModel.setBiometric(false)
                            }
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("কতক্ষণ পর নিজে থেকে লক হবে", style = MaterialTheme.typography.titleSmall)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0 to "সাথে সাথে", 1 to "১ মিনিট", 5 to "৫ মিনিট", 15 to "১৫ মিনিট", 30 to "৩০ মিনিট")
                        .forEach { (minutes, label) ->
                            FilterChip(
                                selected = autoLockMinutes == minutes,
                                onClick = { viewModel.setAutoLock(minutes) },
                                label = { Text(label) }
                            )
                        }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = viewModel::lockNow,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("এখনই লক করুন") }
                Text(
                    "পিন ভুলে গেলে অ্যাপের ডেটা ফেরত পাওয়ার কোনো উপায় নেই — কোনো মাস্টার পিন নেই। তাই পিন মনে রাখুন এবং এনক্রিপ্ট করা ব্যাকআপের পাসওয়ার্ড আলাদাভাবে সাবধানে রাখুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsCard(title = "পছন্দ") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("ড্যাশবোর্ডে টাকার অঙ্ক আড়াল করুন")
                        Text(
                            "পাশে কেউ থাকলে গোপনীয়তার জন্য",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.hideAmountsOnDashboard,
                        onCheckedChange = viewModel::setHideAmounts
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("রিমাইন্ডার কত দিন আগে জানাবে", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 1, 2, 3, 7).forEach { days ->
                        FilterChip(
                            selected = settings.reminderLeadDays == days,
                            onClick = { viewModel.setLeadDays(days) },
                            label = {
                                Text(
                                    if (days == 0) "ঐদিনই"
                                    else "${BengaliNumber.formatCount(days.toLong())} দিন আগে"
                                )
                            }
                        )
                    }
                }
            }

            SettingsCard(title = "ব্যাকআপ ও এক্সপোর্ট") {
                KvRow(
                    "সর্বশেষ ব্যাকআপ",
                    settings.lastBackupAtMs?.let { Dates.formatShortDate(it) } ?: "এখনো নেওয়া হয়নি"
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showBackupPassword = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("এনক্রিপ্ট করা ব্যাকআপ তৈরি করুন") }
                OutlinedButton(
                    onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("ব্যাকআপ থেকে ফেরত আনুন") }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                OutlinedButton(
                    onClick = viewModel::createJsonExport,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("সব ডেটা JSON হিসাবে এক্সপোর্ট") }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.createCsvExport(allTime = false) },
                        modifier = Modifier.weight(1f)
                    ) { Text("CSV (এই মাস)") }
                    OutlinedButton(
                        onClick = { viewModel.createCsvExport(allTime = true) },
                        modifier = Modifier.weight(1f)
                    ) { Text("CSV (সব সময়)") }
                }
                Text(
                    "সতর্কতা: JSON ও CSV ফাইলে কোনো এনক্রিপশন থাকে না — যার হাতে পড়বে সে পড়তে পারবে। নিয়মিত সুরক্ষার জন্য এনক্রিপ্ট করা ব্যাকআপ ব্যবহার করুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = Danger600
                )
                Text(
                    "সব তথ্য আপনার ফোনেই থাকে — ইন্টারনেট ছাড়াই ব্যাকআপ তৈরি ও রিস্টোর করা যায়।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsCard(title = "অ্যাপ সম্পর্কে") {
                OutlinedButton(
                    onClick = onOpenAbout,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("হিসাবঘর সম্পর্কে জানুন") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showChangePin) {
        ChangePinDialog(
            onDismiss = { showChangePin = false },
            onSubmit = { oldPin, newPin, onError ->
                viewModel.changePin(oldPin, newPin) { error ->
                    if (error == null) {
                        showChangePin = false
                        scope.launch { snackbarHost.showSnackbar("পিন বদলানো হয়েছে") }
                    } else {
                        onError(error)
                    }
                }
            }
        )
    }

    if (showBackupPassword) {
        PasswordDialog(
            title = "ব্যাকআপের পাসওয়ার্ড দিন",
            message = "এই পাসওয়ার্ড ছাড়া ব্যাকআপ খোলা যাবে না। কমপক্ষে ৬ অক্ষরের শক্ত পাসওয়ার্ড দিন এবং সেটি ভুলবেন না।",
            confirmRequired = true,
            onDismiss = { showBackupPassword = false },
            onConfirm = { password ->
                showBackupPassword = false
                viewModel.createEncryptedBackup(password)
            }
        )
    }

    if (showRestorePassword) {
        PasswordDialog(
            title = "ব্যাকআপের পাসওয়ার্ড দিন",
            message = "যে পাসওয়ার্ড দিয়ে এই ব্যাকআপ তৈরি করেছিলেন সেটি দিন।",
            confirmRequired = false,
            onDismiss = { showRestorePassword = false },
            onConfirm = { password ->
                showRestorePassword = false
                restoreFileBytes?.let { viewModel.inspectRestore(it, password) }
            }
        )
    }

    state.restorePreview?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text("ব্যাকআপ ফেরত আনবেন?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    KvRow("ব্যাকআপের তারিখ", Dates.formatShortDate(preview.createdAtEpochMs))
                    KvRow("অ্যাপ সংস্করণ", BengaliNumber.toBengaliDigits(preview.appVersion))
                    KvRow(
                        "অ্যাকাউন্ট",
                        BengaliNumber.formatCount(preview.accountCount.toLong()) + "টি"
                    )
                    KvRow(
                        "লেনদেন",
                        BengaliNumber.formatCount(preview.transactionCount.toLong()) + "টি"
                    )
                    KvRow("দোকান", BengaliNumber.formatCount(preview.shopCount.toLong()) + "টি")
                    KvRow("ঋণ/ধার", BengaliNumber.formatCount(preview.loanCount.toLong()) + "টি")
                    KvRow(
                        "রসিদের ছবি",
                        BengaliNumber.formatCount(preview.attachmentFilesIncluded.toLong()) + "টি আছে" +
                            if (preview.attachmentFilesMissing > 0)
                                ", ${BengaliNumber.formatCount(preview.attachmentFilesMissing.toLong())}টি পাওয়া যায়নি"
                            else ""
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "সতর্কতা: এখনকার সব তথ্য মুছে ব্যাকআপের তথ্য বসানো হবে। দরকার হলে আগে বর্তমান ডেটার আলাদা ব্যাকআপ নিয়ে রাখুন।",
                        color = Danger600,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRestore) { Text("হ্যাঁ, ফেরত আনুন") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRestore) { Text("বাতিল") }
            }
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    onSubmit: (oldPin: String, newPin: String, onError: (String) -> Unit) -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("পিন বদলান") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PinField(oldPin, { oldPin = it }, "বর্তমান পিন")
                PinField(newPin, { newPin = it }, "নতুন পিন (কমপক্ষে ৪ সংখ্যা)")
                PinField(confirmPin, { confirmPin = it }, "নতুন পিন আবার দিন")
                error?.let { Text(it, color = Danger600) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    oldPin.length < 4 -> error = "বর্তমান পিন দিন"
                    newPin.length < 4 || !newPin.all { it.isDigit() } ->
                        error = "নতুন পিন কমপক্ষে ৪টি সংখ্যা হতে হবে"
                    newPin != confirmPin -> error = "নতুন পিন দুইবার একরকম হয়নি"
                    else -> onSubmit(oldPin, newPin) { error = it }
                }
            }) { Text("বদলান") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onValueChange(v.filter { it.isDigit() }.take(12)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PasswordDialog(
    title: String,
    message: String,
    confirmRequired: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("পাসওয়ার্ড") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (confirmRequired) {
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("পাসওয়ার্ড আবার দিন") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                error?.let { Text(it, color = Danger600) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    password.isBlank() -> error = "পাসওয়ার্ড দিন"
                    confirmRequired && password.length < 6 ->
                        error = "কমপক্ষে ৬ অক্ষরের পাসওয়ার্ড দিন"
                    confirmRequired && password != confirm ->
                        error = "পাসওয়ার্ড দুইবার একরকম হয়নি"
                    else -> onConfirm(password)
                }
            }) { Text("ঠিক আছে") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}
