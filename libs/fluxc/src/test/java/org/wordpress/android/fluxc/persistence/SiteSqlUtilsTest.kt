package org.wordpress.android.fluxc.persistence

import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.encryption.EncryptionUtils
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(RobolectricTestRunner::class)
class SiteSqlUtilsTest {
    private val siteSqlUtils = SiteSqlUtils(EncryptionUtils())

    // EncryptionUtils uses the AndroidKeyStore provider, which Robolectric can't load, so the production
    // util's encrypt/decrypt throw here. A stubbed util lets us exercise the encrypting writers' own
    // ContentValues mapping (which half of each encrypt() Pair lands in which column) against a real DB —
    // the SiteStore-level tests can't, since they verify a mocked SiteSqlUtils and never run the body.
    private val fakeEncryptionUtils = mock<EncryptionUtils>()
    private val encryptingSiteSqlUtils = SiteSqlUtils(fakeEncryptionUtils)

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

    @Test
    fun `insertOrUpdateSite update does not clobber credential columns from a stale model`() {
        WellSql.insert(SiteModel().apply {
            siteId = 42
            url = "https://example.test"
            apiRestUsernameEncrypted = "enc_user"
            apiRestUsernameIV = "iv_user"
            apiRestPasswordEncrypted = "enc_pass"
            apiRestPasswordIV = "iv_pass"
        }).execute()
        val localId = storedSite().id

        // A credential-less inbound model (e.g. a /me/sites sync) must not zero the credential columns.
        val stale = SiteModel().apply {
            id = localId
            siteId = 42
            url = "https://example.test"
            name = "Updated name"
        }
        siteSqlUtils.insertOrUpdateSite(stale)

        val stored = storedSite()
        assertThat(stored.apiRestUsernameEncrypted).isEqualTo("enc_user")
        assertThat(stored.apiRestUsernameIV).isEqualTo("iv_user")
        assertThat(stored.apiRestPasswordEncrypted).isEqualTo("enc_pass")
        assertThat(stored.apiRestPasswordIV).isEqualTo("iv_pass")
        assertThat(stored.name).isEqualTo("Updated name") // other columns still update
    }

    @Test
    fun `clearApplicationPasswordCredentials empties the credential columns`() {
        WellSql.insert(SiteModel().apply {
            url = "https://example.test"
            apiRestUsernameEncrypted = "enc_user"
            apiRestUsernameIV = "iv_user"
            apiRestPasswordEncrypted = "enc_pass"
            apiRestPasswordIV = "iv_pass"
        }).execute()
        val localId = storedSite().id

        val rows = siteSqlUtils.clearApplicationPasswordCredentials(localId)

        assertThat(rows).isEqualTo(1)
        val stored = storedSite()
        assertThat(stored.apiRestUsernameEncrypted).isEmpty()
        assertThat(stored.apiRestUsernameIV).isEmpty()
        assertThat(stored.apiRestPasswordEncrypted).isEmpty()
        assertThat(stored.apiRestPasswordIV).isEmpty()
    }

    @Test
    fun `reading a site with credential ciphertext but a blank IV does not attempt decryption`() {
        // A row carrying credential ciphertext without its IV is malformed — decrypting it would fail. The
        // read path must treat it as having no decryptable credentials instead of throwing. Both ciphertexts
        // are present here so only the blank-IV guard (not the empty-ciphertext one) can short-circuit.
        WellSql.insert(SiteModel().apply {
            url = "https://example.test"
            apiRestUsernameEncrypted = "enc_user"
            apiRestUsernameIV = ""
            apiRestPasswordEncrypted = "enc_pass"
            apiRestPasswordIV = "iv_pass"
        }).execute()

        val stored = siteSqlUtils.getSites().single()

        assertThat(stored.apiRestUsernamePlain).isNullOrEmpty()
        assertThat(stored.apiRestPasswordPlain).isNullOrEmpty()
    }

    @Test
    fun `updateApplicationPasswordCredentials writes each encrypted value into its matching column`() {
        // ciphertext (encrypt().first) -> *_ENCRYPTED column, IV (.second) -> *_IV column, and username
        // must not be cross-wired with password. A .first/.second swap, a wrong column, or a dropped
        // cv.put would all read back wrong here — the failure modes the SiteStore mock can't catch.
        whenever(fakeEncryptionUtils.encrypt("user")).thenReturn("enc_user" to "iv_user")
        whenever(fakeEncryptionUtils.encrypt("pass")).thenReturn("enc_pass" to "iv_pass")
        WellSql.insert(SiteModel().apply { url = "https://example.test" }).execute()
        val localId = storedSite().id

        val rows = encryptingSiteSqlUtils.updateApplicationPasswordCredentials(localId, "user", "pass")

        assertThat(rows).isEqualTo(1)
        val stored = storedSite()
        assertThat(stored.apiRestUsernameEncrypted).isEqualTo("enc_user")
        assertThat(stored.apiRestUsernameIV).isEqualTo("iv_user")
        assertThat(stored.apiRestPasswordEncrypted).isEqualTo("enc_pass")
        assertThat(stored.apiRestPasswordIV).isEqualTo("iv_pass")
    }

    @Test
    fun `updateApplicationPasswordCredentials persists credentials that decrypt back to the originals`() {
        whenever(fakeEncryptionUtils.encrypt("user")).thenReturn("enc_user" to "iv_user")
        whenever(fakeEncryptionUtils.encrypt("pass")).thenReturn("enc_pass" to "iv_pass")
        whenever(fakeEncryptionUtils.decrypt("enc_user", "iv_user")).thenReturn("user")
        whenever(fakeEncryptionUtils.decrypt("enc_pass", "iv_pass")).thenReturn("pass")
        WellSql.insert(SiteModel().apply { url = "https://example.test" }).execute()
        val localId = storedSite().id

        encryptingSiteSqlUtils.updateApplicationPasswordCredentials(localId, "user", "pass")

        // Read back through the decrypting path: the stored ciphertext/IV must round-trip to the inputs.
        val stored = encryptingSiteSqlUtils.getSites().single()
        assertThat(stored.apiRestUsernamePlain).isEqualTo("user")
        assertThat(stored.apiRestPasswordPlain).isEqualTo("pass")
    }

    @Test
    fun `updateApplicationPasswordCredentialsForWPAPISite writes the matching WPAPI site by url`() {
        whenever(fakeEncryptionUtils.encrypt("user")).thenReturn("enc_user" to "iv_user")
        whenever(fakeEncryptionUtils.encrypt("pass")).thenReturn("enc_pass" to "iv_pass")
        WellSql.insert(SiteModel().apply {
            url = "https://selfhosted.test"
            origin = SiteModel.ORIGIN_WPAPI
        }).execute()

        val rows = encryptingSiteSqlUtils.updateApplicationPasswordCredentialsForWPAPISite(
                siteUrl = "https://selfhosted.test",
                usernamePlain = "user",
                passwordPlain = "pass"
        )

        assertThat(rows).isEqualTo(1)
        val stored = storedSite()
        assertThat(stored.apiRestUsernameEncrypted).isEqualTo("enc_user")
        assertThat(stored.apiRestUsernameIV).isEqualTo("iv_user")
        assertThat(stored.apiRestPasswordEncrypted).isEqualTo("enc_pass")
        assertThat(stored.apiRestPasswordIV).isEqualTo("iv_pass")
    }

    @Test
    fun `updateApplicationPasswordCredentialsForWPAPISite leaves a non-WPAPI site with the same url untouched`() {
        WellSql.insert(SiteModel().apply {
            url = "https://shared.test"
            origin = SiteModel.ORIGIN_WPCOM_REST
        }).execute()

        val rows = encryptingSiteSqlUtils.updateApplicationPasswordCredentialsForWPAPISite(
                siteUrl = "https://shared.test",
                usernamePlain = "user",
                passwordPlain = "pass"
        )

        assertThat(rows).isEqualTo(0)
        val stored = storedSite()
        assertThat(stored.apiRestUsernameEncrypted).isNullOrEmpty()
        assertThat(stored.apiRestPasswordEncrypted).isNullOrEmpty()
    }

    @Test
    fun `updateXmlRpcUrl writes the column and leaves other fields alone`() {
        WellSql.insert(SiteModel().apply {
            siteId = 42
            url = "https://example.test"
            name = "Example"
            xmlRpcUrl = null
        }).execute()
        val localId = siteSqlUtils.getSites().single().id

        val rowsUpdated = siteSqlUtils.updateXmlRpcUrl(localId, "https://example.test/xmlrpc.php")

        assertThat(rowsUpdated).isEqualTo(1)
        val stored = siteSqlUtils.getSites().single()
        assertThat(stored.xmlRpcUrl).isEqualTo("https://example.test/xmlrpc.php")
        assertThat(stored.url).isEqualTo("https://example.test")
        assertThat(stored.name).isEqualTo("Example")
    }

    @Test
    fun `updateXmlRpcUrl returns 0 when no site row matches the local id`() {
        val rowsUpdated = siteSqlUtils.updateXmlRpcUrl(localId = 999, xmlRpcUrl = "https://example.test/xmlrpc.php")

        assertThat(rowsUpdated).isEqualTo(0)
    }

    @Test
    fun `insertOrUpdateSite update does not clobber xmlRpcUrl from a stale model`() {
        // Absence is preserved: a partial writer that doesn't carry xmlRpcUrl (e.g. the WPAPI fetch) must
        // not clear a stored/rediscovered value, even though XMLRPC_URL is not excluded from the mapper.
        val xmlRpc = "https://example.test/xmlrpc.php"
        WellSql.insert(SiteModel().apply {
            siteId = 42
            url = "https://example.test"
            name = "Example"
            xmlRpcUrl = xmlRpc
        }).execute()
        val localId = siteSqlUtils.getSites().single().id

        val stale = SiteModel().apply {
            id = localId
            siteId = 42
            url = "https://example.test"
            name = "Updated name"
            xmlRpcUrl = null
        }
        siteSqlUtils.insertOrUpdateSite(stale)

        val stored = siteSqlUtils.getSites().single()
        assertThat(stored.xmlRpcUrl).isEqualTo(xmlRpc) // preserved
        assertThat(stored.name).isEqualTo("Updated name") // other columns still update
    }

    @Test
    fun `insertOrUpdateSite update persists a changed xmlRpcUrl carried by the inbound model`() {
        // Presence is authoritative: the WP.com sync reliably carries meta.links.xmlrpc, so a non-empty
        // inbound value (e.g. a domain migration) must overwrite the stored one — only absence is preserved.
        WellSql.insert(SiteModel().apply {
            siteId = 42
            url = "https://example.test"
            xmlRpcUrl = "https://example.test/xmlrpc.php"
        }).execute()
        val localId = siteSqlUtils.getSites().single().id

        val migrated = SiteModel().apply {
            id = localId
            siteId = 42
            url = "https://example.test"
            xmlRpcUrl = "https://migrated.test/xmlrpc.php"
        }
        siteSqlUtils.insertOrUpdateSite(migrated)

        assertThat(siteSqlUtils.getSites().single().xmlRpcUrl)
                .isEqualTo("https://migrated.test/xmlrpc.php")
    }

    @Test
    fun `insertOrUpdateSiteReturningId returns the matched row id on update`() {
        WellSql.insert(SiteModel().apply {
            siteId = 42
            url = "https://example.test"
        }).execute()
        val existingId = siteSqlUtils.getSites().single().id

        // A fresh inbound model (id = 0) that matches the existing row by SITE_ID + URL.
        val returnedId = siteSqlUtils.insertOrUpdateSiteReturningId(SiteModel().apply {
            siteId = 42
            url = "https://example.test"
            name = "Updated"
        })

        assertThat(returnedId).isEqualTo(existingId)
    }

    @Test
    fun `insertOrUpdateSiteReturningId returns the new row id on insert`() {
        val returnedId = siteSqlUtils.insertOrUpdateSiteReturningId(SiteModel().apply {
            siteId = 99
            url = "https://newsite.test"
        })

        assertThat(returnedId).isNotEqualTo(0)
        assertThat(siteSqlUtils.getSites().single().id).isEqualTo(returnedId)
    }

    // Raw read that bypasses SiteSqlUtils' decryptAPIRestCredentials, so tests can assert on the stored
    // ciphertext columns directly without invoking the AndroidKeyStore-backed EncryptionUtils.
    private fun storedSite(): SiteModel = WellSql.select(SiteModel::class.java).asModel.single()
}
