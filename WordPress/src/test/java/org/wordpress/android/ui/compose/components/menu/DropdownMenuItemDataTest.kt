package org.wordpress.android.ui.compose.components.menu

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DropdownMenuItemDataTest {
    @Test
    fun `DropdownMenuItemData Item should have the correct isDefault default value`() {
        val actual = DropdownMenuItemData.Item("", "", {}).isDefault
        val expected = false
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `DropdownMenuItemData Item should have the correct hasDivider default value`() {
        val actual = DropdownMenuItemData.Item("", "", {}).hasDivider
        val expected = false
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `DropdownMenuItemData Item should have the correct leftIcon default value`() {
        val actual = DropdownMenuItemData.Item("", "", {}).leftIcon
        val expected = NO_ICON
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `DropdownMenuItemData Item should have the correct rightIcon default value`() {
        val actual = DropdownMenuItemData.Item("", "", {}).rightIcon
        val expected = NO_ICON
        assertThat(actual).isEqualTo(expected)
    }
}
