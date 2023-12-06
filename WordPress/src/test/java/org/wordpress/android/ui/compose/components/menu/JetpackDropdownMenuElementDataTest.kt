package org.wordpress.android.ui.compose.components.menu

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JetpackDropdownMenuElementDataTest {
    @Test
    fun `DropdownMenuItemData Item should have the correct hasDivider default value`() {
        val actual = JetpackDropdownMenuElementData.Item("", {}).hasDivider
        val expected = false
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Item should have the correct leadingIcon default value`() {
        val actual = JetpackDropdownMenuElementData.Item("", {}).leadingIcon
        val expected = NO_ICON
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `SubMenu should have the correct hasDivider default value`() {
        val actual = JetpackDropdownMenuElementData.SubMenu("", emptyList()).hasDivider
        val expected = false
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `DropdownMenuItemData SubMenu should have the correct leadingIcon value`() {
        val actual = JetpackDropdownMenuElementData.SubMenu("", emptyList()).leadingIcon
        val expected = NO_ICON
        assertThat(actual).isEqualTo(expected)
    }
}
