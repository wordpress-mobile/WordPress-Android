package org.wordpress.android.ui.prefs.timezone

import android.content.Context
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.inOrder
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.getOrAwaitValue
import org.wordpress.android.viewmodel.ResourceProvider


class SiteSettingsTimezoneViewModelTest : BaseUnitTest() {
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var context: Context

    private lateinit var viewModel: SiteSettingsTimezoneViewModel

    @Before
    fun setUp() {
        viewModel = SiteSettingsTimezoneViewModel(resourceProvider)
    }

    @Test
    fun testSearchTimezones() {
        viewModel.searchTimezones("Sydney")
        assertEquals(viewModel.timezones.getOrAwaitValue(), listOf<TimezonesList>())
    }

    @Test
    fun testGetShowEmptyView() {
        viewModel.filterTimezones("")
        assertEquals(viewModel.showEmptyView.getOrAwaitValue(), true)
    }

    @Test
    fun testGetSelectedTimezone() {
        viewModel.onTimezoneSelected("Australia/Sydney")
        assertEquals(viewModel.selectedTimezone.getOrAwaitValue(), "Australia/Sydney")
    }

    @Test
    fun testOnTimezoneSelected() {
        viewModel.onTimezoneSelected("Australia/Sydney")
        assertEquals(viewModel.dismissBottomSheet.getOrAwaitValue(), null)
    }

    @Test
    fun testOnSearchCancelled() {
        viewModel.onSearchCancelled()
        assertEquals(viewModel.timezones.getOrAwaitValue(), listOf<TimezonesList>())
    }

    @Ignore("Difficult to test volley requests without significant test setup")
    @Test
    fun testTestGetTimezones() {
        Mockito.`when`(viewModel.getTimezones(context)).thenReturn(Unit)

        viewModel.getTimezones(context)

        inOrder(viewModel.showProgressView).apply {
            verify(viewModel.showProgressView).getOrAwaitValue(anyOrNull())
            verify(viewModel.timezones).getOrAwaitValue(anyOrNull())
            verifyNoMoreInteractions()
        }
    }
}
