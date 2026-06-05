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

    @Test
    fun `insertOrUpdateSite update does not clobber wpApiRestUrl from a stale model`() {
        val healed = "https://example.test/wp-json/"
        WellSql.insert(SiteModel().apply {
            siteId = 42
            url = "https://example.test"
            name = "Example"
            wpApiRestUrl = healed
        }).execute()
        // WellSql auto-assigns the primary key on insert, so read back the id it actually allocated.
        val localId = siteSqlUtils.getSites().single().id

        // A later writer (e.g. a FETCH_SITE round-trip) carries a stale, null wpApiRestUrl but a fresh name.
        val stale = SiteModel().apply {
            id = localId
            siteId = 42
            url = "https://example.test"
            name = "Updated name"
            wpApiRestUrl = null
        }
        val rows = siteSqlUtils.insertOrUpdateSite(stale)

        assertThat(rows).isEqualTo(1)
        val stored = siteSqlUtils.getSites().single()
        assertThat(stored.wpApiRestUrl).isEqualTo(healed) // preserved
        assertThat(stored.name).isEqualTo("Updated name") // other columns still update
    }

    @Test
    fun `insertOrUpdateSite insert still persists wpApiRestUrl for a new site`() {
        val rows = siteSqlUtils.insertOrUpdateSite(SiteModel().apply {
            siteId = 99
            url = "https://newsite.test"
            name = "New"
            wpApiRestUrl = "https://newsite.test/wp-json/"
        })

        assertThat(rows).isEqualTo(1)
        assertThat(siteSqlUtils.getSites().single().wpApiRestUrl)
                .isEqualTo("https://newsite.test/wp-json/")
    }

    @Test
    fun `clearWpApiRestUrl empties the stored url`() {
        WellSql.insert(SiteModel().apply {
            url = "https://example.test"
            wpApiRestUrl = "https://example.test/wp-json/"
        }).execute()
        val localId = siteSqlUtils.getSites().single().id

        val rows = siteSqlUtils.clearWpApiRestUrl(localId)

        assertThat(rows).isEqualTo(1)
        assertThat(siteSqlUtils.getSites().single().wpApiRestUrl).isEmpty()
    }

    @Test
    fun `updateWpApiRestUrlForWPAPISite updates the matching WPAPI site by url`() {
        WellSql.insert(SiteModel().apply {
            url = "https://selfhosted.test"
            origin = SiteModel.ORIGIN_WPAPI
            wpApiRestUrl = null
        }).execute()

        val rows = siteSqlUtils.updateWpApiRestUrlForWPAPISite(
                siteUrl = "https://selfhosted.test",
                wpApiRestUrl = "https://selfhosted.test/wp-json/"
        )

        assertThat(rows).isEqualTo(1)
        assertThat(siteSqlUtils.getSites().single().wpApiRestUrl)
                .isEqualTo("https://selfhosted.test/wp-json/")
    }

    @Test
    fun `updateWpApiRestUrlForWPAPISite leaves a non-WPAPI site with the same url untouched`() {
        WellSql.insert(SiteModel().apply {
            url = "https://shared.test"
            origin = SiteModel.ORIGIN_WPCOM_REST
            wpApiRestUrl = null
        }).execute()

        val rows = siteSqlUtils.updateWpApiRestUrlForWPAPISite(
                siteUrl = "https://shared.test",
                wpApiRestUrl = "https://shared.test/wp-json/"
        )

        assertThat(rows).isEqualTo(0)
        assertThat(siteSqlUtils.getSites().single().wpApiRestUrl).isNull()
    }
}
