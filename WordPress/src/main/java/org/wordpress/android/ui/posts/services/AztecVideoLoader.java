package org.wordpress.android.ui.posts.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore.Images.Thumbnails;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.utils.AuthenticationUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.aztec.Html;

import java.io.File;
import java.lang.ref.WeakReference;

import javax.inject.Inject;

public class AztecVideoLoader implements Html.VideoThumbnailGetter {
    private final Context mContext;
    private final Drawable mLoadingInProgress;
    @Inject AuthenticationUtils mAuthenticationUtils;

    public AztecVideoLoader(Context context, Drawable loadingInProgressDrawable) {
        ((WordPress) WordPress.getContext().getApplicationContext()).component().inject(this);
        mContext = context;
        mLoadingInProgress = loadingInProgressDrawable;
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
        new LoadAztecVideoTask(mContext, mAuthenticationUtils, url, maxWidth, callbacks).execute();
    }

    @SuppressWarnings("deprecation")
    private static class LoadAztecVideoTask extends AsyncTask<Void, Void, Bitmap> {
        final String mUrl;
        final int mMaxWidth;
        final Html.VideoThumbnailGetter.Callbacks mCallbacks;
        final AuthenticationUtils mAuthenticationUtils;
        final WeakReference<Context> mContext;

        LoadAztecVideoTask(Context context,
                           AuthenticationUtils authenticationUtils,
                           String url,
                           int maxWidth,
                           Html.VideoThumbnailGetter.Callbacks callbacks) {
            mContext = new WeakReference<>(context);
            mAuthenticationUtils = authenticationUtils;
            mUrl = url;
            mMaxWidth = maxWidth;
            mCallbacks = callbacks;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            // If local file
            if (new File(mUrl).exists()) {
                return ThumbnailUtils.createVideoThumbnail(mUrl, Thumbnails.FULL_SCREEN_KIND);
            }

            return ImageUtils.getVideoFrameFromVideo(mUrl, mMaxWidth, mAuthenticationUtils.getAuthHeaders(mUrl));
        }

        @Override
        protected void onPostExecute(Bitmap thumb) {
            if (mContext.get() == null) {
                return;
            }
            if (thumb == null) {
                mCallbacks.onThumbnailFailed();
                return;
            }
            thumb = ImageUtils.getScaledBitmapAtLongestSide(thumb, mMaxWidth);
            thumb.setDensity(DisplayMetrics.DENSITY_DEFAULT);
            BitmapDrawable bitmapDrawable = new BitmapDrawable(mContext.get().getResources(), thumb);
            mCallbacks.onThumbnailLoaded(bitmapDrawable);
        }
    }
}
