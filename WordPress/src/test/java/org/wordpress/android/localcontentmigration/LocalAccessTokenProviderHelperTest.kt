package org.wordpress.android.localcontentmigration

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

import org.junit.Before
import org.wordpress.android.fluxc.store.AccountStore

class LocalAccessTokenProviderHelperTest {
    private val accountStore: AccountStore = mock()
    private val mockAccountToken = "p455w0rd"

    // Object under test
    private val localAccessTokenProviderHelper = LocalAccessTokenProviderHelper(accountStore)

    @Before
    fun setUp() {
        whenever(accountStore.accessToken).thenReturn(mockAccountToken)
    }

    @Test
    fun `when the token is present`() {
        val data = localAccessTokenProviderHelper.getData()
        assertThat(data.token).isEqualTo(mockAccountToken)
    }
}
