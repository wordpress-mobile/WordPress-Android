package org.wordpress.android.ui.jetpack.restore

import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.util.wizard.WizardManager

@InternalCoroutinesApi
class RestoreViewModelTest : BaseUnitTest() {
    @Mock lateinit var wizardManager: WizardManager<RestoreStep>

    private lateinit var viewModel: RestoreViewModel

    @Before
    fun setUp() {
        viewModel = RestoreViewModel(wizardManager)
    }

    @Test
    fun `sample test`() {
    } // TODO:
}
