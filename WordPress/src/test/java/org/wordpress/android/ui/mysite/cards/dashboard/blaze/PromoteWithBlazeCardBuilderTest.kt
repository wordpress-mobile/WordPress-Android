package org.wordpress.android.ui.mysite.cards.dashboard.blaze

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PromoteWithBlazeCardBuilderParams
import org.wordpress.android.ui.mysite.cards.blaze.PromoteWithBlazeCardBuilder
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString

class PromoteWithBlazeCardBuilderTest {
    private lateinit var builder: PromoteWithBlazeCardBuilder

    @Before
    fun setUp() {
        builder = PromoteWithBlazeCardBuilder()
    }
    @Test
    fun `when is eligible for blaze, then return the PromoteWithBlazeCard`() {
        // Arrange
        val params = PromoteWithBlazeCardBuilderParams(
            isEligible = true,
            onClick = { },
            onHideMenuItemClick = { },
            onMoreMenuClick = { }
        )

        // Act
        val result = builder.build(params)

        // Assert
        assertNotNull(result)
        assertEquals(R.string.promote_blaze_card_title, (result!!.title as UiString.UiStringRes).stringRes)
        assertEquals(R.string.promote_blaze_card_sub_title, (result.subtitle as UiString.UiStringRes).stringRes)
        assertNotNull(result.onClick)
        assertNotNull(result.onHideMenuItemClick)
        assertNotNull(result.onMoreMenuClick)

    }

    @Test
    fun `when is not eligible for blaze, then return null`() {
        // Arrange
        val params = PromoteWithBlazeCardBuilderParams(
            isEligible = false,
            onClick = { },
            onHideMenuItemClick = { },
            onMoreMenuClick = { }
        )

        // Act
        val result = builder.build(params)

        // Assert
        assertNull(result)
    }
}
