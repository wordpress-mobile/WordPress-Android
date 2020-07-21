package org.wordpress.android.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.ViewConfiguration;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.imageeditor.preview.PreviewImageFragment;
import org.wordpress.android.imageeditor.preview.PreviewImageFragment.Companion.EditImageData;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog.T;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WPMediaUtils {
    public interface LaunchCameraCallback {
        void onMediaCapturePathReady(String mediaCapturePath);
    }

    // Max picture size will be 3000px wide. That's the maximum resolution you can set in the current picker.
    public static final int OPTIMIZE_IMAGE_MAX_SIZE = 3000;
    public static final int OPTIMIZE_IMAGE_ENCODER_QUALITY = 85;
    public static final int OPTIMIZE_VIDEO_MAX_WIDTH = 1280;
    public static final int OPTIMIZE_VIDEO_ENCODER_BITRATE_KB = 3000;

    public static Uri getOptimizedMedia(Context context, String path, boolean isVideo) {
        if (isVideo) {
            return null;
        }
        if (!AppPrefs.isImageOptimize()) {
            return null;
        }

        int resizeDimension =
                AppPrefs.getImageOptimizeMaxSize() > 1 ? AppPrefs.getImageOptimizeMaxSize() : Integer.MAX_VALUE;
        int quality = AppPrefs.getImageOptimizeQuality();
        // do not optimize if original-size and 100% quality are set.
        if (resizeDimension == Integer.MAX_VALUE && quality == 100) {
            return null;
        }

        String optimizedPath = ImageUtils.optimizeImage(context, path, resizeDimension, quality);
        if (optimizedPath == null) {
            AppLog.e(AppLog.T.EDITOR, "Optimized picture was null!");
            AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_PHOTO_OPTIMIZE_ERROR);
        } else {
            AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_PHOTO_OPTIMIZED);
            return Uri.parse(optimizedPath);
        }
        return null;
    }

    public static Uri fixOrientationIssue(Context context, String path, boolean isVideo) {
        if (isVideo) {
            return null;
        }

        String rotatedPath = ImageUtils.rotateImageIfNecessary(context, path);
        if (rotatedPath != null) {
            return Uri.parse(rotatedPath);
        }

        return null;
    }

    public static boolean isVideoOptimizationEnabled() {
        return AppPrefs.isVideoOptimize();
    }

    /**
     * Check if we should advertise image optimization feature for the current site.
     * <p>
     * The following condition need to be all true:
     * 1) Image optimization is OFF on the site.
     * 2) Didn't already ask to enable the feature.
     * 3) The user has granted storage access to the app.
     * This is because we don't want to ask so much things to users the first time they try to add a picture to the app.
     *
     * @param context The context
     * @return true if we should advertise the feature, false otherwise.
     */
    public static boolean shouldAdvertiseImageOptimization(final Context context) {
        boolean isPromoRequired = AppPrefs.isImageOptimizePromoRequired();
        if (!isPromoRequired) {
            return false;
        }

        // Check we can access storage before asking for optimizing image
        boolean hasStoreAccess = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (!hasStoreAccess) {
            return false;
        }

        // Check whether image optimization is already available for the site
        return !AppPrefs.isImageOptimize();
    }

    public interface OnAdvertiseImageOptimizationListener {
        void done();
    }

    public static void advertiseImageOptimization(final Context context,
                                                  final OnAdvertiseImageOptimizationListener listener) {
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    if (AppPrefs.isImageOptimize()) {
                        // null or image optimization already ON. We should not be here though.
                    } else {
                        AppPrefs.setImageOptimize(true);
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

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context);
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
     * @return String The associated error message.
     */
    public static @Nullable
    String getErrorMessage(final Context context, final MediaModel media, final MediaError error) {
        if (context == null || error == null) {
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
                if (media == null) {
                    return null;
                }

                if (media.isVideo()) {
                    return context.getString(R.string.media_error_http_too_large_video_upload);
                } else {
                    return context.getString(R.string.media_error_http_too_large_photo_upload);
                }
            case SERVER_ERROR:
                return context.getString(R.string.media_error_internal_server_error);
            case TIMEOUT:
                return context.getString(R.string.media_error_timeout);
            case CONNECTION_ERROR:
                return context.getString(R.string.connection_to_server_lost);
            case EXCEEDS_FILESIZE_LIMIT:
                return context.getString(R.string.media_error_exceeds_php_filesize);
            case EXCEEDS_MEMORY_LIMIT:
                return context.getString(R.string.media_error_exceeds_memory_limit);
            case PARSE_ERROR:
                return context.getString(R.string.error_media_parse_error);
            case GENERIC_ERROR:
                return context.getString(R.string.error_generic_error);
        }
        return null;
    }

    private static void showSDCardRequiredDialog(Context context) {
        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(context);
        dialogBuilder.setTitle(context.getResources().getText(R.string.sdcard_title));
        dialogBuilder.setMessage(context.getResources().getText(R.string.sdcard_message));
        dialogBuilder.setPositiveButton(context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }

    public static void launchVideoLibrary(Activity activity, boolean multiSelect) {
        activity.startActivityForResult(prepareVideoLibraryIntent(activity, multiSelect),
                                        RequestCodes.VIDEO_LIBRARY);
    }

    public static void launchMediaLibrary(Activity activity, boolean multiSelect) {
        activity.startActivityForResult(prepareMediaLibraryIntent(activity, multiSelect),
                RequestCodes.MEDIA_LIBRARY);
    }

    public static void launchFileLibrary(Activity activity, boolean multiSelect) {
        activity.startActivityForResult(prepareFileLibraryIntent(activity, multiSelect),
                RequestCodes.FILE_LIBRARY);
    }

    private static Intent prepareVideoLibraryIntent(Context context, boolean multiSelect) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        if (multiSelect) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        return Intent.createChooser(intent, context.getString(R.string.pick_video));
    }

    private static Intent prepareMediaLibraryIntent(Context context, boolean multiSelect) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"image/*", "video/*"});
        if (multiSelect) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        return Intent.createChooser(intent, context.getString(R.string.pick_media));
    }

    private static Intent prepareFileLibraryIntent(Context context, boolean multiSelect) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"image/*", "video/*", "audio/*", "application/*"});
        if (multiSelect) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        return Intent.createChooser(intent, context.getString(R.string.pick_file));
    }

    public static void launchVideoCamera(Activity activity) {
        activity.startActivityForResult(prepareVideoCameraIntent(), RequestCodes.TAKE_VIDEO);
    }

    private static Intent prepareVideoCameraIntent() {
        return new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
    }

    public static void launchPictureLibrary(Activity activity, boolean multiSelect) {
        activity.startActivityForResult(
                preparePictureLibraryIntent(activity.getString(R.string.pick_photo), multiSelect),
                RequestCodes.PICTURE_LIBRARY);
    }

    private static Intent preparePictureLibraryIntent(String title, boolean multiSelect) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (multiSelect) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        return Intent.createChooser(intent, title);
    }

    private static Intent prepareGalleryIntent(String title) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        return Intent.createChooser(intent, title);
    }

    public static void launchCamera(Activity activity, String applicationId, LaunchCameraCallback callback) {
        Intent intent = prepareLaunchCamera(activity, applicationId, callback);
        if (intent != null) {
            activity.startActivityForResult(intent, RequestCodes.TAKE_PHOTO);
        }
    }

    private static Intent prepareLaunchCamera(Context context, String applicationId, LaunchCameraCallback callback) {
        String state = android.os.Environment.getExternalStorageState();
        if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
            showSDCardRequiredDialog(context);
            return null;
        } else {
            try {
                return getLaunchCameraIntent(context, applicationId, callback);
            } catch (IOException e) {
                // No need to write log here
                return null;
            }
        }
    }

    private static Intent getLaunchCameraIntent(Context context, String applicationId, LaunchCameraCallback callback)
            throws IOException {
        File externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        String mediaCapturePath =
                externalStoragePublicDirectory + File.separator + "Camera" + File.separator + "wp-" + System
                        .currentTimeMillis() + ".jpg";

        // make sure the directory we plan to store the recording in exists
        File directory = new File(mediaCapturePath).getParentFile();
        if (directory == null || (!directory.exists() && !directory.mkdirs())) {
            try {
                throw new IOException("Path to file could not be created: " + mediaCapturePath);
            } catch (IOException e) {
                AppLog.e(T.MEDIA, e);
                throw e;
            }
        }

        Uri fileUri;
        try {
            fileUri = FileProvider.getUriForFile(context, applicationId + ".provider", new File(mediaCapturePath));
        } catch (IllegalArgumentException e) {
            AppLog.e(T.MEDIA, "Cannot access the file planned to store the new media", e);
            throw new IOException("Cannot access the file planned to store the new media");
        } catch (NullPointerException e) {
            AppLog.e(T.MEDIA, "Cannot access the file planned to store the new media - "
                              + "FileProvider.getUriForFile cannot find a valid provider for the authority: "
                              + applicationId + ".provider", e);
            throw new IOException("Cannot access the file planned to store the new media");
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, fileUri);

        if (callback != null) {
            callback.onMediaCapturePathReady(mediaCapturePath);
        }
        return intent;
    }

    private static Intent makePickOrCaptureIntent(Context context, String applicationId,
                                                  LaunchCameraCallback callback) {
        Intent pickPhotoIntent = prepareGalleryIntent(context.getString(R.string.capture_or_pick_photo));

        if (DeviceUtils.getInstance().hasCamera(context)) {
            try {
                Intent cameraIntent = getLaunchCameraIntent(context, applicationId, callback);
                pickPhotoIntent.putExtra(
                        Intent.EXTRA_INITIAL_INTENTS,
                        new Intent[]{cameraIntent});
            } catch (IOException e) {
                // No need to write log here
            }
        }

        return pickPhotoIntent;
    }

    public static int getPlaceholder(String url) {
        if (MediaUtils.isValidImage(url)) {
            return R.drawable.ic_image_white_24dp;
        } else if (MediaUtils.isDocument(url)) {
            return R.drawable.ic_pages_white_24dp;
        } else if (MediaUtils.isPowerpoint(url)) {
            return R.drawable.media_powerpoint;
        } else if (MediaUtils.isSpreadsheet(url)) {
            return R.drawable.media_spreadsheet;
        } else if (MediaUtils.isVideo(url)) {
            return R.drawable.ic_video_camera_white_24dp;
        } else if (MediaUtils.isAudio(url)) {
            return R.drawable.ic_audio_white_24dp;
        } else {
            return R.drawable.ic_image_multiple_white_24dp;
        }
    }

    public static boolean canDeleteMedia(MediaModel mediaModel) {
        String state = mediaModel.getUploadState();
        return state == null || (!state.equalsIgnoreCase("uploading") && !state.equalsIgnoreCase("deleted"));
    }

    /**
     * Returns a poster (thumbnail) URL given a VideoPress video URL
     *
     * @param videoUrl the remote URL to the VideoPress video
     */
    public static String getVideoPressVideoPosterFromURL(String videoUrl) {
        String posterUrl = "";

        if (videoUrl != null) {
            int fileTypeLocation = videoUrl.lastIndexOf(".");
            if (fileTypeLocation > 0) {
                posterUrl = videoUrl.substring(0, fileTypeLocation) + "_std.original.jpg";
            }
        }

        return posterUrl;
    }

    /*
     * passes a newly-created media file to the media scanner service so it's available to
     * the media content provider - use this after capturing or downloading media to ensure
     * that it appears in the stock Gallery app
     */
    public static void scanMediaFile(@NonNull Context context, @NonNull String localMediaPath) {
        MediaScannerConnection.scanFile(context,
                                        new String[]{localMediaPath}, null,
                                        new MediaScannerConnection.OnScanCompletedListener() {
                                            public void onScanCompleted(String path, Uri uri) {
                                                AppLog.d(T.MEDIA, "Media scanner finished scanning " + path);
                                            }
                                        });
    }

    /*
     * returns true if the current user has permission to upload new media to the passed site
     */
    public static boolean currentUserCanUploadMedia(@NonNull SiteModel site) {
        boolean isSelfHosted = !site.isUsingWpComRestApi();
        // self-hosted sites don't have capabilities so always return true
        return isSelfHosted || site.getHasCapabilityUploadFiles();
    }

    public static boolean currentUserCanDeleteMedia(@NonNull SiteModel site) {
        return currentUserCanUploadMedia(site);
    }

    /*
     * returns the minimum distance for a fling which determines whether to disable loading
     * thumbnails in the media grid or photo picker - used to conserve memory usage during
     * a reasonably-sized fling
     */
    public static int getFlingDistanceToDisableThumbLoading(@NonNull Context context) {
        return ViewConfiguration.get(context).getScaledMaximumFlingVelocity() / 2;
    }


    public interface MediaFetchDoNext {
        void doNext(Uri uri);
    }

    /**
     * Downloads the {@code mediaUri} and returns the {@link Uri} for the downloaded file
     * <p>
     * If the {@code mediaUri} is already in the the local store, no download will be done and the given
     * {@code mediaUri} will be returned instead. This may return null if the download fails.
     * <p>
     * The current thread is blocked until the download is finished.
     *
     * @return A local {@link Uri} or null if the download failed
     */
    public static @Nullable Uri fetchMedia(@NonNull Context context, @NonNull Uri mediaUri) {
        if (MediaUtils.isInMediaStore(mediaUri)) {
            return mediaUri;
        }

        try {
            // Do not download the file in async task. See
            // https://github.com/wordpress-mobile/WordPress-Android/issues/5818
            return MediaUtils.downloadExternalMedia(context, mediaUri);
        } catch (IllegalStateException e) {
            // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/5823
            AppLog.e(AppLog.T.UTILS, "Can't download the image at: " + mediaUri.toString()
                                     + " See issue #5823", e);

            return null;
        }
    }

    /**
     * Downloads the given {@code mediaUri} and calls {@code listener} if successful
     * <p>
     * If the download fails, a {@link android.widget.Toast} will be shown.
     *
     * @return A {@link Boolean} indicating whether the download was successful
     */
    public static boolean fetchMediaAndDoNext(Context context, Uri mediaUri, MediaFetchDoNext listener) {
        final Uri downloadedUri = fetchMedia(context, mediaUri);
        if (downloadedUri != null) {
            listener.doNext(downloadedUri);
            return true;
        } else {
            ToastUtils.showToast(context, R.string.error_downloading_image,
                    ToastUtils.Duration.SHORT);
            return false;
        }
    }

    public static List<Uri> retrieveImageEditorResult(Intent data) {
        if (data != null && data.hasExtra(PreviewImageFragment.ARG_EDIT_IMAGE_DATA)) {
            return convertEditImageOutputToListOfUris(data.getParcelableArrayListExtra(
                    PreviewImageFragment.ARG_EDIT_IMAGE_DATA));
        } else {
            return new ArrayList<Uri>();
        }
    }

    private static List<Uri> convertEditImageOutputToListOfUris(List<EditImageData.OutputData> data) {
        List<Uri> uris = new ArrayList<>(data.size());
        for (EditImageData.OutputData item : data) {
            uris.add(Uri.parse(item.getOutputFilePath()));
        }
        return uris;
    }

    public static List<Uri> retrieveMediaUris(Intent data) {
        ClipData clipData = data.getClipData();
        ArrayList<Uri> uriList = new ArrayList<>();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                uriList.add(item.getUri());
            }
        } else {
            uriList.add(data.getData());
        }
        return uriList;
    }

    public static ArrayList<EditImageData.InputData> createListOfEditImageInputData(Context ctx, List<Uri> uris) {
        ArrayList<EditImageData.InputData> inputData = new ArrayList<>(uris.size());
        for (Uri uri : uris) {
            String outputFileExtension = getFileExtension(ctx, uri);
            inputData.add(new EditImageData.InputData(uri.toString(), null, outputFileExtension));
        }
        return inputData;
    }

    public static String getFileExtension(Context ctx, Uri uri) {
        String fileExtension;
        if (uri.getScheme() != null && uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = ctx.getContentResolver();
            String mimeType = cr.getType(uri);
            fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        } else {
            fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        }
        return fileExtension;
    }
}
