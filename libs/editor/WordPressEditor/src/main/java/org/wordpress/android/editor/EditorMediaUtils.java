package org.wordpress.android.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.util.DisplayMetrics;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class EditorMediaUtils {
    public static BitmapDrawable getAztecPlaceholderDrawableFromResID(Context context, @DrawableRes int drawableId,
                                                                      int maxImageSizeForVisualEditor) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
            bitmap = ImageUtils.getScaledBitmapAtLongestSide(bitmap, maxImageSizeForVisualEditor);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && drawable instanceof VectorDrawable) {
            bitmap = Bitmap.createBitmap(maxImageSizeForVisualEditor, maxImageSizeForVisualEditor,
                                         Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        } else {
            throw new IllegalArgumentException("Unsupported Drawable Type: " + drawable.getClass().getName());
        }
        bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static int getMaximumThumbnailSizeForEditor(Context context) {
        Point size = DisplayUtils.getDisplayPixelSize(context);
        int screenWidth = size.x;
        int screenHeight = size.y;
        int maximumThumbnailWidthForEditor = screenWidth > screenHeight ? screenHeight : screenWidth;
        int padding = DisplayUtils.dpToPx(context, 48) * 2;
        maximumThumbnailWidthForEditor -= padding;
        return maximumThumbnailWidthForEditor;
    }

    public static String getVideoThumbnail(Context context, String videoPath, Map<String, String> headers) {
        String thumbnailPath = null;
        try {
            File outputFile = File.createTempFile("thumb", ".png", context.getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            Bitmap thumb = ImageUtils.getVideoFrameFromVideo(
                    videoPath,
                    EditorMediaUtils.getMaximumThumbnailSizeForEditor(context),
                    headers
            );
            if (thumb != null) {
                thumb.compress(Bitmap.CompressFormat.PNG, 75, outputStream);
                thumbnailPath = outputFile.getAbsolutePath();
            }
        } catch (IOException e) {
            AppLog.i(T.MEDIA, "Can't create thumbnail for video: " + videoPath);
        }
        return thumbnailPath;
    }
}
