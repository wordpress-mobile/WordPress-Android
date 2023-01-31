package org.wordpress.android.util.compose

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

class ComposeUiTestingUtils(
    private val composeTestRule: ComposeTestRule
) {

    fun performClickOnNodeWithText(text: String) {
        composeTestRule.onNodeWithText(text).performClick()
    }
}
