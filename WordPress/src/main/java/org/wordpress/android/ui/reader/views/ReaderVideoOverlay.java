package org.wordpress.android.ui.reader.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

import org.wordpress.android.R;

/**
 * video player overlay image
 */

public class ReaderVideoOverlay extends ImageView {
    private int mOverlaySize;

    public ReaderVideoOverlay(Context context) {
        super(context);
        initialize(context);
    }

    public ReaderVideoOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public ReaderVideoOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ReaderVideoOverlay(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initialize(Context context) {
        mOverlaySize = context.getResources().getDimensionPixelSize(R.dimen.reader_video_overlay_size);
        setImageResource(R.drawable.reader_video_overlay);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mOverlaySize, mOverlaySize);
    }
}
