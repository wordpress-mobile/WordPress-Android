package org.wordpress.android.util;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

public class DisplayUtils {
    private DisplayUtils() {
        throw new AssertionError();
    }

    public static boolean isLandscape(Context context) {
        if (context == null)
            return false;
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static Point getDisplayPixelSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static int getDisplayPixelWidth(Context context) {
        Point size = getDisplayPixelSize(context);
        return (size.x);
    }

    public static int getDisplayPixelHeight(Context context) {
        Point size = getDisplayPixelSize(context);
        return (size.y);
    }

    public static float spToPx(Context context, float sp){
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        final float scale = displayMetrics.scaledDensity;
        return sp * scale;
    }

    public static int dpToPx(Context context, int dp) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
        return (int) px;
    }

    public static int pxToDp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) ((px/displayMetrics.density)+0.5);
    }

    public static boolean isXLarge(Context context) {
        if ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            return true;
        }
        return false;
    }

    /**
     * returns the height of the ActionBar if one is enabled - supports both the native ActionBar
     * and ActionBarSherlock - http://stackoverflow.com/a/15476793/1673548
     */
    public static int getActionBarHeight(Context context) {
        if (context == null) {
            return 0;
        }
        TypedValue tv = new TypedValue();
        if (context.getTheme() != null
                && context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }

        // if we get this far, it's because the device doesn't support an ActionBar,
        // so return the standard ActionBar height (48dp)
        return dpToPx(context, 48);
    }

    /**
     * detect when FEATURE_ACTION_BAR_OVERLAY has been set
     */
    public static boolean hasActionBarOverlay(Window window) {
        return window.hasFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
    }
}
