package org.wordpress.android.fluxc.persistence;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wellsql.generated.CommentModelTable;
import com.wellsql.generated.LikeModelTable;
import com.yarolegovich.wellsql.ConditionClauseBuilder;
import com.yarolegovich.wellsql.SelectQuery;
import com.yarolegovich.wellsql.SelectQuery.Order;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.LikeModel;
import org.wordpress.android.fluxc.model.LikeModel.LikeType;
import org.wordpress.android.fluxc.model.SiteModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static org.wordpress.android.fluxc.model.LikeModel.TIMESTAMP_THRESHOLD;

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

    public static int removeCommentGaps(SiteModel site, List<CommentModel> comments, int maxEntriesInResponse,
                                        int requestOffset, @Nullable CommentStatus... statuses) {
        if (site == null || comments == null || comments.isEmpty()) {
            return 0;
        }

        Collections.sort(comments, new Comparator<CommentModel>() {
            @Override
            public int compare(CommentModel o1, CommentModel o2) {
                long x = o2.getPublishedTimestamp();
                long y = o1.getPublishedTimestamp();
                return (x < y) ? -1 : ((x == y) ? 0 : 1);
            }
        });

        ArrayList<Long> remoteIds = new ArrayList<>();
        for (CommentModel comment : comments) {
            remoteIds.add(comment.getRemoteCommentId());
        }

        long startOfRange = comments.get(0).getPublishedTimestamp();
        long endOfRange = comments.get(comments.size() - 1).getPublishedTimestamp();

        ArrayList<CommentStatus> targetStatuses = new ArrayList<>();
        if (Arrays.asList(statuses).contains(CommentStatus.ALL)) {
            targetStatuses.add(CommentStatus.APPROVED);
            targetStatuses.add(CommentStatus.UNAPPROVED);
        } else {
            targetStatuses.addAll(Arrays.asList(statuses));
        }

        int numOfDeletedComments = 0;

        // try to trim comments from the top
        if (requestOffset == 0) {
            numOfDeletedComments += WellSql.delete(CommentModel.class)
                                           .where()
                                           .equals(CommentModelTable.LOCAL_SITE_ID, site.getId())
                                           .isIn(CommentModelTable.STATUS, targetStatuses)
                                           .isNotIn(CommentModelTable.REMOTE_COMMENT_ID, remoteIds)
                                           .greaterThenOrEqual(CommentModelTable.PUBLISHED_TIMESTAMP, startOfRange)
                                           .endWhere()
                                           .execute();
        }

        // try to trim comments from the bottom
        if (comments.size() < maxEntriesInResponse) {
            numOfDeletedComments += WellSql.delete(CommentModel.class)
                                           .where()
                                           .equals(CommentModelTable.LOCAL_SITE_ID, site.getId())
                                           .isIn(CommentModelTable.STATUS, targetStatuses)
                                           .isNotIn(CommentModelTable.REMOTE_COMMENT_ID, remoteIds)
                                           .lessThenOrEqual(CommentModelTable.PUBLISHED_TIMESTAMP, endOfRange)
                                           .endWhere()
                                           .execute();
        }

        // remove comments from the middle
        return numOfDeletedComments + WellSql.delete(CommentModel.class)
                                             .where()
                                             .equals(CommentModelTable.LOCAL_SITE_ID, site.getId())
                                             .isIn(CommentModelTable.STATUS, targetStatuses)
                                             .isNotIn(CommentModelTable.REMOTE_COMMENT_ID, remoteIds)
                                             .lessThenOrEqual(CommentModelTable.PUBLISHED_TIMESTAMP, startOfRange)
                                             .greaterThenOrEqual(CommentModelTable.PUBLISHED_TIMESTAMP, endOfRange)
                                             .endWhere()
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

    @Nullable
    public static CommentModel getCommentBySiteAndRemoteId(@NonNull SiteModel site, long remoteCommentId) {
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
        return getCommentsQueryForSite(site, 0, statuses);
    }

    private static SelectQuery<CommentModel> getCommentsQueryForSite(SiteModel site, int limit,
                                                                     CommentStatus... statuses) {
        if (site == null) {
            return null;
        }

        SelectQuery<CommentModel> query = WellSql.select(CommentModel.class);

        if (limit > 0) {
            query.limit(limit);
        }

        ConditionClauseBuilder<SelectQuery<CommentModel>> selectQueryBuilder =
                query.where().beginGroup()
                     .equals(CommentModelTable.LOCAL_SITE_ID, site.getId());

        // Check if statuses contains ALL
        if (!Arrays.asList(statuses).contains(CommentStatus.ALL)) {
            selectQueryBuilder.isIn(CommentModelTable.STATUS, Arrays.asList(statuses));
        }
        return selectQueryBuilder.endGroup().endWhere();
    }

    public static List<CommentModel> getCommentsForSite(SiteModel site, @Order int order, CommentStatus... statuses) {
        return getCommentsForSite(site, order, 0, statuses);
    }

    public static List<CommentModel> getCommentsForSite(SiteModel site, @Order int order, int limit,
                                                        CommentStatus... statuses) {
        if (site == null) {
            return Collections.emptyList();
        }

        return getCommentsQueryForSite(site, limit, statuses)
                .orderBy(CommentModelTable.DATE_PUBLISHED, order)
                .getAsModel();
    }

    public static int getCommentsCountForSite(SiteModel site, CommentStatus... statuses) {
        if (site == null) {
            return 0;
        }

        return (int) getCommentsQueryForSite(site, statuses).count();
    }

    public static int deleteCommentLikesAndPurgeExpired(long siteId, long remoteCommentId) {
        int numDeleted = WellSql.delete(LikeModel.class)
                                .where()
                                .beginGroup()
                                .equals(LikeModelTable.TYPE, LikeType.COMMENT_LIKE.getTypeName())
                                .equals(LikeModelTable.REMOTE_SITE_ID, siteId)
                                .equals(LikeModelTable.REMOTE_ITEM_ID, remoteCommentId)
                                .endGroup()
                                .endWhere()
                                .execute();

        SQLiteDatabase db = WellSql.giveMeWritableDb();
        db.beginTransaction();
        try {
            List<LikeModel> likeResult = WellSql.select(LikeModel.class)
                                                .columns(LikeModelTable.REMOTE_SITE_ID, LikeModelTable.REMOTE_ITEM_ID)
                                                .where().beginGroup()
                                                .equals(LikeModelTable.TYPE, LikeType.COMMENT_LIKE.getTypeName())
                                                .not().equals(LikeModelTable.REMOTE_SITE_ID, siteId)
                                                .not().equals(LikeModelTable.REMOTE_ITEM_ID, remoteCommentId)
                                                .lessThen(LikeModelTable.TIMESTAMP_FETCHED,
                                                        (new Date().getTime()) - TIMESTAMP_THRESHOLD)
                                                .endGroup().endWhere()
                                                .getAsModel();

            for (LikeModel likeModel : likeResult) {
                numDeleted += WellSql.delete(LikeModel.class)
                                     .where()
                                     .beginGroup()
                                     .equals(LikeModelTable.TYPE, LikeType.COMMENT_LIKE.getTypeName())
                                     .equals(LikeModelTable.REMOTE_SITE_ID, likeModel.getRemoteSiteId())
                                     .equals(LikeModelTable.REMOTE_ITEM_ID, likeModel.getRemoteItemId())
                                     .endGroup()
                                     .endWhere()
                                     .execute();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return numDeleted;
    }

    public static int insertOrUpdateCommentLikes(long siteId, long remoteCommentId, LikeModel like) {
        if (null == like) {
            return 0;
        }

        List<LikeModel> likeResult;

        // If the like already exists and has an id, we want to update it.
        likeResult = WellSql.select(LikeModel.class).where()
                            .beginGroup()
                            .equals(LikeModelTable.TYPE, LikeType.COMMENT_LIKE.getTypeName())
                            .equals(LikeModelTable.REMOTE_SITE_ID, siteId)
                            .equals(LikeModelTable.REMOTE_ITEM_ID, remoteCommentId)
                            .equals(LikeModelTable.LIKER_ID, like.getLikerId())
                            .endGroup().endWhere().getAsModel();

        if (likeResult.isEmpty()) {
            // insert
            WellSql.insert(like).asSingleTransaction(true).execute();
            return 1;
        } else {
            // update
            int oldId = likeResult.get(0).getId();
            return WellSql.update(LikeModel.class).whereId(oldId)
                          .put(like, new UpdateAllExceptId<>(LikeModel.class)).execute();
        }
    }

    public static List<LikeModel> getCommentLikesByCommentId(long siteId, long remoteCommentId) {
        return WellSql.select(LikeModel.class)
                      .where()
                      .beginGroup()
                      .equals(LikeModelTable.TYPE, LikeType.COMMENT_LIKE.getTypeName())
                      .equals(LikeModelTable.REMOTE_SITE_ID, siteId)
                      .equals(LikeModelTable.REMOTE_ITEM_ID, remoteCommentId)
                      .endGroup()
                      .endWhere()
                      .getAsModel();
    }
}
