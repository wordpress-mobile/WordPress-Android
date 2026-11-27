package org.wordpress.android.fluxc.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.PostFormatModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.SitesModel
import org.wordpress.android.fluxc.model.jetpacksocial.JetpackSocialMapper
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsManager
import org.wordpress.android.fluxc.network.rest.wpapi.site.SiteWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookie
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.NewSiteResponsePayload
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient
import org.wordpress.android.fluxc.persistence.JetpackCPConnectedSitesDao
import org.wordpress.android.fluxc.persistence.PostSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.jetpacksocial.JetpackSocialDao
import org.wordpress.android.fluxc.store.SiteStore.FetchSitesPayload
import org.wordpress.android.fluxc.store.SiteStore.FetchedPostFormatsPayload
import org.wordpress.android.fluxc.store.SiteStore.NewSiteError
import org.wordpress.android.fluxc.store.SiteStore.NewSiteErrorType.SITE_NAME_INVALID
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload
import org.wordpress.android.fluxc.store.SiteStore.OnPostFormatsChanged
import org.wordpress.android.fluxc.store.SiteStore.PostFormatsError
import org.wordpress.android.fluxc.store.SiteStore.PostFormatsErrorType.INVALID_SITE
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.SiteStore.SiteFilter.WPCOM
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility.PUBLIC
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import javax.inject.Provider
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class SiteStoreTest {
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var postSqlUtils: PostSqlUtils
    @Mock lateinit var siteRestClient: SiteRestClient
    @Mock lateinit var siteXMLRPCClient: SiteXMLRPCClient
    @Mock lateinit var siteWPAPIClient: SiteWPAPIRestClient
    @Mock lateinit var privateAtomicCookie: PrivateAtomicCookie
    @Mock lateinit var siteSqlUtils: SiteSqlUtils
    @Mock lateinit var jetpackCPConnectedSitesDao: JetpackCPConnectedSitesDao
    @Mock lateinit var jetpackSocialDao: JetpackSocialDao
    @Mock lateinit var jetpackSocialMapper: JetpackSocialMapper
    private lateinit var siteStore: SiteStore

    @Before
    fun setUp() {
        siteStore = SiteStore(
            dispatcher,
            postSqlUtils,
            siteRestClient,
            siteXMLRPCClient,
            siteWPAPIClient,
            privateAtomicCookie,
            siteSqlUtils,
            jetpackCPConnectedSitesDao,
            jetpackSocialDao,
            jetpackSocialMapper,
            initCoroutineEngine()
        )
    }

    @Test
    fun `fetchSite from WPCom endpoint and stores it to DB`() = test {
        val site = SiteModel()
        site.setIsWPCom(true)
        site.origin = SiteModel.ORIGIN_WPCOM_REST
        val updatedSite = SiteModel()
        whenever(siteRestClient.fetchSite(site)).thenReturn(updatedSite)

        assertSiteFetched(updatedSite, site)
    }

    @Test
    fun `fetchSite error from WPCom endpoint returns error`() = test {
        val site = SiteModel()
        site.setIsWPCom(true)
        site.origin = SiteModel.ORIGIN_WPCOM_REST
        val errorSite = SiteModel()
        errorSite.error = BaseNetworkError(PARSE_ERROR)
        whenever(siteRestClient.fetchSite(site)).thenReturn(errorSite)

        assertSiteFetchError(site)
    }

    @Test
    fun `fetchSite from XMLRPC endpoint and stores it to DB`() = test {
        val site = SiteModel()
        site.setIsWPCom(false)
        val updatedSite = SiteModel()
        whenever(siteXMLRPCClient.fetchSite(site)).thenReturn(updatedSite)

        assertSiteFetched(updatedSite, site)
    }

    @Test
    fun `fetchSite error from XMLRPC endpoint returns error`() = test {
        val site = SiteModel()
        site.setIsWPCom(false)
        val errorSite = SiteModel()
        errorSite.error = BaseNetworkError(PARSE_ERROR)
        whenever(siteXMLRPCClient.fetchSite(site)).thenReturn(errorSite)

        assertSiteFetchError(site)
    }

    @Test
    fun `fetchSite from WPAPI endpoint for an application-password site stored as XMLRPC`() = test {
        // The common application-password sign-in stores the row as ORIGIN_XMLRPC, because its
        // wp.getUsersBlogs succeeded. The credentials, not the origin, decide the transport.
        val site = applicationPasswordSite().apply { origin = SiteModel.ORIGIN_XMLRPC }
        val updatedSite = SiteModel()
        whenever(siteWPAPIClient.fetchWPAPISite(site)).thenReturn(updatedSite)

        assertSiteFetched(updatedSite, site)

        verifyNoInteractions(siteXMLRPCClient)
    }

    @Test
    fun `fetchSite from XMLRPC endpoint for a site stored as XMLRPC without application password`() = test {
        val site = SiteModel()
        site.setIsWPCom(false)
        site.origin = SiteModel.ORIGIN_XMLRPC
        val updatedSite = SiteModel()
        whenever(siteXMLRPCClient.fetchSite(site)).thenReturn(updatedSite)

        assertSiteFetched(updatedSite, site)

        verifyNoInteractions(siteWPAPIClient)
    }

    @Test
    fun `fetchSite from WPAPI endpoint for a site stored as WPAPI`() = test {
        val site = SiteModel()
        site.setIsWPCom(false)
        site.origin = SiteModel.ORIGIN_WPAPI
        val updatedSite = SiteModel()
        whenever(siteWPAPIClient.fetchWPAPISite(site)).thenReturn(updatedSite)

        assertSiteFetched(updatedSite, site)

        verifyNoInteractions(siteXMLRPCClient)
    }

    @Test
    fun `fetchSite from WPCom endpoint for a WPCom site that also holds an application password`() = test {
        // Atomic sites carry application-password credentials too, and must keep going to WP.com REST.
        val site = applicationPasswordSite().apply {
            setIsWPCom(true)
            origin = SiteModel.ORIGIN_WPCOM_REST
        }
        val updatedSite = SiteModel()
        whenever(siteRestClient.fetchSite(site)).thenReturn(updatedSite)

        assertSiteFetched(updatedSite, site)

        verifyNoInteractions(siteWPAPIClient)
        verifyNoInteractions(siteXMLRPCClient)
    }

    @Test
    fun `fetchSite yields a blog id another row owns for an application-password site stored as XMLRPC`() =
        test {
            val site = applicationPasswordSite().apply { origin = SiteModel.ORIGIN_XMLRPC }
            val updatedSite = applicationPasswordSite().apply {
                id = 1
                origin = SiteModel.ORIGIN_XMLRPC
                siteId = WPCOM_BLOG_ID
            }
            val meSitesRow = SiteModel().apply {
                id = 2
                siteId = WPCOM_BLOG_ID
            }
            whenever(siteWPAPIClient.fetchWPAPISite(site)).thenReturn(updatedSite)
            whenever(siteSqlUtils.getSitesWithRemoteId(WPCOM_BLOG_ID)).thenReturn(listOf(meSitesRow))

            siteStore.fetchSite(site)

            // The /me/sites row keeps the blog id; this row would otherwise trip UNIQUE (SITE_ID, URL)
            // or overwrite it.
            verify(siteSqlUtils).insertOrUpdateSite(argWhere { it.siteId == 0L })
        }

    @Test
    fun `fetchSite keeps a blog id no other row owns`() = test {
        val site = applicationPasswordSite().apply { origin = SiteModel.ORIGIN_XMLRPC }
        val updatedSite = applicationPasswordSite().apply {
            id = 1
            origin = SiteModel.ORIGIN_XMLRPC
            siteId = WPCOM_BLOG_ID
        }
        whenever(siteWPAPIClient.fetchWPAPISite(site)).thenReturn(updatedSite)
        whenever(siteSqlUtils.getSitesWithRemoteId(WPCOM_BLOG_ID)).thenReturn(listOf(updatedSite))

        siteStore.fetchSite(site)

        verify(siteSqlUtils).insertOrUpdateSite(argWhere { it.siteId == WPCOM_BLOG_ID })
    }

    @Test
    fun `fetchSite keeps the blog id of a Jetpack row that also holds an application password`() = test {
        // A Jetpack-connected row is not isWPCom(), so the credential check alone wouldn't exclude it --
        // but it addresses every WP.com REST call with this blog id and must never yield it (CMM-2114).
        val site = applicationPasswordSite().apply { origin = SiteModel.ORIGIN_WPCOM_REST }
        val updatedSite = applicationPasswordSite().apply {
            id = 1
            origin = SiteModel.ORIGIN_WPCOM_REST
            setIsJetpackConnected(true)
            siteId = WPCOM_BLOG_ID
        }
        whenever(siteRestClient.fetchSite(site)).thenReturn(updatedSite)

        siteStore.fetchSite(site)

        // The guard returns before the lookup, so the blog id is never even a candidate to yield.
        verify(siteSqlUtils, never()).getSitesWithRemoteId(any())
        verify(siteSqlUtils).insertOrUpdateSite(argWhere { it.siteId == WPCOM_BLOG_ID })
    }

    @Test
    fun `fetchSite takes one XMLRPC refresh to learn a missing software version`() = test {
        // software_version has no REST equivalent, and an empty one fails every version gate.
        val site = applicationPasswordSite().apply { xmlRpcUrl = XMLRPC_URL }
        val updatedSite = SiteModel()
        whenever(siteXMLRPCClient.fetchSite(site)).thenReturn(updatedSite)

        assertSiteFetched(updatedSite, site)

        verifyNoInteractions(siteWPAPIClient)
    }

    @Test
    fun `fetchSite goes to REST once the software version is known`() = test {
        val site = applicationPasswordSite().apply {
            xmlRpcUrl = XMLRPC_URL
            softwareVersion = "6.7"
        }
        val updatedSite = SiteModel()
        whenever(siteWPAPIClient.fetchWPAPISite(site)).thenReturn(updatedSite)

        assertSiteFetched(updatedSite, site)

        verifyNoInteractions(siteXMLRPCClient)
    }

    @Test
    fun `fetchSite falls through to REST when the XMLRPC version fetch fails`() = test {
        val site = applicationPasswordSite().apply { xmlRpcUrl = XMLRPC_URL }
        val erroredSite = SiteModel().apply { error = BaseNetworkError(PARSE_ERROR) }
        val updatedSite = SiteModel()
        whenever(siteXMLRPCClient.fetchSite(site)).thenReturn(erroredSite)
        whenever(siteWPAPIClient.fetchWPAPISite(site)).thenReturn(updatedSite)

        assertSiteFetched(updatedSite, site)
    }

    @Test
    fun `fetchSite skips the XMLRPC version refresh for a site with no XMLRPC endpoint`() = test {
        val site = applicationPasswordSite()
        val updatedSite = SiteModel()
        whenever(siteWPAPIClient.fetchWPAPISite(site)).thenReturn(updatedSite)

        assertSiteFetched(updatedSite, site)

        verifyNoInteractions(siteXMLRPCClient)
    }

    private fun applicationPasswordSite() = SiteModel().apply {
        setIsWPCom(false)
        apiRestUsernamePlain = "user"
        apiRestPasswordPlain = "app password"
    }

    private suspend fun assertSiteFetchError(site: SiteModel) {
        val onSiteChanged = siteStore.fetchSite(site)

        assertThat(onSiteChanged.rowsAffected).isEqualTo(0)
        assertThat(onSiteChanged.error).isEqualTo(SiteError(GENERIC_ERROR, null))
        // fetchSite reads the stored row to decide which client refreshes it, but an errored
        // fetch must write nothing.
        verify(siteSqlUtils, never()).insertOrUpdateSite(any())
    }

    private suspend fun assertSiteFetched(
        updatedSite: SiteModel,
        site: SiteModel
    ) {
        val rowsChanged = 1
        whenever(siteSqlUtils.insertOrUpdateSite(updatedSite)).thenReturn(rowsChanged)

        val onSiteChanged = siteStore.fetchSite(site)

        assertThat(onSiteChanged.rowsAffected).isEqualTo(rowsChanged)
        assertThat(onSiteChanged.error).isNull()
        verify(siteSqlUtils).insertOrUpdateSite(updatedSite)
    }

    @Test
    fun `fetchSites saves fetched sites to DB and removes absent sites`() = test {
        val payload = FetchSitesPayload(listOf(WPCOM))
        val sitesModel = SitesModel()
        val siteA = SiteModel()
        val siteB = SiteModel()
        sitesModel.sites = listOf(siteA, siteB)
        whenever(siteRestClient.fetchSites(payload.filters, false)).thenReturn(sitesModel)
        whenever(siteSqlUtils.insertOrUpdateSiteReturningId(siteA)).thenReturn(1)
        whenever(siteSqlUtils.insertOrUpdateSiteReturningId(siteB)).thenReturn(1)

        val onSiteChanged = siteStore.fetchSites(payload)

        assertThat(onSiteChanged.rowsAffected).isEqualTo(2)
        assertThat(onSiteChanged.error).isNull()
        val inOrder = inOrder(siteSqlUtils)
        inOrder.verify(siteSqlUtils).insertOrUpdateSiteReturningId(siteA)
        inOrder.verify(siteSqlUtils).insertOrUpdateSiteReturningId(siteB)
        inOrder.verify(siteSqlUtils).removeWPComRestSitesAbsentFromList(postSqlUtils, sitesModel.sites)
    }

    @Test
    fun `fetchSites saves jetpack CP connected sites to DB`() = test {
        val payload = FetchSitesPayload(listOf(WPCOM))
        val sitesModel = SitesModel()
        val siteA = SiteModel().apply {
            siteId = 1
            url = "http://A"
            name = "A"
            description = "A description"
            setIsJetpackCPConnected(false)
        }
        val siteB = SiteModel().apply {
            siteId = 2
            url = "http://B"
            name = "B"
            description = "B description"
            setIsJetpackCPConnected(false)
        }
        val siteC = SiteModel().apply {
            siteId = 3
            url = "http://C"
            name = "C"
            description = "C description"
            activeJetpackConnectionPlugins = "jetpack-boost"
            setIsJetpackCPConnected(true)
        }
        sitesModel.sites = listOf(siteA, siteB)
        sitesModel.jetpackCPSites = listOf(siteC)
        whenever(siteRestClient.fetchSites(payload.filters, false)).thenReturn(sitesModel)
        whenever(siteSqlUtils.insertOrUpdateSiteReturningId(siteA)).thenReturn(1)
        whenever(siteSqlUtils.insertOrUpdateSiteReturningId(siteB)).thenReturn(1)

        siteStore.fetchSites(payload)

        val inOrder = inOrder(jetpackCPConnectedSitesDao)
        inOrder.verify(jetpackCPConnectedSitesDao).deleteAll()
        inOrder.verify(jetpackCPConnectedSitesDao).insert(argWhere { it.size == 1 })
    }

    @Test
    fun `fetchSites doesn't save jetpack CP connected sites to DB`() = test {
        val payload = FetchSitesPayload(listOf(WPCOM))
        val sitesModel = SitesModel()
        val siteA = SiteModel().apply {
            siteId = 1
            url = "http://A"
            name = "A"
            description = "A description"
            setIsJetpackCPConnected(false)
        }
        val siteB = SiteModel().apply {
            siteId = 2
            url = "http://B"
            name = "B"
            description = "B description"
            setIsJetpackCPConnected(false)
        }
        sitesModel.sites = listOf(siteA, siteB)
        whenever(siteRestClient.fetchSites(payload.filters, false)).thenReturn(sitesModel)
        whenever(siteSqlUtils.insertOrUpdateSiteReturningId(siteA)).thenReturn(1)
        whenever(siteSqlUtils.insertOrUpdateSiteReturningId(siteB)).thenReturn(1)

        siteStore.fetchSites(payload)

        verify(jetpackCPConnectedSitesDao, never()).deleteAll()
        verify(jetpackCPConnectedSitesDao, never()).insert(argWhere { it.size == 1 })
    }

    @Test
    fun `fetchSites returns error`() = test {
        val payload = FetchSitesPayload(listOf(WPCOM))
        val sitesModel = SitesModel()
        sitesModel.error = BaseNetworkError(PARSE_ERROR)
        whenever(siteRestClient.fetchSites(payload.filters, false)).thenReturn(sitesModel)

        val onSiteChanged = siteStore.fetchSites(payload)

        assertThat(onSiteChanged.rowsAffected).isEqualTo(0)
        assertThat(onSiteChanged.error).isEqualTo(SiteError(GENERIC_ERROR, null))
        verifyNoInteractions(siteSqlUtils)
    }

    @Test
    fun `creates a new site`() = test {
        val dryRun = false
        val name = "New site"
        val payload = NewSitePayload(name, null, "CZ", "Europe/London", PUBLIC, null, dryRun)
        val newSiteRemoteId: Long = 123
        val url = "new.wp.com"
        val response = NewSiteResponsePayload(newSiteRemoteId, siteUrl = url, dryRun)
        whenever(
                siteRestClient.newSite(
                        name,
                        null,
                        payload.language,
                        payload.timeZoneId,
                        payload.visibility,
                        null,
                        null,
                        null,
                        payload.dryRun
                )
        ).thenReturn(response)

        val result = siteStore.createNewSite(payload)

        assertThat(result.dryRun).isEqualTo(dryRun)
        assertThat(result.newSiteRemoteId).isEqualTo(newSiteRemoteId)
        assertEquals(url, result.url)
    }

    @Test
    fun `fails to create a new site`() = test {
        val dryRun = false
        val payload = NewSitePayload("New site", "CZ", "Europe/London", PUBLIC, dryRun)
        val response = NewSiteResponsePayload()
        val newSiteError = NewSiteError(SITE_NAME_INVALID, "Site name invalid")
        response.error = newSiteError
        whenever(
                siteRestClient.newSite(
                        payload.siteName,
                        null,
                        payload.language,
                        payload.timeZoneId,
                        payload.visibility,
                        null,
                        null,
                        null,
                        payload.dryRun
                )
        ).thenReturn(response)

        val result = siteStore.createNewSite(payload)

        assertThat(result.dryRun).isEqualTo(dryRun)
        assertThat(result.newSiteRemoteId).isEqualTo(0)
        assertThat(result.error).isEqualTo(newSiteError)
    }

    @Test
    fun `fetches post formats for WPCom site`() = test {
        val site = SiteModel()
        site.setIsWPCom(true)
        val postFormatModel = PostFormatModel(123)
        postFormatModel.slug = "Slug"
        postFormatModel.displayName = "Display name"
        postFormatModel.siteId = 123
        val postFormats = listOf(
                postFormatModel
        )
        val payload = FetchedPostFormatsPayload(site, postFormats)
        whenever(siteRestClient.fetchPostFormats(site)).thenReturn(payload)

        assertPostFormatsFetched(site, payload)
    }

    @Test
    fun `fetches post formats for XMLRPC site`() = test {
        val site = SiteModel()
        site.setIsWPCom(false)
        val postFormatModel = PostFormatModel(123)
        postFormatModel.slug = "Slug"
        postFormatModel.displayName = "Display name"
        postFormatModel.siteId = 123
        val postFormats = listOf(
                postFormatModel
        )
        val payload = FetchedPostFormatsPayload(site, postFormats)
        whenever(siteXMLRPCClient.fetchPostFormats(site)).thenReturn(payload)

        assertPostFormatsFetched(site, payload)
    }

    private suspend fun assertPostFormatsFetched(
        site: SiteModel,
        payload: FetchedPostFormatsPayload
    ) {
        val onPostFormatsChanged: OnPostFormatsChanged = siteStore.fetchPostFormats(site)

        assertThat(onPostFormatsChanged.site).isEqualTo(site)
        assertThat(onPostFormatsChanged.error).isNull()
        verify(siteSqlUtils).insertOrReplacePostFormats(payload.site, payload.postFormats)
    }

    @Test
    fun `fails to fetch post formats for WPCom site`() = test {
        val site = SiteModel()
        site.setIsWPCom(true)
        val payload = FetchedPostFormatsPayload(site, emptyList())
        payload.error = PostFormatsError(INVALID_SITE, "Invalid site")
        whenever(siteRestClient.fetchPostFormats(site)).thenReturn(payload)

        assertPostFormatsFetchFailed(site, payload)
    }

    @Test
    fun `fails to fetch post formats from XMLRPC`() = test {
        val site = SiteModel()
        site.setIsWPCom(false)
        val payload = FetchedPostFormatsPayload(site, emptyList())
        payload.error = PostFormatsError(INVALID_SITE, "Invalid site")
        whenever(siteXMLRPCClient.fetchPostFormats(site)).thenReturn(payload)

        assertPostFormatsFetchFailed(site, payload)
    }

    private suspend fun assertPostFormatsFetchFailed(
        site: SiteModel,
        payload: FetchedPostFormatsPayload
    ) {
        val onPostFormatsChanged: OnPostFormatsChanged = siteStore.fetchPostFormats(site)

        assertThat(onPostFormatsChanged.site).isEqualTo(site)
        assertThat(onPostFormatsChanged.error).isEqualTo(payload.error)
        verifyNoInteractions(siteSqlUtils)
    }

    @Test
    fun `hasSiteAccessedViaWPAPI returns true when WPAPI sites exist`() {
        val wpapiSite = SiteModel().apply {
            origin = SiteModel.ORIGIN_WPAPI
        }
        whenever(siteSqlUtils.sitesAccessedViaWPAPI).thenReturn(listOf(wpapiSite))

        assertThat(siteStore.hasSiteAccessedViaWPAPI()).isTrue()
    }

    @Test
    fun `hasSiteAccessedViaWPAPI returns false when no WPAPI sites exist`() {
        whenever(siteSqlUtils.sitesAccessedViaWPAPI).thenReturn(emptyList())

        assertThat(siteStore.hasSiteAccessedViaWPAPI()).isFalse()
    }

    @Test
    fun `fetchSiteWPAPIFromApplicationPassword sets AP credentials and wpApiRestUrl`() =
        test {
            // Given
            val payload =
                SiteStore.RefreshSitesXMLRPCApplicationPasswordCredentialsPayload(
                    username = "appUser",
                    password = "appPass",
                    url = "https://example.com",
                    apiRootUrl = "https://example.com/wp-json/",
                )
            val fetchedSite = SiteModel().apply {
                name = "Test Site"
                origin = SiteModel.ORIGIN_WPAPI
                url = "https://example.com"
                apiRestUsernamePlain = "appUser"
                apiRestPasswordPlain = "appPass"
            }
            whenever(siteWPAPIClient.fetchWPAPISite(any<SiteStore.FetchWPAPISitePayload>(), anyOrNull()))
                .thenReturn(fetchedSite)
            whenever(siteSqlUtils.insertOrUpdateSite(fetchedSite))
                .thenReturn(1)

            // When
            val result =
                siteStore.fetchSiteWPAPIFromApplicationPassword(payload)

            // Then
            assertThat(result.isError).isFalse()
            assertThat(fetchedSite.username).isNullOrEmpty()
            assertThat(fetchedSite.password).isNullOrEmpty()
            assertThat(fetchedSite.apiRestUsernamePlain)
                .isEqualTo("appUser")
            assertThat(fetchedSite.apiRestPasswordPlain)
                .isEqualTo("appPass")
            assertThat(fetchedSite.wpApiRestUrl)
                .isEqualTo("https://example.com/wp-json/")
            // updateSite's full-row write skips WP_API_REST_URL, so the discovered URL must be persisted
            // via the URL-keyed targeted writer (the fetched site has no local id to key on).
            verify(siteSqlUtils).updateWpApiRestUrlForWPAPISite(
                "https://example.com",
                "https://example.com/wp-json/"
            )
            verify(siteSqlUtils).updateApplicationPasswordCredentialsForWPAPISite(
                "https://example.com",
                "appUser",
                "appPass"
            )
        }

    @Test
    fun `fetchSitesXmlRpcFromApplicationPassword falls back to WPAPI without persisting an xmlrpc endpoint`() =
        test {
            org.mockito.Mockito.mockStatic(
                org.wordpress.android.util.AppLog::class.java
            ).use {
                // Given: the XML-RPC fetch against the conventional (unverified) xmlrpc.php endpoint fails.
                // Since payload.url is only a guess, the fallback must NOT persist it — the My Site card's
                // rediscovery verifies and persists a real endpoint later, and it only runs while xmlRpcUrl
                // is empty. This holds for any error, transient (429) or definitive.
                val payload =
                    SiteStore.RefreshSitesXMLRPCApplicationPasswordCredentialsPayload(
                        username = "appUser",
                        password = "appPass",
                        url = "https://example.com/xmlrpc.php",
                        apiRootUrl = "https://example.com/wp-json/",
                    )
                val erroredSites = SitesModel().apply { error = BaseNetworkError(PARSE_ERROR) }
                whenever(
                    siteXMLRPCClient.fetchSitesFromApplicationPassword(
                        payload.url,
                        payload.apiRootUrl,
                        payload.username,
                        payload.password
                    )
                ).thenReturn(erroredSites)
                val fetchedSite = SiteModel().apply {
                    name = "Test Site"
                    origin = SiteModel.ORIGIN_WPAPI
                    url = "https://example.com"
                }
                whenever(siteWPAPIClient.fetchWPAPISite(any<SiteStore.FetchWPAPISitePayload>(), anyOrNull()))
                    .thenReturn(fetchedSite)
                whenever(siteSqlUtils.insertOrUpdateSite(fetchedSite)).thenReturn(1)

                // When
                val result = siteStore.fetchSitesXmlRpcFromApplicationPassword(payload)

                // Then: the site is stored via WPAPI, but no xmlrpc endpoint is recorded (rediscovery heals it).
                assertThat(result.isError).isFalse()
                verify(siteSqlUtils, never()).updateXmlRpcUrlForWPAPISite(any(), any())
            }
        }

    @Test
    fun `updateApplicationPassword persists credentials and wpApiRestUrl via targeted writers`() {
        val existing = SiteModel().apply {
            id = 3
            url = "https://selfhosted.test"
        }
        whenever(siteSqlUtils.getSitesWithLocalId(3)).thenReturn(listOf(existing))
        val incoming = SiteModel().apply {
            id = 3
            apiRestUsernamePlain = "user"
            apiRestPasswordPlain = "pass"
            wpApiRestUrl = "https://selfhosted.test/wp-json/"
        }

        siteStore.onAction(SiteActionBuilder.newUpdateApplicationPasswordAction(incoming))

        verify(siteSqlUtils).updateApplicationPasswordCredentials(3, "user", "pass")
        verify(siteSqlUtils).updateWpApiRestUrl(3, "https://selfhosted.test/wp-json/")
    }

    @Test
    fun `removeApplicationPassword clears credentials and wpApiRestUrl via targeted writers`() {
        val existing = SiteModel().apply {
            id = 4
            url = "https://selfhosted.test"
            wpApiRestUrl = "https://selfhosted.test/wp-json/"
        }
        whenever(siteSqlUtils.getSitesWithLocalId(4)).thenReturn(listOf(existing))
        siteStore.onAction(
            SiteActionBuilder.newRemoveApplicationPasswordAction(SiteModel().apply { id = 4 })
        )

        verify(siteSqlUtils).clearApplicationPasswordCredentials(4)
        verify(siteSqlUtils).clearWpApiRestUrl(4)
    }

    @Test
    fun `clearApplicationPasswordColumns clears credentials but preserves wpApiRestUrl`() {
        val existing = SiteModel().apply {
            id = 8
            url = "https://selfhosted.test"
        }
        whenever(siteSqlUtils.getSitesWithLocalId(8)).thenReturn(listOf(existing))
        siteStore.applicationPasswordsManagerProvider = Provider { mock<ApplicationPasswordsManager>() }

        siteStore.deleteStoredApplicationPasswordCredentials(SiteModel().apply { id = 8 })

        verify(siteSqlUtils).clearApplicationPasswordCredentials(8)
        verify(siteSqlUtils, never()).clearWpApiRestUrl(8)
    }

    @Test
    fun `persistXmlRpcUrl writes the column via the targeted writer`() = test {
        siteStore.persistXmlRpcUrl(9, "https://selfhosted.test/xmlrpc.php")

        verify(siteSqlUtils).updateXmlRpcUrl(9, "https://selfhosted.test/xmlrpc.php")
    }

    @Test
    fun `createOrUpdateSites persists app-password credentials on the stamped row`() {
        val appPwSite = SiteModel().apply {
            selfHostedSiteId = 77L
            url = "https://selfhosted.test"
            xmlRpcUrl = "https://selfhosted.test/xmlrpc.php"
            origin = SiteModel.ORIGIN_XMLRPC
            apiRestUsernamePlain = "user"
            apiRestPasswordPlain = "pass"
            wpApiRestUrl = "https://selfhosted.test/wp-json/"
        }
        whenever(siteSqlUtils.insertOrUpdateSiteReturningId(any())).thenReturn(9)

        siteStore.onAction(SiteActionBuilder.newUpdateSitesAction(SitesModel(listOf(appPwSite))))

        verify(siteSqlUtils).updateApplicationPasswordCredentials(9, "user", "pass")
        verify(siteSqlUtils).updateWpApiRestUrl(9, "https://selfhosted.test/wp-json/")
    }

    @Test
    fun `fetchPostFormats returns empty list for WPAPI site without crashing`() =
        test {
            org.mockito.Mockito.mockStatic(
                org.wordpress.android.util.AppLog::class.java
            ).use {
                // Given
                val site = SiteModel().apply {
                    origin = SiteModel.ORIGIN_WPAPI
                    // xmlRpcUrl is intentionally null
                }

                // When
                val result = siteStore.fetchPostFormats(site)

                // Then - no crash, empty post formats
                assertThat(result.isError).isFalse()
                verifyNoInteractions(siteXMLRPCClient)
                verifyNoInteractions(siteRestClient)
            }
        }

    @Test
    fun `sitesAccessedViaWPAPICount returns correct count`() {
        val wpapiSites = listOf(
            SiteModel().apply { origin = SiteModel.ORIGIN_WPAPI },
            SiteModel().apply { origin = SiteModel.ORIGIN_WPAPI }
        )
        whenever(siteSqlUtils.sitesAccessedViaWPAPI).thenReturn(wpapiSites)

        assertThat(siteStore.sitesAccessedViaWPAPICount).isEqualTo(2)
    }

    companion object {
        private const val WPCOM_BLOG_ID = 1234L
        private const val XMLRPC_URL = "https://site.com/xmlrpc.php"
    }
}
