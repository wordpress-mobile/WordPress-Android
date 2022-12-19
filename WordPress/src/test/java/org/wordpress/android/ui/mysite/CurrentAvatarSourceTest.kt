package org.wordpress.android.ui.mysite

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
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
    private lateinit var isRefreshing: MutableList<Boolean>

    @Before
    fun setUp() {
        currentAvatarSource = CurrentAvatarSource(accountStore)
        isRefreshing = mutableListOf()
    }

    @Test
    fun `current avatar is empty on start`() = test {
        var result: CurrentAvatarUrl? = null
        currentAvatarSource.build(testScope()).observeForever { it?.let { result = it } }

        assertThat(result!!.url).isEqualTo("")
    }

    @Test
    fun `current avatar is loaded on refresh from account store`() = test {
        whenever(accountStore.account).thenReturn(accountModel)
        val avatarUrl = "avatar.jpg"
        whenever(accountModel.avatarUrl).thenReturn(avatarUrl)

        var result: CurrentAvatarUrl? = null
        currentAvatarSource.build(testScope()).observeForever { it?.let { result = it } }

        currentAvatarSource.refresh()

        assertThat(result!!.url).isEqualTo(avatarUrl)
    }

    @Test
    fun `when buildSource is invoked, then refresh is true`() = test {
        currentAvatarSource.refresh.observeForever { isRefreshing.add(it) }

        currentAvatarSource.build(testScope())

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `when refresh is invoked, then refresh is true`() = test {
        currentAvatarSource.refresh.observeForever { isRefreshing.add(it) }

        currentAvatarSource.refresh()

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `when data has been refreshed, then refresh is set to false`() = test {
        whenever(accountStore.account).thenReturn(accountModel)
        val avatarUrl = "avatar.jpg"
        whenever(accountModel.avatarUrl).thenReturn(avatarUrl)
        currentAvatarSource.refresh.observeForever { isRefreshing.add(it) }

        currentAvatarSource.build(testScope()).observeForever { }
        currentAvatarSource.refresh()

        assertThat(isRefreshing.last()).isFalse
    }
}
