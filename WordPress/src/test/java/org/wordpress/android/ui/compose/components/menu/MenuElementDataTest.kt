package org.wordpress.android.ui.compose.components.menu

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.compose.components.menu.dropdown.MenuElementData
import org.wordpress.android.ui.compose.components.menu.dropdown.NO_ICON
import org.wordpress.android.ui.utils.UiString.UiStringText

class MenuElementDataTest {
    @Test
    fun `Single should have the correct leadingIcon default value`() {
        val actual = MenuElementData.Item.Single("id", UiStringText("")).leadingIcon
        val expected = NO_ICON
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `SubMenu should have the correct leadingIcon value`() {
        val actual = MenuElementData.Item.SubMenu("id", UiStringText(""), emptyList()).leadingIcon
        val expected = NO_ICON
        assertThat(actual).isEqualTo(expected)
    }
}
