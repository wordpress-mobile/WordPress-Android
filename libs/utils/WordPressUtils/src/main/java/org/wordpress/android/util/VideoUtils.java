package org.wordpress.android.util;


import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.io.File;

public class VideoUtils {
    public static long getVideoDurationMS(Context context, File file) {
        if(context == null || file == null) {
            AppLog.e(AppLog.T.MEDIA, "context and file can't be null.");
            return 0L;
        }
        return getVideoDurationMS(context, Uri.fromFile(file));
    }

    public static long getVideoDurationMS(Context context, Uri videoUri) {
        if(context == null || videoUri == null) {
            AppLog.e(AppLog.T.MEDIA, "context and videoUri can't be null.");
            return 0L;
        }
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, videoUri);
        } catch (IllegalArgumentException | SecurityException e) {
            AppLog.e(AppLog.T.MEDIA, "Can't read duration of the video.", e);
            return 0L;
        } catch (RuntimeException e) {
            // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/5431
            AppLog.e(AppLog.T.MEDIA, "Can't read duration of the video due to a Runtime Exception happened setting the datasource", e);
            return 0L;
        }

        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (time == null) {
            return 0L;
        }
        return Long.parseLong(time);
    }
}
