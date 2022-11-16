package org.wordpress.android.localcontentmigration

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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
