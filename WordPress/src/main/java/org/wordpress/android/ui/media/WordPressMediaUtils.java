package org.wordpress.android.ui.media;

import android.app.Activity;
import android.app.AlertDialog;
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

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.helpers.Version;
import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;

import static org.wordpress.mediapicker.MediaUtils.fadeInImage;

public class WordPressMediaUtils {
    public class RequestCode {
        public static final int ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY = 1000;
        public static final int ACTIVITY_REQUEST_CODE_TAKE_PHOTO = 1100;
        public static final int ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY = 1200;
        public static final int ACTIVITY_REQUEST_CODE_TAKE_VIDEO = 1300;
    }

    public interface LaunchCameraCallback {
        public void onMediaCapturePathReady(String mediaCapturePath);
    }

    private static void showSDCardRequiredDialog(Activity activity) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(activity.getResources().getText(R.string.sdcard_title));
        dialogBuilder.setMessage(activity.getResources().getText(R.string.sdcard_message));
        dialogBuilder.setPositiveButton(activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }

    public static void launchVideoLibrary(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        AppLockManager.getInstance().setExtendedTimeout();
        activity.startActivityForResult(Intent.createChooser(intent, activity.getString(R.string.pick_video)),
                RequestCode.ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY);
    }

    public static void launchVideoCamera(Activity activity) {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        activity.startActivityForResult(intent, RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO);
        AppLockManager.getInstance().setExtendedTimeout();
    }


    public static void launchPictureLibrary(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        AppLockManager.getInstance().setExtendedTimeout();
        activity.startActivityForResult(Intent.createChooser(intent, activity.getString(R.string.pick_photo)),
                RequestCode.ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY);
    }

    public static void launchCamera(Activity activity, LaunchCameraCallback callback) {
        String state = android.os.Environment.getExternalStorageState();
        if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
            showSDCardRequiredDialog(activity);
        } else {
            Intent intent = prepareLaunchCameraIntent(callback);
            activity.startActivityForResult(intent, RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO);
            AppLockManager.getInstance().setExtendedTimeout();
        }
    }

    private static Intent prepareLaunchCameraIntent(LaunchCameraCallback callback) {
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
            return R.drawable.media_movieclip;
        }
        return 0;
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
        if (state != null && state.equals("uploading")) {
            return false;
        }
        return true;
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
}
