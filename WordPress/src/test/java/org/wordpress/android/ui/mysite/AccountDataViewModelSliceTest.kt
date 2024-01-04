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

@ExperimentalCoroutinesApi
class AccountDataViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    @Mock
    lateinit var accountModel: AccountModel

    private lateinit var viewModelSlice: AccountDataViewModelSlice

    private lateinit var isRefreshing: MutableList<Boolean>

    @Before
    fun setUp() {
        viewModelSlice = AccountDataViewModelSlice(
            accountStore,
            buildConfigWrapper,
            jetpackFeatureRemovalPhaseHelper
        )
        viewModelSlice.initialize(testScope())
        isRefreshing = mutableListOf()
    }


    @Test
    fun `given jp app, card is not built`() = test {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures()).thenReturn(true)

        var result: AccountData? = null
        viewModelSlice.uiModel.observeForever { result = it }

        assertThat(result).isNull()
    }

    @Test
    fun `given wp app, when in not correct phase, card is not built`() = test {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures()).thenReturn(false)

        var result: AccountData? = null
        viewModelSlice.uiModel.observeForever { result = it }

        assertThat(result).isNull()
    }

    @Test
    fun `current avatar is loaded on refresh from account store`() = test {
        whenever(accountStore.account).thenReturn(accountModel)
        val avatarUrl = "avatar.jpg"
        whenever(accountModel.avatarUrl).thenReturn(avatarUrl)

        var result: AccountData? = null
        viewModelSlice.build(testScope()).observeForever {
            it?.let { result = it }
        }

        viewModelSlice.refresh()

        assertThat(result!!.url).isEqualTo(avatarUrl)
    }

    @Test
    fun `when buildSource is invoked, then refresh is true`() = test {
        viewModelSlice.refresh.observeForever { isRefreshing.add(it) }

        viewModelSlice.build(testScope())

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `when refresh is invoked, then refresh is true`() = test {
        viewModelSlice.refresh.observeForever { isRefreshing.add(it) }

        viewModelSlice.refresh()

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `when data has been refreshed, then refresh is set to false`() = test {
        whenever(accountStore.account).thenReturn(accountModel)
        val avatarUrl = "avatar.jpg"
        whenever(accountModel.avatarUrl).thenReturn(avatarUrl)
        viewModelSlice.refresh.observeForever { isRefreshing.add(it) }

        viewModelSlice.build(testScope()).observeForever { }
        viewModelSlice.refresh()

        assertThat(isRefreshing.last()).isFalse
    }
}
