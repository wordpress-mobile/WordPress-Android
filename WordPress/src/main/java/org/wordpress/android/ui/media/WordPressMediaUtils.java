package org.wordpress.android.ui.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.helpers.Version;
import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;

import static org.wordpress.mediapicker.MediaUtils.fadeInImage;

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

    public static void launchCamera(Activity activity, LaunchCameraCallback callback) {
        Intent intent = preparelaunchCamera(activity, callback);
        if (intent != null) {
            AppLockManager.getInstance().setExtendedTimeout();
            activity.startActivityForResult(intent, RequestCodes.TAKE_PHOTO);
        }
    }

    public static void launchCamera(Fragment fragment, LaunchCameraCallback callback) {
        if (!fragment.isAdded()) {
            return;
        }
        Intent intent = preparelaunchCamera(fragment.getActivity(), callback);
        if (intent != null) {
            AppLockManager.getInstance().setExtendedTimeout();
            fragment.startActivityForResult(intent, RequestCodes.TAKE_PHOTO);
        }
    }

    private static Intent preparelaunchCamera(Context context, LaunchCameraCallback callback) {
        String state = android.os.Environment.getExternalStorageState();
        if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
            showSDCardRequiredDialog(context);
            return null;
        } else {
            return getLaunchCameraIntent(callback);
        }
    }

    private static Intent getLaunchCameraIntent(LaunchCameraCallback callback) {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        String mediaCapturePath = path + File.separator + "Camera" + File.separator + "wp-" + System.currentTimeMillis() + ".jpg";
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(mediaCapturePath)));

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

    public static void launchPictureLibraryOrCapture(Fragment fragment, LaunchCameraCallback callback) {
        if (!fragment.isAdded()) {
            return;
        }
        AppLockManager.getInstance().setExtendedTimeout();
        fragment.startActivityForResult(makePickOrCaptureIntent(fragment.getActivity(), callback),
                RequestCodes.PICTURE_LIBRARY_OR_CAPTURE);
    }

    private static Intent makePickOrCaptureIntent(Context context, LaunchCameraCallback callback) {
        Intent pickPhotoIntent = prepareGalleryIntent(context.getString(R.string.capture_or_pick_photo));

        if (DeviceUtils.getInstance().hasCamera(context)) {
            Intent cameraIntent = getLaunchCameraIntent(callback);
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

    /**
     * This is a workaround for WP3.4.2 that deletes the media from the server when editing media properties
     * within the app. See: https://github.com/wordpress-mobile/WordPress-Android/issues/204
     */
    public static boolean isWordPressVersionWithMediaEditingCapabilities() {
        if (WordPress.currentBlog == null) {
            return false;
        }

        if (WordPress.currentBlog.getWpVersion() == null) {
            return true;
        }

        if (WordPress.currentBlog.isDotcomFlag()) {
            return true;
        }

        Version minVersion;
        Version currentVersion;
        try {
            minVersion = new Version("3.5.2");
            currentVersion = new Version(WordPress.currentBlog.getWpVersion());

            if (currentVersion.compareTo(minVersion) == -1) {
                return false;
            }
        } catch (IllegalArgumentException e) {
            AppLog.e(T.POSTS, e);
        }

        return true;
    }

    public static boolean canDeleteMedia(String blogId, String mediaID) {
        Cursor cursor = WordPress.wpDB.getMediaFile(blogId, mediaID);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return false;
        }
        String state = cursor.getString(cursor.getColumnIndex("uploadState"));
        cursor.close();
        return state == null || !state.equals("uploading");
    }

    public static class BackgroundDownloadWebImage extends AsyncTask<Uri, String, Bitmap> {
        WeakReference<ImageView> mReference;

        public BackgroundDownloadWebImage(ImageView resultStore) {
            mReference = new WeakReference<>(resultStore);
        }

        @Override
        protected Bitmap doInBackground(Uri... params) {
            try {
                String uri = params[0].toString();
                Bitmap bitmap = WordPress.getBitmapCache().getBitmap(uri);

                if (bitmap == null) {
                    URL url = new URL(uri);
                    bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    WordPress.getBitmapCache().put(uri, bitmap);
                }

                return bitmap;
            }
            catch(IOException notFoundException) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            ImageView imageView = mReference.get();

            if (imageView != null) {
                if (imageView.getTag() == this) {
                    imageView.setImageBitmap(result);
                    fadeInImage(imageView, result);
                }
            }
        }
    }

    public static Cursor getWordPressMediaImages(String blogId) {
        return WordPress.wpDB.getMediaImagesForBlog(blogId);
    }

    public static Cursor getWordPressMediaVideos(String blogId) {
        return WordPress.wpDB.getMediaFilesForBlog(blogId);
    }

    /**
     * Given a media file cursor, returns the thumbnail network URL. Will use photon if available, using the specified
     * width.
     * @param cursor the media file cursor
     * @param width width to use for photon request (if applicable)
     */
    public static String getNetworkThumbnailUrl(Cursor cursor, int width) {
        String thumbnailURL = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_THUMBNAIL_URL));

        // Allow non-private wp.com and Jetpack blogs to use photon to get a higher res thumbnail
        if ((WordPress.getCurrentBlog() != null && WordPress.getCurrentBlog().isPhotonCapable())) {
            String imageURL = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_URL));
            if (imageURL != null) {
                thumbnailURL = PhotonUtils.getPhotonImageUrl(imageURL, width, 0);
            }
        }

        return thumbnailURL;
    }

    /**
     * Loads the given network image URL into the {@link NetworkImageView}, using the default {@link ImageLoader}.
     */
    public static void loadNetworkImage(String imageUrl, NetworkImageView imageView) {
        loadNetworkImage(imageUrl, imageView, WordPress.imageLoader);
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

            // no default image while downloading
            imageView.setDefaultImageResId(0);

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
