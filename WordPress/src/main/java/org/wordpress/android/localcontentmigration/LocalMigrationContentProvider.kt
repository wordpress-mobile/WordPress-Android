package org.wordpress.android.localcontentmigration

import android.database.Cursor
import android.net.Uri
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.localcontentmigration.LocalContentEntity.AccessToken
import org.wordpress.android.localcontentmigration.LocalContentEntity.EligibilityStatus
import org.wordpress.android.localcontentmigration.LocalContentEntity.Post
import org.wordpress.android.localcontentmigration.LocalContentEntity.Sites
import org.wordpress.android.localcontentmigration.LocalContentEntity.UserFlags
import org.wordpress.android.provider.query.QueryResult
import java.lang.Integer.parseInt

class LocalMigrationContentProvider: TrustedQueryContentProvider() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface LocalMigrationContentProviderEntryPoint {
        fun queryResult(): QueryResult
        fun localSiteProviderHelper(): LocalSiteProviderHelper
        fun localPostProviderHelper(): LocalPostProviderHelper
        fun localEligibilityStatusProviderHelper(): LocalEligibilityStatusProviderHelper
        fun localAccessTokenProviderHelper(): LocalAccessTokenProviderHelper
        fun userFlagsProviderHelper(): UserFlagsProviderHelper
    }

    override fun query(uri: Uri): Cursor {
        val path = checkNotNull(uri.path) { "This provider does not support queries without a path." }
        // Find the matching entity and its captured groups
        val (entity, groups) = LocalContentEntity.values().firstNotNullOf { entity ->
            entity.contentIdCapturePattern.find(path)?.let { match ->
                return@firstNotNullOf Pair(entity, match.groups)
            }
        }
        val (localSiteId, localEntityId) = extractParametersFromGroups(groups)
        return query(entity, localSiteId, localEntityId)
    }

    private fun extractParametersFromGroups(groups: MatchGroupCollection): Pair<Int?, Int?> {
        // The first group is the entire match, so we drop that and parse the remaining captured groups as integers
        val parameters = groups.drop(1).mapNotNull { it?.value?.let { id -> parseInt(id) } }
        return Pair(parameters.getOrNull(0), parameters.getOrNull(1))
    }

    private fun query(entity: LocalContentEntity, localSiteId: Int?, localEntityId: Int?): Cursor {
        val context = checkNotNull(context) { "Cannot find context from the provider." }
        with(EntryPointAccessors.fromApplication(context.applicationContext,
                LocalMigrationContentProviderEntryPoint::class.java)) {
            val response = when (entity) {
                EligibilityStatus -> localEligibilityStatusProviderHelper().getData()
                AccessToken -> localAccessTokenProviderHelper().getData()
                UserFlags -> userFlagsProviderHelper().getData()
                Sites -> localSiteProviderHelper().getData()
                Post -> localPostProviderHelper().getData(localSiteId, localEntityId)
            }
            return queryResult().createCursor(response)
        }
    }
}

interface LocalDataProviderHelper {
    fun getData(localSiteId: Int? = null, localEntityId: Int? = null): LocalContentEntityData
}
