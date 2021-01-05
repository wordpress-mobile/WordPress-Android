package org.wordpress.android.ui.jetpack.backup.download

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.assertj.core.api.Assertions.assertThat
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R.string
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.backup.download.progress.BackupDownloadProgressStateListItemBuilder
import org.wordpress.android.ui.jetpack.backup.download.progress.BackupDownloadProgressViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.backup.download.progress.BackupDownloadProgressViewModel
import org.wordpress.android.ui.jetpack.backup.download.progress.BackupDownloadProgressViewModel.UiState
import org.wordpress.android.ui.jetpack.backup.download.usecases.GetBackupDownloadStatusUseCase
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadListItemState.ProgressState
import java.util.Date

@InternalCoroutinesApi
class BackupDownloadProgressViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: BackupDownloadProgressViewModel
    @Mock private lateinit var parentViewModel: BackupDownloadViewModel
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var backupDownloadStatusUseCase: GetBackupDownloadStatusUseCase
    private lateinit var stateListItemBuilder: BackupDownloadProgressStateListItemBuilder

    private val backupDownloadState = BackupDownloadState(
            activityId = "activityId",
            rewindId = "rewindId",
            downloadId = 100L,
            siteId = 200L,
            url = null,
            published = Date(1609690147756)
    )

    @Before
    fun setUp() = test {
        stateListItemBuilder = BackupDownloadProgressStateListItemBuilder()
        viewModel = BackupDownloadProgressViewModel(
                backupDownloadStatusUseCase,
                stateListItemBuilder,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
        whenever(backupDownloadStatusUseCase.getBackupDownloadStatus(anyOrNull(), anyOrNull()))
                .thenReturn(flow { emit(getStatusProgress) })
    }

    @Test
    fun `when started, the content view is shown`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start(site, backupDownloadState, parentViewModel)

        assertThat(uiStates[0]).isInstanceOf(Content::class.java)
    }

    @Test
    fun `when started, the progress is set to zero `() = test {
        val uiStates = initObservers().uiStates
        whenever(backupDownloadStatusUseCase.getBackupDownloadStatus(anyOrNull(), anyOrNull()))
                .thenReturn(flow { emit(getStatusProgress) })

        viewModel.start(site, backupDownloadState, parentViewModel)

        assertThat((((uiStates[0] as Content).items).first { it is ProgressState } as ProgressState).progress)
                .isEqualTo(0)
    }

    @Test
    fun `snackbar message is shown when request encounters a network connection issue`() = test {
        whenever(backupDownloadStatusUseCase.getBackupDownloadStatus(anyOrNull(), anyOrNull()))
                .thenReturn(flow { emit(getStatusNetworkError) })

        val msgs = initObservers().snackbarMessages

        viewModel.start(site, backupDownloadState, parentViewModel)

        assertThat(msgs[0].peekContent().message).isEqualTo(UiStringRes(string.error_network_connection))
    }

    @Test
    fun `snackbar message is shown when request encounters a request issue`() = test {
        whenever(backupDownloadStatusUseCase.getBackupDownloadStatus(anyOrNull(), anyOrNull()))
                .thenReturn(flow { emit(getStatusRemoteRequestError) })

        val msgs = initObservers().snackbarMessages

        viewModel.start(site, backupDownloadState, parentViewModel)

        assertThat(msgs[0].peekContent().message).isEqualTo(UiStringRes(string.backup_download_generic_failure))
    }

    private fun initObservers(): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        val snackbarMessages = mutableListOf<Event<SnackbarMessageHolder>>()
        viewModel.snackbarEvents.observeForever {
            snackbarMessages.add(it)
        }
        return Observers(uiStates, snackbarMessages)
    }

    private data class Observers(
        val uiStates: List<UiState>,
        val snackbarMessages: List<Event<SnackbarMessageHolder>>
    )

    private val getStatusNetworkError = BackupDownloadRequestState.Failure.NetworkUnavailable
    private val getStatusRemoteRequestError = BackupDownloadRequestState.Failure.RemoteRequestFailure
    private val getStatusProgress = BackupDownloadRequestState.Progress(rewindId = "rewindId", progress = 0)
}
