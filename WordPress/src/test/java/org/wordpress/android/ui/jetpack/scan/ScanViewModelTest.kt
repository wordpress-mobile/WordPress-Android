package org.wordpress.android.ui.jetpack.scan

import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel

@InternalCoroutinesApi
class ScanViewModelTest : BaseUnitTest() {
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var scanStatusService: ScanStatusService

    private lateinit var viewModel: ScanViewModel

    @Before
    fun setUp() {
        viewModel = ScanViewModel(scanStatusService)
        viewModel.site = site
    }

    @Test
    fun `sample test`() {
    } // TODO:
}
