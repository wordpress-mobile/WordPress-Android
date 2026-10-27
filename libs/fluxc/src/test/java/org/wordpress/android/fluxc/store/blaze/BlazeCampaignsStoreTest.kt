package org.wordpress.android.fluxc.store.blaze

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaign
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignListResponse
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsError
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsFetchedPayload
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsRestClient.Companion.DEFAULT_PER_PAGE
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsUtils
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.CampaignImage
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao.BlazeCampaignEntity
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

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
    private val blazeCampaignsDao: BlazeCampaignsDao = mock()
    private val siteModel = SiteModel().apply { siteId = SITE_ID }

    private lateinit var store: BlazeCampaignsStore

    private val successResponse = BLAZE_CAMPAIGNS_RESPONSE
    private val errorResponse = BlazeCampaignsError(type = GENERIC_ERROR)

    @Before
    fun setUp() {
        store = BlazeCampaignsStore(
            campaignsRestClient = blazeCampaignsRestClient,
            campaignsDao = blazeCampaignsDao,
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
}
