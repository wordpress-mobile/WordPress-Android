package org.wordpress.android.ui.mysite.cards.applicationpassword

import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import kotlin.test.assertNotNull

private const val TEST_URL = "https://www.test.com"
private const val TEST_SITE_NAME = "My Site"
private const val TEST_SITE_ID = 1
private const val TEST_SITE_ICON = "http://site.com/icon.jpg"
private const val TEST_URL_AUTH = "https://www.test.com/auth"
private const val TEST_URL_AUTH_SUFFIX = "?app_name=android-jetpack-client&success_url=callback://callback"

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ApplicationPasswordViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper

    @Mock
    lateinit var siteStore: SiteStore

    @Mock
    lateinit var experimentalFeatures: ExperimentalFeatures

    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    private lateinit var siteTest: SiteModel

    private var applicationPasswordCard: MySiteCardAndItem.Card? = null

    private lateinit var applicationPasswordViewModelSlice: ApplicationPasswordViewModelSlice

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        applicationPasswordViewModelSlice = ApplicationPasswordViewModelSlice(
            applicationPasswordLoginHelper,
            siteStore,
            experimentalFeatures,
            appLogWrapper
        ).apply {
            initialize(testScope())
            whenever(experimentalFeatures.isEnabled(any())).thenReturn(true)
        }
        siteTest = SiteModel().apply {
            id = TEST_SITE_ID
            url = TEST_URL
            name = TEST_SITE_NAME
            iconUrl = TEST_SITE_ICON
            siteId = TEST_SITE_ID.toLong()
        }

        applicationPasswordCard = null
        applicationPasswordViewModelSlice.uiModel.observeForever { card ->
            applicationPasswordCard = card
        }
    }

    @Test
    fun `given proper site, when api discovery is success, then add the application password card`() = runTest {
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(eq(TEST_URL)))
            .thenReturn("$TEST_URL_AUTH$TEST_URL_AUTH_SUFFIX")

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNotNull(applicationPasswordCard)
        verify(applicationPasswordLoginHelper).getAuthorizationUrlComplete(eq(TEST_URL))
    }

    @Test
    fun `given login scenario, when api discovery is empty, then show no card`() = runTest {
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(eq(TEST_URL)))
            .thenReturn("")

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNull(applicationPasswordCard)
        verify(applicationPasswordLoginHelper).getAuthorizationUrlComplete(eq(TEST_URL))
    }

    @Test
    fun `given site already authenticated, when calling api discovery, then show no card`() = runTest {
        whenever(siteStore.sites)
        .thenReturn(
            listOf(
                SiteModel().apply {
                    id = siteTest.id
                    apiRestUsernamePlain = "user"
                    apiRestPasswordPlain = "password"
                }
            )
        )

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNull(applicationPasswordCard)
        verify(siteStore).sites
        verify(applicationPasswordLoginHelper, times(0)).getAuthorizationUrlComplete(any())
    }
}
