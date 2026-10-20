package com.shohankhan.hisabghor.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Optional biometric unlock. Availability is checked at runtime (hardware,
 * enrollment and device lock state all matter); the PIN always remains as
 * the fallback. Biometric success only opens the app UI — it never replaces
 * the backup password.
 */
object BiometricAuth {

    fun canAuthenticate(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        onError(errString.toString())
                    }
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("হিসাবঘর আনলক করুন")
            .setSubtitle("ফিঙ্গারপ্রিন্ট বা ফেস দিয়ে নিশ্চিত করুন")
            .setNegativeButtonText("পিন ব্যবহার করুন")
            .build()
        prompt.authenticate(info)
    }
}
