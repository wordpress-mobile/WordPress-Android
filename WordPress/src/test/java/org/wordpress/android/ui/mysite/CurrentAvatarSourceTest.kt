package org.wordpress.android.ui.mysite

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.test
import org.wordpress.android.testScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CurrentAvatarUrl

class CurrentAvatarSourceTest : BaseUnitTest() {
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var accountModel: AccountModel
    private lateinit var currentAvatarSource: CurrentAvatarSource

    @Before
    fun setUp() {
        currentAvatarSource = CurrentAvatarSource(accountStore)
    }

    @Test
    fun `current avatar is empty on start`() = test {
        var result: CurrentAvatarUrl? = null
        currentAvatarSource.buildSource(testScope()).observeForever { it?.let { result = it } }

        assertThat(result!!.url).isEqualTo("")
    }

    @Test
    fun `current avatar is loaded on refresh from account store`() = test {
        whenever(accountStore.account).thenReturn(accountModel)
        val avatarUrl = "avatar.jpg"
        whenever(accountModel.avatarUrl).thenReturn(avatarUrl)

        var result: CurrentAvatarUrl? = null
        currentAvatarSource.buildSource(testScope()).observeForever { it?.let { result = it } }

        currentAvatarSource.refresh()

        assertThat(result!!.url).isEqualTo(avatarUrl)
    }
}
