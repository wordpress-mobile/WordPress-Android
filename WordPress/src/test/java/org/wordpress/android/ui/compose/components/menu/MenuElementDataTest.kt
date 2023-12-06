package org.wordpress.android.ui.compose.components.menu

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.compose.components.menu.dropdown.MenuElementData
import org.wordpress.android.ui.compose.components.menu.dropdown.NO_ICON

class MenuElementDataTest {
    @Test
    fun `DropdownMenuItemData Item should have the correct hasDivider default value`() {
        val actual = MenuElementData.Item("", {}).hasDivider
        val expected = false
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Item should have the correct leadingIcon default value`() {
        val actual = MenuElementData.Item("", {}).leadingIcon
        val expected = NO_ICON
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `SubMenu should have the correct hasDivider default value`() {
        val actual = MenuElementData.SubMenu("", emptyList()).hasDivider
        val expected = false
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `DropdownMenuItemData SubMenu should have the correct leadingIcon value`() {
        val actual = MenuElementData.SubMenu("", emptyList()).leadingIcon
        val expected = NO_ICON
        assertThat(actual).isEqualTo(expected)
    }
}
