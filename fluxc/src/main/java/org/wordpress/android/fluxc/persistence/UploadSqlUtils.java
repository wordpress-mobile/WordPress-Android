package org.wordpress.android.fluxc.persistence;

import android.content.ContentValues;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wellsql.generated.MediaModelTable;
import com.wellsql.generated.MediaUploadModelTable;
import com.wellsql.generated.PostModelTable;
import com.wellsql.generated.PostUploadModelTable;
import com.yarolegovich.wellsql.WellCursor;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostUploadModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.wordpress.android.fluxc.persistence.WellSqlConfig.SQLITE_MAX_VARIABLE_NUMBER;

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

    public static int updateMediaProgressOnly(MediaUploadModel media) {
        if (media == null) return 0;

        List<MediaUploadModel> existingMedia;
        existingMedia = WellSql.select(MediaUploadModel.class)
                .where()
                .equals(MediaUploadModelTable.ID, media.getId())
                .endWhere().getAsModel();

        if (existingMedia.isEmpty()) {
            // We're only interested in updating the progress for existing MediaUploadModels
            return 0;
        } else {
            // Update, media item already exists
            int oldId = existingMedia.get(0).getId();
            return WellSql.update(MediaUploadModel.class).whereId(oldId)
                    .put(media, new InsertMapper<MediaUploadModel>() {
                        @Override
                        public ContentValues toCv(MediaUploadModel item) {
                            ContentValues cv = new ContentValues();
                            cv.put(MediaUploadModelTable.PROGRESS, item.getProgress());
                            return cv;
                        }
                    }).execute();
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

    public static Set<MediaUploadModel> getMediaUploadModelsForPostId(int localPostId) {
        WellCursor<MediaModel> mediaModelCursor = WellSql.select(MediaModel.class)
                .columns(MediaModelTable.ID)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_POST_ID, localPostId)
                .endGroup().endWhere()
                .getAsCursor();

        Set<MediaUploadModel> mediaUploadModels = new HashSet<>();
        while (mediaModelCursor.moveToNext()) {
            MediaUploadModel mediaUploadModel = getMediaUploadModelForLocalId(mediaModelCursor.getInt(0));
            if (mediaUploadModel != null) {
                mediaUploadModels.add(mediaUploadModel);
            }
        }
        mediaModelCursor.close();

        return mediaUploadModels;
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

    public static @NonNull List<PostUploadModel> getAllPostUploadModels() {
        return WellSql.select(PostUploadModel.class).getAsModel();
    }

    public static @NonNull List<PostModel> getPostModelsForPostUploadModels(List<PostUploadModel> postUploadModels) {
        if (postUploadModels.size() > 0) {
            List<List<PostUploadModel>> batches = getBatches(postUploadModels, SQLITE_MAX_VARIABLE_NUMBER);
            List<PostModel> postModelList = new ArrayList<>();

            for (List<PostUploadModel> batch : batches) {
                Set<Integer> postIdSet = new HashSet<>();

                for (PostUploadModel postUploadModel : batch) {
                    postIdSet.add(postUploadModel.getId());
                }
                postModelList.addAll(
                        WellSql.select(PostModel.class)
                               .where()
                               .isIn(PostModelTable.ID, postIdSet)
                               .endWhere()
                               .getAsModel()
                );
            }
            return postModelList;
        }
        return Collections.emptyList();
    }

    public static <T> List<List<T>> getBatches(List<T> collection, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < collection.size(); i += batchSize) {
            batches.add(collection.subList(i, Math.min(i + batchSize, collection.size())));
        }
        return batches;
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
