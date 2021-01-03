package org.wordpress.android.ui.jetpack.backup.download

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.assertj.core.api.Assertions.assertThat
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.backup.download.progress.BackupDownloadProgressViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadStatusHandler
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadStatusHandler.BackupDownloadStatusHandlerState
import org.wordpress.android.ui.jetpack.backup.download.progress.BackupDownloadProgressViewModel
import org.wordpress.android.ui.jetpack.backup.download.progress.BackupDownloadProgressViewModel.UiState
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.viewmodel.Event
import java.util.Date

@InternalCoroutinesApi
class BackupDownloadProgressViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: BackupDownloadProgressViewModel
    @Mock private lateinit var parentViewModel: BackupDownloadViewModel
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var backupDownloadStatusHandler: BackupDownloadStatusHandler

    private var snackbarEvents = MutableLiveData<Event<SnackbarMessageHolder>>()
    private var handlerState = MutableLiveData<BackupDownloadStatusHandlerState>()
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
        viewModel = BackupDownloadProgressViewModel(
                backupDownloadStatusHandler,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )

        whenever(backupDownloadStatusHandler.snackbarEvents).thenReturn(snackbarEvents)
        whenever(backupDownloadStatusHandler.statusUpdate).thenReturn(handlerState)
    }

    @Test
    fun `when started, the content view is shown`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start(site, backupDownloadState, parentViewModel)

        assertThat(uiStates[0]).isInstanceOf(Content::class.java)
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
