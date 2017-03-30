package org.wordpress.android.ui.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.io.IOException;

public class WordPressMediaUtils {
    public interface LaunchCameraCallback {
        void onMediaCapturePathReady(String mediaCapturePath);
    }

    private static void showSDCardRequiredDialog(Context context) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle(context.getResources().getText(R.string.sdcard_title));
        dialogBuilder.setMessage(context.getResources().getText(R.string.sdcard_message));
        dialogBuilder.setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }

    public static void launchVideoLibrary(Activity activity) {
        AppLockManager.getInstance().setExtendedTimeout();
        activity.startActivityForResult(prepareVideoLibraryIntent(activity),
                RequestCodes.VIDEO_LIBRARY);
    }

    public static void launchVideoLibrary(Fragment fragment) {
        if (!fragment.isAdded()) {
            return;
        }
        AppLockManager.getInstance().setExtendedTimeout();
        fragment.startActivityForResult(prepareVideoLibraryIntent(fragment.getActivity()),
                RequestCodes.VIDEO_LIBRARY);
    }


    public static Intent prepareVideoLibraryIntent(Context context) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        return Intent.createChooser(intent, context.getString(R.string.pick_video));
    }

    public static void launchVideoCamera(Activity activity) {
        AppLockManager.getInstance().setExtendedTimeout();
        activity.startActivityForResult(prepareVideoCameraIntent(), RequestCodes.TAKE_VIDEO);
    }

    public static void launchVideoCamera(Fragment fragment) {
        if (!fragment.isAdded()) {
            return;
        }
        AppLockManager.getInstance().setExtendedTimeout();
        fragment.startActivityForResult(prepareVideoCameraIntent(), RequestCodes.TAKE_VIDEO);
    }

    private static Intent prepareVideoCameraIntent() {
        return new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
    }

    public static void launchPictureLibrary(Activity activity) {
        AppLockManager.getInstance().setExtendedTimeout();
        activity.startActivityForResult(preparePictureLibraryIntent(activity.getString(R.string.pick_photo)),
                RequestCodes.PICTURE_LIBRARY);
    }

    public static void launchPictureLibrary(Fragment fragment) {
        if (!fragment.isAdded()) {
            return;
        }
        AppLockManager.getInstance().setExtendedTimeout();
        fragment.startActivityForResult(preparePictureLibraryIntent(fragment.getActivity()
                .getString(R.string.pick_photo)), RequestCodes.PICTURE_LIBRARY);
    }

    private static Intent preparePictureLibraryIntent(String title) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        return Intent.createChooser(intent, title);
    }

    private static Intent prepareGalleryIntent(String title) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        return Intent.createChooser(intent, title);
    }

    public static void launchCamera(Activity activity, String applicationId, LaunchCameraCallback callback) {
        Intent intent = preparelaunchCamera(activity, applicationId, callback);
        if (intent != null) {
            AppLockManager.getInstance().setExtendedTimeout();
            activity.startActivityForResult(intent, RequestCodes.TAKE_PHOTO);
        }
    }

    public static void launchCamera(Fragment fragment, String applicationId, LaunchCameraCallback callback) {
        if (!fragment.isAdded()) {
            return;
        }
        Intent intent = preparelaunchCamera(fragment.getActivity(), applicationId, callback);
        if (intent != null) {
            AppLockManager.getInstance().setExtendedTimeout();
            fragment.startActivityForResult(intent, RequestCodes.TAKE_PHOTO);
        }
    }

    private static Intent preparelaunchCamera(Context context, String applicationId, LaunchCameraCallback callback) {
        String state = android.os.Environment.getExternalStorageState();
        if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
            showSDCardRequiredDialog(context);
            return null;
        } else {
            return getLaunchCameraIntent(context, applicationId, callback);
        }
    }

    private static Intent getLaunchCameraIntent(Context context, String applicationId, LaunchCameraCallback callback) {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        String mediaCapturePath = path + File.separator + "Camera" + File.separator + "wp-" + System
                .currentTimeMillis() + ".jpg";
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(context,
                applicationId + ".provider", new File(mediaCapturePath)));

        if (callback != null) {
            callback.onMediaCapturePathReady(mediaCapturePath);
        }

        // make sure the directory we plan to store the recording in exists
        File directory = new File(mediaCapturePath).getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            try {
                throw new IOException("Path to file could not be created.");
            } catch (IOException e) {
                AppLog.e(T.POSTS, e);
            }
        }
        return intent;
    }

    public static void launchPictureLibraryOrCapture(Fragment fragment, String applicationId, LaunchCameraCallback
            callback) {
        if (!fragment.isAdded()) {
            return;
        }
        AppLockManager.getInstance().setExtendedTimeout();
        fragment.startActivityForResult(makePickOrCaptureIntent(fragment.getActivity(), applicationId, callback),
                RequestCodes.PICTURE_LIBRARY_OR_CAPTURE);
    }

    private static Intent makePickOrCaptureIntent(Context context, String applicationId, LaunchCameraCallback callback) {
        Intent pickPhotoIntent = prepareGalleryIntent(context.getString(R.string.capture_or_pick_photo));

        if (DeviceUtils.getInstance().hasCamera(context)) {
            Intent cameraIntent = getLaunchCameraIntent(context, applicationId, callback);
            pickPhotoIntent.putExtra(
                    Intent.EXTRA_INITIAL_INTENTS,
                    new Intent[]{ cameraIntent });
        }

        return pickPhotoIntent;
    }

    public static int getPlaceholder(String url) {
        if (MediaUtils.isValidImage(url)) {
            return R.drawable.media_image_placeholder;
        } else if (MediaUtils.isDocument(url)) {
            return R.drawable.media_document;
        } else if (MediaUtils.isPowerpoint(url)) {
            return R.drawable.media_powerpoint;
        } else if (MediaUtils.isSpreadsheet(url)) {
            return R.drawable.media_spreadsheet;
        } else if (MediaUtils.isVideo(url)) {
            return org.wordpress.android.editor.R.drawable.media_movieclip;
        } else if (MediaUtils.isAudio(url)) {
            return R.drawable.media_audio;
        } else {
            return 0;
        }
    }

    public static boolean canDeleteMedia(MediaModel mediaModel) {
        String state = mediaModel.getUploadState();
        return state == null || (!state.equalsIgnoreCase("uploading") && !state.equalsIgnoreCase("deleted"));
    }

    /**
     * Loads the given network image URL into the {@link NetworkImageView}.
     */
    public static void loadNetworkImage(String imageUrl, NetworkImageView imageView, ImageLoader imageLoader) {
        if (imageUrl != null) {
            Uri uri = Uri.parse(imageUrl);
            String filepath = uri.getLastPathSegment();

            int placeholderResId = WordPressMediaUtils.getPlaceholder(filepath);
            imageView.setErrorImageResId(placeholderResId);

            // default image while downloading
            imageView.setDefaultImageResId(R.drawable.media_item_background);

            if (MediaUtils.isValidImage(filepath)) {
                imageView.setTag(imageUrl);
                imageView.setImageUrl(imageUrl, imageLoader);
            } else {
                imageView.setImageResource(placeholderResId);
            }
        } else {
            imageView.setImageResource(0);
        }
    }

    /**
     * Returns a poster (thumbnail) URL given a VideoPress video URL
     * @param videoUrl the remote URL to the VideoPress video
     */
    public static String getVideoPressVideoPosterFromURL(String videoUrl) {
        String posterUrl = "";

        if (videoUrl != null) {
            int filetypeLocation = videoUrl.lastIndexOf(".");
            if (filetypeLocation > 0) {
                posterUrl = videoUrl.substring(0, filetypeLocation) + "_std.original.jpg";
            }
        }

        return posterUrl;
    }
}
