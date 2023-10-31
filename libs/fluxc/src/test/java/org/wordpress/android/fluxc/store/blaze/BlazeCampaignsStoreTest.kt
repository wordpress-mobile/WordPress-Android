package org.wordpress.android.fluxc.store.blaze

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsError
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsFetchedPayload
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsUtils
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.Campaign
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.CampaignStats
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.ContentConfig
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao.BlazeCampaignEntity
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

const val SITE_ID = 1L

/* Campaign */
const val CAMPAIGN_ID = 1
const val TITLE = "title"
const val IMAGE_URL = "imageUrl"
const val CREATED_AT = "2023-06-02T00:00:00.000Z"
const val END_DATE = "2023-06-02T00:00:00.000Z"
const val UI_STATUS = "rejected"
const val BUDGET_CENTS = 5000L
const val IMPRESSIONS = 0L
const val CLICKS = 0L

const val PAGE = 1
const val TOTAL_ITEMS = 1
const val TOTAL_PAGES = 1


private val CONTENT_CONFIG_RESPONSE = ContentConfig(
    title = TITLE,
    imageUrl = IMAGE_URL
)

private val CONTENT_CAMPAIGN_STATS = CampaignStats(
    impressionsTotal = 0,
    clicksTotal = 0
)

private val CAMPAIGN_RESPONSE = Campaign(
    campaignId = CAMPAIGN_ID,
    createdAt = CREATED_AT,
    endDate = END_DATE,
    budgetCents = BUDGET_CENTS,
    uiStatus = UI_STATUS,
    contentConfig = CONTENT_CONFIG_RESPONSE,
    campaignStats = CONTENT_CAMPAIGN_STATS
)

private val BLAZE_CAMPAIGNS_RESPONSE = BlazeCampaignsResponse(
    campaigns = listOf(CAMPAIGN_RESPONSE),
    page = PAGE,
    totalItems = TOTAL_ITEMS,
    totalPages = TOTAL_PAGES
)

private val BLAZE_CAMPAIGN_MODEL = BlazeCampaignEntity(
    siteId = SITE_ID,
    campaignId = CAMPAIGN_ID,
    title = TITLE,
    imageUrl = IMAGE_URL,
    createdAt = BlazeCampaignsUtils.stringToDate(CREATED_AT),
    endDate = BlazeCampaignsUtils.stringToDate(END_DATE),
    uiStatus = UI_STATUS,
    budgetCents = BUDGET_CENTS,
    impressions = IMPRESSIONS,
    clicks = CLICKS
)
private val BLAZE_CAMPAIGNS_MODEL = BlazeCampaignsModel(
    campaigns = listOf(BLAZE_CAMPAIGN_MODEL.toDomainModel()),
    page = PAGE,
    totalItems = TOTAL_ITEMS,
    totalPages = TOTAL_PAGES
)

private val NO_RESULTS_BLAZE_CAMPAIGNS_MODEL = BlazeCampaignsModel(
    campaigns = listOf(),
    page = 1,
    totalItems = 0,
    totalPages = 0
)

@RunWith(MockitoJUnitRunner::class)
class BlazeCampaignsStoreTest {
    @Mock private lateinit var restClient: BlazeCampaignsRestClient
    @Mock private lateinit var dao: BlazeCampaignsDao
    @Mock private lateinit var siteModel: SiteModel
    private lateinit var store: BlazeCampaignsStore

    private val successResponse = BLAZE_CAMPAIGNS_RESPONSE
    private val errorResponse = BlazeCampaignsError(type = GENERIC_ERROR)

    @Before
    fun setUp() {
        store = BlazeCampaignsStore(restClient, dao, initCoroutineEngine())
        whenever(siteModel.siteId).thenReturn(SITE_ID)
    }

    @Test
    fun `given success, when fetch blaze campaigns is triggered, then values are inserted`() =
        test {
            val payload = BlazeCampaignsFetchedPayload(successResponse)
            whenever(restClient.fetchBlazeCampaigns(siteModel, PAGE)).thenReturn(payload)

            store.fetchBlazeCampaigns(siteModel, PAGE)

            verify(dao).insertCampaignsAndPageInfoForSite(SITE_ID, BLAZE_CAMPAIGNS_MODEL)
        }

    @Test
    fun `given error, when fetch blaze campaigns is triggered, then error result is returned`() =
        test {
            whenever(restClient.fetchBlazeCampaigns(any(), any())).thenReturn(
                BlazeCampaignsFetchedPayload(errorResponse)
            )
            val result = store.fetchBlazeCampaigns(siteModel)

            verifyNoInteractions(dao)
            assertThat(result.model).isNull()
            assertEquals(GENERIC_ERROR, result.error.type)
            assertNull(result.error.message)
        }

    @Test
    fun `given unmatched site, when get is triggered, then empty campaigns list returned`() = test {
        whenever(dao.getCampaignsAndPaginationForSite(SITE_ID)).thenReturn(
            NO_RESULTS_BLAZE_CAMPAIGNS_MODEL
        )

        val result = store.getBlazeCampaigns(siteModel)

        assertThat(result).isNotNull
        assertThat(result.campaigns).isEmpty()
        assertEquals(result.page, 1)
        assertEquals(result.totalPages, 0)
        assertEquals(result.totalItems, 0)
    }

    @Test
    fun `given matched site, when get recent is triggered, then campaign is returned`() = test {
        whenever(dao.getMostRecentCampaignForSite(SITE_ID)).thenReturn(BLAZE_CAMPAIGN_MODEL)

        val result = store.getMostRecentBlazeCampaign(siteModel)

        assertThat(result).isNotNull
        assertEquals(result?.campaignId, CAMPAIGN_ID)
        assertEquals(result?.title, TITLE)
        assertEquals(result?.imageUrl, IMAGE_URL)
        assertEquals(result?.createdAt, BlazeCampaignsUtils.stringToDate(CREATED_AT))
        assertEquals(result?.endDate, BlazeCampaignsUtils.stringToDate(END_DATE))
        assertEquals(result?.uiStatus, UI_STATUS)
        assertEquals(result?.budgetCents, BUDGET_CENTS)
        assertEquals(result?.impressions, IMPRESSIONS)
        assertEquals(result?.clicks, CLICKS)
    }

    @Test
    fun `given unmatched site, when get recent is triggered, then campaign is returned`() = test {
        val result = store.getMostRecentBlazeCampaign(siteModel)

        assertThat(result).isNull()
    }
}
