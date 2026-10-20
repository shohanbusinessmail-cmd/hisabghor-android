package com.shohankhan.hisabghor.feature.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgButtonStyle
import com.shohankhan.hisabghor.core.designsystem.HgNotice
import com.shohankhan.hisabghor.core.designsystem.HgTone
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.Space
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
    Surface(Modifier.fillMaxSize(), color = Hg.colors.canvas) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Space.xxl))
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Hg.colors.brandTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Hg.colors.brand,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(Space.md))
            Text(
                "হিসাবঘরে স্বাগতম",
                style = MaterialTheme.typography.headlineMedium,
                color = Hg.colors.ink
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                "আপনার সব হিসাব এই ফোনেই নিরাপদে থাকবে — ইন্টারনেটের প্রয়োজন নেই।",
                style = MaterialTheme.typography.bodyMedium,
                color = Hg.colors.inkSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Space.xl))
            Text(
                when (state.stage) {
                    PinSetupStage.ENTER -> "নতুন পিন দিন (৪–৬ সংখ্যা)"
                    PinSetupStage.CONFIRM -> "আবার পিন দিন"
                    PinSetupStage.DONE -> "পিন তৈরি হয়েছে"
                },
                style = MaterialTheme.typography.titleMedium,
                color = Hg.colors.ink
            )
            Spacer(Modifier.height(Space.md))
            PinDots(entered = state.enteredLength, maxLength = PIN_MAX_LENGTH)
            Spacer(Modifier.height(Space.sm))
            state.message?.let {
                HgNotice(
                    title = it,
                    tone = if (state.isError) HgTone.Expense else HgTone.Neutral
                )
                Spacer(Modifier.height(Space.sm))
            }
            Spacer(Modifier.height(Space.sm))
            PinPad(
                onDigit = viewModel::setupDigit,
                onBackspace = viewModel::setupBackspace
            )
            Spacer(Modifier.height(Space.md))
            HgButton(
                text = if (state.stage == PinSetupStage.ENTER) "পরের ধাপ" else "পিন তৈরি করুন",
                onClick = viewModel::setupNext,
                enabled = state.enteredLength >= PIN_MIN_LENGTH,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Space.lg))
            Text(
                "পিনটি মনে রাখুন। পিন ভুলে গেলে অ্যাপের তথ্য ফিরে পাওয়ার একমাত্র উপায় " +
                    "আগে থেকে তৈরি করা এনক্রিপ্টেড ব্যাকআপ — কোনো গোপন পিন নেই।",
                style = MaterialTheme.typography.bodySmall,
                color = Hg.colors.inkTertiary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Space.xxl))
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

    Surface(Modifier.fillMaxSize(), color = Hg.colors.canvas) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(Space.huge))
            Text(
                "হিসাবঘর",
                style = MaterialTheme.typography.headlineLarge,
                color = Hg.colors.brand
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                "আপনার সব হিসাব, এক জায়গায়।",
                style = MaterialTheme.typography.bodyMedium,
                color = Hg.colors.inkSecondary
            )
            Spacer(Modifier.height(Space.xxl))
            Text(
                "আনলক করতে পিন দিন",
                style = MaterialTheme.typography.titleMedium,
                color = Hg.colors.ink
            )
            Spacer(Modifier.height(Space.md))
            PinDots(entered = state.enteredLength, maxLength = PIN_MAX_LENGTH)
            Spacer(Modifier.height(Space.sm))

            val notice = if (state.lockedOutSeconds > 0) {
                "অনুগ্রহ করে ${BengaliNumber.formatCount(state.lockedOutSeconds)} সেকেন্ড অপেক্ষা করুন…"
            } else state.message
            notice?.let {
                HgNotice(title = it, tone = HgTone.Expense)
                Spacer(Modifier.height(Space.sm))
            }

            Spacer(Modifier.height(Space.sm))
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
                                tint = Hg.colors.brand
                            )
                        }
                    }
                } else null
            )
            Spacer(Modifier.height(Space.md))
            HgButton(
                text = "আনলক করুন",
                onClick = viewModel::submitUnlock,
                enabled = state.enteredLength >= PIN_MIN_LENGTH && state.lockedOutSeconds <= 0,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Space.md))
            Text(
                "পিন কেউ দেখছে না তো? পিন আপনার ফোনেই জমা থাকে, কোথাও পাঠানো হয় না।",
                style = MaterialTheme.typography.bodySmall,
                color = Hg.colors.inkTertiary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}
