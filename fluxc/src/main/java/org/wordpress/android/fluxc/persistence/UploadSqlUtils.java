package org.wordpress.android.fluxc.persistence;

import android.support.annotation.Nullable;

import com.wellsql.generated.MediaUploadModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.MediaUploadModel;

import java.util.List;

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
}
