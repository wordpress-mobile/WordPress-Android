package org.wordpress.android.localcontentmigration

import com.wellsql.generated.PostModelTable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostsData
import org.wordpress.android.resolver.DbWrapper
import javax.inject.Inject

class LocalPostProviderHelper @Inject constructor(
    private val postStore: PostStore,
    private val dbWrapper: DbWrapper,
    private val localMigrationSiteProviderHelper: LocalSiteProviderHelper,
): LocalDataProviderHelper {
    override fun getData(localEntityId: Int?) = localEntityId?.let { localPostId ->
        PostData(post = postStore.getPostByLocalPostId(localPostId))
    } ?: run {
        localMigrationSiteProviderHelper.getData().let { (sites) ->
            PostsData(localIds = sites.flatMap(::getPostAndPageIdsForSite))
        }
    }

    /**
     * Since posts and pages share the same table, we can directly query the database for the ids without filtering
     * on `isPage`.
     */
    private fun getPostAndPageIdsForSite(site: SiteModel?) = site?.id?.let { localSiteId ->
        with(dbWrapper.giveMeReadableDb()) {
            val ids = mutableListOf<Int>()
            query(
                    "PostModel",
                    arrayOf(PostModelTable.ID),
                    "${PostModelTable.LOCAL_SITE_ID}=?",
                    arrayOf("$localSiteId"),
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
