package org.wordpress.android.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Created by mikepenz on 14.03.15.
 * This class implements a hack to change the layout padding on bottom if the keyboard is shown
 * to allow long lists with editTextViews
 * Basic idea for this solution found here: http://stackoverflow.com/a/9108219/325479
 */
public class KeyboardResizeViewUtil {
    @SuppressWarnings("FieldMayBeFinal")
    private View mDecorView;
    @SuppressWarnings("FieldMayBeFinal")
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
            mContentView.post(() -> {
                Rect r = new Rect();
                // r will be populated with the coordinates of your view that area still visible.
                mDecorView.getWindowVisibleDisplayFrame(r);

                // get screen height and calculate the difference with the useable area from the r
                int diff = getRealScreenHeight() - (r.bottom + getInsetBottom());

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
            });
        }
    };

    private int getInsetBottom() {
        int insetsBottom = 0;
        try {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(mContentView);
            insetsBottom = insets != null ? insets.getSystemWindowInsetBottom() : 0;
        } catch (NullPointerException e) {
            AppLog.e(AppLog.T.PREPUBLISHING_NUDGES, "Error in getting window insets on keyboard resize:", e);
        }
        return insetsBottom;
    }

    private int getRealScreenHeight() {
        WindowManager windowManager = (WindowManager) mDecorView.getContext().getSystemService(Context.WINDOW_SERVICE);
        Point realSize = new Point();
        if (windowManager != null) windowManager.getDefaultDisplay().getRealSize(realSize);
        return realSize.y;
    }
}
