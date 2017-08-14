package org.wordpress.android.fluxc.persistence;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.wellsql.generated.MediaUploadModelTable;
import com.wellsql.generated.PostModelTable;
import com.wellsql.generated.PostUploadModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostUploadModel;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UploadSqlUtils {
    public static int insertOrUpdateMedia(MediaUploadModel media) {
        if (media == null) return 0;

        List<MediaUploadModel> existingMedia;
        existingMedia = WellSql.select(MediaUploadModel.class)
                .where()
                .equals(MediaUploadModelTable.ID, media.getId())
                .endWhere().getAsModel();

        if (existingMedia.isEmpty()) {
            // Insert, media item does not exist
            WellSql.insert(media).asSingleTransaction(true).execute();
            return 1;
        } else {
            // Update, media item already exists
            int oldId = existingMedia.get(0).getId();
            return WellSql.update(MediaUploadModel.class).whereId(oldId)
                    .put(media, new UpdateAllExceptId<>(MediaUploadModel.class)).execute();
        }
    }

    public static @Nullable MediaUploadModel getMediaUploadModelForLocalId(int localMediaId) {
        List<MediaUploadModel> result = WellSql.select(MediaUploadModel.class).where()
                .equals(MediaUploadModelTable.ID, localMediaId)
                .endWhere()
                .getAsModel();
        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }

    public static int deleteMediaUploadModelWithLocalId(int localMediaId) {
        return WellSql.delete(MediaUploadModel.class)
                .where()
                .equals(MediaUploadModelTable.ID, localMediaId)
                .endWhere()
                .execute();
    }

    public static int deleteMediaUploadModelsWithLocalIds(Set<Integer> localMediaIds) {
        if (localMediaIds.size() > 0) {
            return WellSql.delete(MediaUploadModel.class)
                    .where()
                    .isIn(MediaUploadModelTable.ID, localMediaIds)
                    .endWhere()
                    .execute();
        }
        return 0;
    }

    public static int insertOrUpdatePost(PostUploadModel post) {
        if (post == null) return 0;

        List<PostUploadModel> existingPosts;
        existingPosts = WellSql.select(PostUploadModel.class)
                .where()
                .equals(PostUploadModelTable.ID, post.getId())
                .endWhere().getAsModel();

        if (existingPosts.isEmpty()) {
            // Insert, post does not exist
            WellSql.insert(post).asSingleTransaction(true).execute();
            return 1;
        } else {
            // Update, post already exists
            int oldId = existingPosts.get(0).getId();
            return WellSql.update(PostUploadModel.class).whereId(oldId)
                    .put(post, new UpdateAllExceptId<>(PostUploadModel.class)).execute();
        }
    }

    public static @Nullable PostUploadModel getPostUploadModelForLocalId(int localPostId) {
        List<PostUploadModel> result = WellSql.select(PostUploadModel.class).where()
                .equals(PostUploadModelTable.ID, localPostId)
                .endWhere()
                .getAsModel();
        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }

    public static @NonNull List<PostUploadModel> getPostUploadModelsWithState(@PostUploadModel.UploadState int state) {
        return WellSql.select(PostUploadModel.class).where()
                .equals(PostUploadModelTable.UPLOAD_STATE, state)
                .endWhere()
                .getAsModel();
    }

    public static @NonNull List<PostModel> getPostModelsForPostUploadModels(List<PostUploadModel> postUploadModels) {
        if (postUploadModels.size() > 0) {
            Set<Integer> postIdSet = new HashSet<>();
            for (PostUploadModel postUploadModel : postUploadModels) {
                postIdSet.add(postUploadModel.getId());
            }
            return WellSql.select(PostModel.class).where()
                    .isIn(PostModelTable.ID, postIdSet)
                    .endWhere().getAsModel();
        }
        return Collections.emptyList();
    }

    public static int deletePostUploadModelWithLocalId(int localPostId) {
        return WellSql.delete(PostUploadModel.class)
                .where()
                .equals(PostUploadModelTable.ID, localPostId)
                .endWhere()
                .execute();
    }

    public static int deletePostUploadModelsWithLocalIds(Set<Integer> localPostIds) {
        if (localPostIds.size() > 0) {
            return WellSql.delete(PostUploadModel.class)
                    .where()
                    .isIn(PostUploadModelTable.ID, localPostIds)
                    .endWhere()
                    .execute();
        }
        return 0;
    }
}
