package org.wordpress.android.ui.mysite

import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationSource
import org.wordpress.android.ui.mysite.cards.post.PostCardsSource
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardSource
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardsSource
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
}
