package org.wordpress.android.fluxc.store

import android.content.SharedPreferences
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.InsightTypeSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.InsightType.COMMENTS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.FOLLOWERS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.MOST_POPULAR_DAY_AND_HOUR
import org.wordpress.android.fluxc.store.StatsStore.InsightType.POSTING_ACTIVITY
import org.wordpress.android.fluxc.store.StatsStore.ManagementType
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.FILE_DOWNLOADS
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.utils.PreferenceUtils.PreferenceUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class StatsStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var insightTypesSqlUtils: InsightTypeSqlUtils
    @Mock lateinit var preferenceUtilsWrapper: PreferenceUtilsWrapper
    @Mock lateinit var sharedPreferences: SharedPreferences
    @Mock lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    @Mock lateinit var statsSqlUtils: StatsSqlUtils
    private lateinit var store: StatsStore

    @Before
    fun setUp() {
        store = StatsStore(
                initCoroutineEngine(),
                insightTypesSqlUtils,
                preferenceUtilsWrapper,
                statsSqlUtils
        )
        whenever(preferenceUtilsWrapper.getFluxCPreferences()).thenReturn(sharedPreferences)
        whenever(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
    }

    @Test
    fun `returns default stats types when DB is empty`() = test {
        whenever(insightTypesSqlUtils.selectAddedItemsOrderedByStatus(site)).thenReturn(listOf())

        val result = store.getAddedInsights(site)

        assertThat(result).containsExactly(*DEFAULT_INSIGHTS.toTypedArray())
    }

    @Test
    fun `returns updated stats types from DB`() = test {
        whenever(insightTypesSqlUtils
                .selectAddedItemsOrderedByStatus(site))
                .thenReturn(listOf(MOST_POPULAR_DAY_AND_HOUR))

        val result = store.getAddedInsights(site)

        assertThat(result).containsExactly(MOST_POPULAR_DAY_AND_HOUR)
    }

    @Test
    fun `updates types with added and removed`() = test {
        val addedTypes = listOf(
                MOST_POPULAR_DAY_AND_HOUR
        )
        val removedTypes = store.getRemovedInsights(addedTypes)
        store.updateTypes(site, addedTypes)

        verify(insightTypesSqlUtils).insertOrReplaceAddedItems(site, addedTypes)
        verify(insightTypesSqlUtils).insertOrReplaceRemovedItems(site, removedTypes)
    }

    @Test
    fun `moves type up in the list when it is last`() = test {
        val insightType = listOf(
                MOST_POPULAR_DAY_AND_HOUR,
                FOLLOWERS,
                COMMENTS
        )
        whenever(insightTypesSqlUtils.selectAddedItemsOrderedByStatus(site)).thenReturn(insightType)

        store.moveTypeUp(site, COMMENTS)

        verify(insightTypesSqlUtils).insertOrReplaceAddedItems(
                site,
                listOf(MOST_POPULAR_DAY_AND_HOUR, COMMENTS, FOLLOWERS
        ))
    }

    @Test
    fun `does not move type up in the list when it is first`() = test {
        val insightType = listOf(
                COMMENTS,
                MOST_POPULAR_DAY_AND_HOUR,
                FOLLOWERS
        )
        whenever(insightTypesSqlUtils.selectAddedItemsOrderedByStatus(site)).thenReturn(insightType)

        store.moveTypeUp(site, COMMENTS)

        verify(insightTypesSqlUtils, never()).insertOrReplaceAddedItems(eq(site), any())
    }

    @Test
    fun `moves type down in the list when it is first`() = test {
        val insightType = listOf(
                MOST_POPULAR_DAY_AND_HOUR,
                FOLLOWERS,
                COMMENTS
        )
        whenever(insightTypesSqlUtils.selectAddedItemsOrderedByStatus(site)).thenReturn(insightType)

        store.moveTypeDown(site, MOST_POPULAR_DAY_AND_HOUR)

        verify(insightTypesSqlUtils).insertOrReplaceAddedItems(
                site, listOf(FOLLOWERS, MOST_POPULAR_DAY_AND_HOUR, COMMENTS
        ))
    }

    @Test
    fun `does not move type down in the list when it is last`() = test {
        val insightType = listOf(
                COMMENTS,
                FOLLOWERS,
                MOST_POPULAR_DAY_AND_HOUR
        )
        whenever(insightTypesSqlUtils.selectAddedItemsOrderedByStatus(site)).thenReturn(insightType)

        store.moveTypeDown(site, MOST_POPULAR_DAY_AND_HOUR)

        verify(insightTypesSqlUtils, never()).insertOrReplaceAddedItems(eq(site), any())
    }

    @Test
    fun `removes type from list`() = test {
        store.removeType(site, MOST_POPULAR_DAY_AND_HOUR)

        val addedTypes = DEFAULT_INSIGHTS - MOST_POPULAR_DAY_AND_HOUR

        // executed twice, because the first time the default list is inserted first
        verify(insightTypesSqlUtils).insertOrReplaceAddedItems(
                site,
                addedTypes
        )

        verify(insightTypesSqlUtils).insertOrReplaceRemovedItems(
                site,
                store.getRemovedInsights(addedTypes)
        )

        store.removeType(site, POSTING_ACTIVITY)

        verify(insightTypesSqlUtils).insertOrReplaceAddedItems(
                site,
                addedTypes - POSTING_ACTIVITY
        )
        verify(insightTypesSqlUtils).insertOrReplaceRemovedItems(
                site,
                store.getRemovedInsights(addedTypes - POSTING_ACTIVITY)
        )
    }

    @Test @Ignore
    fun `insight types starts with news type and ends with control type when news card was not shown`() = test {
        whenever(insightTypesSqlUtils.selectAddedItemsOrderedByStatus(site)).thenReturn(listOf(COMMENTS))
        whenever(sharedPreferences.getBoolean(INSIGHTS_MANAGEMENT_NEWS_CARD_SHOWN, false)).thenReturn(false)

        val insightTypes = store.getInsightTypes(site)

        assertThat(insightTypes).hasSize(3)
        assertThat(insightTypes[0]).isEqualTo(ManagementType.NEWS_CARD)
        assertThat(insightTypes[1]).isEqualTo(COMMENTS)
        assertThat(insightTypes[2]).isEqualTo(ManagementType.CONTROL)
    }

    @Test @Ignore
    fun `insight types does not start with news type when news card was shown`() = test {
        whenever(insightTypesSqlUtils.selectAddedItemsOrderedByStatus(site)).thenReturn(listOf(COMMENTS))
        whenever(sharedPreferences.getBoolean(INSIGHTS_MANAGEMENT_NEWS_CARD_SHOWN, false)).thenReturn(true)

        val insightTypes = store.getInsightTypes(site)

        assertThat(insightTypes).hasSize(2)
        assertThat(insightTypes[0]).isEqualTo(COMMENTS)
        assertThat(insightTypes[1]).isEqualTo(ManagementType.CONTROL)
    }

    @Test
    fun `hide news card sets shared prefs`() {
        whenever(sharedPreferencesEditor.putBoolean(any(), any())).thenReturn(sharedPreferencesEditor)

        store.hideInsightsManagementNewsCard()

        verify(sharedPreferences).edit()
        Mockito.inOrder(sharedPreferencesEditor).apply {
            this.verify(sharedPreferencesEditor).putBoolean(INSIGHTS_MANAGEMENT_NEWS_CARD_SHOWN, true)
            this.verify(sharedPreferencesEditor).apply()
        }
    }

    @Test
    fun `is news card showing returns from shared prefs`() {
        val prefsValue = true
        whenever(sharedPreferences.getBoolean(INSIGHTS_MANAGEMENT_NEWS_CARD_SHOWN, true)).thenReturn(prefsValue)

        val insightsManagementNewsCardShowing = store.isInsightsManagementNewsCardShowing()

        assertThat(insightsManagementNewsCardShowing).isEqualTo(prefsValue)
    }

    @Test
    fun `deletes all stats`() = test {
        store.deleteAllData()

        verify(statsSqlUtils).deleteAllStats()
    }

    @Test
    fun `deletes all stats for a site`() = test {
        val site = SiteModel()

        store.deleteSiteData(site)

        verify(statsSqlUtils).deleteSiteStats(site)
    }

    @Test
    fun `filters out file downloads on Jetpack site`() = test {
        val site = SiteModel()
        site.setIsJetpackConnected(true)

        val timeStatsTypes = store.getTimeStatsTypes(site)

        assertThat(timeStatsTypes).doesNotContain(FILE_DOWNLOADS)
    }

    @Test
    fun `does not filter out file downloads on non-Jetpack site`() = test {
        val site = SiteModel()
        site.setIsJetpackConnected(false)

        val timeStatsTypes = store.getTimeStatsTypes(site)

        assertThat(timeStatsTypes).contains(FILE_DOWNLOADS)
    }
}
