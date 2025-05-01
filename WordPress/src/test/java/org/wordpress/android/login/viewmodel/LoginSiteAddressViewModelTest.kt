package org.wordpress.android.login.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.mockito.Mock
import rs.wordpress.api.kotlin.WpLoginClient

@ExperimentalCoroutinesApi
 class LoginSiteAddressViewModelTest {
    @Mock
    lateinit var wpLoginClient: WpLoginClient

    private lateinit var viewModel: LoginSiteAddressViewModel

    @Before
    fun setUp() {
        viewModel = LoginSiteAddressViewModel(wpLoginClient)
    }

    // NOTE: there's actually nothing to test yet, since the initial function is just logging the server call
 }
