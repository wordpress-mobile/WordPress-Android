package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.PostListModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.PostListModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostListSqlUtils @Inject constructor() {
    /**
     * This function inserts the [postList] in the [PostListModelTable].
     *
     * Unique constraint in [PostListModel] will ignore duplicate records which is what we want. That'll ensure that
     * the order of the items will not be altered while the user is browsing the list. The order will fix itself
     * once the list data is refreshed.
     */
    fun insertPostList(postList: List<PostListModel>) {
        WellSql.insert(postList).asSingleTransaction(true).execute()
    }

    /**
     * This function returns a list of [PostListModel] records for the given [listId].
     */
    fun getPostList(listId: Int): List<PostListModel> =
            WellSql.select(PostListModel::class.java)
                    .where()
                    .equals(PostListModelTable.LIST_ID, listId)
                    .endWhere()
                    .asModel

    /**
     * This function deletes every [PostListModel] record of a site with [localSiteId] for the given [postId].
     */
    fun deletePost(localSiteId: Int, postId: Int) {
        WellSql.delete(PostListModel::class.java)
                .where()
                .equals(PostListModelTable.LOCAL_SITE_ID, localSiteId)
                .equals(PostListModelTable.POST_ID, postId)
                .endWhere()
                .execute()
    }
}
