package org.wordpress.android.localcontentmigration

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.localcontentmigration.EligibilityState.Eligible
import org.wordpress.android.localcontentmigration.EligibilityState.Ineligible.WPNotLoggedIn
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
        return EligibilityStatusData(
                eligibilityState = when (hasToken || hasSelfHostedSites) {
                    true -> Eligible
                    false -> WPNotLoggedIn
                }
        )
    }
}

fun <E: LocalMigrationError> LocalMigrationResult<EligibilityStatusData, E>.validate() = when (this) {
    is Success -> when(this.value.eligibilityState) {
        is Eligible -> this
        is WPNotLoggedIn -> Failure(Ineligibility(this.value.eligibilityState))
    }
    is Failure -> this
}
