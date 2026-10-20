package com.shohankhan.hisabghor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.shohankhan.hisabghor.core.designsystem.HisabGhorTheme
import com.shohankhan.hisabghor.feature.lock.PinPad
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Focused UI test: the PIN pad emits Bengali-labelled digits correctly. */
class PinPadUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pinPad_emitsDigitsAndBackspace() {
        val entered = StringBuilder()
        var backspaces = 0
        composeRule.setContent {
            HisabGhorTheme {
                PinPad(
                    onDigit = { entered.append(it) },
                    onBackspace = { backspaces++ }
                )
            }
        }

        composeRule.onNodeWithContentDescription("সংখ্যা ১").performClick()
        composeRule.onNodeWithContentDescription("সংখ্যা ২").performClick()
        composeRule.onNodeWithContentDescription("সংখ্যা ৩").performClick()
        composeRule.onNodeWithContentDescription("সংখ্যা ০").performClick()
        composeRule.onNodeWithContentDescription("শেষ সংখ্যা মুছুন").performClick()

        assertEquals("1230", entered.toString())
        assertEquals(1, backspaces)
    }
}
