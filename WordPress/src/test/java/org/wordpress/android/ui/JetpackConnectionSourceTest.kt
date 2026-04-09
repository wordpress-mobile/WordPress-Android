package org.wordpress.android.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.BaseUnitTest

@ExperimentalCoroutinesApi
class JetpackConnectionSourceTest : BaseUnitTest() {
    @Test
    fun `given valid notifications source, when fromString requested, then results match enum`() {
        val value = "notifications"

        val result = JetpackConnectionSource.fromString(value)

        assertThat(result).isEqualTo(JetpackConnectionSource.NOTIFICATIONS)
    }

    @Test
    fun `given valid stats source, when fromString requested, then results match enum`() {
        val value = "stats"

        val result = JetpackConnectionSource.fromString(value)

        assertThat(result).isEqualTo(JetpackConnectionSource.STATS)
    }

    @Test
    fun `given valid xmlrpc_disabled source, when fromString requested, then results match enum`() {
        val value = "xmlrpc_disabled"

        val result = JetpackConnectionSource.fromString(value)

        assertThat(result).isEqualTo(JetpackConnectionSource.XMLRPC_DISABLED)
    }

    @Test
    fun `given invalid source, when fromString requested, then results are null`() {
        val value = "abcdefg"

        val result = JetpackConnectionSource.fromString(value)

        assertThat(result).isNull()
    }

    @Test
    fun `given invalid notifications source, when fromString requested, then results are null`() {
        val value = "NoTiFiCaTiOnS"

        val result = JetpackConnectionSource.fromString(value)

        assertThat(result).isNull()
    }

    @Test
    fun `given invalid stats source, when fromString requested, then results are null`() {
        val value = "StAtS"

        val result = JetpackConnectionSource.fromString(value)

        assertThat(result).isNull()
    }
}
