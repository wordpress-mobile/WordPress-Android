package org.wordpress.android.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * Created by nbradbury on 7/22/13.
 */
public class ImageUtils {

    /*
     * used for round avatars in Reader
     */
    public static Bitmap getRoundedBitmap(final Bitmap bitmap) {
        if (bitmap==null)
            return null;

        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.RED);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        // outline
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(Color.DKGRAY);
        canvas.drawOval(rectF, paint);

        return output;
    }

   /* public static Bitmap addVideoOverlay(final Bitmap bitmap) {
        if (bitmap==null)
            return null;

        Bitmap bmpOverlay = BitmapFactory.decodeResource(WPReader.getInstance().getResources(), R.drawable.video_overlay, null);
        int overlayWidth  = (int)(bmpOverlay.getWidth() * 1.75f);
        int overlayHeight  = (int)(bmpOverlay.getHeight() * 1.75f);

        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();

        // return passed bitmap w/o overlay if it's smaller than our overlay
        if (srcWidth < overlayWidth || srcHeight < overlayHeight)
            return bitmap;

        Bitmap bmpCopy = Bitmap.createBitmap(srcWidth, srcHeight, bitmap.getConfig());

        Canvas canvas = new Canvas(bmpCopy);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        int left = (srcWidth / 2) - (overlayWidth / 2);
        int top = (srcHeight / 2) - (overlayHeight / 2);
        Rect rcDst = new Rect(left, top, left + overlayWidth, top + overlayHeight);

        canvas.drawBitmap(bmpOverlay, null, rcDst, paint);

        return bmpCopy;
    }*/

}
