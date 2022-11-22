package org.wordpress.android.localcontentmigration

import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostsData
import javax.inject.Inject

class LocalPostProviderHelper @Inject constructor(
        private val postStore: PostStore,
        private val localMigrationSiteProviderHelper: LocalSiteProviderHelper,
    ): LocalDataProviderHelper {
    override fun getData(localEntityId: Int?): LocalContentEntityData {
        localEntityId?.let { localPostId ->
            val post = postStore.getPostByLocalPostId(localPostId)
            return PostData(post = post)
        } ?: run {
            val (sites) = localMigrationSiteProviderHelper.getData()
            return PostsData(localIds = sites.flatMap { site ->
                postStore.getPostsForSite(site).mapNotNull { it.id }
            })
        }
    }
}
