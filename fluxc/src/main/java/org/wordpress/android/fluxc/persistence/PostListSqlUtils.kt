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
     * To avoid duplicate rows, it'll first delete the existing records for [postList]. In order to optimize the
     * queries, it will first group the [postList] by the [PostListModel.listId]. It will then run the `delete` query
     * for each `listId`. In practice, it's likely that the [postList] will have a single `listId` which means
     * there will be a single `delete` query, but it does allow for inserting a [postList] with multiple `listId`s.
     */
    fun insertPostList(postList: List<PostListModel>) {
        val listIdToPostIdsMap = postList.groupBy({ it.listId }, { it.postId })
        listIdToPostIdsMap.keys.forEach { listId ->
            WellSql.delete(PostListModel::class.java)
                    .where()
                    .equals(PostListModelTable.LIST_ID, listId)
                    .isIn(PostListModelTable.POST_ID, listIdToPostIdsMap[listId])
                    .endWhere()
                    .execute()
        }
        WellSql.insert(postList).asSingleTransaction(true).execute()
    }

    /**
     * This function returns a list of [PostListModel] records for the given [listId].
     */
    fun getPostList(listId: Int): List<PostListModel>? =
            WellSql.select(PostListModel::class.java)
                    .where()
                    .equals(PostListModelTable.LIST_ID, listId)
                    .endWhere()
                    .asModel

    /**
     * This function deletes every [PostListModel] record for the given [postId].
     */
    fun deletePost(postId: Int) {
        WellSql.delete(PostListModel::class.java)
                .where()
                .equals(PostListModelTable.POST_ID, postId)
                .endWhere()
                .execute()
    }
}
