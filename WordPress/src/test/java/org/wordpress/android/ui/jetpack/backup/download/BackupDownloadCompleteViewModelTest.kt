package org.wordpress.android.ui.jetpack.backup.download

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.jetpack.backup.download.complete.BackupDownloadCompleteViewModel

@RunWith(MockitoJUnitRunner::class)
class BackupDownloadCompleteViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: BackupDownloadCompleteViewModel

    @Before
    fun setUp() {
        viewModel = BackupDownloadCompleteViewModel()
    }

    @Test
    fun `sample test`() {
    } // TODO:
}
