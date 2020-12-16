package org.wordpress.android.ui.jetpack.backup

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.util.wizard.WizardManager

@RunWith(MockitoJUnitRunner::class)
class BackupDownloadViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var wizardManager: WizardManager<BackupDownloadStep>

    private lateinit var viewModel: BackupDownloadViewModel

    @Before
    fun setUp() {
        viewModel = BackupDownloadViewModel(wizardManager)
    }

    @Test
    fun `sample test`() {
    } // TODO:
}
