package org.wordpress.android.ui.accounts.login

import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER

@InternalCoroutinesApi
class JetpackLoginPrologueViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: JetpackLoginPrologueViewModel

    @Before
    fun setUp() {
        viewModel = JetpackLoginPrologueViewModel(TEST_DISPATCHER)
    }
}
