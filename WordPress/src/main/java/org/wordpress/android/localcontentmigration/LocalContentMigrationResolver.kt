package org.wordpress.android.localcontentmigration

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.localcontentmigration.LocalContentEntityData.EligibilityStatusData
import org.wordpress.android.localcontentmigration.LocalContentEntity.EligibilityStatus
import org.wordpress.android.localcontentmigration.LocalContentEntity.Post
import org.wordpress.android.localcontentmigration.LocalContentEntity.Site
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostsData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.SitesData
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

private fun ContentResolver.query(
    builder: Uri.Builder,
    entityType: LocalContentEntity,
    siteId: Int?,
    entityId: Int?,
) : Cursor? {
    val entityPath = entityType.getPathForContent(siteId, entityId)
    builder.appendEncodedPath(entityPath)
    return query(builder.build(), arrayOf(), "", arrayOf(), "")
}


class LocalContentMigrationResolver @Inject constructor(
    private val contextProvider: ContextProvider,
    private val wordPressPublicData: WordPressPublicData,
    private val queryResult: QueryResult,
    private val dispatcher: Dispatcher,
){
    private inline fun <reified T : LocalContentEntityData> getDataForEntityType(
        entityType: LocalContentEntity,
        siteId: Int? = null,
        entityId: Int? = null
    ): T {
        wordPressPublicData.currentPackageId().let { packageId ->
            Uri.Builder().apply {
                scheme("content")
                authority("${packageId}.${LocalContentMigrationProvider::class.simpleName}")
            }
        }.let { uriBuilder ->
            with (contextProvider.getContext().contentResolver) {
                val cursor = query(uriBuilder, entityType, siteId, entityId)!!
                return cursor.getValue()!!
            }
        }
    }

    fun migrateLocalContent() {
        val (isEligible) = getDataForEntityType<EligibilityStatusData>(EligibilityStatus)
        if (!isEligible) return // TODO: do something more graceful here?
        val sites: SitesData = getDataForEntityType(Site)
        for (localSiteId in sites.localIds) {
            val posts: PostsData = getDataForEntityType(Post, localSiteId)
            for (localPostId in posts.localIds) {
                val postData: PostData = getDataForEntityType(Post, localSiteId, localPostId)
                dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(postData.post))
            }
        }
    }

    private inline fun <reified T : LocalContentEntityData> Cursor.getValue() = queryResult.getValue<T>(this)
}
