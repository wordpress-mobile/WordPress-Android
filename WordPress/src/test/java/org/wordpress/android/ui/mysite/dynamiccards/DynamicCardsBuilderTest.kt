package org.wordpress.android.ui.mysite.dynamiccards

import org.junit.Before
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.mysite.dynamiccards.quickstart.QuickStartItemBuilder
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig

class DynamicCardsBuilderTest : BaseUnitTest() {
    @Mock lateinit var quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig
    @Mock lateinit var quickStartItemBuilder: QuickStartItemBuilder
    private lateinit var dynamicCardsBuilder: DynamicCardsBuilder

    @Before
    fun setUp() {
        setUpDynamicCardsBuilder()
    }

    private fun setUpDynamicCardsBuilder() {
        dynamicCardsBuilder = DynamicCardsBuilder(quickStartDynamicCardsFeatureConfig, quickStartItemBuilder)
    }
}
