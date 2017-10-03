package org.wordpress.android.ui.posts.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.WordPress;
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
        // FIXME: Aztec has now the option to set the desired image width. We should respect it
        // Ignore the maxWidth passed from Aztec, since it's the MAX of screen width/height
        final int maxWidthForEditor = ImageUtils.getMaximumThumbnailWidthForEditor(context);

        final String cacheKey = url + maxWidthForEditor;
        Bitmap cachedBitmap = WordPress.getBitmapCache().get(cacheKey);
        if (cachedBitmap != null) {
            callbacks.onImageLoaded(new BitmapDrawable(context.getResources(), cachedBitmap));
            return;
        }

        if (new File(url).exists()) {
            int orientation = ImageUtils.getImageOrientation(this.context, url);
            byte[] bytes = ImageUtils.createThumbnailFromUri(
                   context, Uri.parse(url), maxWidthForEditor, null, orientation);
            if (bytes != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    WordPress.getBitmapCache().putBitmap(cacheKey, bitmap);
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
                    final String cacheKey = url + maxWidthForEditor;
                    WordPress.getBitmapCache().putBitmap(cacheKey, bitmap);
                    BitmapDrawable bitmapDrawable = new BitmapDrawable(context.getResources(), bitmap);
                    callbacks.onImageLoaded(bitmapDrawable);
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                callbacks.onImageFailed();
            }
        }, maxWidthForEditor, 0);
    }
}
