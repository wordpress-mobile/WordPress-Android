package org.wordpress.android.fluxc.persistence

import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.encryption.EncryptionUtils
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(RobolectricTestRunner::class)
class SiteSqlUtilsTest {
    private val siteSqlUtils = SiteSqlUtils(EncryptionUtils())

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.getApplication().applicationContext
        val config = WellSqlConfig(appContext)
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun `updateWpApiRestUrl writes the column and leaves other fields alone`() {
        val site = SiteModel().apply {
            id = 1
            siteId = 42
            url = "https://example.test"
            name = "Example"
            wpApiRestUrl = null
        }
        WellSql.insert(site).execute()

        val rowsUpdated = siteSqlUtils.updateWpApiRestUrl(localId = 1, wpApiRestUrl = "https://example.test/wp-json/")

        assertThat(rowsUpdated).isEqualTo(1)
        val stored = siteSqlUtils.getSitesWithLocalId(1).single()
        assertThat(stored.wpApiRestUrl).isEqualTo("https://example.test/wp-json/")
        assertThat(stored.url).isEqualTo("https://example.test")
        assertThat(stored.name).isEqualTo("Example")
        assertThat(stored.siteId).isEqualTo(42)
    }

    @Test
    fun `updateWpApiRestUrl returns 0 when no site row matches the local id`() {
        val rowsUpdated = siteSqlUtils.updateWpApiRestUrl(localId = 999, wpApiRestUrl = "https://example.test/wp-json/")

        assertThat(rowsUpdated).isEqualTo(0)
    }
}
