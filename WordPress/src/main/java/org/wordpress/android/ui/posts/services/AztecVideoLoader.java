package org.wordpress.android.ui.posts.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import org.wordpress.android.editor.MediaUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.aztec.Html;

import java.io.File;

public class AztecVideoLoader implements Html.VideoThumbnailGetter {

    private Context context;
    private final Drawable loadingInProgress;

    public AztecVideoLoader(Context context, Drawable loadingInProgressDrawable) {
        this.context = context;
        this.loadingInProgress = loadingInProgressDrawable;
    }

    public void loadVideoThumbnail(final String url, final Html.VideoThumbnailGetter.Callbacks callbacks,
                                   final int maxWidth) {
        loadVideoThumbnail(url, callbacks, maxWidth, 0);
    }

    public void loadVideoThumbnail(final String url, final Html.VideoThumbnailGetter.Callbacks callbacks,
                                   final int maxWidth, final int minWidth) {
        if (TextUtils.isEmpty(url) || maxWidth <= 0) {
            callbacks.onThumbnailFailed();
            return;
        }

        callbacks.onThumbnailLoading(loadingInProgress);

        new AsyncTask<Void, Void, Bitmap>() {
            protected Bitmap doInBackground(Void... params) {
                // If local file
                if (new File(url).exists()) {
                   return ThumbnailUtils.createVideoThumbnail(url, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);
                }

                // `getVideoFrameFromVideo` takes in consideration both width and height of the picture.
                // We need to make sure to set the correct max width for the current thumb
                // keeping the correct aspect ratio and the max width setting from the editor.
                // Request double the width for now
                return ImageUtils.getVideoFrameFromVideo(url, 2 * maxWidth);
            }

            protected void onPostExecute(Bitmap thumb) {
                if (thumb == null) {
                    callbacks.onThumbnailFailed();
                }
                int maxRequestedSize =  MediaUtils.getMaxMediaSizeForEditor(thumb.getWidth(), thumb.getHeight(), maxWidth);
                thumb = ImageUtils.getScaledBitmapAtLongestSide(thumb, maxRequestedSize);
                thumb.setDensity(DisplayMetrics.DENSITY_DEFAULT);
                BitmapDrawable bitmapDrawable = new BitmapDrawable(context.getResources(), thumb);
                callbacks.onThumbnailLoaded(bitmapDrawable);
            }
        }.execute();
    }
}
