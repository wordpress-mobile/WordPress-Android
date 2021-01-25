package org.wordpress.android.ui.jetpack.backup.download

import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.backup.download.complete.BackupDownloadCompleteStateListItemBuilder
import org.wordpress.android.ui.jetpack.backup.download.complete.BackupDownloadCompleteViewModel
import java.util.Date

@InternalCoroutinesApi
class BackupDownloadCompleteViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: BackupDownloadCompleteViewModel
    @Mock private lateinit var parentViewModel: BackupDownloadViewModel
    @Mock private lateinit var site: SiteModel
    private lateinit var stateListItemBuilder: BackupDownloadCompleteStateListItemBuilder

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
        stateListItemBuilder = BackupDownloadCompleteStateListItemBuilder()
        viewModel = BackupDownloadCompleteViewModel(
                stateListItemBuilder,
                TEST_DISPATCHER
        )
    }

    @Test
    fun `sample test`() {
    } // TODO:
}
