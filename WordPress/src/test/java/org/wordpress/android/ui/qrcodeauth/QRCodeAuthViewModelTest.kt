package org.wordpress.android.ui.qrcodeauth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class QRCodeAuthViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: QRCodeAuthViewModel

    @Before
    fun setUp() {
        viewModel = QRCodeAuthViewModel()
    }

    @Test
    fun `sample test`() {
    } // TODO:
}
