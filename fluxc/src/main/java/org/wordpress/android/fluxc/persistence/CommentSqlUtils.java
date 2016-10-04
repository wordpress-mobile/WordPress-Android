package org.wordpress.android.fluxc.persistence;

import com.wellsql.generated.CommentModelTable;
import com.wellsql.generated.PostModelTable;
import com.yarolegovich.wellsql.SelectQuery;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.SiteModel;

import java.util.Collections;
import java.util.List;

public class CommentSqlUtils {
    public static int insertOrUpdateComment(CommentModel comment) {
        if (comment == null) {
            return 0;
        }

        List<CommentModel> commentResult;
        commentResult = WellSql.select(CommentModel.class)
                .where()
                .beginGroup()
                    .equals(CommentModelTable.ID, comment.getId())
                .or()
                    .beginGroup()
                        .equals(CommentModelTable.REMOTE_COMMENT_ID, comment.getRemoteCommentId())
                        .equals(CommentModelTable.LOCAL_SITE_ID, comment.getLocalSiteId())
                    .endGroup()
                .endGroup()
                .endWhere().getAsModel();

        if (commentResult.isEmpty()) {
            // insert
            WellSql.insert(comment).asSingleTransaction(true).execute();
            return 1;
        } else {
            // update
            int oldId = commentResult.get(0).getId();
            return WellSql.update(CommentModel.class).whereId(oldId)
                    .put(comment, new UpdateAllExceptId<CommentModel>()).execute();
        }
    }

    public static int deleteComment(CommentModel comment) {
        if (comment == null) {
            return 0;
        }

        return WellSql.delete(CommentModel.class)
                .where().equals(PostModelTable.ID, comment.getId()).endWhere()
                .execute();
    }

    public static List<CommentModel> getCommentsForSite(SiteModel site) {
        if (site == null) {
            return Collections.emptyList();
        }

        return WellSql.select(CommentModel.class)
                .where().beginGroup()
                .equals(CommentModelTable.LOCAL_SITE_ID, site.getId())
                .endGroup().endWhere().getAsModel();
    }
}
