package org.wordpress.android.ui.jetpack.restore

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.CONTENTS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.MEDIA_UPLOADS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.PLUGINS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.ROOTS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.SQLS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.THEMES
import org.wordpress.android.ui.jetpack.restore.usecases.PostRestoreUseCase
import org.wordpress.android.ui.jetpack.restore.warning.RestoreWarningStateListItemBuilder
import org.wordpress.android.ui.jetpack.restore.warning.RestoreWarningViewModel
import org.wordpress.android.ui.jetpack.restore.warning.RestoreWarningViewModel.UiState
import java.util.Date

@InternalCoroutinesApi
class RestoreWarningViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: RestoreWarningViewModel
    private lateinit var stateListItemBuilder: RestoreWarningStateListItemBuilder
    @Mock private lateinit var postRestoreUseCase: PostRestoreUseCase
    @Mock private lateinit var parentViewModel: RestoreViewModel
    @Mock private lateinit var site: SiteModel

    private val restoreState = RestoreState(
            activityId = "activityId",
            rewindId = "rewindId",
            restoreId = 100L,
            siteId = 200L,
            optionsSelected = listOf(
                    Pair(THEMES.id, true),
                    Pair(PLUGINS.id, true),
                    Pair(MEDIA_UPLOADS.id, true),
                    Pair(SQLS.id, true),
                    Pair(ROOTS.id, true),
                    Pair(CONTENTS.id, true)),
            published = Date(1609690147756)
    )

    @Before
    fun setUp() = test {
        stateListItemBuilder = RestoreWarningStateListItemBuilder()
        viewModel = RestoreWarningViewModel(
                postRestoreUseCase,
                stateListItemBuilder,
                TEST_DISPATCHER
        )
    }

    @Test
    fun `given a network connection issue, then an error event is posted`() = test {
        whenever(postRestoreUseCase.postRestoreRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(postRestoreNetworkError)

        val uiStates = initObservers().uiStates
        val errorEvents = initObservers().errorEvents

        viewModel.start(site, restoreState, parentViewModel)

        ((uiStates.last().items).first { it is ActionButtonState } as ActionButtonState).onClick.invoke()

        Assertions.assertThat(errorEvents.last()).isEqualTo(RestoreErrorTypes.NetworkUnavailable)
    }

    @Test
    fun `given a remote request issue, then an error event is posted`() = test {
        whenever(postRestoreUseCase.postRestoreRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(postRestoreRemoteRequestError)

        val uiStates = initObservers().uiStates
        val errorEvents = initObservers().errorEvents

        viewModel.start(site, restoreState, parentViewModel)

        ((uiStates.last().items).first { it is ActionButtonState } as ActionButtonState).onClick.invoke()

        Assertions.assertThat(errorEvents.last()).isEqualTo(RestoreErrorTypes.RemoteRequestFailure)
    }

    @Test
    fun `given another restore is running, then an error event is posted`() = test {
        whenever(postRestoreUseCase.postRestoreRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(postRestoreRunningError)

        val uiStates = initObservers().uiStates
        val errorEvents = initObservers().errorEvents

        viewModel.start(site, restoreState, parentViewModel)

        ((uiStates.last().items).first { it is ActionButtonState } as ActionButtonState).onClick.invoke()

        Assertions.assertThat(errorEvents.last()).isEqualTo(RestoreErrorTypes.OtherRequestRunning)
    }

    private fun initObservers(): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }

        val errorEvents = mutableListOf<RestoreErrorTypes>()
        viewModel.errorEvents.observeForever { errorEvents.add(it.peekContent()) }

        return Observers(uiStates, errorEvents)
    }

    private data class Observers(
        val uiStates: List<UiState>,
        val errorEvents: List<RestoreErrorTypes>
    )

    private val postRestoreNetworkError = RestoreRequestState.Failure.NetworkUnavailable
    private val postRestoreRemoteRequestError = RestoreRequestState.Failure.RemoteRequestFailure
    private val postRestoreRunningError = RestoreRequestState.Failure.OtherRequestRunning
}
