package com.shohankhan.hisabghor

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.shohankhan.hisabghor.core.designsystem.HgBadge
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgNotice
import com.shohankhan.hisabghor.core.designsystem.HgTone
import com.shohankhan.hisabghor.core.designsystem.HisabGhorTheme
import org.junit.Rule
import org.junit.Test

/**
 * Renders the shared design-system pieces that every redesigned screen uses.
 * Runs on a device/emulator (see docs/TESTING.md); CI compiles it so import
 * and signature breaks are caught even without an emulator.
 */
class DesignSystemSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun coreComponents_renderBengaliCopy() {
        composeRule.setContent {
            HisabGhorTheme {
                Column {
                    Text("হিসাবঘর")
                    HgButton(text = "সংরক্ষণ করুন", onClick = {})
                    HgNotice(title = "সতর্কতা", message = "ব্যাকআপ নিন", tone = HgTone.Warning)
                    HgBadge(text = "চলমান")
                }
            }
        }

        composeRule.onNodeWithText("হিসাবঘর").assertIsDisplayed()
        composeRule.onNodeWithText("সংরক্ষণ করুন").assertIsDisplayed()
        composeRule.onNodeWithText("সতর্কতা").assertIsDisplayed()
        composeRule.onNodeWithText("চলমান").assertIsDisplayed()
    }
}
