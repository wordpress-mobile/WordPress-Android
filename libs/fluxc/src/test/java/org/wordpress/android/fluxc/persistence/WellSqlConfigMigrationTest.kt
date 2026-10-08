package org.wordpress.android.fluxc.persistence

import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Covers schema migrations, which the app runs exactly once per install and never revisits. A wrong
 * predicate or case label ships as a silent no-op — the upgrade "succeeds" and the rows it was meant
 * to repair stay broken — so each one is run through [WellSqlConfig.onUpgrade] and its effect asserted.
 *
 * Rows are seeded with raw SQL on purpose: [SiteSqlUtils] now refuses the very values these
 * migrations exist to clean up, so its writers can't set the state under test.
 */
@RunWith(RobolectricTestRunner::class)
class WellSqlConfigMigrationTest {
    private lateinit var config: WellSqlConfig

    @Before
    fun setUp() {
        config = WellSqlConfig(RuntimeEnvironment.getApplication().applicationContext)
        WellSql.init(config)
        config.reset()
    }

    /** onUpgrade only runs when the version rises, so installs on this version need a later one. */
    @Test
    fun `the database version is past the proxy root migration`() {
        assertThat(config.dbVersion).isGreaterThan(PROXY_ROOT_MIGRATION)
    }

    @Test
    fun `clearing proxy roots nulls the synthesized wp-v2 form`() {
        insertSite(localId = 1, restUrl = "https://public-api.wordpress.com/wp/v2/sites/12345")

        runMigration()

        assertThat(storedRestUrl(1)).isNull()
    }

    /**
     * The match covers the whole proxy host, not just the shape the removed getter synthesized: no
     * URL under it can serve as a direct-host root, whatever its path or query.
     */
    @Test
    fun `clearing proxy roots nulls any other url under the proxy host`() {
        insertSite(
            localId = 1,
            restUrl = "https://public-api.wordpress.com/wp-json/?rest_route=/sites/example.com"
        )

        runMigration()

        assertThat(storedRestUrl(1)).isNull()
    }

    @Test
    fun `clearing proxy roots leaves a direct host root untouched`() {
        insertSite(localId = 1, restUrl = "https://example.com/wp-json/")

        runMigration()

        assertThat(storedRestUrl(1)).isEqualTo("https://example.com/wp-json/")
    }

    @Test
    fun `clearing proxy roots leaves a site hosted on a similar domain untouched`() {
        insertSite(localId = 1, restUrl = "https://public-api.wordpress.com.example.net/wp-json/")

        runMigration()

        assertThat(storedRestUrl(1))
            .isEqualTo("https://public-api.wordpress.com.example.net/wp-json/")
    }

    @Test
    fun `clearing proxy roots leaves rows with no stored root alone`() {
        insertSite(localId = 1, restUrl = null)

        runMigration()

        assertThat(storedRestUrl(1)).isNull()
    }

    // Runs only this version's case, so a migration filed under the wrong label, or shadowed by an
    // earlier duplicate, fails here.
    private fun runMigration() {
        config.onUpgrade(WellSql.giveMeWritableDb(), mock(), PROXY_ROOT_MIGRATION, PROXY_ROOT_MIGRATION)
    }

    private fun insertSite(localId: Int, restUrl: String?) {
        val value = restUrl?.let { "'$it'" } ?: "NULL"
        WellSql.giveMeWritableDb().execSQL(
            "INSERT INTO SiteModel (_id, SITE_ID, URL, WP_API_REST_URL) " +
                    "VALUES ($localId, $localId, 'https://example.com', $value)"
        )
    }

    private fun storedRestUrl(localId: Int): String? =
        WellSql.giveMeWritableDb()
            .rawQuery("SELECT WP_API_REST_URL FROM SiteModel WHERE _id = ?", arrayOf("$localId"))
            .use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                if (cursor.isNull(0)) null else cursor.getString(0)
            }

    companion object {
        private const val PROXY_ROOT_MIGRATION = 212
    }
}
