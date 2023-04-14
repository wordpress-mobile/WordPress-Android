package org.wordpress.android.ui.mysite.cards.dashboard.domain

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.utils.UiString

class DashboardDomainCardBuilderTest {
    private lateinit var builder: DashboardDomainCardBuilder

    @Before
    fun setUp() {
        builder = DashboardDomainCardBuilder()
    }
    @Test
    fun `when is eligible for domain, then return the DashboardDomainCard`() {
        // Arrange
        val params = MySiteCardAndItemBuilderParams.DashboardCardDomainBuilderParams(
            isEligible = true,
            onClick = { },
            onHideMenuItemClick = { },
            onMoreMenuClick = { }
        )

        // Act
        val result = builder.build(params)

        // Assert
        Assert.assertNotNull(result)
        Assert.assertEquals(
            R.string.dashboard_card_domain_title,
            (result!!.title as UiString.UiStringRes).stringRes
        )
        Assert.assertEquals(
            R.string.dashboard_card_domain_sub_title,
            (result.subtitle as UiString.UiStringRes).stringRes
        )
        Assert.assertNotNull(result.onClick)
        Assert.assertNotNull(result.onHideMenuItemClick)
        Assert.assertNotNull(result.onMoreMenuClick)
    }

    @Test
    fun `when is not eligible for blaze, then return null`() {
        // Arrange
        val params = MySiteCardAndItemBuilderParams.DashboardCardDomainBuilderParams(
            isEligible = false,
            onClick = { },
            onHideMenuItemClick = { },
            onMoreMenuClick = { }
        )

        // Act
        val result = builder.build(params)

        // Assert
        Assert.assertNull(result)
    }
}
