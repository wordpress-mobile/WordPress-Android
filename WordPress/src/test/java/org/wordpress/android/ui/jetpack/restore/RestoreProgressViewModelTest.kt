package org.wordpress.android.ui.jetpack.restore

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.restore.RestoreListItemState.ProgressState
import org.wordpress.android.ui.jetpack.restore.progress.RestoreProgressStateListItemBuilder
import org.wordpress.android.ui.jetpack.restore.progress.RestoreProgressViewModel
import org.wordpress.android.ui.jetpack.restore.progress.RestoreProgressViewModel.UiState
import org.wordpress.android.ui.jetpack.restore.usecases.GetRestoreStatusUseCase
import java.util.Date

@InternalCoroutinesApi
class RestoreProgressViewModelTest: BaseUnitTest() {
    private lateinit var viewModel: RestoreProgressViewModel
    @Mock private lateinit var parentViewModel: RestoreViewModel
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var getStatusUseCase: GetRestoreStatusUseCase
    private lateinit var stateListItemBuilder: RestoreProgressStateListItemBuilder

    private val restoreState = RestoreState(
            activityId = "activityId",
            rewindId = "rewindId",
            restoreId = 100L,
            siteId = 200L,
            published = Date(1609690147756)
    )

    @Before
    fun setUp() = test {
        stateListItemBuilder = RestoreProgressStateListItemBuilder()
        viewModel = RestoreProgressViewModel(
                getStatusUseCase,
                stateListItemBuilder,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
        whenever(getStatusUseCase.getRestoreStatus(anyOrNull(), anyOrNull()))
                .thenReturn(flow { emit(getStatusProgress) })
    }

    @Test
    fun `when started, the content view is shown`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start(site, restoreState, parentViewModel)

        Assertions.assertThat(uiStates[0]).isInstanceOf(UiState::class.java)
    }

    @Test
    fun `when started, the progress is set to zero `() = test {
        val uiStates = initObservers().uiStates

        viewModel.start(site, restoreState, parentViewModel)

        Assertions.assertThat(((uiStates[0].items).first { it is ProgressState } as ProgressState).progress)
                .isEqualTo(0)
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

    private val getStatusProgress = RestoreRequestState.Progress(rewindId = "rewindId", progress = 0)
}
