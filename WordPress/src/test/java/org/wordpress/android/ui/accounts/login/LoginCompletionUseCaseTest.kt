package org.wordpress.android.ui.accounts.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.login.LoginMode
import org.wordpress.android.ui.accounts.login.LoginCompletionUseCase.LoginCompletionAction
import org.wordpress.android.ui.accounts.login.LoginCompletionUseCase.MainNavigationDestination

@ExperimentalCoroutinesApi
class LoginCompletionUseCaseTest : BaseUnitTest() {
    private lateinit var useCase: LoginCompletionUseCase

    @Before
    fun setUp() {
        useCase = LoginCompletionUseCase()
    }

    // region shouldWaitForSitesToLoad tests

    @Test
    fun `given doLoginUpdate true and no sites, then should wait for sites`() {
        val result = useCase.shouldWaitForSitesToLoad(doLoginUpdate = true, hasSites = false)

        assertThat(result).isTrue()
    }

    @Test
    fun `given doLoginUpdate true and has sites, then should not wait`() {
        val result = useCase.shouldWaitForSitesToLoad(doLoginUpdate = true, hasSites = true)

        assertThat(result).isFalse()
    }

    @Test
    fun `given doLoginUpdate false and no sites, then should not wait`() {
        val result = useCase.shouldWaitForSitesToLoad(doLoginUpdate = false, hasSites = false)

        assertThat(result).isFalse()
    }

    @Test
    fun `given doLoginUpdate false and has sites, then should not wait`() {
        val result = useCase.shouldWaitForSitesToLoad(doLoginUpdate = false, hasSites = true)

        assertThat(result).isFalse()
    }

    // endregion

    // region getLoginCompletionAction tests

    @Test
    fun `given FULL login mode, then action is NAVIGATE_TO_MAIN`() {
        val result = useCase.getLoginCompletionAction(LoginMode.FULL)

        assertThat(result).isEqualTo(LoginCompletionAction.NAVIGATE_TO_MAIN)
    }

    @Test
    fun `given JETPACK_LOGIN_ONLY mode, then action is NAVIGATE_TO_MAIN`() {
        val result = useCase.getLoginCompletionAction(LoginMode.JETPACK_LOGIN_ONLY)

        assertThat(result).isEqualTo(LoginCompletionAction.NAVIGATE_TO_MAIN)
    }

    @Test
    fun `given WPCOM_LOGIN_ONLY mode, then action is NAVIGATE_TO_MAIN`() {
        val result = useCase.getLoginCompletionAction(LoginMode.WPCOM_LOGIN_ONLY)

        assertThat(result).isEqualTo(LoginCompletionAction.NAVIGATE_TO_MAIN)
    }

    @Test
    fun `given SELFHOSTED_ONLY mode, then action is FINISH_WITH_NEW_SITE`() {
        val result = useCase.getLoginCompletionAction(LoginMode.SELFHOSTED_ONLY)

        assertThat(result).isEqualTo(LoginCompletionAction.FINISH_WITH_NEW_SITE)
    }

    @Test
    fun `given JETPACK_SELFHOSTED mode, then action is FINISH_WITH_NEW_SITE`() {
        val result = useCase.getLoginCompletionAction(LoginMode.JETPACK_SELFHOSTED)

        assertThat(result).isEqualTo(LoginCompletionAction.FINISH_WITH_NEW_SITE)
    }

    @Test
    fun `given SHARE_INTENT mode, then action is FINISH_WITH_NEW_SITE`() {
        val result = useCase.getLoginCompletionAction(LoginMode.SHARE_INTENT)

        assertThat(result).isEqualTo(LoginCompletionAction.FINISH_WITH_NEW_SITE)
    }

    @Test
    fun `given JETPACK_STATS mode, then action is NAVIGATE_TO_MAIN`() {
        val result = useCase.getLoginCompletionAction(LoginMode.JETPACK_STATS)

        assertThat(result).isEqualTo(LoginCompletionAction.NAVIGATE_TO_MAIN)
    }

    @Test
    fun `given JETPACK_REST_CONNECT mode, then action is NAVIGATE_TO_MAIN`() {
        val result = useCase.getLoginCompletionAction(LoginMode.JETPACK_REST_CONNECT)

        assertThat(result).isEqualTo(LoginCompletionAction.NAVIGATE_TO_MAIN)
    }

    @Test
    fun `given WPCOM_LOGIN_DEEPLINK mode, then action is NAVIGATE_TO_MAIN`() {
        val result = useCase.getLoginCompletionAction(LoginMode.WPCOM_LOGIN_DEEPLINK)

        assertThat(result).isEqualTo(LoginCompletionAction.NAVIGATE_TO_MAIN)
    }

    @Test
    fun `given WPCOM_REAUTHENTICATE mode, then action is NAVIGATE_TO_MAIN`() {
        val result = useCase.getLoginCompletionAction(LoginMode.WPCOM_REAUTHENTICATE)

        assertThat(result).isEqualTo(LoginCompletionAction.NAVIGATE_TO_MAIN)
    }

    // endregion

    // region getMainNavigationDestination tests

    @Test
    fun `given FULL mode, then destination is MAIN_ACTIVITY`() {
        val result = useCase.getMainNavigationDestination(LoginMode.FULL)

        assertThat(result).isEqualTo(MainNavigationDestination.MAIN_ACTIVITY)
    }

    @Test
    fun `given JETPACK_LOGIN_ONLY mode, then destination is MAIN_ACTIVITY`() {
        val result = useCase.getMainNavigationDestination(LoginMode.JETPACK_LOGIN_ONLY)

        assertThat(result).isEqualTo(MainNavigationDestination.MAIN_ACTIVITY)
    }

    @Test
    fun `given WPCOM_LOGIN_ONLY mode, then destination is MAIN_ACTIVITY`() {
        val result = useCase.getMainNavigationDestination(LoginMode.WPCOM_LOGIN_ONLY)

        assertThat(result).isEqualTo(MainNavigationDestination.MAIN_ACTIVITY)
    }

    @Test
    fun `given SELFHOSTED_ONLY mode, then destination is FINISH_ONLY`() {
        val result = useCase.getMainNavigationDestination(LoginMode.SELFHOSTED_ONLY)

        assertThat(result).isEqualTo(MainNavigationDestination.FINISH_ONLY)
    }

    @Test
    fun `given JETPACK_SELFHOSTED mode, then destination is FINISH_ONLY`() {
        val result = useCase.getMainNavigationDestination(LoginMode.JETPACK_SELFHOSTED)

        assertThat(result).isEqualTo(MainNavigationDestination.FINISH_ONLY)
    }

    @Test
    fun `given SHARE_INTENT mode, then destination is FINISH_ONLY`() {
        val result = useCase.getMainNavigationDestination(LoginMode.SHARE_INTENT)

        assertThat(result).isEqualTo(MainNavigationDestination.FINISH_ONLY)
    }

    @Test
    fun `given JETPACK_STATS mode, then destination is FINISH_ONLY`() {
        val result = useCase.getMainNavigationDestination(LoginMode.JETPACK_STATS)

        assertThat(result).isEqualTo(MainNavigationDestination.FINISH_ONLY)
    }

    @Test
    fun `given JETPACK_REST_CONNECT mode, then destination is FINISH_ONLY`() {
        val result = useCase.getMainNavigationDestination(LoginMode.JETPACK_REST_CONNECT)

        assertThat(result).isEqualTo(MainNavigationDestination.FINISH_ONLY)
    }

    @Test
    fun `given WPCOM_LOGIN_DEEPLINK mode, then destination is FINISH_ONLY`() {
        val result = useCase.getMainNavigationDestination(LoginMode.WPCOM_LOGIN_DEEPLINK)

        assertThat(result).isEqualTo(MainNavigationDestination.FINISH_ONLY)
    }

    @Test
    fun `given WPCOM_REAUTHENTICATE mode, then destination is FINISH_ONLY`() {
        val result = useCase.getMainNavigationDestination(LoginMode.WPCOM_REAUTHENTICATE)

        assertThat(result).isEqualTo(MainNavigationDestination.FINISH_ONLY)
    }

    // endregion
}
