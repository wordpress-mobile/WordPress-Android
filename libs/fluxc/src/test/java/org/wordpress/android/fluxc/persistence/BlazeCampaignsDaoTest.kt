package org.wordpress.android.fluxc.persistence

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsUtils
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao
import org.wordpress.android.fluxc.store.blaze.BUDGET_CENTS
import org.wordpress.android.fluxc.store.blaze.CAMPAIGN_ID
import org.wordpress.android.fluxc.store.blaze.CLICKS
import org.wordpress.android.fluxc.store.blaze.CREATED_AT
import org.wordpress.android.fluxc.store.blaze.END_DATE
import org.wordpress.android.fluxc.store.blaze.IMAGE_URL
import org.wordpress.android.fluxc.store.blaze.IMPRESSIONS
import org.wordpress.android.fluxc.store.blaze.PAGE
import org.wordpress.android.fluxc.store.blaze.TITLE
import org.wordpress.android.fluxc.store.blaze.TOTAL_ITEMS
import org.wordpress.android.fluxc.store.blaze.TOTAL_PAGES
import org.wordpress.android.fluxc.store.blaze.UI_STATUS
import java.io.IOException
import kotlin.test.assertEquals

private val BLAZE_CAMPAIGN_MODEL = BlazeCampaignModel(
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
    campaigns = listOf(BLAZE_CAMPAIGN_MODEL),
    page = PAGE,
    totalItems = TOTAL_ITEMS,
    totalPages = TOTAL_PAGES
)

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class BlazeCampaignsDaoTest {
    private lateinit var dao: BlazeCampaignsDao
    private lateinit var db: WPAndroidDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().context
        db = Room.inMemoryDatabaseBuilder(
            context, WPAndroidDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.blazeCampaignsDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `when insert followed by update, then updated campaign is returned`(): Unit = runBlocking {
        // when
        var model = BLAZE_CAMPAIGNS_MODEL
        dao.insertCampaignsAndPageInfoForSite(defaultSiteId, model)

        // then
        var observedStatus = dao.getCampaignsAndPaginationForSite(defaultSiteId)
        assertThat(observedStatus).isEqualTo(BLAZE_CAMPAIGNS_MODEL)

        // when
        model = model.copy(campaigns = model.campaigns.map { it.copy(title = secondaryTitle) })
        dao.insertCampaignsAndPageInfoForSite(defaultSiteId, model)

        // then
        observedStatus = dao.getCampaignsAndPaginationForSite(defaultSiteId)
        assertThat(observedStatus.campaigns[0].title).isEqualTo(secondaryTitle)
    }

    @Test
    fun `when insert of page 1, then db is cleared before insert`(): Unit = runBlocking {
        // when
        var model = BLAZE_CAMPAIGNS_MODEL
        dao.insertCampaignsAndPageInfoForSite(defaultSiteId, model)
        var observedStatus = dao.getCampaignsAndPaginationForSite(defaultSiteId)
        assertEquals(observedStatus.campaigns.size, 1)

        model = model.copy(page = 2, campaigns = model.campaigns.map { it.copy(campaignId = 2) })
        dao.insertCampaignsAndPageInfoForSite(defaultSiteId, model)

        observedStatus = dao.getCampaignsAndPaginationForSite(defaultSiteId)
        assertEquals(observedStatus.campaigns.size, 2)

        // then
        model = model.copy(page = 1)
        dao.insertCampaignsAndPageInfoForSite(defaultSiteId, model)
        observedStatus = dao.getCampaignsAndPaginationForSite(defaultSiteId)
        assertEquals(observedStatus.campaigns.size, 1)
    }

    @Test
    fun `when insert of page 2, then db is not cleared`(): Unit = runBlocking {
        // when
        var model = BLAZE_CAMPAIGNS_MODEL
        dao.insertCampaignsAndPageInfoForSite(defaultSiteId, model)
        var observedStatus = dao.getCampaignsAndPaginationForSite(defaultSiteId)
        assertEquals(observedStatus.campaigns.size, 1)

        model = model.copy(page = 2, campaigns = model.campaigns.map { it.copy(campaignId = 2) })
        dao.insertCampaignsAndPageInfoForSite(defaultSiteId, model)

        observedStatus = dao.getCampaignsAndPaginationForSite(defaultSiteId)
        assertEquals(observedStatus.campaigns.size, 2)
    }

    @Test
    fun `when clear is requested, then all rows are deleted for site`(): Unit = runBlocking {
        // when
        val model = BLAZE_CAMPAIGNS_MODEL
        dao.insertCampaignsAndPageInfoForSite(1, model)
        dao.insertCampaignsAndPageInfoForSite(2, model)
        dao.insertCampaignsAndPageInfoForSite(3, model)

        dao.clear(1)
        assertEmptyResult(dao.getCampaignsAndPaginationForSite(1).campaigns)
        assertNotEmptyResult(dao.getCampaignsAndPaginationForSite(2).campaigns)
        assertNotEmptyResult(dao.getCampaignsAndPaginationForSite(3).campaigns)
    }

    @Test
    fun `given site not in db, when get with page, campaigns list is empty`(): Unit = runBlocking {
        // when
        val emptyList = emptyList<BlazeCampaignModel>()

        // then
        val observedStatus = dao.getCampaignsAndPaginationForSite(defaultSiteId)

        // when
        assertThat(observedStatus.campaigns).isEqualTo(emptyList)
    }

    @Test
    fun `given no site, when recent campaign req, then null returned `(): Unit = runBlocking {
        // then
        val observedStatus = dao.getMostRecentCampaignForSite(defaultSiteId)

        // when
        assertThat(observedStatus).isNull()
    }

    @Test
    fun `given no site, when campaign list req, then empty list is return `(): Unit = runBlocking {
        // then
        val observedStatus = dao.getCampaigns(defaultSiteId)

        // when
        assertThat(observedStatus).isEmpty()
    }

    private fun assertEmptyResult(campaigns: List<BlazeCampaignModel>) {
        assertThat(campaigns).isNotNull
        assertThat(campaigns).isEmpty()
    }

    private fun assertNotEmptyResult(campaigns: List<BlazeCampaignModel>) {
        assertThat(campaigns).isNotNull
        assertThat(campaigns).isNotEmpty
    }

    companion object {
        private const val defaultSiteId = 1234L
        private const val secondaryTitle = "secondary title"
    }
}
