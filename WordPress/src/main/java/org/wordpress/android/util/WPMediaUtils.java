package org.wordpress.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;

public class WPMediaUtils {

    // Max picture size will be 3000px wide. That's the maximum resolution you can set in the current picker.
    public static final int OPTIMIZE_IMAGE_MAX_WIDTH = 3000;
    public static final int OPTIMIZE_IMAGE_ENCODER_QUALITY = 85;
    public static final int OPTIMIZE_VIDEO_MAX_WIDTH = 1280;
    public static final int OPTIMIZE_VIDEO_ENCODER_BITRATE_KB = 3000;

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
        return BuildConfig.VIDEO_OPTIMIZATION_AVAILABLE
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    public static boolean isVideoOptimizationEnabled(Activity activity, SiteModel siteModel) {
        SiteSettingsInterface siteSettings = SiteSettingsInterface.getInterface(activity, siteModel, null);
        return isVideoOptimizationAvailable() && siteSettings != null && siteSettings.init(false).getOptimizedVideo();
    }

    public static boolean isImageOptimizationEnabled(Activity activity, SiteModel siteModel) {
        SiteSettingsInterface siteSettings = SiteSettingsInterface.getInterface(activity, siteModel, null);
        return siteSettings != null && siteSettings.init(false).getOptimizedImage();
    }

    /**
     *
     * Check if we should advertise image optimization feature for the current site.
     *
     * The following condition need to be all true:
     * 1) Image optimization is OFF on the site.
     * 2) Didn't already ask to enable the feature.
     * 3) The user has granted storage access to the app.
     * This is because we don't want to ask so much things to users the first time they try to add a picture to the app.
     *
     * @param act The host activity
     * @param site The site where to check if optimize image is already on or not.
     * @return true if we should advertise the feature, false otherwise.
     */
    public static boolean shouldAdvertiseImageOptimization(final Activity act, final SiteModel site) {
        boolean isPromoRequired = AppPrefs.isImageOptimizePromoRequired();
        if (!isPromoRequired) {
            return false;
        }

        // Check we can access storage before asking for optimizing image
        boolean hasStoreAccess = ContextCompat.checkSelfPermission(
                act, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (!hasStoreAccess) {
            return false;
        }

        // Check whether image optimization is already available for the site
       return !isImageOptimizationEnabled(act, site);
    }

    public interface OnAdvertiseImageOptimizationListener {
        void done();
    }

    public static void advertiseImageOptimization(final Activity activity,
                                                  final SiteModel site,
                                                  final OnAdvertiseImageOptimizationListener listener) {

        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    SiteSettingsInterface siteSettings = SiteSettingsInterface.getInterface(activity, site, null);
                    if (siteSettings == null || siteSettings.init(false).getOptimizedImage()) {
                        // null or image optimization already ON. We should not be here though.
                    } else {
                        siteSettings.setOptimizedImage(true);
                        siteSettings.saveSettings();
                    }
                }

                listener.done();
            }
        };

        DialogInterface.OnCancelListener onCancelListener = new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                listener.done();
            }
        };

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle(org.wordpress.android.R.string.image_optimization_promo_title);
        builder.setMessage(org.wordpress.android.R.string.image_optimization_promo_desc);
        builder.setPositiveButton(R.string.turn_on, onClickListener);
        builder.setNegativeButton(R.string.leave_off, onClickListener);
        builder.setOnCancelListener(onCancelListener);
        builder.show();
        // Do not ask again
        AppPrefs.setImageOptimizePromoRequired(false);
    }

    /**
     * Given a media error returns the error message to display on the UI.
     *
     * @param error The media error occurred
     * @return String  The associated error message.
     */
    public static String getErrorMessage(final Context context, boolean suggestMediaOptimization, final MediaModel media, final MediaStore.MediaError error) {
        if (context == null || media == null || error == null) {
            return null;
        }

        switch (error.type) {
            case FS_READ_PERMISSION_DENIED:
                return context.getString(R.string.error_media_insufficient_fs_permissions);
            case NOT_FOUND:
                return context.getString(R.string.error_media_not_found);
            case AUTHORIZATION_REQUIRED:
                return context.getString(R.string.media_error_no_permission_upload);
            case REQUEST_TOO_LARGE:
                if (media.isVideo()) {
                    return context.getString(R.string.media_error_http_too_large_video_upload);
                } else {
                    if (!suggestMediaOptimization) {
                        return context.getString(R.string.media_error_http_too_large_photo_upload);
                    } else {
                        return context.getString(R.string.media_error_http_too_large_photo_upload) + ". " +
                                context.getString(R.string.media_error_suggest_optimize_image);
                    }
                }
            case SERVER_ERROR:
                return context.getString(R.string.media_error_internal_server_error);
            case TIMEOUT:
                return context.getString(R.string.media_error_timeout);
            case CONNECTION_ERROR:
                return context.getString(R.string.media_error_generic_connection_error);
            case EXCEEDS_FILESIZE_LIMIT:
                return context.getString(R.string.media_error_exceeds_php_filesize);
            case EXCEEDS_MEMORY_LIMIT:
                return context.getString(R.string.media_error_exceeds_memory_limit);
            case PARSE_ERROR:
                return context.getString(R.string.error_media_parse_error);
        }

        return null;
    }
}
