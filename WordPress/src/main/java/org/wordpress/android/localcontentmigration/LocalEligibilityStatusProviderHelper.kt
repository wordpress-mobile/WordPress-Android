package org.wordpress.android.localcontentmigration

import com.wellsql.generated.PostModelTable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.localcontentmigration.LocalContentEntityData.Companion.IneligibleReason.LocalDraftContentIsPresent
import org.wordpress.android.localcontentmigration.LocalContentEntityData.Companion.IneligibleReason.WPNotLoggedIn
import org.wordpress.android.localcontentmigration.LocalContentEntityData.EligibilityStatusData
import org.wordpress.android.localcontentmigration.LocalMigrationError.Ineligibility
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import org.wordpress.android.resolver.DbWrapper
import javax.inject.Inject

class LocalEligibilityStatusProviderHelper @Inject constructor(
    private val dbWrapper: DbWrapper,
    private val localMigrationSiteProviderHelper: LocalSiteProviderHelper,
): LocalDataProviderHelper {
    @Suppress("ForbiddenComment")
    // TODO: check for eligibility of media? - I guess this might be covered by posts and pages - except for
    // media that is not part of a post or page ??
    override fun getData(localEntityId: Int?): LocalContentEntityData {
        val (eligibleSites) = localMigrationSiteProviderHelper.getData()
        return when {
            eligibleSites.isEmpty() -> EligibilityStatusData(false, WPNotLoggedIn)
            else -> when {
                eligibleSites.flatMap(::getLocalDraftPostAndPageIdsForSite).isNotEmpty() ->
                    EligibilityStatusData(false, LocalDraftContentIsPresent)
                else -> EligibilityStatusData(true)
            }
        }
    }

    /**
     * Since posts and pages share the same table, we can directly query the database for the ids without filtering
     * on `isPage`.
     */
    private fun getLocalDraftPostAndPageIdsForSite(site: SiteModel?) = site?.id?.let {
        with(dbWrapper.giveMeReadableDb()) {
            val ids = mutableListOf<Int>()
            query(
                    "PostModel",
                    arrayOf(PostModelTable.ID),
                    "${PostModelTable.IS_LOCAL_DRAFT}=1",
                    null,
                    null,
                    null,
                    "${PostModelTable.ID} ASC",
            ).apply {
                runCatching {
                    while (moveToNext()) {
                        ids.add(getInt(0))
                    }
                }.also {
                    close()
                }.getOrThrow()
            }
            ids
        }
    } ?: emptyList()
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
