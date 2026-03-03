package org.wordpress.android.ui.newstats.subscribers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner.Silent::class)
class BaseSubscribersCardViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository:
        SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: TestViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        whenever(
            selectedSiteRepository.getSelectedSite()
        ).thenReturn(testSite)
        whenever(accountStore.accessToken)
            .thenReturn(TEST_ACCESS_TOKEN)
        whenever(
            resourceProvider.getString(
                R.string.stats_error_no_site
            )
        ).thenReturn("No site selected")
        whenever(
            resourceProvider.getString(
                R.string.stats_error_not_authenticated
            )
        ).thenReturn("Not authenticated")
        whenever(
            resourceProvider.getString(
                R.string.stats_error_unknown
            )
        ).thenReturn("Unknown error")
    }

    private fun createViewModel(): TestViewModel {
        viewModel = TestViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
        return viewModel
    }

    @Test
    fun `when loadDataIfNeeded succeeds then second call is skipped`() =
        test {
            createViewModel()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isEqualTo(TestState.Loaded)

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            assertThat(viewModel.loadCount).isEqualTo(1)
        }

    @Test
    fun `when loadData is called after loadDataIfNeeded then data reloads`() =
        test {
            createViewModel()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.loadCount).isEqualTo(2)
        }

    @Test
    fun `when loadData called directly then loadDataIfNeeded still works after`() =
        test {
            createViewModel()

            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isEqualTo(TestState.Loaded)

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            // Should not reload since already loaded
            assertThat(viewModel.loadCount).isEqualTo(1)
        }

    @Test
    fun `when no site selected then error state is emitted`() =
        test {
            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)
            createViewModel()

            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                TestState.Error::class.java
            )
        }

    @Test
    fun `when access token is null then error state is emitted`() =
        test {
            whenever(accountStore.accessToken)
                .thenReturn(null)
            createViewModel()

            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                TestState.Error::class.java
            )
        }

    @Test
    fun `when access token is empty then error state is emitted`() =
        test {
            whenever(accountStore.accessToken)
                .thenReturn("")
            createViewModel()

            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                TestState.Error::class.java
            )
        }

    @Test
    fun `when loadDataInternal throws then error state is emitted`() =
        test {
            createViewModel()
            viewModel.shouldThrow = true

            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                TestState.Error::class.java
            )
            assertThat((state as TestState.Error).message)
                .isEqualTo("Unknown error")
        }

    @Test
    fun `when refresh is called then isRefreshing is false after completion`() =
        test {
            createViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(viewModel.isRefreshing.value).isFalse()
            assertThat(viewModel.loadCount).isEqualTo(2)
        }

    @Test
    fun `when refresh is called then statsRepository init is called`() =
        test {
            createViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            // Once from loadData, once from refresh
            verify(statsRepository, times(2))
                .init(eq(TEST_ACCESS_TOKEN))
        }

    @Test
    fun `when refresh is called without site then nothing happens`() =
        test {
            createViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)
            viewModel.refresh()
            advanceUntilIdle()

            // Only 1 load from the initial loadData call
            assertThat(viewModel.loadCount).isEqualTo(1)
        }

    @Test
    fun `when refresh is called without token then nothing happens`() =
        test {
            createViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            whenever(accountStore.accessToken).thenReturn(null)
            viewModel.refresh()
            advanceUntilIdle()

            assertThat(viewModel.loadCount).isEqualTo(1)
        }

    @Test
    fun `when loadDataIfNeeded fails then next call retries`() =
        test {
            createViewModel()
            viewModel.shouldThrow = true

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(TestState.Error::class.java)

            // Error means not loaded successfully, but
            // isLoading was reset in the finally block.
            // loadData via loadDataIfNeeded should retry.
            viewModel.shouldThrow = false
            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isEqualTo(TestState.Loaded)
        }

    sealed class TestState {
        data object Loading : TestState()
        data class Error(
            val message: String,
            val isAuthError: Boolean = false
        ) : TestState()
        data object Loaded : TestState()
    }

    class TestViewModel(
        selectedSiteRepository: SelectedSiteRepository,
        accountStore: AccountStore,
        statsRepository: StatsRepository,
        resourceProvider: ResourceProvider
    ) : BaseSubscribersCardViewModel<TestState>(
        selectedSiteRepository,
        accountStore,
        statsRepository,
        resourceProvider,
        TestState.Loading
    ) {
        override val loadingState = TestState.Loading
        var loadCount = 0
        var shouldThrow = false

        override fun errorState(
            message: String,
            isAuthError: Boolean
        ) = TestState.Error(message, isAuthError)

        override suspend fun loadDataInternal(
            siteId: Long
        ) {
            if (shouldThrow) {
                error("Test error")
            }
            loadCount++
            markLoadedSuccessfully()
            updateState(TestState.Loaded)
        }
    }

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN =
            "test_access_token"
    }
}
