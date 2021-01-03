package org.wordpress.android.ui.jetpack.backup.download

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadStatusHandler
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadStatusHandler.BackupDownloadStatusHandlerState
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadStatusHandler.BackupDownloadStatusHandlerState.Progress
import org.wordpress.android.ui.jetpack.backup.download.usecases.BackupDownloadStatusUseCase
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringText

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BackupDownloadStatusHandlerTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var backupDownloadStatusUseCase: BackupDownloadStatusUseCase
    @Mock private lateinit var site: SiteModel
    private lateinit var backupDownloadStatusHandler: BackupDownloadStatusHandler
    private var handlerState: BackupDownloadStatusHandlerState? = null
    private var snackbarHolder: SnackbarMessageHolder? = null
    private val rewindId = "rewindId"
    private val downloadId = 100L
    private val progress = 25
    private val url = "https://www.google.com"
    private val message = UiStringText("error message")

    @Before
    fun setup() {
        backupDownloadStatusHandler = BackupDownloadStatusHandler(backupDownloadStatusUseCase, TEST_DISPATCHER)
    }

    @Test
    fun `getBackupDownloadStatus Progress collects expected status`() = test {
        val state = Progress(
                rewindId = rewindId,
                progress = progress
        )
        whenever(backupDownloadStatusUseCase.getBackupDownloadStatus(site, downloadId)).thenReturn(
                flow { emit(state) }
        )

        setupObservers()

        backupDownloadStatusHandler.handleBackupDownloadStatus(site, downloadId)

        requireNotNull(handlerState).let {
            Assertions.assertThat(it).isEqualTo(state)
        }
    }

    @Test
    fun `getBackupDownloadStatus error collects expected status`() = test {
        val state = BackupDownloadStatusHandlerState.Error(
                message = message
        )

        whenever(backupDownloadStatusUseCase.getBackupDownloadStatus(site, downloadId)).thenReturn(
                flow { emit(state) }
        )

        setupObservers()

        backupDownloadStatusHandler.handleBackupDownloadStatus(site, downloadId)

        requireNotNull(snackbarHolder).let {
            Assertions.assertThat(it.message).isEqualTo(message)
        }
    }

    @Test
    fun `getBackupDownloadStatus complete collects expected status`() = test {
        val state = BackupDownloadStatusHandlerState.Complete(
                rewindId = rewindId,
                downloadId = downloadId,
                url = url
        )
        whenever(backupDownloadStatusUseCase.getBackupDownloadStatus(site, downloadId)).thenReturn(
                flow { emit(state) }
        )

        setupObservers()

        backupDownloadStatusHandler.handleBackupDownloadStatus(site, downloadId)

        requireNotNull(handlerState).let {
            Assertions.assertThat(it).isEqualTo(state)
        }
    }

    private fun setupObservers() {
        handlerState = null
        backupDownloadStatusHandler.statusUpdate.observeForever {
            handlerState = it
        }

        snackbarHolder = null
        backupDownloadStatusHandler.snackbarEvents.observeForever { event ->
            event.applyIfNotHandled {
                snackbarHolder = this
            }
        }
    }
}
