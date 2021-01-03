package org.wordpress.android.ui.jetpack.backup.download

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadRequestTypes
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadHandler
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadHandler.BackupDownloadHandlerStatus
import org.wordpress.android.ui.jetpack.backup.download.usecases.PostBackupDownloadUseCase
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringText

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BackupDownloadHandlerTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var postBackupDownloadUseCase: PostBackupDownloadUseCase
    @Mock private lateinit var site: SiteModel
    private lateinit var backupDownloadHandler: BackupDownloadHandler
    private var handlerStatus: BackupDownloadHandlerStatus? = null
    private var snackbarHolder: SnackbarMessageHolder? = null

    private val message = UiStringText("error message")
    private val rewindId = "rewindId"
    private val downloadId = 100L
    private val types = BackupDownloadRequestTypes(themes = false,
            contents = true,
            sqls = true,
            uploads = true,
            plugins = true,
            roots = true)

    @Before
    fun setup() {
        backupDownloadHandler = BackupDownloadHandler(postBackupDownloadUseCase, TEST_DISPATCHER)
    }

    @Test
    fun `handleBackupDownloadRequest success collects expected status`() = test {
        val status = BackupDownloadHandlerStatus.Success(
                rewindId = rewindId,
                downloadId = downloadId
        )

        whenever(postBackupDownloadUseCase.postBackupDownloadRequest(rewindId, site, types)).thenReturn(
                flow { emit(status) }
        )

        setupObservers()

        backupDownloadHandler.handleBackupDownloadRequest(rewindId, site, types)

        requireNotNull(handlerStatus).let {
            assertThat(it).isEqualTo(status)
        }
    }

    @Test
    fun `handleBackupDownloadRequest failure collects expected status`() = test {
        val status = BackupDownloadHandlerStatus.Failure(
                rewindId = rewindId,
                message = message
        )
        whenever(postBackupDownloadUseCase.postBackupDownloadRequest(rewindId, site, types)).thenReturn(
                flow { emit(status) }
        )

        setupObservers()

        backupDownloadHandler.handleBackupDownloadRequest(rewindId, site, types)

        requireNotNull(snackbarHolder).let {
            assertThat(it.message).isEqualTo(message)
        }
    }

    private fun setupObservers() {
        handlerStatus = null

        backupDownloadHandler.statusUpdate.observeForever {
            handlerStatus = it
        }

        snackbarHolder = null
        backupDownloadHandler.snackbarEvents.observeForever { event ->
            event.applyIfNotHandled {
                snackbarHolder = this
            }
        }
    }
}
