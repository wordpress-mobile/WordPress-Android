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
    private Bitmap mEditIconBitmap;

    public WPEditImageSpan(Context context, Bitmap b, Uri src) {
        super(context, b, src);
        init(context);
    }

    public WPEditImageSpan(Context context, int resId, Uri src) {
        super(context, resId, src);
        init(context);
    }

    private void init(Context context) {
        mEditIconBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ab_icon_edit);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom,
                     Paint paint) {
        super.draw(canvas, text, start, end, x, top, y, bottom, paint);

        if (!mMediaFile.isVideo()) {
            // Add 'edit' icon at bottom right of image
            int width = getSize(paint, text, start, end, paint.getFontMetricsInt());
            float editIconXPosition = (x + width) - mEditIconBitmap.getWidth();
            float editIconYPosition = bottom - mEditIconBitmap.getHeight();

            // Add a black background with a bit of alpha
            Paint bgPaint = new Paint();
            bgPaint.setColor(Color.argb(200, 0, 0, 0));
            canvas.drawRect(editIconXPosition, editIconYPosition, editIconXPosition + mEditIconBitmap.getWidth(),
                    editIconYPosition + mEditIconBitmap.getHeight(), bgPaint);

            // Add the icon to the canvas
            canvas.drawBitmap(mEditIconBitmap, editIconXPosition, editIconYPosition, paint);
        }
    }
}
