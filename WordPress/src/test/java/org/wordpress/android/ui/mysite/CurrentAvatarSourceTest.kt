package org.wordpress.android.ui.mysite

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.test

@RunWith(MockitoJUnitRunner::class)
class CurrentAvatarSourceTest {
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var accountModel: AccountModel
    private lateinit var currentAvatarSource: CurrentAvatarSource

    @Before
    fun setUp() {
        currentAvatarSource = CurrentAvatarSource(accountStore)
    }

    @Test
    fun `current avatar is empty on start`() = test {
        val result = currentAvatarSource.buildSource().take(1).toList()

        assertThat(result.last().url).isEqualTo("")
    }

    @Test
    fun `current avatar is loaded on refresh from account store`() = test {
        whenever(accountStore.account).thenReturn(accountModel)
        val avatarUrl = "avatar.jpg"
        whenever(accountModel.avatarUrl).thenReturn(avatarUrl)

        currentAvatarSource.refresh()

        val result = currentAvatarSource.buildSource().take(1).toList()
        assertThat(result.last().url).isEqualTo(avatarUrl)
    }
}
