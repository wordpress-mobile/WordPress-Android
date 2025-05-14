package org.wordpress.android.ui.mysite.cards.applicationpassword

import android.content.SharedPreferences
import com.sun.jna.Pointer
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinksItem.QuickLinkItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.viewmodel.Event
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import rs.wordpress.api.kotlin.WpLoginClient
import uniffi.wp_api.AutoDiscoveryAttemptSuccess
import uniffi.wp_api.ParseUrlException
import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.WpApiDetails
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val TEST_URL = "https://www.test.com"
private const val TEST_SITE_NAME = "My Site"
private const val TEST_SITE_ID = 1
private const val TEST_SITE_ICON = "http://site.com/icon.jpg"
private const val TEST_URL_AUTH = "https://www.test.com/auth"
private const val TEST_URL_AUTH_SUFFIX = "?app_name=android-jetpack-client&success_url=callback://callback"
private const val SITE_ALREADY_OPENED_PREFIX = "site_already_opened_"
private const val DISMISSED_AUTHORIZATION_DIALOG_PREFIX = "dismissed_authorization_dialog_"
private const val FIRST_TIME_SITE_OPENED_SP_TAG = "$SITE_ALREADY_OPENED_PREFIX$TEST_URL"
private const val SKIPPED_SITE_SP_TAG = "$DISMISSED_AUTHORIZATION_DIALOG_PREFIX$TEST_URL"

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ApplicationPasswordViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var wpLoginClient: WpLoginClient

    @Mock
    lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper

    @Mock
    lateinit var wpApiDetails: WpApiDetails

    @Mock
    lateinit var authParsedUrl: ParsedUrl

    @Mock
    lateinit var emptyAuthParsedUrl: ParsedUrl

    @Mock
    lateinit var siteSqlUtils: SiteSqlUtils

    private lateinit var siteTest: SiteModel

    private var applicationPasswordCard: MySiteCardAndItem.Card? = null

    private lateinit var applicationPasswordViewModelSlice: ApplicationPasswordViewModelSlice

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        applicationPasswordViewModelSlice = ApplicationPasswordViewModelSlice(
            testDispatcher(),
            applicationPasswordLoginHelper,
            siteSqlUtils,
            wpLoginClient,
        ).apply {
            initialize(testScope())
        }
        siteTest = SiteModel().apply {
            id = TEST_SITE_ID
            url = TEST_URL
            name = TEST_SITE_NAME
            iconUrl = TEST_SITE_ICON
            siteId = TEST_SITE_ID.toLong()
        }

        whenever(authParsedUrl.url()).thenReturn(TEST_URL_AUTH)
        whenever(emptyAuthParsedUrl.url()).thenReturn("")

        applicationPasswordCard = null
        applicationPasswordViewModelSlice.uiModel.observeForever { card ->
            applicationPasswordCard = card
        }
    }

    @Test
    fun `given proper site, when api discovery is success, then add the application password card`() = runTest {
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL)))
            .thenReturn(
                ApiDiscoveryResult.Success(
                    AutoDiscoveryAttemptSuccess(
                        ParsedUrl(Pointer.createConstant(1)),
                        ParsedUrl(Pointer.createConstant(1)),
                        wpApiDetails,
                        authParsedUrl
                    )
                )
            )
        whenever(applicationPasswordLoginHelper.appendParamsToRestAuthorizationUrl(any()))
            .thenReturn("$TEST_URL_AUTH$TEST_URL_AUTH_SUFFIX")

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNotNull(applicationPasswordCard)
        verify(wpLoginClient).apiDiscovery(eq(TEST_URL))
    }

    @Test
    fun `given login scenario, when api discovery is fails, then show no card`() = runTest {
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL))).doThrow(RuntimeException("API discovery failed"))

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNull(applicationPasswordCard)
        verify(wpLoginClient).apiDiscovery(eq(TEST_URL))
    }

    @Test
    fun `given login scenario, when api discovery is empty, then show no card`() = runTest {
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL)))
            .thenReturn(
                ApiDiscoveryResult.Success(
                    AutoDiscoveryAttemptSuccess(
                        ParsedUrl(Pointer.createConstant(1)),
                        ParsedUrl(Pointer.createConstant(1)),
                        wpApiDetails,
                        emptyAuthParsedUrl
                    )
                )
            )

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNull(applicationPasswordCard)
        verify(wpLoginClient).apiDiscovery(eq(TEST_URL))
    }

    @Test
    fun `given site already authenticated, when calling api discovery, then show no card`() = runTest {
        whenever(siteSqlUtils.getSiteWithLocalId(eq(siteTest.localId()))
        ).thenReturn(SiteModel().apply {
            apiRestUsername = "user"
            apiRestPassword = "password"
        })

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNull(applicationPasswordCard)
        verify(siteSqlUtils).getSiteWithLocalId(eq(siteTest.localId()))
        verify(wpLoginClient, times(0)).apiDiscovery(any())
    }

    @Test
    fun `given site url cached, when calling api discovery, then show card but don't call api discovery`() = runTest {
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL)))
            .thenReturn(
                ApiDiscoveryResult.Success(
                    AutoDiscoveryAttemptSuccess(
                        ParsedUrl(Pointer.createConstant(1)),
                        ParsedUrl(Pointer.createConstant(1)),
                        wpApiDetails,
                        authParsedUrl
                    )
                )
            )
        whenever(applicationPasswordLoginHelper.appendParamsToRestAuthorizationUrl(any()))
            .thenReturn("$TEST_URL_AUTH$TEST_URL_AUTH_SUFFIX")

        // Add site tp the cache
        applicationPasswordViewModelSlice.buildCard(siteTest)

        // call function again
        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNotNull(applicationPasswordCard)
        verify(siteSqlUtils).getSiteWithLocalId(eq(siteTest.localId()))
        verify(wpLoginClient, times(1)).apiDiscovery(any()) // only called once
    }

    @Test
    fun `given site with no url cached, when calling api discovery, then don't show card and don't call api discovery`() = runTest {
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL)))
            .thenReturn(
                ApiDiscoveryResult.Success(
                    AutoDiscoveryAttemptSuccess(
                        ParsedUrl(Pointer.createConstant(1)),
                        ParsedUrl(Pointer.createConstant(1)),
                        wpApiDetails,
                        emptyAuthParsedUrl
                    )
                )
            )
        whenever(applicationPasswordLoginHelper.appendParamsToRestAuthorizationUrl(any()))
            .thenReturn("")

        // Add site tp the cache
        applicationPasswordViewModelSlice.buildCard(siteTest)

        // call function again
        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNull(applicationPasswordCard)
        verify(siteSqlUtils).getSiteWithLocalId(eq(siteTest.localId()))
        verify(wpLoginClient, times(1)).apiDiscovery(any()) // only called once
    }


    @Test
    fun `given login scenario, when api discovery is failed, then show nothing`() = runTest {
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL)))
            .thenReturn(
                ApiDiscoveryResult.FailureParseSiteUrl(
                    ParseUrlException.Generic("")
                )
            )

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNull(applicationPasswordCard)
        verify(wpLoginClient).apiDiscovery(eq(TEST_URL))
    }
}
