package org.wordpress.android.ui.jetpack.backup

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.BackupAvailableItemsProvider
import org.wordpress.android.ui.jetpack.backup.details.BackupDownloadDetailsViewModel
import org.wordpress.android.ui.jetpack.backup.details.BackupDownloadDetailsViewModel.UiState
import org.wordpress.android.ui.jetpack.backup.details.BackupDownloadDetailsViewModel.UiState.Content

@InternalCoroutinesApi
class BackupDownloadDetailsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: BackupDownloadDetailsViewModel
    private lateinit var availableItemsProvider: BackupAvailableItemsProvider

    @Before
    fun setUp() {
        availableItemsProvider = BackupAvailableItemsProvider()
        viewModel = BackupDownloadDetailsViewModel(
                availableItemsProvider,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
    }

    @Test
    fun `when available items are fetched, the content view is shown`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start()

        assertThat(uiStates[0]).isInstanceOf(Content::class.java)
    }

    @Test
    fun `given item is checked, when item is clicked, then item gets unchecked`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start()

        ((uiStates.last() as Content).items[1]).onClick.invoke()

        assertThat(((uiStates.last() as Content).items[1]).checked).isFalse()
    }

    @Test
    fun `given item is unchecked, when item is clicked, then item gets checked`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start()

        ((uiStates.last() as Content).items[1]).onClick.invoke()
        ((uiStates.last() as Content).items[1]).onClick.invoke()

        assertThat(((uiStates.last() as Content).items[1]).checked).isTrue()
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
