package org.wordpress.android.util;

import android.app.Activity;
import android.net.Uri;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;

public class WPMediaUtils {
    public static Uri getOptimizedMedia(Activity activity, SiteModel siteModel, String path, boolean isVideo) {
        boolean isOptimized = false;
        if (!NetworkUtils.isWiFiConnected(activity) && !isVideo) {
            SiteSettingsInterface siteSettings = SiteSettingsInterface.getInterface(activity, siteModel, null);
            // Site Settings are implemented on .com/Jetpack sites only
            if (siteSettings != null && siteSettings.init(false).getOptimizedImage()) {
                // Not on WiFi and optimize image is set to ON
                // Max picture size will be 3000px wide. That's the maximum resolution you can set in the current picker.
                String optimizedPath = ImageUtils.optimizeImage(activity, path, 3000, 85);

                if (optimizedPath == null) {
                    AppLog.e(AppLog.T.EDITOR, "Optimized picture was null!");
                    AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_PHOTO_OPTIMIZE_ERROR);
                } else {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_PHOTO_OPTIMIZED);
                    return Uri.parse(optimizedPath);
                }
            }
        }
        return null;
    }
}