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
import java.io.IOException
import kotlin.test.assertEquals

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
        dao.insertCampaigns(SITE_ID, model)

        // then
        var observedStatus = dao.getCachedCampaigns(SITE_ID)
        assertThat(observedStatus).isEqualTo(listOf(BLAZE_CAMPAIGN_MODEL))

        // when
        model = model.copy(campaigns = model.campaigns.map { it.copy(title = SECONDARY_TITLE) })
        dao.insertCampaigns(SITE_ID, model)

        // then
        observedStatus = dao.getCachedCampaigns(SITE_ID)
        assertThat(observedStatus[0].title).isEqualTo(SECONDARY_TITLE)
    }

    @Test
    fun `when insert of first items batch, then db is cleared before insert`(): Unit = runBlocking {
        // when
        var model = BLAZE_CAMPAIGNS_MODEL
        dao.insertCampaigns(SITE_ID, model)
        var observedStatus = dao.getCachedCampaigns(SITE_ID)
        assertEquals(observedStatus.size, 1)

        model = model.copy(skipped = 1, campaigns = model.campaigns.map { it.copy(campaignId = "2") })
        dao.insertCampaigns(SITE_ID, model)

        observedStatus = dao.getCachedCampaigns(SITE_ID)
        assertEquals(observedStatus.size, 2)

        // then
        model = model.copy(skipped = 0)
        dao.insertCampaigns(SITE_ID, model)
        observedStatus = dao.getCachedCampaigns(SITE_ID)
        assertEquals(observedStatus.size, 1)
    }

    @Test
    fun `when insert second batch of items, then db is not cleared`(): Unit = runBlocking {
        // when
        var model = BLAZE_CAMPAIGNS_MODEL
        dao.insertCampaigns(SITE_ID, model)
        var observedStatus = dao.getCachedCampaigns(SITE_ID)
        assertEquals(observedStatus.size, 1)

        model = model.copy(skipped = 1, campaigns = model.campaigns.map { it.copy(campaignId = "2") })
        dao.insertCampaigns(SITE_ID, model)

        observedStatus = dao.getCachedCampaigns(SITE_ID)
        assertEquals(observedStatus.size, 2)
    }

    @Test
    fun `when clear is requested, then all rows are deleted for site`(): Unit = runBlocking {
        // when
        val model = BLAZE_CAMPAIGNS_MODEL
        dao.insertCampaigns(1, model)
        dao.insertCampaigns(2, model)
        dao.insertCampaigns(3, model)

        dao.clearBlazeCampaigns(1)
        assertEmptyResult(dao.getCachedCampaigns(1))
        assertNotEmptyResult(dao.getCachedCampaigns(2))
        assertNotEmptyResult(dao.getCachedCampaigns(3))
    }

    @Test
    fun `given site not in db, when get with page, campaigns list is empty`(): Unit = runBlocking {
        // when
        val emptyList = emptyList<BlazeCampaignModel>()

        // then
        val observedStatus = dao.getCachedCampaigns(SITE_ID)

        // when
        assertThat(observedStatus).isEqualTo(emptyList)
    }

    @Test
    fun `given no site, when recent campaign req, then null returned `(): Unit = runBlocking {
        // then
        val observedStatus = dao.getMostRecentCampaignForSite(SITE_ID)

        // when
        assertThat(observedStatus).isNull()
    }

    @Test
    fun `given no site, when campaign list req, then empty list is return `(): Unit = runBlocking {
        // then
        val observedStatus = dao.getCampaigns(SITE_ID)

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

    private companion object {
        const val SITE_ID = 1234L
        const val SECONDARY_TITLE = "secondary title"
        const val CAMPAIGN_ID = "1234"
        const val TITLE = "title"
        const val IMAGE_URL = "imageUrl"
        const val CREATED_AT = "2023-06-02T00:00:00.000Z"
        const val DURATION_DAYS = 4
        const val UI_STATUS = "rejected"
        const val IMPRESSIONS = 0L
        const val CLICKS = 0L
        const val TOTAL_BUDGET = 100.0
        const val SPENT_BUDGET = 0.0
        const val TARGET_URN = "urn:wpcom:post:199247490:9"

        const val SKIP = 0
        const val TOTAL_ITEMS = 1

        val BLAZE_CAMPAIGN_MODEL = BlazeCampaignModel(
            campaignId = CAMPAIGN_ID,
            title = TITLE,
            imageUrl = IMAGE_URL,
            startTime = BlazeCampaignsUtils.stringToDate(CREATED_AT),
            durationInDays = DURATION_DAYS,
            uiStatus = UI_STATUS,
            impressions = IMPRESSIONS,
            clicks = CLICKS,
            targetUrn = TARGET_URN,
            totalBudget = TOTAL_BUDGET,
            spentBudget = SPENT_BUDGET,
            isEndlessCampaign = false
        )
        val BLAZE_CAMPAIGNS_MODEL = BlazeCampaignsModel(
            campaigns = listOf(BLAZE_CAMPAIGN_MODEL),
            skipped = SKIP,
            totalItems = TOTAL_ITEMS,
        )
    }
}
