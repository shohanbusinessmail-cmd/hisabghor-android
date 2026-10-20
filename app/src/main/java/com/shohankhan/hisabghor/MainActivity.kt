package com.shohankhan.hisabghor

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.shohankhan.hisabghor.core.designsystem.HisabGhorTheme
import com.shohankhan.hisabghor.core.navigation.MainNavigation
import com.shohankhan.hisabghor.core.security.LockState
import com.shohankhan.hisabghor.di.rememberAppContainer
import com.shohankhan.hisabghor.feature.lock.LockScreen
import com.shohankhan.hisabghor.feature.lock.PinSetupScreen

/**
 * Single-activity host (FragmentActivity so BiometricPrompt can attach).
 * The financial UI is only composed after the app is unlocked.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HisabGhorTheme {
                HisabGhorRoot()
            }
        }
    }
}

@Composable
private fun HisabGhorRoot() {
    val container = rememberAppContainer()
    val lockState by container.appLockManager.state.collectAsState()
    when (lockState) {
        LockState.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        LockState.NEEDS_PIN_SETUP -> PinSetupScreen()
        LockState.LOCKED -> LockScreen()
        LockState.UNLOCKED -> MainNavigation()
    }
}
