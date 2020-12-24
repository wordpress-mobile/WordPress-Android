package org.wordpress.android.ui.jetpack.scan.details

import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest

@InternalCoroutinesApi
class ThreatDetailsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ThreatDetailsViewModel

    @Before
    fun setUp() {
        viewModel = ThreatDetailsViewModel()
    }

    @Test
    fun dummyTest() { // TODO: ashiagr added for CI to run fine, to be removed after first test is added
    }
}
