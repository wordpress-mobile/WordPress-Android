package org.wordpress.android.util;

import android.app.Activity;
import android.net.Uri;
import android.os.Build;

import org.wordpress.android.*;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;

public class WPMediaUtils {

    // Max picture size will be 3000px wide. That's the maximum resolution you can set in the current picker.
    public static final int OPTIMIZE_IMAGE_MAX_WIDTH = 3000;
    public static final int OPTIMIZE_IMAGE_ENCODER_QUALITY = 85;

    public static Uri getOptimizedMedia(Activity activity, SiteModel siteModel, String path, boolean isVideo) {
        if (isVideo) {
            return null;
        }
        // TODO implement site settings for .org sites
        SiteSettingsInterface siteSettings = SiteSettingsInterface.getInterface(activity, siteModel, null);
        // Site Settings are implemented on .com/Jetpack sites only
        if (siteSettings != null && siteSettings.init(false).getOptimizedImage()) {
            int resizeWidth = siteSettings.getMaxImageWidth() > 0 ? siteSettings.getMaxImageWidth() : Integer.MAX_VALUE;
            int quality = siteSettings.getImageQuality();
            // do not optimize if original-size and 100% quality are set.
            if (resizeWidth == Integer.MAX_VALUE && quality == 100) {
                return null;
            }

            String optimizedPath = ImageUtils.optimizeImage(activity, path, resizeWidth, quality);
            if (optimizedPath == null) {
                AppLog.e(AppLog.T.EDITOR, "Optimized picture was null!");
                AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_PHOTO_OPTIMIZE_ERROR);
            } else {
                AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_PHOTO_OPTIMIZED);
                return Uri.parse(optimizedPath);
            }
        }
        return null;
    }

    public static Uri fixOrientationIssue(Activity activity, String path, boolean isVideo) {
        if (isVideo) {
            return null;
        }

        String rotatedPath = ImageUtils.rotateImageIfNecessary(activity, path);
        if (rotatedPath != null) {
            return Uri.parse(rotatedPath);
        }

        return null;
    }

    public static boolean isVideoOptimizationAvailable() {
        return BuildConfig.VIDEO_OPTIMIZATION_AVAILABLE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }
}
