package org.wordpress.android.ui.posts.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.editor.AztecEditorFragment;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.aztec.Html;

import java.io.File;

public class AztecImageLoader implements Html.ImageGetter {

    private Context context;

    public AztecImageLoader(Context context) {
        this.context = context;
    }

    @Override
    public void loadImage(String url, final Callbacks callbacks, int maxWidth) {
        // Ignore the maxWidth passed from Aztec, since it's the MAX of screen width/height
        final int maxWidthForEditor = ImageUtils.getMaximumThumbnailWidthForEditor(context);

        if (new File(url).exists()) {
            int orientation = ImageUtils.getImageOrientation(this.context, url);
            byte[] bytes = ImageUtils.createThumbnailFromUri(
                   context, Uri.parse(url), maxWidthForEditor, null, orientation);
            if (bytes != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                BitmapDrawable bitmapDrawable = new BitmapDrawable(context.getResources(), bitmap);
                callbacks.onImageLoaded(bitmapDrawable);
            } else {
                callbacks.onImageFailed();
            }
            return;
        }

        Drawable drawable = context.getResources().getDrawable(R.drawable.ic_gridicons_image);
        drawable.setBounds(0, 0,
                AztecEditorFragment.DEFAULT_MEDIA_PLACEHOLDER_DIMENSION_DP,
                AztecEditorFragment.DEFAULT_MEDIA_PLACEHOLDER_DIMENSION_DP);
        callbacks.onImageLoading(drawable);

        WordPress.sImageLoader.get(url, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                Bitmap bitmap = response.getBitmap();

                if (bitmap == null && !isImmediate) {
                    // isImmediate is true as soon as the request starts.
                    callbacks.onImageFailed();
                } else if (bitmap != null) {
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
