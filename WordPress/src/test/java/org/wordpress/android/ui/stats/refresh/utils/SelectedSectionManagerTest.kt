package org.wordpress.android.ui.stats.refresh.utils

import android.content.SharedPreferences
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.MONTHS

class SelectedSectionManagerTest : BaseUnitTest() {
    @Mock lateinit var sharedPreferences: SharedPreferences
    @Mock lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var selectedSectionManager: SelectedSectionManager
    @Before
    fun setUp() {
        selectedSectionManager = SelectedSectionManager(sharedPreferences)
        whenever(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
    }

    @Test
    fun `inserts tab selection into shared prefs`() {
        whenever(sharedPreferencesEditor.putString(any(), any())).thenReturn(sharedPreferencesEditor)

        var selectedSection: StatsSection? = null
        selectedSectionManager.liveSelectedSection.observeForever { selectedSection = it }

        selectedSectionManager.setSelectedSection(MONTHS)

        val inOrder = inOrder(sharedPreferencesEditor)
        inOrder.verify(sharedPreferencesEditor).putString(SELECTED_SECTION_KEY, "MONTHS")
        inOrder.verify(sharedPreferencesEditor).apply()

        assertThat(selectedSection).isEqualTo(MONTHS)
    }

    @Test
    fun `selects tab selection from shared prefs`() {
        whenever(sharedPreferences.getString(eq(SELECTED_SECTION_KEY), any())).thenReturn("MONTHS")

        val selectedSection = selectedSectionManager.getSelectedSection()

        assertThat(selectedSection).isEqualTo(MONTHS)
    }
}
