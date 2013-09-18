package org.wordpress.android.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

public class DisplayUtils {

	private DisplayUtils() {
		throw new AssertionError();
	}

	public static boolean isLandscape(Context context) {
        if (context==null)
            return false;
		return context.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
	}

    public static boolean isLandscapeTablet(Context context) {
        return isLandscape(context) && isTablet(context);
    }

    @SuppressLint("NewApi")
    public static Point getDisplayPixelSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        if (SysUtils.isGteAndroid4()) {
            Point size = new Point();
            display.getSize(size);
            return size;
        } else {
            return new Point(display.getWidth(), display.getHeight());
        }
    }

    public static int getDisplayPixelWidth(Context context) {
        Point size = getDisplayPixelSize(context);
        return (size.x);
        //DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        //return metrics.widthPixels;
	}
	
	public static int getDisplayPixelHeight(Context context) {
        Point size = getDisplayPixelSize(context);
        return (size.y);
        //DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        //return metrics.heightPixels;
	}

	public static int dpToPx(Context context, int dp) {
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
		return (int) px;
	}

    public static boolean isTablet(Context context) {
        // http://stackoverflow.com/a/8427523/1673548
        if (context==null)
            return false;
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
