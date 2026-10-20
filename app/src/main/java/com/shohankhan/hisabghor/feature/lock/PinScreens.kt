package com.shohankhan.hisabghor.feature.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.core.security.BiometricAuth
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer

@Composable
fun rememberLockViewModel(): LockViewModel {
    val container = rememberAppContainer()
    return viewModel(
        factory = hisabViewModelFactory {
            LockViewModel(container.securityRepository, container.appLockManager)
        }
    )
}

/** First-run PIN creation (shown before any financial data is visible). */
@Composable
fun PinSetupScreen(viewModel: LockViewModel = rememberLockViewModel()) {
    val state by viewModel.setupState.collectAsState()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(44.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("হিসাবঘরে স্বাগতম!", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "আপনার সব হিসাব এই ফোনেই নিরাপদে থাকবে — ইন্টারনেটের প্রয়োজন নেই। " +
                    "শুরুতে একটি পিন তৈরি করুন, যা দিয়ে অ্যাপটি খুলবেন।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Text(
                when (state.stage) {
                    PinSetupStage.ENTER -> "নতুন পিন দিন (৪–৬ সংখ্যা)"
                    PinSetupStage.CONFIRM -> "আবার পিন দিন"
                    PinSetupStage.DONE -> "পিন তৈরি হয়েছে!"
                },
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(14.dp))
            PinDots(entered = state.enteredLength, maxLength = PIN_MAX_LENGTH)
            Spacer(Modifier.height(10.dp))
            state.message?.let {
                Text(
                    it,
                    color = if (state.isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
            }
            PinPad(
                onDigit = viewModel::setupDigit,
                onBackspace = viewModel::setupBackspace
            )
            Button(
                onClick = viewModel::setupNext,
                enabled = state.enteredLength >= PIN_MIN_LENGTH,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.stage == PinSetupStage.ENTER) "পরের ধাপ" else "পিন তৈরি করুন")
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "পিনটি মনে রাখুন। পিন ভুলে গেলে অ্যাপের তথ্য ফিরে পাওয়ার একমাত্র উপায় " +
                    "আগে থেকে তৈরি করা এনক্রিপ্টেড ব্যাকআপ — কোনো গোপন পিন নেই।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** App unlock screen (PIN + optional biometrics). */
@Composable
fun LockScreen(viewModel: LockViewModel = rememberLockViewModel()) {
    val state by viewModel.unlockState.collectAsState()
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricAvailable = remember(context) { BiometricAuth.canAuthenticate(context) }
    val showBiometric = biometricEnabled && biometricAvailable && activity != null

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                "হিসাবঘর",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "আপনার সব হিসাব, এক জায়গায়।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))
            Text("আনলক করতে পিন দিন", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(14.dp))
            PinDots(entered = state.enteredLength, maxLength = PIN_MAX_LENGTH)
            Spacer(Modifier.height(10.dp))

            if (state.lockedOutSeconds > 0) {
                Text(
                    "অনুগ্রহ করে ${BengaliNumber.formatCount(state.lockedOutSeconds)} সেকেন্ড অপেক্ষা করুন…",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
            } else {
                state.message?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            PinPad(
                onDigit = viewModel::unlockDigit,
                onBackspace = viewModel::unlockBackspace,
                enabled = state.lockedOutSeconds <= 0 && !state.verifying,
                leftAction = if (showBiometric) {
                    {
                        IconButton(
                            onClick = {
                                BiometricAuth.showPrompt(
                                    activity = activity!!,
                                    onSuccess = viewModel::onBiometricSuccess,
                                    onError = viewModel::onBiometricError
                                )
                            }
                        ) {
                            Icon(
                                Icons.Filled.Fingerprint,
                                contentDescription = "ফিঙ্গারপ্রিন্ট দিয়ে আনলক করুন",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else null
            )
            Button(
                onClick = viewModel::submitUnlock,
                enabled = state.enteredLength >= PIN_MIN_LENGTH && state.lockedOutSeconds <= 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("আনলক করুন")
            }
        }
    }
}
