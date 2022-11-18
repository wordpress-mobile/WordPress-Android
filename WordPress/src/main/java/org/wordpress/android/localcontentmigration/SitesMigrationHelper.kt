package org.wordpress.android.localcontentmigration

import org.wordpress.android.localcontentmigration.LocalContentEntity.Sites
import org.wordpress.android.localcontentmigration.LocalContentEntityData.SitesData
import org.wordpress.android.localcontentmigration.LocalMigrationError.PersistenceError.FailedToSaveSites
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import org.wordpress.android.resolver.ResolverUtility
import javax.inject.Inject

class SitesMigrationHelper @Inject constructor(
    private val resolverUtility: ResolverUtility,
    private val localMigrationContentResolver: LocalMigrationContentResolver,
) {
    fun migrateSites() = localMigrationContentResolver.getResultForEntityType<SitesData>(Sites).thenWith { sitesData ->
        runCatching {
            resolverUtility.copySitesWithIndexes(sitesData.sites)
            Success(sitesData)
        }.getOrDefault(Failure(FailedToSaveSites))
    }
}
