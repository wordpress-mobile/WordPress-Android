package org.wordpress.android.fluxc.persistence

import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Covers the SQL behind schema migrations, which the app runs exactly once per install and never
 * revisits. A wrong predicate ships as a silent no-op — the upgrade "succeeds" and the rows it was
 * meant to repair stay broken — so the statements are asserted directly rather than trusted.
 *
 * Rows are seeded with raw SQL on purpose: [SiteSqlUtils.updateWpApiRestUrl] now refuses the very
 * values these migrations exist to clean up, so the writer can't set the state under test.
 */
@RunWith(RobolectricTestRunner::class)
class WellSqlConfigMigrationTest {
    @Before
    fun setUp() {
        val config = WellSqlConfig(RuntimeEnvironment.getApplication().applicationContext)
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun `clearing proxy roots nulls the synthesized wp-v2 form`() {
        insertSite(localId = 1, restUrl = "https://public-api.wordpress.com/wp/v2/sites/12345")

        runMigration()

        assertThat(storedRestUrl(1)).isNull()
    }

    /**
     * The shape WP.com Simple sites advertise during REST discovery. It reaches the column through
     * application-password login, and an earlier revision of this migration missed it.
     */
    @Test
    fun `clearing proxy roots nulls the rest_route form`() {
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

    private fun runMigration() {
        WellSql.giveMeWritableDb().execSQL(WellSqlConfig.CLEAR_WPCOM_PROXY_REST_ROOTS)
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
}
