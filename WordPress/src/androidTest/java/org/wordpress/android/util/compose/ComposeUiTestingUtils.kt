package org.wordpress.android.util.compose

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

class ComposeUiTestingUtils(
    private val composeTestRule: ComposeTestRule
) {
    /**
     * Performs a click on a node with the given text.
     *
     * @param text The text of the node to click on.
     */
    fun performClickOnNodeWithText(text: String) {
        composeTestRule.onNodeWithText(text).performClick()
    }
}
