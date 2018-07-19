package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.PostListModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.PostListModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostListSqlUtils @Inject constructor() {
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

    fun getPostList(listId: Int): List<PostListModel>? =
            WellSql.select(PostListModel::class.java)
                    .where()
                    .equals(PostListModelTable.LIST_ID, listId)
                    .endWhere()
                    .asModel
}
