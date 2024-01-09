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
class AccountDataViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var accountModel: AccountModel
    private lateinit var accountDataViewModelSlice: AccountDataViewModelSlice
    private lateinit var isRefreshing: MutableList<Boolean>
    private lateinit var uiModel: MutableList<AccountData>

    @Before
    fun setUp() {
        accountDataViewModelSlice = AccountDataViewModelSlice(accountStore)
        isRefreshing = mutableListOf()
        uiModel = mutableListOf()
        accountDataViewModelSlice.initialize(testScope())
    }

    @Test
    fun `current avatar is empty on refresh`() = test {
        var result: AccountData? = null
        accountDataViewModelSlice.uiModel.observeForever {
            it?.let { result = it }
        }
        accountDataViewModelSlice.refresh()

        assertThat(result?.url).isEmpty()
    }

    @Test
    fun `uimodel is null when accessed before refresh request`() = test {
        var result: AccountData? = null

        accountDataViewModelSlice.uiModel.observeForever {
            it?.let { result = it }
        }

        assertThat(result).isNull()
    }

    @Test
    fun `when refresh is invoked, then isRefreshing is true`() = test {
        accountDataViewModelSlice.isRefreshing.observeForever {
            it?.let { isRefreshing.add(it) }
        }

        accountDataViewModelSlice.refresh()


        assertThat(isRefreshing.first()).isTrue
    }

    @Test
    fun `when data has been refreshed, then refresh is set to false`() = test {
        val avatarUrl = "avatar.jpg"
        val displayName = "Display Name"
        whenever(accountStore.account).thenReturn(accountModel)
        whenever(accountModel.avatarUrl).thenReturn(avatarUrl)
        whenever(accountModel.displayName).thenReturn(displayName)
        accountDataViewModelSlice.isRefreshing.observeForever { isRefreshing.add(it) }

        accountDataViewModelSlice.refresh()

        assertThat(isRefreshing.last()).isFalse
    }

    @Test
    fun `when data has been refreshed, then uiModel contains data from the account store`() = test {
        val avatarUrl = "avatar.jpg"
        val displayName = "Display Name"
        whenever(accountStore.account).thenReturn(accountModel)
        whenever(accountModel.avatarUrl).thenReturn(avatarUrl)
        whenever(accountModel.displayName).thenReturn(displayName)


        accountDataViewModelSlice.uiModel.observeForever {
            it?.let { uiModel.add(it) }
        }

        accountDataViewModelSlice.refresh()

        assertThat(uiModel.last()).isNotNull
        assertThat(uiModel.last().url).isEqualTo(avatarUrl)
        assertThat(uiModel.last().name).isEqualTo(displayName)
    }

    @Test
    fun `when display name is empty, then user name is used`() = test {
        val avatarUrl = "avatar.jpg"
        val displayName = ""
        val userName = "User Name"
        whenever(accountStore.account).thenReturn(accountModel)
        whenever(accountModel.avatarUrl).thenReturn(avatarUrl)
        whenever(accountModel.displayName).thenReturn(displayName)
        whenever(accountModel.userName).thenReturn(userName)


        accountDataViewModelSlice.uiModel.observeForever {
            it?.let { uiModel.add(it) }
        }

        accountDataViewModelSlice.refresh()

        assertThat(uiModel.last().name).isEqualTo(userName)
    }

    @Test
    fun `when display and user name are empty, then name is empty`() = test {
        val avatarUrl = "avatar.jpg"
        val displayName = ""
        val userName = ""
        whenever(accountStore.account).thenReturn(accountModel)
        whenever(accountModel.avatarUrl).thenReturn(avatarUrl)
        whenever(accountModel.displayName).thenReturn(displayName)
        whenever(accountModel.userName).thenReturn(userName)


        accountDataViewModelSlice.uiModel.observeForever {
            it?.let { uiModel.add(it) }
        }

        accountDataViewModelSlice.refresh()

        assertThat(uiModel.last().name).isEmpty()
    }
}
