package org.wordpress.android.ui.stats.refresh.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.util.RtlUtils
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class ContentDescriptionHelperTest {
    @Mock
    lateinit var rtlUtils: RtlUtils

    @Mock
    lateinit var resourceProvider: ResourceProvider
    private lateinit var contentDescriptionHelper: ContentDescriptionHelper

    @Before
    fun setUp() {
        contentDescriptionHelper = ContentDescriptionHelper(resourceProvider, rtlUtils)
    }

    @Test
    fun `returns RTL text when isRtl true`() {
        val keyResource = R.string.stats_views
        val keyLabel = "Views"
        val value: Long = 53

        whenever(resourceProvider.getString(keyResource)).thenReturn(keyLabel)
        whenever(rtlUtils.isRtl).thenReturn(true)

        val contentDescription = contentDescriptionHelper.buildContentDescription(keyResource, value)

        assertThat(contentDescription).isEqualTo("53 :Views")
    }

    @Test
    fun `returns LTR text when isRtl is false`() {
        val keyResource = R.string.stats_views
        val keyLabel = "Views"
        val value: Long = 53

        whenever(resourceProvider.getString(keyResource)).thenReturn(keyLabel)
        whenever(rtlUtils.isRtl).thenReturn(false)

        val contentDescription = contentDescriptionHelper.buildContentDescription(keyResource, value)

        assertThat(contentDescription).isEqualTo("Views: 53")
    }
}
