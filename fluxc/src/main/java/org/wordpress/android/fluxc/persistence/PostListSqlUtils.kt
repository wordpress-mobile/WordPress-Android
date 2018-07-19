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
     * To avoid duplicate rows, it'll first delete the existing records for [postList].
     */
    fun insertPostList(postList: List<PostListModel>) {
        for (postListModel in postList) {
            WellSql.delete(PostListModel::class.java)
                    .where()
                    .equals(PostListModelTable.LIST_ID, postListModel.listId)
                    .equals(PostListModelTable.POST_ID, postListModel.postId)
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
