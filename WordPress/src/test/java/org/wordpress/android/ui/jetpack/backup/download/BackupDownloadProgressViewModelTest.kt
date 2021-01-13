package org.wordpress.android.ui.jetpack.backup.download

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadListItemState.ProgressState
import org.wordpress.android.ui.jetpack.backup.download.progress.BackupDownloadProgressStateListItemBuilder
import org.wordpress.android.ui.jetpack.backup.download.progress.BackupDownloadProgressViewModel
import org.wordpress.android.ui.jetpack.backup.download.progress.BackupDownloadProgressViewModel.UiState
import org.wordpress.android.ui.jetpack.backup.download.usecases.GetBackupDownloadStatusUseCase
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

        assertThat(uiStates[0]).isInstanceOf(UiState::class.java)
    }

    @Test
    fun `when started, the progress is set to zero `() = test {
        val uiStates = initObservers().uiStates

        viewModel.start(site, backupDownloadState, parentViewModel)

        assertThat(((uiStates[0].items).first { it is ProgressState } as ProgressState).progress)
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

    private val getStatusProgress = BackupDownloadRequestState.Progress(rewindId = "rewindId", progress = 0)
}
