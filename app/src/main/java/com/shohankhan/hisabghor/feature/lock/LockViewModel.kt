package com.shohankhan.hisabghor.feature.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.core.security.AppLockManager
import com.shohankhan.hisabghor.core.security.PinVerifyResult
import com.shohankhan.hisabghor.core.security.SecurityRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val PIN_MIN_LENGTH = 4
const val PIN_MAX_LENGTH = 6

enum class PinSetupStage { ENTER, CONFIRM, DONE }

data class PinSetupUiState(
    val stage: PinSetupStage = PinSetupStage.ENTER,
    val enteredLength: Int = 0,
    val message: String? = null,
    val isError: Boolean = false
)

data class UnlockUiState(
    val enteredLength: Int = 0,
    val message: String? = null,
    val lockedOutSeconds: Long = 0,
    val verifying: Boolean = false
)

class LockViewModel(
    private val security: SecurityRepository,
    private val appLock: AppLockManager
) : ViewModel() {

    val biometricEnabled: StateFlow<Boolean> = security.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ---------- PIN setup / change ----------

    private val _setupState = MutableStateFlow(PinSetupUiState())
    val setupState: StateFlow<PinSetupUiState> = _setupState

    private var setupBuffer = StringBuilder()
    private var firstPin: String? = null

    fun setupDigit(digit: Char) {
        if (setupBuffer.length >= PIN_MAX_LENGTH) return
        setupBuffer.append(digit)
        _setupState.update { it.copy(enteredLength = setupBuffer.length, message = null) }
        if (setupBuffer.length == PIN_MAX_LENGTH) setupNext()
    }

    fun setupBackspace() {
        if (setupBuffer.isNotEmpty()) setupBuffer.deleteCharAt(setupBuffer.lastIndex)
        _setupState.update { it.copy(enteredLength = setupBuffer.length) }
    }

    fun setupNext() {
        val pin = setupBuffer.toString()
        if (pin.length < PIN_MIN_LENGTH) {
            _setupState.update {
                it.copy(message = "কমপক্ষে ${BengaliNumber.formatCount(PIN_MIN_LENGTH.toLong())} সংখ্যার পিন দিন", isError = true)
            }
            return
        }
        when (_setupState.value.stage) {
            PinSetupStage.ENTER -> {
                firstPin = pin
                setupBuffer = StringBuilder()
                _setupState.value = PinSetupUiState(
                    stage = PinSetupStage.CONFIRM,
                    message = "নিশ্চিত করতে আবার একই পিন দিন"
                )
            }
            PinSetupStage.CONFIRM -> {
                if (pin == firstPin) {
                    viewModelScope.launch {
                        security.setPin(pin)
                        _setupState.value = PinSetupUiState(stage = PinSetupStage.DONE)
                        appLock.onPinConfigured()
                    }
                } else {
                    firstPin = null
                    setupBuffer = StringBuilder()
                    _setupState.value = PinSetupUiState(
                        stage = PinSetupStage.ENTER,
                        message = "দুইবার দেওয়া পিন মিলল না — আবার শুরু করুন",
                        isError = true
                    )
                }
            }
            PinSetupStage.DONE -> Unit
        }
    }

    // ---------- Unlock ----------

    private val _unlockState = MutableStateFlow(UnlockUiState())
    val unlockState: StateFlow<UnlockUiState> = _unlockState

    private var unlockBuffer = StringBuilder()

    init {
        refreshLockout()
    }

    fun unlockDigit(digit: Char) {
        if (_unlockState.value.lockedOutSeconds > 0) return
        if (unlockBuffer.length >= PIN_MAX_LENGTH) return
        unlockBuffer.append(digit)
        _unlockState.update { it.copy(enteredLength = unlockBuffer.length, message = null) }
        if (unlockBuffer.length == PIN_MAX_LENGTH) submitUnlock()
    }

    fun unlockBackspace() {
        if (unlockBuffer.isNotEmpty()) unlockBuffer.deleteCharAt(unlockBuffer.lastIndex)
        _unlockState.update { it.copy(enteredLength = unlockBuffer.length) }
    }

    fun submitUnlock() {
        val pin = unlockBuffer.toString()
        if (pin.length < PIN_MIN_LENGTH || _unlockState.value.verifying) return
        _unlockState.update { it.copy(verifying = true) }
        viewModelScope.launch {
            when (val result = security.verifyPin(pin)) {
                PinVerifyResult.Success -> {
                    unlockBuffer = StringBuilder()
                    _unlockState.value = UnlockUiState()
                    appLock.unlock()
                }
                is PinVerifyResult.Wrong -> {
                    unlockBuffer = StringBuilder()
                    _unlockState.value = UnlockUiState(
                        message = "পিন সঠিক নয়। আর ${BengaliNumber.formatCount(result.attemptsBeforeDelay.toLong())} বার চেষ্টা করা যাবে, তারপর কিছুক্ষণ অপেক্ষা করতে হবে।"
                    )
                }
                is PinVerifyResult.LockedOut -> {
                    unlockBuffer = StringBuilder()
                    _unlockState.value = UnlockUiState(message = "অনেকবার ভুল পিন দেওয়া হয়েছে।")
                    refreshLockout()
                }
                PinVerifyResult.NotConfigured -> {
                    _unlockState.value = UnlockUiState(message = "পিন সেট করা নেই।")
                }
            }
        }
    }

    fun onBiometricSuccess() {
        unlockBuffer = StringBuilder()
        _unlockState.value = UnlockUiState()
        appLock.unlock()
    }

    fun onBiometricError(message: String) {
        _unlockState.update { it.copy(message = message) }
    }

    private fun refreshLockout() {
        viewModelScope.launch {
            while (true) {
                val remainingMs = security.lockoutRemainingMs()
                _unlockState.update { it.copy(lockedOutSeconds = (remainingMs + 999) / 1000) }
                if (remainingMs <= 0) break
                delay(1000)
            }
        }
    }

    // ---------- Change PIN (from Settings) ----------

    suspend fun verifyCurrentPin(pin: String): Boolean =
        security.verifyPin(pin) == PinVerifyResult.Success

    fun nowLabel(): String = Dates.formatDateTime(Dates.nowMs())
}
