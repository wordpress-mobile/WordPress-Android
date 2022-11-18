package org.wordpress.android.localcontentmigration

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.localcontentmigration.LocalContentEntityData.Companion.IneligibleReason.WPNotLoggedIn
import org.wordpress.android.localcontentmigration.LocalContentEntityData.EligibilityStatusData
import org.wordpress.android.localcontentmigration.LocalMigrationError.Ineligibility
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import javax.inject.Inject

class LocalEligibilityStatusProviderHelper @Inject constructor(
    private val siteStore: SiteStore,
    private val accountStore: AccountStore,
): LocalDataProviderHelper {
    override fun getData(localEntityId: Int?): LocalContentEntityData {
        @Suppress("ForbiddenComment")
        // TODO: check for eligibility of local content
        val hasToken = !accountStore.accessToken.isNullOrBlank()
        val hasSelfHostedSites = siteStore.sites.any { !it.isUsingWpComRestApi }
        val isEligible = hasToken || hasSelfHostedSites
        val reason = if (!isEligible) WPNotLoggedIn else null
        return EligibilityStatusData(isEligible, reason)
    }
}

fun <E: LocalMigrationError> LocalMigrationResult<EligibilityStatusData, E>.validate() = when (this) {
    is Success -> if (this.value.isEligible) {
        this
    } else {
        checkNotNull(this.value.reason) { "Migration should never be ineligible without a reason." }
        Failure(Ineligibility(this.value.reason))
    }
    is Failure -> this
}
