package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.ui.mysite.MySiteSource.MySiteRefreshSource
import org.wordpress.android.ui.mysite.MySiteSource.SiteIndependentSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState
import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationSource
import org.wordpress.android.ui.mysite.cards.post.PostCardsSource
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardSource
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardsSource
import org.wordpress.android.util.config.MySiteDashboardPhase2FeatureConfig
import javax.inject.Inject

@Suppress("LongParameterList")
class MySiteSourceManager @Inject constructor(
    private val currentAvatarSource: CurrentAvatarSource,
    private val domainRegistrationSource: DomainRegistrationSource,
    private val dynamicCardsSource: DynamicCardsSource,
    private val quickStartCardSource: QuickStartCardSource,
    private val scanAndBackupSource: ScanAndBackupSource,
    private val selectedSiteSource: SelectedSiteSource,
    postCardsSource: PostCardsSource,
    siteIconProgressSource: SiteIconProgressSource,
    private val mySiteDashboardPhase2FeatureConfig: MySiteDashboardPhase2FeatureConfig
) {
    val mySiteSources: List<MySiteSource<*>> = listOf(
            selectedSiteSource,
            siteIconProgressSource,
            quickStartCardSource,
            currentAvatarSource,
            domainRegistrationSource,
            scanAndBackupSource,
            dynamicCardsSource,
            postCardsSource
    )

    fun build(siteLocalId: Int?, coroutineScope: CoroutineScope): List<LiveData<out PartialState>> {
        return if (siteLocalId != null) {
            mySiteSources.map { source -> source.build(coroutineScope, siteLocalId).distinctUntilChanged() }
        } else {
            mySiteSources.filterIsInstance(SiteIndependentSource::class.java)
                    .map { source -> source.build(coroutineScope).distinctUntilChanged() }
        }
    }

    fun isRefreshing(): Boolean {
        if (mySiteDashboardPhase2FeatureConfig.isEnabled()) {
            mySiteSources.filterIsInstance(MySiteRefreshSource::class.java).forEach {
                if (it.isRefreshing() == true) {
                    return false
                }
            }
        }
        return true
    }
}
