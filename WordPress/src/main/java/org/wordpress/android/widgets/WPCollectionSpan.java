package org.wordpress.android.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.style.ImageSpan;

import org.wordpress.android.R;

import java.util.ArrayList;
import java.util.List;

public class WPCollectionSpan extends ImageSpan {
    private Context mContext;
    private List<String> mContent;

    public WPCollectionSpan(Context context, Bitmap bitmap) {
        super(context, bitmap);
        mContext = context;
        mContent = new ArrayList<>();
    }

    public void addContent(String content) {
        mContent.add(content);
    }

    public List<String> getContent() {
        return mContent;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text,
                     int start, int end, float x,
                     int top, int y, int bottom, Paint paint) {
        super.draw(canvas, text, start, end, x, top, y, bottom, paint);

        // Add 'edit' icon at bottom right of image
        int width = getSize(paint, text, start, end, paint.getFontMetricsInt());
        Bitmap editIconBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_add_white_24dp);
        float editIconXPosition = (x + width) - editIconBitmap.getWidth();
        float editIconYPosition = bottom - editIconBitmap.getHeight();
        // Add a black background with a bit of alpha
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.argb(200, 0, 0, 0));
        canvas.drawRect(editIconXPosition, editIconYPosition, editIconXPosition + editIconBitmap.getWidth(), editIconYPosition + editIconBitmap.getHeight(), bgPaint);
        // Add the icon to the canvas
        canvas.drawBitmap(editIconBitmap, editIconXPosition, editIconYPosition, paint);
    }
}
