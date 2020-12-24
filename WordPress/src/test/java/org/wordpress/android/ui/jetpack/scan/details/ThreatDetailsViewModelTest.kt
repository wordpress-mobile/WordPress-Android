package org.wordpress.android.ui.jetpack.scan.details

import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.jetpack.usecases.GetThreatDetailsUseCase

@InternalCoroutinesApi
class ThreatDetailsViewModelTest : BaseUnitTest() {
    @Mock private lateinit var getThreatDetailsUseCase: GetThreatDetailsUseCase
    @Mock private lateinit var builder: ThreatDetailsListItemsBuilder
    private lateinit var viewModel: ThreatDetailsViewModel

    @Before
    fun setUp() {
        viewModel = ThreatDetailsViewModel(getThreatDetailsUseCase, builder)
    }

    @Test
    fun dummyTest() { // TODO: ashiagr added for CI to run fine, to be removed after first test is added
    }
}
