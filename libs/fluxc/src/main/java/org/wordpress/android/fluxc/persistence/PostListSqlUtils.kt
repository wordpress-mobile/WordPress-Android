package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.PostListModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.PostListModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostListSqlUtils @Inject constructor() {
    /**
     * This function inserts the [postList] for a [listId] in the [PostListModelTable]. Every post in the [postList]
     * needs to have the same [PostListModel.listId] as the passed in [listId].
     *
     * To avoid duplicate rows, it'll first delete the existing records for [postList].
     *
     * The [listId] parameter is passed in to optimize the delete query. If it's not passed in, we'd need to run
     * several `delete` queries for different [PostListModel.listId] & [PostListModel.postId] combinations. Since this
     * function is intended to be used for inserting a set of [PostListModel]s for a single list, this is a nice
     * optimization especially when the post list gets larger.
     */
    fun insertPostList(listId: Int, postList: List<PostListModel>) {
        val postIds = postList.map { it.postId }
        WellSql.delete(PostListModel::class.java)
                .where()
                .equals(PostListModelTable.LIST_ID, listId)
                .isIn(PostListModelTable.POST_ID, postIds)
                .endWhere()
                .execute()
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
