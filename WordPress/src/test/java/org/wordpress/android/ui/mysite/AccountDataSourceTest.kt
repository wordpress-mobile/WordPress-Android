package org.wordpress.android.ui.mysite

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.AccountData

@ExperimentalCoroutinesApi
class AccountDataSourceTest : BaseUnitTest() {
    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var accountModel: AccountModel
    private lateinit var accountDataSource: AccountDataSource
    private lateinit var isRefreshing: MutableList<Boolean>

    @Before
    fun setUp() {
        accountDataSource = AccountDataSource(accountStore)
        isRefreshing = mutableListOf()
    }

    @Test
    fun `current avatar is empty on start`() = test {
        var result: AccountData? = null
        accountDataSource.build(testScope()).observeForever {
            it?.let { result = it }
        }

        assertThat(result!!.url).isEqualTo("")
    }

    @Test
    fun `current avatar is loaded on refresh from account store`() = test {
        whenever(accountStore.account).thenReturn(accountModel)
        val avatarUrl = "avatar.jpg"
        whenever(accountModel.avatarUrl).thenReturn(avatarUrl)

        var result: AccountData? = null
        accountDataSource.build(testScope()).observeForever {
            it?.let { result = it }
        }

        accountDataSource.refresh()

        assertThat(result!!.url).isEqualTo(avatarUrl)
    }

    @Test
    fun `when buildSource is invoked, then refresh is true`() = test {
        accountDataSource.refresh.observeForever { isRefreshing.add(it) }

        accountDataSource.build(testScope())

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `when refresh is invoked, then refresh is true`() = test {
        accountDataSource.refresh.observeForever { isRefreshing.add(it) }

        accountDataSource.refresh()

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `when data has been refreshed, then refresh is set to false`() = test {
        whenever(accountStore.account).thenReturn(accountModel)
        val avatarUrl = "avatar.jpg"
        whenever(accountModel.avatarUrl).thenReturn(avatarUrl)
        accountDataSource.refresh.observeForever { isRefreshing.add(it) }

        accountDataSource.build(testScope()).observeForever { }
        accountDataSource.refresh()

        assertThat(isRefreshing.last()).isFalse
    }
}
