package org.wordpress.android.util;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Created by mikepenz on 14.03.15.
 * This class implements a hack to change the layout padding on bottom if the keyboard is shown
 * to allow long lists with editTextViews
 * Basic idea for this solution found here: http://stackoverflow.com/a/9108219/325479
 */
public class KeyboardResizeViewUtil {
    private View mDecorView;
    private View mContentView;

    public KeyboardResizeViewUtil(Activity activity, View contentView) {
        this.mDecorView = activity.getWindow().getDecorView();
        this.mContentView = contentView;

        // only required on newer android versions. it was working on API level 19 (Build.VERSION_CODES.KITKAT)
        mDecorView.getViewTreeObserver().addOnGlobalLayoutListener(mOnGlobalLayoutListener);
    }

    public void enable() {
        mDecorView.getViewTreeObserver().addOnGlobalLayoutListener(mOnGlobalLayoutListener);
    }

    public void disable() {
        mDecorView.getViewTreeObserver().removeOnGlobalLayoutListener(mOnGlobalLayoutListener);
    }

    // a small helper to allow showing the editText focus
    ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            Rect r = new Rect();
            // r will be populated with the coordinates of your view that area still visible.
            mDecorView.getWindowVisibleDisplayFrame(r);

            // get screen height and calculate the difference with the useable area from the r
            int height = mDecorView.getContext().getResources().getDisplayMetrics().heightPixels;
            int diff = height - r.bottom;

            // if it could be a keyboard add the padding to the view
            if (diff != 0) {
                // if the use-able screen height differs from the total screen height we assume that it shows a
                // keyboard now
                // check if the padding is 0 (if yes set the padding for the keyboard)
                if (mContentView.getPaddingBottom() != diff) {
                    // set the padding of the contentView for the keyboard
                    mContentView.setPadding(0, 0, 0, diff);
                }
            } else {
                // check if the padding is != 0 (if yes reset the padding)
                if (mContentView.getPaddingBottom() != 0) {
                    // reset the padding of the contentView
                    mContentView.setPadding(0, 0, 0, 0);
                }
            }
        }
    };
}
