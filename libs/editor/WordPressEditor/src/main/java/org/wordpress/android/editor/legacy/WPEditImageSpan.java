package org.wordpress.android.editor.legacy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;

import org.wordpress.android.editor.R;
import org.wordpress.android.util.helpers.WPImageSpan;

public class WPEditImageSpan extends WPImageSpan {
    private Context mContext;

    public WPEditImageSpan(Context context, Bitmap b, Uri src) {
        super(context, b, src);
        mContext = context;
    }

    public WPEditImageSpan(Context context, int resId, Uri src) {
        super(context, resId, src);
        mContext = context;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom,
                     Paint paint) {
        super.draw(canvas, text, start, end, x, top, y, bottom, paint);

        if (!mMediaFile.isVideo()) {
            // Add 'edit' icon at bottom right of image
            int width = getSize(paint, text, start, end, paint.getFontMetricsInt());

            Bitmap editIconBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ab_icon_edit);
            float editIconXPosition = (x + width) - editIconBitmap.getWidth();
            float editIconYPosition = bottom - editIconBitmap.getHeight();
            // Add a black background with a bit of alpha
            Paint bgPaint = new Paint();
            bgPaint.setColor(Color.argb(200, 0, 0, 0));
            canvas.drawRect(editIconXPosition, editIconYPosition, editIconXPosition + editIconBitmap.getWidth(),
                    editIconYPosition + editIconBitmap.getHeight(), bgPaint);
            // Add the icon to the canvas
            canvas.drawBitmap(editIconBitmap, editIconXPosition, editIconYPosition, paint);
        }
    }
}
