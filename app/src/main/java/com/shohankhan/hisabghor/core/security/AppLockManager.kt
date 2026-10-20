package com.shohankhan.hisabghor.core.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.shohankhan.hisabghor.core.common.Dates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class LockState { LOADING, NEEDS_PIN_SETUP, LOCKED, UNLOCKED }

/**
 * Tracks app-lock state across foreground/background. When the app returns
 * from background after the configured idle time, it locks again. The UI root
 * shows PIN setup, the lock screen, or the main UI based on [state].
 */
class AppLockManager(
    private val security: SecurityRepository,
    private val scope: CoroutineScope
) : DefaultLifecycleObserver {

    private val _state = MutableStateFlow(LockState.LOADING)
    val state: StateFlow<LockState> = _state

    @Volatile
    private var backgroundedAtMs: Long = 0L

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        scope.launch {
            _state.value = if (security.isPinSet.first()) LockState.LOCKED else LockState.NEEDS_PIN_SETUP
        }
    }

    fun onPinConfigured() {
        _state.value = LockState.UNLOCKED
    }

    fun unlock() {
        _state.value = LockState.UNLOCKED
    }

    fun lockNow() {
        if (_state.value == LockState.UNLOCKED) _state.value = LockState.LOCKED
    }

    override fun onStop(owner: LifecycleOwner) {
        backgroundedAtMs = Dates.nowMs()
    }

    override fun onStart(owner: LifecycleOwner) {
        if (_state.value != LockState.UNLOCKED) return
        val backgroundedAt = backgroundedAtMs
        if (backgroundedAt == 0L) return
        scope.launch {
            val minutes = security.autoLockMinutes.first()
            val elapsed = Dates.nowMs() - backgroundedAt
            if (elapsed >= minutes * 60_000L) {
                _state.value = LockState.LOCKED
            }
        }
    }
}
