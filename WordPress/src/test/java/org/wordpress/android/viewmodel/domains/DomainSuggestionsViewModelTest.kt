package org.wordpress.android.viewmodel.domains

import android.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.helpers.Debouncer

@RunWith(MockitoJUnitRunner::class)
class DomainSuggestionsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var debouncer: Debouncer

    private lateinit var site: SiteModel
    private lateinit var viewModel: DomainSuggestionsViewModel

    @Before
    fun setUp() {
        site = SiteModel().also { it.name = "Test Site" }
        viewModel = DomainSuggestionsViewModel(dispatcher, debouncer)
        viewModel.start(site)
    }

    @Test
    fun isIntroVisibleIsInitializedToTrue() {
        assertNotNull(viewModel.isIntroVisible.value)
        viewModel.isIntroVisible.value?.let { isIntroVisible ->
            assert(isIntroVisible)
        }
    }

    @Test
    fun isIntroVisibleIsSetToFalseWhenSearchQueryIsFirstSetToNonBlankValue() {
        viewModel.updateSearchQuery("Hello World")

        assertNotNull(viewModel.isIntroVisible.value)
        viewModel.isIntroVisible.value?.let { isIntroVisible ->
            assertFalse(isIntroVisible)
        }
    }

    @Test
    fun isIntroVisibleNeverGetsSetBackToTrue() {
        viewModel.updateSearchQuery("Hello World")
        viewModel.updateSearchQuery("")

        assertNotNull(viewModel.isIntroVisible.value)
        viewModel.isIntroVisible.value?.let { isIntroVisible ->
            assertFalse(isIntroVisible)
        }
    }
}
