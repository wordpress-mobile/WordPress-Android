package org.wordpress.android.ui.prefs.timezone

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class SiteSettingsTimezoneViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var resourceProvider: ResourceProvider
    @Mock
    lateinit var context: Context

    private lateinit var viewModel: SiteSettingsTimezoneViewModel

    @Before
    fun setUp() {
        viewModel = SiteSettingsTimezoneViewModel(resourceProvider)
    }

    @Test
    fun testSearchTimezones() {
        var filteredList: List<TimezonesList>? = null
        viewModel.timezones.observeForever {
            filteredList = it
        }

        viewModel.searchTimezones("Sydney")

        Assertions.assertThat(filteredList).isNotNull
    }

    @Test
    fun testGetShowEmptyView() {
        var showEmptyView = false
        viewModel.showEmptyView.observeForever {
            showEmptyView = it
        }

        viewModel.filterTimezones("")

        assertEquals(showEmptyView, true)
    }

    @Test
    fun testGetSelectedTimezone() {
        var selectedTimezone: String? = null
        viewModel.selectedTimezone.observeForever {
            selectedTimezone = it
        }

        viewModel.onTimezoneSelected("Australia/Sydney")

        Assertions.assertThat(selectedTimezone).isNotNull
    }

    @Test
    fun testOnSearchCancelled() {
        var timezones: List<TimezonesList>? = null
        viewModel.timezones.observeForever {
            timezones = it
        }

        viewModel.onSearchCancelled()

        Assertions.assertThat(timezones).isNotNull
    }
}
