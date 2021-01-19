package org.wordpress.android.ui.jetpack.backup.download

import org.wordpress.android.viewmodel.Event
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.backup.download.builders.BackupDownloadStateListItemBuilder
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider
import org.wordpress.android.ui.jetpack.backup.download.details.BackupDownloadDetailsViewModel
import org.wordpress.android.ui.jetpack.backup.download.details.BackupDownloadDetailsViewModel.UiState
import org.wordpress.android.ui.jetpack.backup.download.usecases.PostBackupDownloadUseCase
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.CheckboxState
import org.wordpress.android.ui.jetpack.usecases.GetActivityLogItemUseCase
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import java.util.Date

@InternalCoroutinesApi
class BackupDownloadDetailsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: BackupDownloadDetailsViewModel
    private lateinit var availableItemsProvider: JetpackAvailableItemsProvider
    @Mock private lateinit var getActivityLogItemUseCase: GetActivityLogItemUseCase
    private lateinit var stateListItemBuilder: BackupDownloadStateListItemBuilder
    @Mock private lateinit var parentViewModel: BackupDownloadViewModel
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var postBackupDownloadUseCase: PostBackupDownloadUseCase

    private val activityId = "1"

    @Before
    fun setUp() = test {
        availableItemsProvider = JetpackAvailableItemsProvider()
        stateListItemBuilder = BackupDownloadStateListItemBuilder()
        viewModel = BackupDownloadDetailsViewModel(
                availableItemsProvider,
                getActivityLogItemUseCase,
                stateListItemBuilder,
                postBackupDownloadUseCase,
                TEST_DISPATCHER
        )
        whenever(getActivityLogItemUseCase.get(anyOrNull())).thenReturn(fakeActivityLogModel)
        whenever(postBackupDownloadUseCase.postBackupDownloadRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(postBackupDownloadSuccess)
    }

    @Test
    fun `when available items are fetched, the content view is shown`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start(site, activityId, parentViewModel)

        assertThat(uiStates[0]).isInstanceOf(UiState::class.java)
    }

    @Test
    fun `given item is checked, when item is clicked, then item gets unchecked`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start(site, activityId, parentViewModel)

        ((uiStates.last().items).first { it is CheckboxState } as CheckboxState).onClick.invoke()

        assertThat((((uiStates.last()).items)
            .first { it is CheckboxState } as CheckboxState).checked).isFalse
    }

    @Test
    fun `given item is unchecked, when item is clicked, then item gets checked`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start(site, activityId, parentViewModel)

        ((uiStates.last().items).first { it is CheckboxState } as CheckboxState).onClick.invoke()
        ((uiStates.last().items).first { it is CheckboxState } as CheckboxState).onClick.invoke()

        assertThat(((uiStates.last().items).first { it is CheckboxState } as CheckboxState).checked).isTrue
    }

    @Test
    fun `snackbar message is shown when request encounters a network connection issue`() = test {
        whenever(postBackupDownloadUseCase.postBackupDownloadRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(postBackupDownloadNetworkError)

        val uiStates = initObservers().uiStates
        val msgs = initObservers().snackbarMsgs

        viewModel.start(site, activityId, parentViewModel)

        ((uiStates.last().items).first { it is ActionButtonState } as ActionButtonState).onClick.invoke()

        assertThat(msgs[0].peekContent().message).isEqualTo(UiStringRes(R.string.error_network_connection))
    }

    @Test
    fun `snackbar message is shown when request encounters a request issue`() = test {
        whenever(postBackupDownloadUseCase.postBackupDownloadRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(postBackupDownloadRemoteRequestError)

        val uiStates = initObservers().uiStates
        val messages = initObservers().snackbarMsgs

        viewModel.start(site, activityId, parentViewModel)

        ((uiStates.last().items).first { it is ActionButtonState } as ActionButtonState).onClick.invoke()

        assertThat(messages[0].peekContent().message).isEqualTo(UiStringRes(R.string.backup_download_generic_failure))
    }

    @Test
    fun `snackbar message is shown when request another request is already running`() = test {
        whenever(postBackupDownloadUseCase.postBackupDownloadRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(otherRequestRunningError)

        val uiStates = initObservers().uiStates
        val messages = initObservers().snackbarMsgs

        viewModel.start(site, activityId, parentViewModel)

        ((uiStates.last().items)
                .first { it is ActionButtonState } as ActionButtonState).onClick.invoke()

        assertThat(messages[0].peekContent().message)
                .isEqualTo(UiStringRes(R.string.backup_download_another_download_running))
    }

    private fun initObservers(): Observers {
        val uiStates = mutableListOf<UiState>()
        val snackbarMsgs = mutableListOf<Event<SnackbarMessageHolder>>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        viewModel.snackbarEvents.observeForever {
            snackbarMsgs.add(it)
        }
        return Observers(uiStates, snackbarMsgs)
    }

    private data class Observers(
        val uiStates: List<UiState>,
        val snackbarMsgs: List<Event<SnackbarMessageHolder>>
    )

    private val fakeActivityLogModel: ActivityLogModel = ActivityLogModel(
            activityID = "1",
            summary = "summary",
            content = null,
            name = null,
            type = null,
            gridicon = null,
            status = null,
            rewindable = null,
            rewindID = "rewindId",
            published = Date()
    )

    private val postBackupDownloadSuccess = BackupDownloadRequestState.Success(
            requestRewindId = "rewindId",
            rewindId = "rewindId",
            downloadId = 100L
    )

    private val postBackupDownloadSuccessUnmatched = BackupDownloadRequestState.Success(
            requestRewindId = "requestRewindId",
            rewindId = "rewindId",
            downloadId = 100L
    )

    private val postBackupDownloadNetworkError = BackupDownloadRequestState.Failure.NetworkUnavailable
    private val postBackupDownloadRemoteRequestError = BackupDownloadRequestState.Failure.RemoteRequestFailure
    private val otherRequestRunningError = BackupDownloadRequestState.Failure.OtherRequestRunning
}
