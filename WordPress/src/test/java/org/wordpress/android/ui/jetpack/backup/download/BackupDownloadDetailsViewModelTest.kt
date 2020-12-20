package org.wordpress.android.ui.jetpack.backup.download

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider
import org.wordpress.android.ui.jetpack.backup.download.details.BackupDownloadDetailsViewModel
import org.wordpress.android.ui.jetpack.backup.download.details.BackupDownloadDetailsViewModel.UiState
import org.wordpress.android.ui.jetpack.backup.download.details.BackupDownloadDetailsViewModel.UiState.Content

@InternalCoroutinesApi
class BackupDownloadDetailsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: BackupDownloadDetailsViewModel
    private lateinit var availableItemsProvider: JetpackAvailableItemsProvider
    @Mock private lateinit var parentViewModel: BackupDownloadViewModel
    @Mock private lateinit var site: SiteModel
    private val activityId = "1"

    @Before
    fun setUp() {
        availableItemsProvider = JetpackAvailableItemsProvider()
        viewModel = BackupDownloadDetailsViewModel(
                availableItemsProvider,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
    }

    @Test
    fun `when available items are fetched, the content view is shown`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start(site, activityId, parentViewModel)

        assertThat(uiStates[0]).isInstanceOf(Content::class.java)
    }

    @Test
    fun `given item is checked, when item is clicked, then item gets unchecked`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start(site, activityId, parentViewModel)

        ((uiStates.last() as Content).items[1]).onClick.invoke()

        assertThat(((uiStates.last() as Content).items[1]).checked).isFalse
    }

    @Test
    fun `given item is unchecked, when item is clicked, then item gets checked`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start(site, activityId, parentViewModel)

        ((uiStates.last() as Content).items[1]).onClick.invoke()
        ((uiStates.last() as Content).items[1]).onClick.invoke()

        assertThat(((uiStates.last() as Content).items[1]).checked).isTrue
    }

    private fun initObservers(): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        return Observers(uiStates)
    }

    private data class Observers(
        val uiStates: List<UiState>
    )
}
