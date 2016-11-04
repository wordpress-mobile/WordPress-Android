package org.wordpress.android.fluxc.media;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.persistence.MediaSqlUtils;
import org.wordpress.android.fluxc.utils.MediaUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class MediaTestUtils {
    public static int insertMediaIntoDatabase(MediaModel media) {
        return MediaSqlUtils.insertOrUpdateMedia(media);
    }

    public static List<MediaModel> insertRandomMediaIntoDatabase(long siteId, int count) {
        List<MediaModel> insertedMedia = generateRandomizedMediaList(count, siteId);
        for (MediaModel media : insertedMedia) {
            assertTrue(MediaSqlUtils.insertOrUpdateMedia(media) == 0);
        }
        return insertedMedia;
    }

    public static MediaModel generateMedia(String title, String desc, String caption, String alt) {
        MediaModel media = new MediaModel();
        media.setTitle(title);
        media.setDescription(desc);
        media.setCaption(caption);
        media.setAlt(alt);
        return media;
    }

    public static MediaModel generateMediaFromPath(long siteId, long mediaId, String filePath) {
        MediaModel media = new MediaModel();
        media.setSiteId(siteId);
        media.setMediaId(mediaId);
        media.setFilePath(filePath);
        media.setFileName(MediaUtils.getFileName(filePath));
        media.setFileExtension(MediaUtils.getExtension(filePath));
        media.setMimeType(MediaUtils.getMimeTypeForExtension(media.getFileExtension()));
        media.setTitle(media.getFileName());
        return media;
    }

    public static MediaModel generateRandomizedMedia(long siteId) {
        MediaModel media = generateMedia(randomStr(5), randomStr(5), randomStr(5), randomStr(5));
        media.setSiteId(siteId);
        return media;
    }

    public static List<MediaModel> generateRandomizedMediaList(int size, long siteId) {
        List<MediaModel> mediaList = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            MediaModel newMedia = generateRandomizedMedia(siteId);
            newMedia.setMediaId(i);
            mediaList.add(newMedia);
        }
        return mediaList;
    }

    public static String randomStr(int length) {
        String randomString = UUID.randomUUID().toString();
        return length > randomString.length() ? randomString : randomString.substring(0, length);
    }
}
