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
        whenever(jetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures()).thenReturn(true)

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
        whenever(accountStore.account).then({  })

        viewModelSlice.onResume()

        assertEquals(true, isRefreshing.first())
        assertEquals(AccountData(accountModel.avatarUrl, accountModel.displayName), uiModels.last())
        assertEquals(false, isRefreshing.last())
    }
//
//    @Test
//    fun `current avatar is loaded on refresh from account store`() = test {
//        whenever(accountStore.account).thenReturn(accountModel)
//        val avatarUrl = "avatar.jpg"
//        whenever(accountModel.avatarUrl).thenReturn(avatarUrl)
//
//        var result: AccountData? = null
//        viewModelSlice.build(testScope()).observeForever {
//            it?.let { result = it }
//        }
//
//        viewModelSlice.refresh()
//
//        assertThat(result!!.url).isEqualTo(avatarUrl)
//    }
//
//    @Test
//    fun `when buildSource is invoked, then refresh is true`() = test {
//        viewModelSlice.refresh.observeForever { isRefreshing.add(it) }
//
//        viewModelSlice.build(testScope())
//
//        assertThat(isRefreshing.last()).isTrue
//    }
//
//    @Test
//    fun `when refresh is invoked, then refresh is true`() = test {
//        viewModelSlice.refresh.observeForever { isRefreshing.add(it) }
//
//        viewModelSlice.refresh()
//
//        assertThat(isRefreshing.last()).isTrue
//    }
//
//    @Test
//    fun `when data has been refreshed, then refresh is set to false`() = test {
//        whenever(accountStore.account).thenReturn(accountModel)
//        val avatarUrl = "avatar.jpg"
//        whenever(accountModel.avatarUrl).thenReturn(avatarUrl)
//        viewModelSlice.refresh.observeForever { isRefreshing.add(it) }
//
//        viewModelSlice.build(testScope()).observeForever { }
//        viewModelSlice.refresh()
//
//        assertThat(isRefreshing.last()).isFalse
//    }

    fun getAccountData(): AccountModel {
        val accountModel = AccountModel()
        accountModel.avatarUrl = "avatar.jpg"
        accountModel.displayName = "name"
        return accountModel
    }
}
