package org.wordpress.android.util;

import android.app.Activity;
import android.net.Uri;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;

public class WPMediaUtils {

    // Max picture size will be 3000px wide. That's the maximum resolution you can set in the current picker.
    private static final int OPTIMIZE_IMAGE_MAX_WIDTH = 3000;
    private static final int OPTIMIZE_IMAGE_ENCODER_QUALITY = 85;

    public static Uri getOptimizedMedia(Activity activity, SiteModel siteModel, String path, boolean isVideo) {
        if (isVideo || !NetworkUtils.isMobileConnected(activity)) {
            // Not on mobile data. Skip optimization.
            return null;
        }
        SiteSettingsInterface siteSettings = SiteSettingsInterface.getInterface(activity, siteModel, null);
        // Site Settings are implemented on .com/Jetpack sites only
        if (siteSettings != null && siteSettings.init(false).getOptimizedImage()) {
            String optimizedPath = ImageUtils.optimizeImage(activity, path, OPTIMIZE_IMAGE_MAX_WIDTH, OPTIMIZE_IMAGE_ENCODER_QUALITY);
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
}
