package org.wordpress.android.ui.posts.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.TextUtils;

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

    public void loadVideoThumbnail(final String url, final Html.VideoThumbnailGetter.Callbacks callbacks, final int maxWidth) {
        // Ignore the maxWidth passed from Aztec, since it's the MAX of screen width/height
        final int maxWidthForEditor = ImageUtils.getMaximumThumbnailWidthForEditor(context);
        if (TextUtils.isEmpty(url) || maxWidthForEditor <= 0) {
            callbacks.onThumbnailFailed();
            return;
        }

        callbacks.onThumbnailLoading(loadingInProgress);

        new AsyncTask<Void, Void, Bitmap>() {
            protected Bitmap doInBackground(Void... params) {
                // If local file
                if (new File(url).exists()) {
                    Bitmap thumb = ThumbnailUtils.createVideoThumbnail(url, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);
                    return ImageUtils.getScaledBitmapAtLongestSide(thumb, maxWidthForEditor);
                }

                return ImageUtils.getVideoFrameFromVideo(url, maxWidthForEditor);
            }
            protected void onPostExecute(Bitmap thumb) {
                if (thumb == null) {
                    callbacks.onThumbnailFailed();
                }
                BitmapDrawable bitmapDrawable = new BitmapDrawable(context.getResources(), thumb);
                callbacks.onThumbnailLoaded(bitmapDrawable);
            }
        }.execute();
    }
}
