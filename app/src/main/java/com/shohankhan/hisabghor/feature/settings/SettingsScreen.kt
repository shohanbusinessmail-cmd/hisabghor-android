package com.shohankhan.hisabghor.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.layout.PaddingValues
import com.shohankhan.hisabghor.core.designsystem.AmountText
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgButtonStyle
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgChip
import com.shohankhan.hisabghor.core.designsystem.HgNotice
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.HgTextField
import com.shohankhan.hisabghor.core.designsystem.HgTone
import com.shohankhan.hisabghor.core.designsystem.Radius
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.Space

enum class ExportKind { ENCRYPTED_BACKUP, PLAIN_JSON, CSV, PDF }

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

    fun createPdfExport(allTime: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val nowMs = Dates.nowMs()
                val (fromMs, toMs) = if (allTime) 0L to Long.MAX_VALUE
                else Dates.monthRange(nowMs)
                val periodLabel = if (allTime) "সব সময়"
                else "এই মাস (${Dates.formatShortDate(fromMs)} — ${Dates.formatShortDate(nowMs)})"
                val bytes = backup.createPeriodPdfReport(fromMs, toMs, periodLabel)
                _state.update {
                    it.copy(
                        busy = false,
                        pendingExport = PendingExport(
                            ExportKind.PDF, bytes,
                            "hisabghor_report_${stamp()}.pdf"
                        )
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, message = "PDF তৈরি করা যায়নি") }
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
                        ExportKind.PDF -> "PDF বিবরণী সংরক্ষণ করা হয়েছে"
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
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> writeTo(uri, pendingRef) }

    LaunchedEffect(state.pendingExport) {
        val export = state.pendingExport ?: return@LaunchedEffect
        pendingRef = export
        when (export.kind) {
            ExportKind.ENCRYPTED_BACKUP -> backupLauncher.launch(export.fileName)
            ExportKind.PLAIN_JSON -> jsonLauncher.launch(export.fileName)
            ExportKind.CSV -> csvLauncher.launch(export.fileName)
            ExportKind.PDF -> pdfLauncher.launch(export.fileName)
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

    HgScreen(
        title = "সেটিংস",
        subtitle = "নিরাপত্তা, ব্যাকআপ ও পছন্দ",
        onBack = onBack,
        bottomBar = { SnackbarHost(snackbarHost, Modifier.padding(bottom = Space.sm)) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Spacer(Modifier.height(Space.sm))
            if (state.busy) {
                HgNotice(title = "কাজ চলছে…", tone = HgTone.Neutral)
            }

            HgCard(padding = PaddingValues(vertical = Space.xs)) {
                SectionLabel("নিরাপত্তা")
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "অ্যাপের পিন",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Hg.colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    HgButton(
                        text = "পিন বদলান",
                        onClick = { showChangePin = true },
                        style = HgButtonStyle.Ghost
                    )
                }
                HgDivider(startInset = 20)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("ফিঙ্গারপ্রিন্ট দিয়ে খুলুন", style = MaterialTheme.typography.bodyLarge, color = Hg.colors.ink)
                        Text(
                            if (remember { BiometricAuth.canAuthenticate(context) })
                                "পিনের পাশাপাশি ফিঙ্গারপ্রিন্ট/মুখ দিয়েও লক খোলা যাবে"
                            else "এই ফোনে বায়োমেট্রিক সেট করা নেই",
                            style = MaterialTheme.typography.bodySmall,
                            color = Hg.colors.inkSecondary
                        )
                    }
                    Spacer(Modifier.width(Space.md))
                    Switch(
                        checked = biometricEnabled,
                        enabled = remember { BiometricAuth.canAuthenticate(context) },
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
                        },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                            checkedTrackColor = Hg.colors.brand,
                            uncheckedThumbColor = Hg.colors.surface,
                            uncheckedTrackColor = Hg.colors.surfaceSunken,
                            uncheckedBorderColor = Hg.colors.hairlineStrong
                        )
                    )
                }
                HgDivider(startInset = 20)
                Column(Modifier.padding(horizontal = Space.lg, vertical = Space.md)) {
                    Eyebrow("কতক্ষণ পর নিজে থেকে লক হবে")
                    Spacer(Modifier.height(Space.sm))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Space.sm)
                    ) {
                        listOf(0 to "সাথে সাথে", 1 to "১ মিনিট", 5 to "৫ মিনিট", 15 to "১৫ মিনিট", 30 to "৩০ মিনিট")
                            .forEach { (minutes, label) ->
                                HgChip(
                                    text = label,
                                    selected = autoLockMinutes == minutes,
                                    onClick = { viewModel.setAutoLock(minutes) }
                                )
                            }
                    }
                }
                HgButton(
                    text = "এখনই লক করুন",
                    onClick = viewModel::lockNow,
                    style = HgButtonStyle.Secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space.lg)
                )
                Spacer(Modifier.height(Space.md))
                HgNotice(
                    title = "পিন ভুলে গেলে ডেটা ফেরত পাওয়ার উপায় নেই",
                    message = "কোনো মাস্টার পিন নেই। পিন মনে রাখুন এবং এনক্রিপ্ট করা ব্যাকআপের পাসওয়ার্ড আলাদাভাবে সাবধানে রাখুন।",
                    tone = HgTone.Warning,
                    modifier = Modifier.padding(horizontal = Space.lg)
                )
                Spacer(Modifier.height(Space.md))
            }

            HgCard(padding = PaddingValues(vertical = Space.xs)) {
                SectionLabel("পছন্দ")
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "ড্যাশবোর্ডে টাকার অঙ্ক আড়াল করুন",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Hg.colors.ink
                        )
                        Text(
                            "পাশে কেউ থাকলে গোপনীয়তার জন্য",
                            style = MaterialTheme.typography.bodySmall,
                            color = Hg.colors.inkSecondary
                        )
                    }
                    Spacer(Modifier.width(Space.md))
                    Switch(
                        checked = settings.hideAmountsOnDashboard,
                        onCheckedChange = viewModel::setHideAmounts,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                            checkedTrackColor = Hg.colors.brand,
                            uncheckedThumbColor = Hg.colors.surface,
                            uncheckedTrackColor = Hg.colors.surfaceSunken,
                            uncheckedBorderColor = Hg.colors.hairlineStrong
                        )
                    )
                }
                HgDivider(startInset = 20)
                Column(Modifier.padding(horizontal = Space.lg, vertical = Space.md)) {
                    Eyebrow("রিমাইন্ডার কত দিন আগে জানাবে")
                    Spacer(Modifier.height(Space.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        listOf(0, 1, 2, 3, 7).forEach { days ->
                            HgChip(
                                text = if (days == 0) "ঐদিনই"
                                else "${BengaliNumber.formatCount(days.toLong())} দিন",
                                selected = settings.reminderLeadDays == days,
                                onClick = { viewModel.setLeadDays(days) }
                            )
                        }
                    }
                }
            }

            HgCard(padding = PaddingValues(vertical = Space.xs)) {
                SectionLabel("ব্যাকআপ ও এক্সপোর্ট")
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "সর্বশেষ ব্যাকআপ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Hg.colors.inkSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        settings.lastBackupAtMs?.let { Dates.formatShortDate(it) } ?: "এখনো নেওয়া হয়নি",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Hg.colors.ink
                    )
                }
                Column(Modifier.padding(horizontal = Space.lg)) {
                    HgButton(
                        text = "এনক্রিপ্ট করা ব্যাকআপ তৈরি করুন",
                        onClick = { showBackupPassword = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Space.sm))
                    HgButton(
                        text = "ব্যাকআপ থেকে ফেরত আনুন",
                        onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                        style = HgButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                HgDivider(startInset = 20, modifier = Modifier.padding(vertical = Space.sm))
                Column(Modifier.padding(horizontal = Space.lg)) {
                    Eyebrow("ডেটা এক্সপোর্ট")
                    Spacer(Modifier.height(Space.sm))
                    HgButton(
                        text = "সব ডেটা JSON হিসাবে",
                        onClick = viewModel::createJsonExport,
                        style = HgButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Space.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        HgButton(
                            text = "CSV (এই মাস)",
                            onClick = { viewModel.createCsvExport(allTime = false) },
                            style = HgButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                        HgButton(
                            text = "CSV (সব সময়)",
                            onClick = { viewModel.createCsvExport(allTime = true) },
                            style = HgButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(Space.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        HgButton(
                            text = "PDF (এই মাস)",
                            onClick = { viewModel.createPdfExport(allTime = false) },
                            style = HgButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                        HgButton(
                            text = "PDF (সব সময়)",
                            onClick = { viewModel.createPdfExport(allTime = true) },
                            style = HgButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(Space.md))
                    HgNotice(
                        title = "JSON ও CSV এনক্রিপ্টেড নয়",
                        message = "যার হাতে পড়বে সে পড়তে পারবে। নিয়মিত সুরক্ষার জন্য এনক্রিপ্ট করা ব্যাকআপ ব্যবহার করুন।",
                        tone = HgTone.Warning
                    )
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        "সব তথ্য আপনার ফোনেই থাকে — ইন্টারনেট ছাড়াই ব্যাকআপ তৈরি ও রিস্টোর করা যায়।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkSecondary
                    )
                }
                Spacer(Modifier.height(Space.md))
            }

            HgCard(padding = PaddingValues(vertical = Space.xs)) {
                SectionLabel("অ্যাপ সম্পর্কে")
                Column(Modifier.padding(horizontal = Space.lg, vertical = Space.sm)) {
                    HgButton(
                        text = "হিসাবঘর সম্পর্কে জানুন",
                        onClick = onOpenAbout,
                        style = HgButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(Space.xxl))
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
            containerColor = Hg.colors.surface,
            shape = Radius.panel,
            title = { Text("ব্যাকআপ ফেরত আনবেন?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    RestoreLine("ব্যাকআপের তারিখ", Dates.formatShortDate(preview.createdAtEpochMs))
                    RestoreLine("অ্যাপ সংস্করণ", BengaliNumber.toBengaliDigits(preview.appVersion))
                    RestoreLine(
                        "অ্যাকাউন্ট",
                        BengaliNumber.formatCount(preview.accountCount.toLong()) + "টি"
                    )
                    RestoreLine(
                        "লেনদেন",
                        BengaliNumber.formatCount(preview.transactionCount.toLong()) + "টি"
                    )
                    RestoreLine("দোকান", BengaliNumber.formatCount(preview.shopCount.toLong()) + "টি")
                    RestoreLine("ঋণ/ধার", BengaliNumber.formatCount(preview.loanCount.toLong()) + "টি")
                    RestoreLine(
                        "রসিদের ছবি",
                        BengaliNumber.formatCount(preview.attachmentFilesIncluded.toLong()) + "টি আছে" +
                            if (preview.attachmentFilesMissing > 0)
                                ", ${BengaliNumber.formatCount(preview.attachmentFilesMissing.toLong())}টি পাওয়া যায়নি"
                            else ""
                    )
                    Spacer(Modifier.height(Space.sm))
                    HgNotice(
                        title = "এখনকার সব তথ্য প্রতিস্থাপিত হবে",
                        message = "দরকার হলে আগে বর্তমান ডেটার আলাদা ব্যাকআপ নিয়ে রাখুন।",
                        tone = HgTone.Expense
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRestore) {
                    Text("হ্যাঁ, ফেরত আনুন", color = Hg.colors.expense)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRestore) {
                    Text("বাতিল", color = Hg.colors.inkSecondary)
                }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = Hg.colors.ink,
        modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.md)
    )
}

@Composable
private fun RestoreLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = Space.xxs)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Hg.colors.inkSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = Hg.colors.ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1.4f)
        )
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
        containerColor = Hg.colors.surface,
        shape = Radius.panel,
        title = { Text("পিন বদলান") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                PinField(oldPin, { oldPin = it }, "বর্তমান পিন")
                PinField(newPin, { newPin = it }, "নতুন পিন (কমপক্ষে ৪ সংখ্যা)")
                PinField(confirmPin, { confirmPin = it }, "নতুন পিন আবার দিন")
                error?.let { HgNotice(title = it, tone = HgTone.Expense) }
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
            }) { Text("বদলান", color = Hg.colors.brand) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল", color = Hg.colors.inkSecondary) }
        }
    )
}

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    HgTextField(
        value = value,
        onValueChange = { v -> onValueChange(v.filter { it.isDigit() }.take(12)) },
        label = label,
        keyboardType = KeyboardType.NumberPassword,
        visualTransformation = PasswordVisualTransformation()
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
        containerColor = Hg.colors.surface,
        shape = Radius.panel,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                Text(message, style = MaterialTheme.typography.bodyMedium, color = Hg.colors.inkSecondary)
                HgTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "পাসওয়ার্ড",
                    visualTransformation = PasswordVisualTransformation()
                )
                if (confirmRequired) {
                    HgTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = "পাসওয়ার্ড আবার দিন",
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
                error?.let { HgNotice(title = it, tone = HgTone.Expense) }
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
            }) { Text("ঠিক আছে", color = Hg.colors.brand) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল", color = Hg.colors.inkSecondary) }
        }
    )
}
