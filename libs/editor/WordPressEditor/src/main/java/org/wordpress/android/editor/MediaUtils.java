package org.wordpress.android.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import org.wordpress.android.util.ImageUtils;

public class MediaUtils {
    public static int getMaxMediaSizeForEditor(Context context, String url, int maxImageWidthForVisualEditor) {
        if (TextUtils.isEmpty(url) || context == null) {
            return maxImageWidthForVisualEditor;
        }

        // Most of the functions in ImageUtils uses during resize both the width and height to calculate the max size allowed.
        // Here in the editor we're only using the width of the picture.
        // We need to make sure to set the correct max dimension for the current picture
        // keeping the correct aspect ratio and the max width setting from the editor
        int[] bitmapDimensions = ImageUtils.getImageSize(Uri.parse(url), context);
        int realBitmapWidth = bitmapDimensions[0];
        int realBitmapHeight = bitmapDimensions[1];
        return getMaxMediaSizeForEditor(realBitmapWidth, realBitmapHeight, maxImageWidthForVisualEditor);
    }

    public static int getMaxMediaSizeForEditor(int realBitmapWidth, int realBitmapHeight, int maxImageWidthForVisualEditor) {
        int requiredWidth = maxImageWidthForVisualEditor > realBitmapWidth && realBitmapWidth > 0 ? realBitmapWidth
                : maxImageWidthForVisualEditor;
        int maxRequestedSize = requiredWidth;
        if (realBitmapHeight > realBitmapWidth) {
            // Calculate the max height keeping the correct aspect ratio
            float proportionateHeight = (requiredWidth * realBitmapHeight) / ((float) realBitmapWidth);
            maxRequestedSize = (int) Math.rint(proportionateHeight);
        }
        return maxRequestedSize;
    }

    public static BitmapDrawable getAztecPlaceholderDrawableFromResID(Context context, @DrawableRes int drawableId, int maxImageWidthForVisualEditor) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
            bitmap = ImageUtils.getScaledBitmapAtLongestSide(bitmap, maxImageWidthForVisualEditor);
        } else if (drawable instanceof VectorDrawableCompat || drawable instanceof VectorDrawable) {
            bitmap = Bitmap.createBitmap(maxImageWidthForVisualEditor, maxImageWidthForVisualEditor, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        } else {
            throw new IllegalArgumentException("Unsupported drawable type");
        }
        bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        return new BitmapDrawable(context.getResources(), bitmap);
    }
}
