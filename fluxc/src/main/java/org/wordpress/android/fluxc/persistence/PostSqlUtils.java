package org.wordpress.android.fluxc.persistence;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.wellsql.generated.LocalDiffModelTable;
import com.wellsql.generated.LocalRevisionModelTable;
import com.wellsql.generated.PostModelTable;
import com.yarolegovich.wellsql.ConditionClauseBuilder;
import com.yarolegovich.wellsql.SelectQuery;
import com.yarolegovich.wellsql.SelectQuery.Order;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.revisions.LocalDiffModel;
import org.wordpress.android.fluxc.model.revisions.LocalRevisionModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostSqlUtils {
    public static int insertOrUpdatePost(PostModel post, boolean overwriteLocalChanges) {
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

        if (postResult.isEmpty()) {
            // insert
            WellSql.insert(post).asSingleTransaction(true).execute();
            return 1;
        } else {
            if (postResult.size() > 1) {
                // We've ended up with a duplicate entry, probably due to a push/fetch race condition
                // One matches based on local ID (this is the one we're trying to update with a remote post ID)
                // The other matches based on local site ID + remote post ID, and we got it from a fetch
                // Just remove the entry without a remote post ID (the one matching the current post's local ID)
                return WellSql.delete(PostModel.class).whereId(post.getId());
            }
            // Update only if local changes for this post don't exist
            if (overwriteLocalChanges || !postResult.get(0).isLocallyChanged()) {
                int oldId = postResult.get(0).getId();
                return WellSql.update(PostModel.class).whereId(oldId)
                        .put(post, new UpdateAllExceptId<>(PostModel.class)).execute();
            }
        }
        return 0;
    }

    public static int insertOrUpdatePostKeepingLocalChanges(PostModel post) {
        return insertOrUpdatePost(post, false);
    }

    public static int insertOrUpdatePostOverwritingLocalChanges(PostModel post) {
        return insertOrUpdatePost(post, true);
    }

    public static List<PostModel> getPostsForSite(SiteModel site, boolean getPages) {
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

    public static List<PostModel> getPostsForSiteWithFormat(SiteModel site, List<String> postFormat, boolean getPages) {
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

    public static List<PostModel> getUploadedPostsForSite(SiteModel site, boolean getPages) {
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

    public static List<PostModel> getPostsByRemoteIds(@Nullable List<Long> remoteIds, int localSiteId) {
        if (remoteIds != null && remoteIds.size() > 0) {
            return WellSql.select(PostModel.class)
                          .where().isIn(PostModelTable.REMOTE_POST_ID, remoteIds)
                          .equals(PostModelTable.LOCAL_SITE_ID, localSiteId).endWhere()
                          .getAsModel();
        }
        return Collections.emptyList();
    }

    public static PostModel insertPostForResult(PostModel post) {
        WellSql.insert(post).asSingleTransaction(true).execute();

        return post;
    }

    public static int deletePost(PostModel post) {
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

    public static int deleteUploadedPostsForSite(SiteModel site, boolean pages) {
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

    public static int deleteAllPosts() {
        return WellSql.delete(PostModel.class).execute();
    }

    public static boolean getSiteHasLocalChanges(SiteModel site) {
        return site != null && WellSql.select(PostModel.class)
                .where().beginGroup()
                .equals(PostModelTable.LOCAL_SITE_ID, site.getId())
                .beginGroup()
                .equals(PostModelTable.IS_LOCAL_DRAFT, true)
                .or()
                .equals(PostModelTable.IS_LOCALLY_CHANGED, true)
                .endGroup().endGroup().endWhere().getAsCursor().getCount() > 0;
    }

    public static int getNumLocalChanges() {
        return WellSql.select(PostModel.class)
                .where().beginGroup()
                .equals(PostModelTable.IS_LOCAL_DRAFT, true)
                .or()
                .equals(PostModelTable.IS_LOCALLY_CHANGED, true)
                .endGroup().endWhere()
                .getAsCursor().getCount();
    }

    public static void insertOrUpdateLocalRevision(LocalRevisionModel revision, List<LocalDiffModel> diffs) {
        int localRevisionModels =
                WellSql.select(LocalRevisionModel.class)
                        .where().beginGroup()
                        .equals(LocalRevisionModelTable.REVISION_ID, revision.getRevisionId())
                        .equals(LocalRevisionModelTable.POST_ID, revision.getPostId())
                        .equals(LocalRevisionModelTable.SITE_ID, revision.getSiteId())
                        .endGroup().endWhere().getAsCursor().getCount();
        if (localRevisionModels > 0) {
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

    public static List<LocalRevisionModel> getLocalRevisions(SiteModel site, PostModel post) {
        return WellSql.select(LocalRevisionModel.class)
                .where().beginGroup()
                .equals(LocalRevisionModelTable.POST_ID, post.getRemotePostId())
                .equals(LocalRevisionModelTable.SITE_ID, site.getSiteId())
                .endGroup().endWhere().getAsModel();
    }

    public static List<LocalDiffModel> getLocalRevisionDiffs(LocalRevisionModel revision) {
        return WellSql.select(LocalDiffModel.class)
                .where().beginGroup()
                .equals(LocalDiffModelTable.POST_ID, revision.getPostId())
                .equals(LocalDiffModelTable.REVISION_ID, revision.getRevisionId())
                .equals(LocalDiffModelTable.SITE_ID, revision.getSiteId())
                .endGroup().endWhere().getAsModel();
    }

    public static void deleteLocalRevisionAndDiffs(LocalRevisionModel revision) {
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

    public static void deleteLocalRevisionAndDiffsOfAPostOrPage(PostModel post) {
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

    public static List<LocalId> getLocalPostIdsForFilter(SiteModel site, boolean isPage, String searchQuery,
                                                         String orderBy, @Order int order) {
        ConditionClauseBuilder<SelectQuery<PostModel>> clauseBuilder =
                WellSql.select(PostModel.class).where().beginGroup()
                       .equals(PostModelTable.IS_LOCAL_DRAFT, true)
                       .equals(PostModelTable.LOCAL_SITE_ID, site.getId())
                       .equals(PostModelTable.IS_PAGE, isPage)
                       .endGroup();
        if (!TextUtils.isEmpty(searchQuery)) {
            clauseBuilder = clauseBuilder.beginGroup().contains(PostModelTable.TITLE, searchQuery).or()
                                         .contains(PostModelTable.CONTENT, searchQuery).endGroup();
        }
        // TODO: We should only query the id and return that instead
        List<PostModel> localPosts = clauseBuilder.endWhere().orderBy(orderBy, order).getAsModel();
        List<LocalId> localPostIds = new ArrayList<>();
        for (PostModel post : localPosts) {
            localPostIds.add(new LocalId(post.getId()));
        }
        return localPostIds;
    }
}
