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

import org.wordpress.android.ui.utils.AuthenticationUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.aztec.Html;

import java.io.File;

import javax.inject.Inject;

public class AztecVideoLoader implements Html.VideoThumbnailGetter {
    private Context mContext;
    private final Drawable mLoadingInProgress;
    @Inject AuthenticationUtils mAuthenticationUtils;

    public AztecVideoLoader(Context context, Drawable loadingInProgressDrawable) {
        this.mContext = context;
        this.mLoadingInProgress = loadingInProgressDrawable;
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

        callbacks.onThumbnailLoading(mLoadingInProgress);

        new AsyncTask<Void, Void, Bitmap>() {
            protected Bitmap doInBackground(Void... params) {
                // If local file
                if (new File(url).exists()) {
                    return ThumbnailUtils.createVideoThumbnail(url, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);
                }

                return ImageUtils.getVideoFrameFromVideo(url, maxWidth, mAuthenticationUtils.getAuthHeaders(url));
            }

            protected void onPostExecute(Bitmap thumb) {
                if (thumb == null) {
                    callbacks.onThumbnailFailed();
                    return;
                }
                thumb = ImageUtils.getScaledBitmapAtLongestSide(thumb, maxWidth);
                thumb.setDensity(DisplayMetrics.DENSITY_DEFAULT);
                BitmapDrawable bitmapDrawable = new BitmapDrawable(mContext.getResources(), thumb);
                callbacks.onThumbnailLoaded(bitmapDrawable);
            }
        }.execute();
    }
}
