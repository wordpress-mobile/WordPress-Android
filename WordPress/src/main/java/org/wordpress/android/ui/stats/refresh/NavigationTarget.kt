package org.wordpress.android.ui.stats.refresh

import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.StatsConstants
import org.wordpress.android.util.Event
import java.util.Date

sealed class NavigationTarget : Event() {
    class AddNewPost : NavigationTarget()
    data class ViewPost(val postId: Long, val postUrl: String, val postType: String = StatsConstants.ITEM_TYPE_POST) :
            NavigationTarget()

    data class SharePost(val url: String, val title: String) : NavigationTarget()
    data class ViewPostDetailStats(
        val postId: Long,
        val postTitle: String,
        val postUrl: String?,
        val postType: String = StatsConstants.ITEM_TYPE_POST
    ) : NavigationTarget()

    data class ViewFollowersStats(val selectedTab: Int) : NavigationTarget()
    class ViewCommentsStats(val selectedTab: Int) : NavigationTarget()
    class ViewTagsAndCategoriesStats : NavigationTarget()
    class ViewPublicizeStats : NavigationTarget()
    data class ViewTag(val link: String) : NavigationTarget()
    data class ViewPostsAndPages(val statsGranularity: StatsGranularity, val selectedDate: Date) : NavigationTarget()
    data class ViewReferrers(val statsGranularity: StatsGranularity, val selectedDate: Date) : NavigationTarget()
    data class ViewClicks(val statsGranularity: StatsGranularity, val selectedDate: Date) : NavigationTarget()
    data class ViewCountries(val statsGranularity: StatsGranularity, val selectedDate: Date) : NavigationTarget()
    data class ViewVideoPlays(val statsGranularity: StatsGranularity, val selectedDate: Date) : NavigationTarget()
    data class ViewSearchTerms(val statsGranularity: StatsGranularity, val selectedDate: Date) : NavigationTarget()
    data class ViewAuthors(val statsGranularity: StatsGranularity, val selectedDate: Date) : NavigationTarget()
    data class ViewUrl(val url: String) : NavigationTarget()
    class ViewMonthsAndYearsStats : NavigationTarget()
    class ViewDayAverageStats : NavigationTarget()
    class ViewRecentWeeksStats : NavigationTarget()
}
