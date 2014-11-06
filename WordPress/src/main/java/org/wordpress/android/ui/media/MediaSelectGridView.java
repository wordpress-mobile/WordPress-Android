package org.wordpress.android.ui.media;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * Adapter to hold media content and display it as a grid.
 */

public class MediaSelectGridView extends GridView {
    public MediaSelectGridView(Context context) {
        super(context);
    }

    public MediaSelectGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MediaSelectGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
