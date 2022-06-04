package org.wordpress.android.ui.qrcodeauth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.util.NetworkUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class QRCodeAuthViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var validator: QRCodeAuthValidator
    private val uiStateMapper = QRCodeAuthUiStateMapper()

    private lateinit var viewModel: QRCodeAuthViewModel

    @Before
    fun setUp() {
        viewModel = QRCodeAuthViewModel(uiStateMapper, networkUtilsWrapper, validator)
    }

    @Test
    fun `sample test`() {
    } // TODO:
}
