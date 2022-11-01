package org.wordpress.android.localcontentmigration

import android.database.Cursor
import android.net.Uri
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.model.PostModel
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

    private fun query(entity: LocalContentEntity, siteId: Int?, entityId: Int?): Cursor {
        with(EntryPointAccessors.fromApplication(requireContext().applicationContext,
                LocalMigrationContentProviderEntryPoint::class.java)) {
            val response = when (entity) {
                EligibilityStatus -> localEligibilityStatusProviderHelper().getData(siteId, entityId)
                Site -> localSiteProviderHelper().getData(siteId, entityId)
                Post -> localPostProviderHelper().getData(siteId, entityId)
            }
            return queryResult().createCursor(response)
        }
    }
}

interface LocalDataProviderHelper {
    fun getData(localSiteId: Int?, localPostId: Int?): LocalContentEntityData
}

enum class LocalContentEntity {
    EligibilityStatus {
        override val contentIdCapturePattern: Regex
            get() = Regex(this.name)

        override fun getPathForContent(siteId: Int?, entityId: Int?) : String {
            return this.name
        }
    },
    Site {
        override val contentIdCapturePattern: Regex
            get() = Regex(this.name)

        override fun getPathForContent(siteId: Int?, entityId: Int?): String {
            return this.name
        }
    },
    Post,
    ;

    open val contentIdCapturePattern = Regex("site/(\\d+)/${this.name}(?:/(\\d*))?")
    open fun getPathForContent(siteId: Int?, entityId: Int?) : String {
        return "site/${siteId}/${this.name}${ entityId?.let { "/${it}" } ?: "" }"
    }
}

sealed class LocalContentEntityData {
    data class EligibilityStatusData(
        val isEligible: Boolean,
        val siteCount: Int,
    ): LocalContentEntityData()

    data class SitesData(val localIds: List<Int>): LocalContentEntityData()
    data class PostsData(val localIds: List<Int>): LocalContentEntityData()
    data class PostData(val post: PostModel) : LocalContentEntityData()
}
