
package org.wordpress.android.util;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.TextView;

public class CommentBadgeTextView extends TextView {

    public CommentBadgeTextView(Context context) {
        super(context);
    }

    public CommentBadgeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CommentBadgeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();

        //calculate the center position based on the device's screen density
        float centerPos = 20.0f;
        float scale = getResources().getDisplayMetrics().density;
        int fCenter = (int) (centerPos * scale + 0.5f);

        canvas.rotate(45, fCenter, fCenter);

        super.onDraw(canvas);
        canvas.restore();

    }

}
