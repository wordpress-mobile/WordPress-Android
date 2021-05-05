package org.wordpress.android.ui.accounts.login

import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER

@InternalCoroutinesApi
class LoginSiteCheckErrorViewModelTest  : BaseUnitTest() {
    private lateinit var viewModel: LoginSiteCheckErrorViewModel

    @Before
    fun setUp() {
        viewModel = LoginSiteCheckErrorViewModel(TEST_DISPATCHER)
    }
}
