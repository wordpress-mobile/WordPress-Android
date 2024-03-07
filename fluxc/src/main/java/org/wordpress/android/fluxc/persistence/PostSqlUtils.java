package org.wordpress.android.fluxc.persistence;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wellsql.generated.LikeModelTable;
import com.wellsql.generated.LocalDiffModelTable;
import com.wellsql.generated.LocalRevisionModelTable;
import com.wellsql.generated.PostModelTable;
import com.yarolegovich.wellsql.ConditionClauseBuilder;
import com.yarolegovich.wellsql.SelectQuery;
import com.yarolegovich.wellsql.SelectQuery.Order;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;

import org.wordpress.android.fluxc.model.LikeModel;
import org.wordpress.android.fluxc.model.LikeModel.LikeType;
import org.wordpress.android.fluxc.model.LocalOrRemoteId;
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId;
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.revisions.LocalDiffModel;
import org.wordpress.android.fluxc.model.revisions.LocalRevisionModel;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRemoteAutoSaveModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import javax.inject.Inject;

import static org.wordpress.android.fluxc.model.LikeModel.TIMESTAMP_THRESHOLD;

import dagger.Reusable;

@Reusable
public class PostSqlUtils {
    @Inject public PostSqlUtils() {
    }

    public synchronized int insertOrUpdatePost(PostModel post, boolean overwriteLocalChanges) {
        if (post == null) {
            return 0;
        }
        List<PostModel> postResult;
        if (post.isLocalDraft()) {
            postResult = WellSql.select(PostModel.class)
                    .where()
                    .equals(PostModelTable.ID, post.getId())
                    .endWhere().getAsModel();
        } else {
            postResult = WellSql.select(PostModel.class)
                    .where().beginGroup()
                    .equals(PostModelTable.ID, post.getId())
                    .or()
                    .beginGroup()
                    .equals(PostModelTable.REMOTE_POST_ID, post.getRemotePostId())
                    .equals(PostModelTable.LOCAL_SITE_ID, post.getLocalSiteId())
                    .endGroup()
                    .endGroup().endWhere().getAsModel();
        }
        int numberOfDeletedRows = 0;
        if (postResult.isEmpty()) {
            // insert post
            post.setDbTimestamp(System.currentTimeMillis());
            WellSql.insert(post).asSingleTransaction(true).execute();
            return 1;
        } else {
            if (postResult.size() > 1) {
                // We've ended up with a duplicate entry, probably due to a push/fetch race
                // condition. One matches based on local ID (this is the one we're trying to
                // update with a remote post ID). The other matches based on local site ID +
                // remote post ID, and we got it from a fetch. Just remove the duplicated
                // entry we got from the fetch as the chance the client app is already using it is
                // lower (it was most probably fetched a few ms ago).
                ListIterator<PostModel> postModelListIterator = postResult.listIterator();
                while (postModelListIterator.hasNext()) {
                    PostModel item = postModelListIterator.next();
                    if (item.getId() != post.getId()) {
                        WellSql.delete(PostModel.class).whereId(item.getId());
                        postModelListIterator.remove();
                        numberOfDeletedRows++;
                    }
                }
            }
            // Update only if local changes for this post don't exist
            if (overwriteLocalChanges || !postResult.get(0).isLocallyChanged()) {
                int oldId = postResult.get(0).getId();
                post.setDbTimestamp(System.currentTimeMillis());
                return WellSql.update(PostModel.class).whereId(oldId)
                              .put(post, new UpdateAllExceptId<>(PostModel.class)).execute()
                       + numberOfDeletedRows;
            }
        }
        return numberOfDeletedRows;
    }

    public int insertOrUpdatePostKeepingLocalChanges(PostModel post) {
        return insertOrUpdatePost(post, false);
    }

    public int insertOrUpdatePostOverwritingLocalChanges(PostModel post) {
        return insertOrUpdatePost(post, true);
    }

    public List<PostModel> getPostsForSite(SiteModel site, boolean getPages) {
        if (site == null) {
            return Collections.emptyList();
        }

        return WellSql.select(PostModel.class)
                .where().beginGroup()
                .equals(PostModelTable.LOCAL_SITE_ID, site.getId())
                .equals(PostModelTable.IS_PAGE, getPages)
                .endGroup().endWhere()
                .orderBy(PostModelTable.IS_LOCAL_DRAFT, SelectQuery.ORDER_DESCENDING)
                .orderBy(PostModelTable.DATE_CREATED, SelectQuery.ORDER_DESCENDING)
                .getAsModel();
    }

    public List<PostModel> getPostsForSiteWithFormat(SiteModel site, List<String> postFormat, boolean getPages) {
        if (site == null) {
            return Collections.emptyList();
        }

        return WellSql.select(PostModel.class)
                .where().beginGroup()
                .equals(PostModelTable.LOCAL_SITE_ID, site.getId())
                .isIn(PostModelTable.POST_FORMAT, postFormat)
                .equals(PostModelTable.IS_PAGE, getPages)
                .endGroup().endWhere()
                .orderBy(PostModelTable.IS_LOCAL_DRAFT, SelectQuery.ORDER_DESCENDING)
                .orderBy(PostModelTable.DATE_CREATED, SelectQuery.ORDER_DESCENDING)
                .getAsModel();
    }

    public List<PostModel> getUploadedPostsForSite(SiteModel site, boolean getPages) {
        if (site == null) {
            return Collections.emptyList();
        }

        return WellSql.select(PostModel.class)
                .where().beginGroup()
                .equals(PostModelTable.LOCAL_SITE_ID, site.getId())
                .equals(PostModelTable.IS_PAGE, getPages)
                .equals(PostModelTable.IS_LOCAL_DRAFT, false)
                .endGroup().endWhere()
                .orderBy(PostModelTable.IS_LOCAL_DRAFT, SelectQuery.ORDER_DESCENDING)
                .orderBy(PostModelTable.DATE_CREATED, SelectQuery.ORDER_DESCENDING)
                .getAsModel();
    }

    public List<PostModel> getLocalDrafts(@NonNull Integer localSiteId, boolean isPage) {
        return WellSql.select(PostModel.class)
                      .where()
                      .beginGroup()
                      .equals(PostModelTable.LOCAL_SITE_ID, localSiteId)
                      .equals(PostModelTable.IS_LOCAL_DRAFT, true)
                      .equals(PostModelTable.IS_PAGE, isPage)
                      .endGroup()
                      .endWhere()
                      .getAsModel();
    }

    public List<PostModel> getPostsWithLocalChanges(@NonNull Integer localSiteId, boolean isPage) {
        return WellSql.select(PostModel.class)
                      .where()
                      .equals(PostModelTable.IS_PAGE, isPage)
                      .equals(PostModelTable.LOCAL_SITE_ID, localSiteId)
                      .beginGroup()
                      .equals(PostModelTable.IS_LOCAL_DRAFT, true).or().equals(PostModelTable.IS_LOCALLY_CHANGED, true)
                      .endGroup()
                      .endWhere()
                      .getAsModel();
    }

    public List<PostModel> getPostsByRemoteIds(@Nullable List<Long> remoteIds, int localSiteId) {
        if (remoteIds != null && remoteIds.size() > 0) {
            return WellSql.select(PostModel.class)
                          .where().isIn(PostModelTable.REMOTE_POST_ID, remoteIds)
                          .equals(PostModelTable.LOCAL_SITE_ID, localSiteId).endWhere()
                          .getAsModel();
        }
        return Collections.emptyList();
    }

    public List<PostModel> getPostsByLocalOrRemotePostIds(
            @NonNull List<? extends LocalOrRemoteId> localOrRemoteIds, int localSiteId) {
        if (localOrRemoteIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> localIds = new ArrayList<>();
        List<Long> remoteIds = new ArrayList<>();
        for (LocalOrRemoteId localOrRemoteId : localOrRemoteIds) {
            if (localOrRemoteId instanceof LocalId) {
                localIds.add(((LocalId) localOrRemoteId).getValue());
            } else if (localOrRemoteId instanceof RemoteId) {
                remoteIds.add(((RemoteId) localOrRemoteId).getValue());
            }
        }
        ConditionClauseBuilder<SelectQuery<PostModel>> whereQuery =
                WellSql.select(PostModel.class).where().equals(PostModelTable.LOCAL_SITE_ID, localSiteId).beginGroup();
        boolean addIsInLocalIdsCondition = !localIds.isEmpty();
        if (addIsInLocalIdsCondition) {
            whereQuery = whereQuery.isIn(PostModelTable.ID, localIds);
        }
        if (!remoteIds.isEmpty()) {
            if (addIsInLocalIdsCondition) {
                // Add `or` only if we are checking for both local and remote ids
                whereQuery = whereQuery.or();
            }
            whereQuery = whereQuery.isIn(PostModelTable.REMOTE_POST_ID, remoteIds);
        }
        return whereQuery.endGroup().endWhere().getAsModel();
    }

    public PostModel insertPostForResult(PostModel post) {
        WellSql.insert(post).asSingleTransaction(true).execute();

        return post;
    }

    public int deletePost(PostModel post) {
        if (post == null) {
            return 0;
        }

        return WellSql.delete(PostModel.class)
                .where().beginGroup()
                .equals(PostModelTable.ID, post.getId())
                .equals(PostModelTable.LOCAL_SITE_ID, post.getLocalSiteId())
                .endGroup()
                .endWhere()
                .execute();
    }

    public int deleteUploadedPostsForSite(SiteModel site, boolean pages) {
        if (site == null) {
            return 0;
        }

        return WellSql.delete(PostModel.class)
                .where().beginGroup()
                .equals(PostModelTable.LOCAL_SITE_ID, site.getId())
                .equals(PostModelTable.IS_PAGE, pages)
                .equals(PostModelTable.IS_LOCAL_DRAFT, false)
                .equals(PostModelTable.IS_LOCALLY_CHANGED, false)
                .endGroup()
                .endWhere()
                .execute();
    }

    public int deleteAllPosts() {
        return WellSql.delete(PostModel.class).execute();
    }

    public boolean getSiteHasLocalChanges(SiteModel site) {
        return site != null && WellSql.select(PostModel.class)
                .where().beginGroup()
                .equals(PostModelTable.LOCAL_SITE_ID, site.getId())
                .beginGroup()
                .equals(PostModelTable.IS_LOCAL_DRAFT, true)
                .or()
                .equals(PostModelTable.IS_LOCALLY_CHANGED, true)
                .endGroup().endGroup().endWhere().exists();
    }

    public int getNumLocalChanges() {
        return (int) WellSql.select(PostModel.class)
                            .where().beginGroup()
                            .equals(PostModelTable.IS_LOCAL_DRAFT, true)
                            .or()
                            .equals(PostModelTable.IS_LOCALLY_CHANGED, true)
                            .endGroup().endWhere()
                            .count();
    }

    public int updatePostsAutoSave(SiteModel site, final PostRemoteAutoSaveModel autoSaveModel) {
        return WellSql.update(PostModel.class)
               .where().beginGroup()
               .equals(PostModelTable.LOCAL_SITE_ID, site.getId())
               .equals(PostModelTable.REMOTE_POST_ID, autoSaveModel.getRemotePostId())
               .endGroup().endWhere()
               .put(autoSaveModel, new InsertMapper<PostRemoteAutoSaveModel>() {
                   @Override
                   public ContentValues toCv(PostRemoteAutoSaveModel item) {
                       ContentValues cv = new ContentValues();
                       cv.put(PostModelTable.AUTO_SAVE_REVISION_ID, autoSaveModel.getRevisionId());
                       cv.put(PostModelTable.AUTO_SAVE_MODIFIED, autoSaveModel.getModified());
                       cv.put(PostModelTable.AUTO_SAVE_PREVIEW_URL, autoSaveModel.getPreviewUrl());
                       cv.put(PostModelTable.REMOTE_AUTO_SAVE_MODIFIED, autoSaveModel.getModified());
                       return cv;
                   }
               }).execute();
    }

    public void insertOrUpdateLocalRevision(LocalRevisionModel revision, List<LocalDiffModel> diffs) {
        boolean hasLocalRevisionModels =
                WellSql.select(LocalRevisionModel.class)
                        .where().beginGroup()
                        .equals(LocalRevisionModelTable.REVISION_ID, revision.getRevisionId())
                        .equals(LocalRevisionModelTable.POST_ID, revision.getPostId())
                        .equals(LocalRevisionModelTable.SITE_ID, revision.getSiteId())
                        .endGroup().endWhere().exists();
        if (hasLocalRevisionModels) {
            WellSql.update(LocalRevisionModel.class)
                    .where().beginGroup()
                    .equals(LocalRevisionModelTable.REVISION_ID, revision.getRevisionId())
                    .equals(LocalRevisionModelTable.POST_ID, revision.getPostId())
                    .equals(LocalRevisionModelTable.SITE_ID, revision.getSiteId())
                    .endGroup().endWhere()
                    .put(revision, new UpdateAllExceptId<>(LocalRevisionModel.class)).execute();
        } else {
            WellSql.insert(revision).execute();
        }

        // we need to maintain order of diffs, so it's better to remove all of existing ones beforehand
        WellSql.delete(LocalDiffModel.class)
                .where().beginGroup()
                .equals(LocalDiffModelTable.REVISION_ID, revision.getRevisionId())
                .equals(LocalDiffModelTable.POST_ID, revision.getPostId())
                .equals(LocalDiffModelTable.SITE_ID, revision.getSiteId())
                .endGroup().endWhere().execute();

        for (LocalDiffModel diff : diffs) {
            WellSql.insert(diff).execute();
        }
    }

    public List<LocalRevisionModel> getLocalRevisions(SiteModel site, PostModel post) {
        return WellSql.select(LocalRevisionModel.class)
                .where().beginGroup()
                .equals(LocalRevisionModelTable.POST_ID, post.getRemotePostId())
                .equals(LocalRevisionModelTable.SITE_ID, site.getSiteId())
                .endGroup().endWhere().getAsModel();
    }

    @Nullable
    public LocalRevisionModel getRevisionById(@NonNull final String revisionId, final long postId, final long siteId) {
        final List<LocalRevisionModel> localRevisionModels = WellSql.select(LocalRevisionModel.class)
                      .where().beginGroup()
                      .equals(LocalRevisionModelTable.REVISION_ID, revisionId)
                      .equals(LocalRevisionModelTable.POST_ID, postId)
                      .equals(LocalRevisionModelTable.SITE_ID, siteId)
                      .endGroup().endWhere().getAsModel();
        if (localRevisionModels != null && !localRevisionModels.isEmpty()) {
            return localRevisionModels.get(0);
        } else {
            return null;
        }
    }

    public List<LocalDiffModel> getLocalRevisionDiffs(LocalRevisionModel revision) {
        return WellSql.select(LocalDiffModel.class)
                .where().beginGroup()
                .equals(LocalDiffModelTable.POST_ID, revision.getPostId())
                .equals(LocalDiffModelTable.REVISION_ID, revision.getRevisionId())
                .equals(LocalDiffModelTable.SITE_ID, revision.getSiteId())
                .endGroup().endWhere().getAsModel();
    }

    public void deleteLocalRevisionAndDiffs(LocalRevisionModel revision) {
        WellSql.delete(LocalRevisionModel.class)
                .where().beginGroup()
                .equals(LocalRevisionModelTable.REVISION_ID, revision.getRevisionId())
                .equals(LocalRevisionModelTable.POST_ID, revision.getPostId())
                .equals(LocalRevisionModelTable.SITE_ID, revision.getSiteId())
                .endGroup().endWhere().execute();

        WellSql.delete(LocalDiffModel.class)
                .where().beginGroup()
                .equals(LocalDiffModelTable.REVISION_ID, revision.getRevisionId())
                .equals(LocalDiffModelTable.POST_ID, revision.getPostId())
                .equals(LocalDiffModelTable.SITE_ID, revision.getSiteId())
                .endGroup().endWhere().execute();
    }

    public void deleteLocalRevisionAndDiffsOfAPostOrPage(PostModel post) {
        WellSql.delete(LocalRevisionModel.class)
                .where().beginGroup()
                .equals(LocalRevisionModelTable.POST_ID, post.getRemotePostId())
                .equals(LocalRevisionModelTable.SITE_ID, post.getRemoteSiteId())
                .endGroup().endWhere().execute();

        WellSql.delete(LocalDiffModel.class)
                .where().beginGroup()
                .equals(LocalRevisionModelTable.POST_ID, post.getRemotePostId())
                .equals(LocalRevisionModelTable.SITE_ID, post.getRemoteSiteId())
                .endGroup().endWhere().execute();
    }

    public void deleteAllLocalRevisionsAndDiffs() {
        WellSql.delete(LocalRevisionModel.class).execute();
        WellSql.delete(LocalDiffModel.class).execute();
    }

    public List<LocalId> getLocalPostIdsForFilter(SiteModel site, boolean isPage, String searchQuery,
                                                         String orderBy, @Order int order) {
        ConditionClauseBuilder<SelectQuery<PostModel>> clauseBuilder =
                WellSql.select(PostModel.class)
                       // We only need the local ids
                       .columns(PostModelTable.ID)
                       .where().beginGroup()
                       .equals(PostModelTable.IS_LOCAL_DRAFT, true)
                       .equals(PostModelTable.LOCAL_SITE_ID, site.getId())
                       .equals(PostModelTable.IS_PAGE, isPage)
                       .endGroup();
        if (!TextUtils.isEmpty(searchQuery)) {
            clauseBuilder = clauseBuilder.beginGroup().contains(PostModelTable.TITLE, searchQuery).or()
                                         .contains(PostModelTable.CONTENT, searchQuery).endGroup();
        }
        /*
         * Remember that, since we are only querying the `PostModelTable.ID` column, the rest of the fields for the
         * post won't be there which is exactly what we want.
         */
        List<PostModel> localPosts = clauseBuilder.endWhere().orderBy(orderBy, order).getAsModel();
        List<LocalId> localPostIds = new ArrayList<>();
        for (PostModel post : localPosts) {
            localPostIds.add(new LocalId(post.getId()));
        }
        return localPostIds;
    }

    public int deletePostLikesAndPurgeExpired(long siteId, long remotePostId) {
        int numDeleted = WellSql.delete(LikeModel.class)
                                .where()
                                .beginGroup()
                                .equals(LikeModelTable.TYPE, LikeType.POST_LIKE.getTypeName())
                                .equals(LikeModelTable.REMOTE_SITE_ID, siteId)
                                .equals(LikeModelTable.REMOTE_ITEM_ID, remotePostId)
                                .endGroup()
                                .endWhere()
                                .execute();

        SQLiteDatabase db = WellSql.giveMeWritableDb();
        db.beginTransaction();
        try {
            List<LikeModel> likeResult = WellSql.select(LikeModel.class)
                                                .columns(LikeModelTable.REMOTE_SITE_ID, LikeModelTable.REMOTE_ITEM_ID)
                                                .where().beginGroup()
                                                .equals(LikeModelTable.TYPE, LikeType.POST_LIKE.getTypeName())
                                                .not().equals(LikeModelTable.REMOTE_SITE_ID, siteId)
                                                .not().equals(LikeModelTable.REMOTE_ITEM_ID, remotePostId)
                                                .lessThen(LikeModelTable.TIMESTAMP_FETCHED,
                                                        (new Date().getTime()) - TIMESTAMP_THRESHOLD)
                                                .endGroup().endWhere()
                                                .getAsModel();

            for (LikeModel likeModel : likeResult) {
                numDeleted += WellSql.delete(LikeModel.class)
                                     .where()
                                     .beginGroup()
                                     .equals(LikeModelTable.TYPE, LikeType.POST_LIKE.getTypeName())
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

    public int insertOrUpdatePostLikes(long siteId, long remotePostId, LikeModel like) {
        if (null == like) {
            return 0;
        }

        List<LikeModel> likeResult;

        // If the like already exist and has an id, we want to update it.
        likeResult = WellSql.select(LikeModel.class).where()
                            .beginGroup()
                            .equals(LikeModelTable.TYPE, LikeType.POST_LIKE.getTypeName())
                            .equals(LikeModelTable.REMOTE_SITE_ID, siteId)
                            .equals(LikeModelTable.REMOTE_ITEM_ID, remotePostId)
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

    public List<LikeModel> getPostLikesByPostId(long siteId, long remotePostId) {
        return WellSql.select(LikeModel.class)
                      .where()
                      .beginGroup()
                      .equals(LikeModelTable.TYPE, LikeType.POST_LIKE.getTypeName())
                      .equals(LikeModelTable.REMOTE_SITE_ID, siteId)
                      .equals(LikeModelTable.REMOTE_ITEM_ID, remotePostId)
                      .endGroup()
                      .endWhere()
                      .getAsModel();
    }
}
