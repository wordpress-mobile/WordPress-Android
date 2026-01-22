package org.wordpress.android.ui.main

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.main.ChooseSiteViewHolder.Companion.shouldShowRemoveButton

class ChooseSiteViewHolderTest {
    @Test
    fun `remove button is visible for self-hosted site in normal mode`() {
        val result = shouldShowRemoveButton(mode = ActionMode.None, isSelfHostedSite = true)

        assertThat(result).isTrue()
    }

    @Test
    fun `remove button is hidden for self-hosted site in pin mode`() {
        val result = shouldShowRemoveButton(mode = ActionMode.Pin, isSelfHostedSite = true)

        assertThat(result).isFalse()
    }

    @Test
    fun `remove button is hidden for wpcom site in normal mode`() {
        val result = shouldShowRemoveButton(mode = ActionMode.None, isSelfHostedSite = false)

        assertThat(result).isFalse()
    }

    @Test
    fun `remove button is hidden for wpcom site in pin mode`() {
        val result = shouldShowRemoveButton(mode = ActionMode.Pin, isSelfHostedSite = false)

        assertThat(result).isFalse()
    }
}
