package org.wordpress.android.fluxc.persistence;

import com.wellsql.generated.CommentModelTable;
import com.yarolegovich.wellsql.ConditionClauseBuilder;
import com.yarolegovich.wellsql.SelectQuery;
import com.yarolegovich.wellsql.SelectQuery.Order;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommentSqlUtils {
    public static int insertOrUpdateComment(CommentModel comment) {
        if (comment == null) {
            return 0;
        }

        List<CommentModel> commentResult;

        // If the comment already exist and has an id, we want to update it.
        commentResult = WellSql.select(CommentModel.class).where()
                .beginGroup()
                .equals(CommentModelTable.ID, comment.getId())
                .endGroup().endWhere().getAsModel();

        // If it's not a new comment, try to find the "remote" comment
        if (commentResult.isEmpty()) {
            commentResult = WellSql.select(CommentModel.class).where()
                    .beginGroup()
                    .equals(CommentModelTable.REMOTE_COMMENT_ID, comment.getRemoteCommentId())
                    .equals(CommentModelTable.LOCAL_SITE_ID, comment.getLocalSiteId())
                    .endGroup().endWhere().getAsModel();
        }

        if (commentResult.isEmpty()) {
            // insert
            WellSql.insert(comment).asSingleTransaction(true).execute();
            return 1;
        } else {
            // update
            int oldId = commentResult.get(0).getId();
            return WellSql.update(CommentModel.class).whereId(oldId)
                    .put(comment, new UpdateAllExceptId<>(CommentModel.class)).execute();
        }
    }

    public static CommentModel insertCommentForResult(CommentModel comment) {
        WellSql.insert(comment).asSingleTransaction(true).execute();

        return comment;
    }

    public static int removeComment(CommentModel comment) {
        if (comment == null) {
            return 0;
        }

        return WellSql.delete(CommentModel.class)
                .where().equals(CommentModelTable.ID, comment.getId()).endWhere()
                .execute();
    }

    public static int removeComments(SiteModel site) {
        if (site == null) {
            return 0;
        }

        return WellSql.delete(CommentModel.class)
                .where().equals(CommentModelTable.LOCAL_SITE_ID, site.getId()).endWhere()
                .execute();
    }

    public static int deleteAllComments() {
        return WellSql.delete(CommentModel.class).execute();
    }

    public static CommentModel getCommentByLocalCommentId(int localId) {
        List<CommentModel> results = WellSql.select(CommentModel.class)
                .where().equals(CommentModelTable.ID, localId).endWhere().getAsModel();
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    public static CommentModel getCommentBySiteAndRemoteId(SiteModel site, long remoteCommentId) {
        List<CommentModel> results = WellSql.select(CommentModel.class)
                .where()
                .equals(CommentModelTable.REMOTE_COMMENT_ID, remoteCommentId)
                .equals(CommentModelTable.LOCAL_SITE_ID, site.getId())
                .endWhere().getAsModel();
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    private static SelectQuery<CommentModel> getCommentsQueryForSite(SiteModel site, CommentStatus... statuses) {
        if (site == null) {
            return null;
        }

        ConditionClauseBuilder<SelectQuery<CommentModel>> selectQueryBuilder =
                WellSql.select(CommentModel.class)
                        .where().beginGroup()
                        .equals(CommentModelTable.LOCAL_SITE_ID, site.getId());

        // Check if statuses contains ALL
        if (!Arrays.asList(statuses).contains(CommentStatus.ALL)) {
            selectQueryBuilder.isIn(CommentModelTable.STATUS, Arrays.asList(statuses));
        }
        return selectQueryBuilder.endGroup().endWhere();
    }

    public static List<CommentModel> getCommentsForSite(SiteModel site, @Order int order, CommentStatus... statuses) {
        if (site == null) {
            return Collections.emptyList();
        }

        return getCommentsQueryForSite(site, statuses)
                .orderBy(CommentModelTable.DATE_PUBLISHED, order)
                .getAsModel();
    }

    public static int getCommentsCountForSite(SiteModel site, CommentStatus... statuses) {
        if (site == null) {
            return 0;
        }

        return (int) getCommentsQueryForSite(site, statuses).count();
    }
}
