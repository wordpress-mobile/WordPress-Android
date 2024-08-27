package org.wordpress.android.fluxc.store.blaze

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeAdForecast
import org.wordpress.android.fluxc.model.blaze.BlazeAdSuggestion
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import org.wordpress.android.fluxc.model.blaze.BlazePaymentMethod
import org.wordpress.android.fluxc.model.blaze.BlazePaymentMethodUrls
import org.wordpress.android.fluxc.model.blaze.BlazePaymentMethods
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingDevice
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingLanguage
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingLocation
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingTopic
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaign
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignListResponse
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsError
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsFetchedPayload
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsRestClient.Companion.DEFAULT_PER_PAGE
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsUtils
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCreationRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.CampaignImage
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao.BlazeCampaignEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingDeviceEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingLanguageEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingTopicEntity
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.util.Date
import kotlin.time.Duration.Companion.days

const val SITE_ID = 1L

/* Campaign */
private const val CAMPAIGN_ID = "1234"
private const val TITLE = "title"
private const val IMAGE_URL = "imageUrl"
private const val CREATED_AT = "2023-06-02T00:00:00.000Z"
private const val DURATION_IN_DAYS = 10
private const val UI_STATUS = "rejected"
private const val IMPRESSIONS = 0L
private const val CLICKS = 0L
private const val TOTAL_BUDGET = 100.0
private const val SPENT_BUDGET = 0.0
private const val TARGET_URN = "urn:wpcom:post:199247490:9"

private const val SKIP = 0
private const val TOTAL_ITEMS = 1

private val CAMPAIGN_IMAGE = CampaignImage(
    height = 100f,
    width = 100f,
    mimeType = "image/jpeg",
    url = IMAGE_URL
)

private val CAMPAIGN_RESPONSE = BlazeCampaign(
    id = CAMPAIGN_ID,
    image = CAMPAIGN_IMAGE,
    targetUrl = "https://example.com",
    textSnippet = "Text snippet",
    siteName = TITLE,
    clicks = CLICKS,
    impressions = IMPRESSIONS,
    spentBudget = SPENT_BUDGET,
    totalBudget = TOTAL_BUDGET,
    durationDays = DURATION_IN_DAYS,
    startTime = CREATED_AT,
    targetUrn = TARGET_URN,
    status = UI_STATUS,
    isEvergreen = false
)

private val BLAZE_CAMPAIGNS_RESPONSE = BlazeCampaignListResponse(
    campaigns = listOf(CAMPAIGN_RESPONSE),
    skipped = SKIP,
    totalCount = TOTAL_ITEMS,
)

private val BLAZE_CAMPAIGN_ENTITY = BlazeCampaignEntity(
    siteId = SITE_ID,
    campaignId = CAMPAIGN_ID,
    title = TITLE,
    imageUrl = IMAGE_URL,
    startTime = BlazeCampaignsUtils.stringToDate(CREATED_AT),
    durationInDays = DURATION_IN_DAYS,
    uiStatus = UI_STATUS,
    impressions = IMPRESSIONS,
    clicks = CLICKS,
    targetUrn = TARGET_URN,
    totalBudget = TOTAL_BUDGET,
    spentBudget = SPENT_BUDGET,
    isEndlessCampaign = false
)
private val BLAZE_CAMPAIGNS_MODEL = BlazeCampaignsModel(
    campaigns = listOf(BLAZE_CAMPAIGN_ENTITY.toDomainModel()),
    skipped = SKIP,
    totalItems = TOTAL_ITEMS,
)

class BlazeCampaignsStoreTest {
    private val blazeCampaignsRestClient: BlazeCampaignsRestClient = mock()
    private val creationRestClient: BlazeCreationRestClient = mock()
    private val blazeCampaignsDao: BlazeCampaignsDao = mock()
    private val blazeTargetingDao: BlazeTargetingDao = mock()
    private val siteModel = SiteModel().apply { siteId = SITE_ID }

    private lateinit var store: BlazeCampaignsStore

    private val successResponse = BLAZE_CAMPAIGNS_RESPONSE
    private val errorResponse = BlazeCampaignsError(type = GENERIC_ERROR)

    @Before
    fun setUp() {
        store = BlazeCampaignsStore(
            campaignsRestClient = blazeCampaignsRestClient,
            creationRestClient = creationRestClient,
            campaignsDao = blazeCampaignsDao,
            targetingDao = blazeTargetingDao,
            coroutineEngine = initCoroutineEngine()
        )
    }

    @Test
    fun `given success, when fetch blaze campaigns is triggered, then values are inserted`() =
        test {
            val payload = BlazeCampaignsFetchedPayload(successResponse)
            whenever(
                blazeCampaignsRestClient.fetchBlazeCampaigns(
                    siteModel.siteId, SKIP, DEFAULT_PER_PAGE, "en", null
                )
            ).thenReturn(payload)

            store.fetchBlazeCampaigns(siteModel, SKIP)

            verify(blazeCampaignsDao).insertCampaigns(
                SITE_ID,
                BLAZE_CAMPAIGNS_MODEL
            )
        }

    @Test
    fun `given error, when fetch blaze campaigns is triggered, then error result is returned`() =
        test {
            whenever(
                blazeCampaignsRestClient.fetchBlazeCampaigns(
                    any(), any(), any(), any(),  eq(null)
                )
            ).thenReturn(
                BlazeCampaignsFetchedPayload(errorResponse)
            )
            val result = store.fetchBlazeCampaigns(siteModel)

            verifyNoInteractions(blazeCampaignsDao)
            assertThat(result.model).isNull()
            assertEquals(GENERIC_ERROR, result.error.type)
            assertNull(result.error.message)
        }

    @Test
    fun `given unmatched site, when get is triggered, then empty campaigns list returned`() = test {
        whenever(blazeCampaignsDao.getCachedCampaigns(SITE_ID)).thenReturn(emptyList())

        val campaigns = store.getBlazeCampaigns(siteModel)

        assertThat(campaigns).isNotNull
        assertThat(campaigns).isEmpty()
    }

    @Test
    fun `given matched site, when get recent is triggered, then campaign is returned`() = test {
        whenever(blazeCampaignsDao.getMostRecentCampaignForSite(SITE_ID)).thenReturn(
            BLAZE_CAMPAIGN_ENTITY
        )

        val result = store.getMostRecentBlazeCampaign(siteModel)

        assertThat(result).isNotNull
        assertEquals(result?.campaignId, CAMPAIGN_ID)
        assertEquals(result?.title, TITLE)
        assertEquals(result?.imageUrl, IMAGE_URL)
        assertEquals(result?.startTime, BlazeCampaignsUtils.stringToDate(CREATED_AT))
        assertEquals(result?.durationInDays, DURATION_IN_DAYS)
        assertEquals(result?.uiStatus, UI_STATUS)
        assertEquals(result?.impressions, IMPRESSIONS)
        assertEquals(result?.clicks, CLICKS)
        assertEquals(result?.targetUrn, TARGET_URN)
        assertEquals(result?.totalBudget, TOTAL_BUDGET)
        assertEquals(result?.spentBudget, SPENT_BUDGET)
    }

    @Test
    fun `given unmatched site, when get recent is triggered, then campaign is returned`() = test {
        val result = store.getMostRecentBlazeCampaign(siteModel)

        assertThat(result).isNull()
    }

    @Test
    fun `when fetching targeting locations, then locations are returned`() = test {
        whenever(creationRestClient.fetchTargetingLocations(any(), any(), any())).thenReturn(
            BlazeCreationRestClient.BlazePayload(
                List(10) {
                    BlazeTargetingLocation(
                        id = it.toLong(),
                        name = "location",
                        type = "city",
                        parent = null
                    )
                }
            )
        )

        val locations = store.fetchBlazeTargetingLocations(siteModel, "query")

        assertThat(locations.isError).isFalse()
        assertThat(locations.model).isNotNull
        assertThat(locations.model?.size).isEqualTo(10)
    }

    @Test
    fun `when fetching targeting topics, then persist data in DB`() = test {
        whenever(creationRestClient.fetchTargetingTopics(any(), any())).thenReturn(
            BlazeCreationRestClient.BlazePayload(
                List(10) {
                    BlazeTargetingTopic(
                        id = it.toString(),
                        description = "Topic $it"
                    )
                }
            )
        )

        store.fetchBlazeTargetingTopics(siteModel)

        verify(blazeTargetingDao).replaceTopics(any())
    }

    @Test
    fun `when observing targeting topics, then return data from DB`() = test {
        whenever(blazeTargetingDao.observeTopics(any())).thenReturn(
            flowOf(
                List(10) {
                    BlazeTargetingTopicEntity(
                        id = it.toString(),
                        description = "Topic $it",
                        locale = "en"
                    )
                }
            )
        )

        val topics = store.observeBlazeTargetingTopics().first()

        assertThat(topics).isNotNull
        assertThat(topics.size).isEqualTo(10)
    }

    @Test
    fun `when fetching targeting languages, then persist data in DB`() = test {
        whenever(creationRestClient.fetchTargetingLanguages(any(), any())).thenReturn(
            BlazeCreationRestClient.BlazePayload(
                List(10) {
                    BlazeTargetingLanguage(
                        id = it.toString(),
                        name = "Language $it"
                    )
                }
            )
        )

        store.fetchBlazeTargetingLanguages(siteModel)

        verify(blazeTargetingDao).replaceLanguages(any())
    }

    @Test
    fun `when observing targeting languages, then return data from DB`() = test {
        whenever(blazeTargetingDao.observeLanguages(any())).thenReturn(
            flowOf(
                List(10) {
                    BlazeTargetingLanguageEntity(
                        id = it.toString(),
                        name = "Language $it",
                        locale = "en"
                    )
                }
            )
        )

        val languages = store.observeBlazeTargetingLanguages().first()

        assertThat(languages).isNotNull
        assertThat(languages.size).isEqualTo(10)
    }

    @Test
    fun `when fetching targeting devices, then persist data in DB`() = test {
        whenever(creationRestClient.fetchTargetingDevices(any(), any())).thenReturn(
            BlazeCreationRestClient.BlazePayload(
                List(10) {
                    BlazeTargetingDevice(
                        id = it.toString(),
                        name = "Device $it"
                    )
                }
            )
        )

        store.fetchBlazeTargetingDevices(siteModel)

        verify(blazeTargetingDao).replaceDevices(any())
    }

    @Test
    fun `when observing targeting devices, then return data from DB`() = test {
        whenever(blazeTargetingDao.observeDevices(any())).thenReturn(
            flowOf(
                List(10) {
                    BlazeTargetingDeviceEntity(
                        id = it.toString(),
                        name = "Device $it",
                        locale = "en"
                    )
                }
            )
        )

        val devices = store.observeBlazeTargetingDevices().first()

        assertThat(devices).isNotNull
        assertThat(devices.size).isEqualTo(10)
    }

    @Test
    fun `when fetching targeting ad suggestions, then return data successfully`() = test {
        val suggestions = List(10) {
            BlazeAdSuggestion(
                tagLine = it.toString(),
                description = "Ad $it"
            )
        }

        whenever(creationRestClient.fetchAdSuggestions(any(), any())).thenReturn(
            BlazeCreationRestClient.BlazePayload(suggestions)
        )

        val suggestionsResult = store.fetchBlazeAdSuggestions(siteModel, 1L)

        assertThat(suggestionsResult.isError).isFalse()
        assertThat(suggestionsResult.model).isEqualTo(suggestions)
    }

    @Test
    fun `when fetching ad forecast, then return data successfully`() = test {
        val forecast = BlazeAdForecast(
            minImpressions = 100,
            maxImpressions = 200
        )

        whenever(
            creationRestClient.fetchAdForecast(
                site = any(),
                startDate = any(),
                endDate = any(),
                totalBudget = any(),
                timeZoneId = any(),
                targetingParameters = anyOrNull()
            )
        ).thenReturn(
            BlazeCreationRestClient.BlazePayload(forecast)
        )

        val forecastResult = store.fetchBlazeAdForecast(
            siteModel,
            Date(),
            Date(System.currentTimeMillis() + 7.days.inWholeMilliseconds),
            100.0,
        )

        assertThat(forecastResult.isError).isFalse()
        assertThat(forecastResult.model).isEqualTo(forecast)
    }

    @Test
    fun `when fetching payment methods, then return data successfully`() = test {
        val paymentMethods = BlazePaymentMethods(
            savedPaymentMethods = listOf(
                BlazePaymentMethod(
                    id = "payment-method-id",
                    name = "Visa **** 4689",
                    info = BlazePaymentMethod.PaymentMethodInfo.CreditCardInfo(
                        lastDigits = "4689",
                        expMonth = 12,
                        expYear = 2025,
                        type = "Visa",
                        nickname = "",
                        cardHolderName = "John Doe"
                    )
                ),
                BlazePaymentMethod(
                    id = "payment-method-id-2",
                    name = "MasterCard **** 1234",
                    info = BlazePaymentMethod.PaymentMethodInfo.CreditCardInfo(
                        lastDigits = "1234",
                        expMonth = 12,
                        expYear = 2025,
                        type = "MasterCard",
                        nickname = "",
                        cardHolderName = "John Doe"
                    )
                )
            ),
            addPaymentMethodUrls = BlazePaymentMethodUrls(
                formUrl = "https://example.com/blaze-pm-add",
                successUrl = "https://example.com/blaze-pm-success",
                idUrlParameter = "pmid"
            )
        )

        whenever(creationRestClient.fetchPaymentMethods(any())).thenReturn(
            BlazeCreationRestClient.BlazePayload(paymentMethods)
        )

        val paymentMethodsResult = store.fetchBlazePaymentMethods(siteModel)

        assertThat(paymentMethodsResult.isError).isFalse()
        assertThat(paymentMethodsResult.model).isEqualTo(paymentMethods)
    }

    @Test
    fun `when creating a campaign, then persist it to the DB and return result`() = test {
        val campaign = BlazeCampaignModel(
            campaignId = CAMPAIGN_ID,
            title = TITLE,
            imageUrl = IMAGE_URL,
            startTime = BlazeCampaignsUtils.stringToDate(CREATED_AT),
            durationInDays = DURATION_IN_DAYS,
            uiStatus = UI_STATUS,
            impressions = IMPRESSIONS,
            clicks = CLICKS,
            targetUrn = TARGET_URN,
            totalBudget = TOTAL_BUDGET,
            spentBudget = SPENT_BUDGET,
            isEndlessCampaign = false
        )

        whenever(creationRestClient.createCampaign(any(), any())).thenReturn(
            BlazeCreationRestClient.BlazePayload(campaign)
        )

        val result = store.createCampaign(siteModel, mock())

        assertThat(result.isError).isFalse()
        assertThat(result.model).isEqualTo(campaign)
    }
}
