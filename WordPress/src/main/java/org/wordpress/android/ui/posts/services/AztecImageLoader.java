package org.wordpress.android.ui.posts.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.DisplayMetrics;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.WordPress;
import org.wordpress.android.editor.MediaUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.aztec.Html;

import java.io.File;

public class AztecImageLoader implements Html.ImageGetter {

    private final Drawable loadingInProgress;
    private Context context;

    public AztecImageLoader(Context context, Drawable loadingInProgressDrawable) {
        this.context = context;
        this.loadingInProgress = loadingInProgressDrawable;
    }

    @Override
    public void loadImage(final String url, final Callbacks callbacks, int maxWidth) {
        loadImage(url, callbacks, maxWidth, 0);
    }

    @Override
    public void loadImage(final String url, final Callbacks callbacks, final int maxWidth, final int minWidth) {
        final String cacheKey = url + maxWidth;
        Bitmap cachedBitmap = WordPress.getBitmapCache().get(cacheKey);
        if (cachedBitmap != null) {
            // By default, BitmapFactory.decodeFile sets the bitmap's density to the device default so, we need
            // to correctly set the input density to 160 ourselves.
            cachedBitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
            callbacks.onImageLoaded(new BitmapDrawable(context.getResources(), cachedBitmap));
            return;
        }

        if (new File(url).exists()) {
            int orientation = ImageUtils.getImageOrientation(this.context, url);
            // `createThumbnailFromUri` takes in consideration both width and height of the picture.
            // We need to make sure to set the correct max dimension for the current picture
            // keeping the correct aspect ratio and the max width setting for the editor
            int maxRequestedSize =  MediaUtils.getMaxMediaSizeForEditor(this.context, url, maxWidth);
            byte[] bytes = ImageUtils.createThumbnailFromUri(
                   context, Uri.parse(url), maxRequestedSize, null, orientation);
            if (bytes != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    WordPress.getBitmapCache().putBitmap(cacheKey, bitmap);
                    bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
                }
                BitmapDrawable bitmapDrawable = new BitmapDrawable(context.getResources(), bitmap);
                callbacks.onImageLoaded(bitmapDrawable);
            } else {
                callbacks.onImageFailed();
            }
            return;
        }

        callbacks.onImageLoading(loadingInProgress);

        WordPress.sImageLoader.get(url, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                Bitmap bitmap = response.getBitmap();

                if (bitmap == null && !isImmediate) {
                    // isImmediate is true as soon as the request starts.
                    callbacks.onImageFailed();
                } else if (bitmap != null) {
                    final String cacheKey = url + maxWidth;
                    WordPress.getBitmapCache().putBitmap(cacheKey, bitmap);
                    // By default, BitmapFactory.decodeFile sets the bitmap's density to the device default so, we need
                    // to correctly set the input density to 160 ourselves.
                    bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
                    BitmapDrawable bitmapDrawable = new BitmapDrawable(context.getResources(), bitmap);
                    callbacks.onImageLoaded(bitmapDrawable);
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                callbacks.onImageFailed();
            }
        }, maxWidth, 0);
    }
}
