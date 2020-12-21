package org.wordpress.android.ui.jetpack.backup.download

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.jetpack.backup.download.progress.BackupDownloadProgressViewModel

@RunWith(MockitoJUnitRunner::class)
class BackupDownloadProgressViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: BackupDownloadProgressViewModel

    @Before
    fun setUp() {
        viewModel = BackupDownloadProgressViewModel()
    }

    @Test
    fun `sample test`() {
    } // TODO:
}
