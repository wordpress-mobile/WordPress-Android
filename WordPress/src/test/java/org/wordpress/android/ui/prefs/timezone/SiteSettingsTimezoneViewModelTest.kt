package org.wordpress.android.ui.prefs.timezone

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
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
        viewModel.timezones.observeForever {}
        viewModel.searchTimezones("Sydney")
        assertEquals(viewModel.timezones.value, listOf<TimezonesList>())
    }

    @Test
    fun testGetShowEmptyView() {
        viewModel.showEmptyView.observeForever {}
        viewModel.filterTimezones("")
        assertEquals(viewModel.showEmptyView.value, true)
    }

    @Test
    fun testGetSelectedTimezone() {
        viewModel.selectedTimezone.observeForever {}
        viewModel.onTimezoneSelected("Australia/Sydney")
        assertEquals(viewModel.selectedTimezone.value, "Australia/Sydney")
    }

    @Test
    fun testOnTimezoneSelected() {
        viewModel.dismissBottomSheet.observeForever {}
        viewModel.onTimezoneSelected("Australia/Sydney")
        assertEquals(viewModel.dismissBottomSheet.value, null)
    }

    @Test
    fun testOnSearchCancelled() {
        viewModel.timezones.observeForever {}
        viewModel.onSearchCancelled()
        assertEquals(viewModel.timezones.value, listOf<TimezonesList>())
    }
}
