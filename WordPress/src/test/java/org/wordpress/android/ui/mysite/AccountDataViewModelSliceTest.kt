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
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.AccountData
import org.wordpress.android.util.BuildConfigWrapper
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class AccountDataViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    private lateinit var viewModelSlice: AccountDataViewModelSlice

    private lateinit var isRefreshing: MutableList<Boolean>

    private lateinit var uiModels : MutableList<AccountData?>

    @Before
    fun setUp() {
        viewModelSlice = AccountDataViewModelSlice(
            accountStore,
            buildConfigWrapper,
            jetpackFeatureRemovalPhaseHelper
        )
        viewModelSlice.initialize(testScope())
        isRefreshing = mutableListOf()
        uiModels = mutableListOf()

        viewModelSlice.isRefreshing.observeForever { isRefreshing.add(it) }
        viewModelSlice.uiModel.observeForever { uiModels.add(it) }
    }


    @Test
    fun `given jp app, card is not built`() = test {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)

        viewModelSlice.onResume()

        assertThat(uiModels.last()).isNull()
    }

    @Test
    fun `given wp app, when in not correct phase, card is not built`() = test {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures()).thenReturn(false)

        viewModelSlice.onResume()

        assertThat(uiModels.last()).isNull()
    }

    @Test
    fun `given wp app, when in correct phase, card is built`() = test {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures()).thenReturn(true)
        val accountModel = getAccountData()
        whenever(accountStore.account).thenReturn(accountModel)

        viewModelSlice.onResume()

        assertEquals(true, isRefreshing.first())
        assertEquals(AccountData(accountModel.avatarUrl, accountModel.displayName), uiModels.last())
        assertEquals(false, isRefreshing.last())
    }

    @Test
    fun `uimodel is null when accessed before refresh request`() = test {
        var result: AccountData? = null

        viewModelSlice.uiModel.observeForever {
            it?.let { result = it }
        }

        assertThat(result).isNull()
    }

    @Test
    fun `when refresh is invoked, then isRefreshing is true`() = test {
        viewModelSlice.onRefresh()


        assertThat(isRefreshing.first()).isTrue
    }

    @Test
    fun `when data has been refreshed, then refresh is set to false`() = test {
        val accountModel = getAccountData()
        whenever(accountStore.account).thenReturn(accountModel)

        viewModelSlice.onRefresh()

        assertThat(isRefreshing.last()).isFalse
    }

    @Test
    fun `when data has been refreshed, then uiModel contains data from the account store`() = test {
        val accountModel = getAccountData()
        whenever(accountStore.account).thenReturn(accountModel)

        viewModelSlice.onRefresh()

        assertThat(uiModels.last()).isNotNull
        assertThat(uiModels.last()?.url).isEqualTo(accountModel.avatarUrl)
        assertThat(uiModels.last()?.name).isEqualTo(accountModel.displayName)
    }

    @Test
    fun `when display name is empty, then user name is used`() = test {
        val accountModel = getAccountData()
        val userName1 = "User Name"
        accountModel.apply {
            displayName = ""
            userName = userName1
        }
        whenever(accountStore.account).thenReturn(accountModel)

        viewModelSlice.onRefresh()

        assertThat(uiModels.last()?.name).isEqualTo(userName1)
    }

    @Test
    fun `when display and user name are empty, then name is empty`() = test {
        val avatarUrl = "avatar.jpg"
        val displayName = ""
        val userName = ""
        val accountModel = getAccountData().apply {
            this.avatarUrl = avatarUrl
            this.displayName = displayName
            this.userName = userName
        }
        whenever(accountStore.account).thenReturn(accountModel)

        viewModelSlice.onRefresh()

        assertThat(uiModels.last()?.name).isEmpty()
    }

    fun getAccountData(): AccountModel {
        val accountModel = AccountModel()
        accountModel.avatarUrl = "avatar.jpg"
        accountModel.displayName = "name"
        return accountModel
    }
}
