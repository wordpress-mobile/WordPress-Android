package org.wordpress.android.localcontentmigration

import android.database.Cursor
import android.net.Uri
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.localcontentmigration.LocalContentEntity.AccessToken
import org.wordpress.android.localcontentmigration.LocalContentEntity.EligibilityStatus
import org.wordpress.android.localcontentmigration.LocalContentEntity.Post
import org.wordpress.android.localcontentmigration.LocalContentEntity.Site
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
    }

    override fun query(uri: Uri): Cursor {
        val path = uri.path!!
        val (entity, groups) = LocalContentEntity.values().firstNotNullOf { entity ->
            entity.contentIdCapturePattern.find(path)?.let { match ->
                return@firstNotNullOf Pair(entity, match.groups)
            }
        }
        val parameters = groups.drop(1).mapNotNull { it?.value?.let { id -> parseInt(id) } }
        val siteId = parameters.getOrNull(0)
        val entityId = parameters.getOrNull(1)
        return query(entity, siteId, entityId)
    }

    private fun query(entity: LocalContentEntity, localSiteId: Int?, localEntityId: Int?): Cursor {
        with(EntryPointAccessors.fromApplication(requireContext().applicationContext,
                LocalMigrationContentProviderEntryPoint::class.java)) {
            val response = when (entity) {
                EligibilityStatus -> localEligibilityStatusProviderHelper().getData()
                AccessToken -> localAccessTokenProviderHelper().getData()
                Site -> localSiteProviderHelper().getData(localEntityId = localEntityId)
                Post -> localPostProviderHelper().getData(localSiteId, localEntityId)
            }
            return queryResult().createCursor(response)
        }
    }
}

interface LocalDataProviderHelper {
    fun getData(localSiteId: Int? = null, localEntityId: Int? = null): LocalContentEntityData
}

enum class LocalContentEntity(private val isSiteContent: Boolean = false) {
    EligibilityStatus,
    AccessToken,
    Site,
    Post(isSiteContent = true),
    ;

    open val contentIdCapturePattern = when (isSiteContent) {
        true -> Regex("site/(\\d+)/${name}(?:/(\\d+))?")
        false -> Regex(name)
    }

    open fun getPathForContent(localSiteId: Int?, localEntityId: Int?) = when (this.isSiteContent) {
        true -> "site/${localSiteId}/${name}${ localEntityId?.let { "/${it}" } ?: "" }"
        false -> name
    }
}

sealed class LocalContentEntityData {
    data class EligibilityStatusData(
        val isEligible: Boolean,
        val siteCount: Int,
    ): LocalContentEntityData()

    data class AccessTokenData(val token: String): LocalContentEntityData()
    data class SitesData(val localIds: List<Int>): LocalContentEntityData()
    data class PostsData(val localIds: List<Int>): LocalContentEntityData()
    data class PostData(val post: PostModel) : LocalContentEntityData()
}
