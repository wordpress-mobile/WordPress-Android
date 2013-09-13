package org.wordpress.android.ui.media;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;

import org.wordpress.android.R;

public class MediaUtils {

    public class RequestCode {
        public static final int ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY = 1000;
        public static final int ACTIVITY_REQUEST_CODE_TAKE_PHOTO = 1100;
        public static final int ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY = 1200;
        public static final int ACTIVITY_REQUEST_CODE_TAKE_VIDEO = 1300;
        public static final int ACTIVITY_REQUEST_CODE_BROWSE_FILES = 1400;
        public static final int ACTIVITY_REQUEST_CODE_PICTURE_VIDEO_LIBRARY = 1500;
    }
    
    public interface LaunchCameraCallback {
        public void onMediaCapturePathReady(String mediaCapturePath);
    }
    
    public static boolean isValidImage(String url) {
        if (url == null) 
            return false;
        
        if (url.endsWith(".png") || url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".gif"))
            return true;
        return false;
    }
    
    public static boolean isDocument(String url) {
        if (url == null)
            return false;
        
        if (url.endsWith(".doc") || url.endsWith(".docx") || url.endsWith(".odt") || url.endsWith(".pdf"))
            return true;
        return false;
    }
    
    public static boolean isPowerpoint(String url) {
        if (url == null)
            return false;
        
        if (url.endsWith(".ppt") || url.endsWith(".pptx") || url.endsWith(".pps") || url.endsWith(".ppsx") || url.endsWith(".key"))
            return true;
        return false;
    }
    
    public static boolean isSpreadsheet(String url) {
        if (url == null)
            return false;
        
        if (url.endsWith(".xls") || url.endsWith(".xlsx"))
            return true;
        return false;
    }
    
    public static boolean isVideo(String url) {
        if (url == null)
            return false;
        if (url.endsWith(".ogv") || url.endsWith(".mp4") || url.endsWith(".m4v") || url.endsWith(".mov") || 
                url.endsWith(".wmv") || url.endsWith(".avi") || url.endsWith(".mpg") || url.endsWith(".3gp") || url.endsWith(".3g2"))
            return true;
        return false;
    }

    public static int getPlaceholder(String url) {
        if (isValidImage(url))
            return R.drawable.media_image_placeholder;
        else if(isDocument(url))
            return R.drawable.media_document;
        else if(isPowerpoint(url))
            return R.drawable.media_powerpoint;
        else if(isSpreadsheet(url))
            return R.drawable.media_spreadsheet;
        else if(isVideo(url))
            return R.drawable.media_movieclip;
        return 0;
    }
    
    /** E.g. Jul 2, 2013 @ 21:57 **/
    public static String getDate(long ms) {
        Date date = new Date(ms);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy '@' HH:mm", Locale.ENGLISH);
        
        // The timezone on the website is at GMT
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        return sdf.format(date);
    }
 
    
    public static void launchPictureLibrary(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        
        activity.startActivityForResult(intent, RequestCode.ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY);
    }
    
    public static void launchPictureLibrary(Fragment fragment) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        
        fragment.startActivityForResult(intent, RequestCode.ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY);
    }
    
    public static void launchCamera(Fragment fragment, LaunchCameraCallback callback) {
        String state = android.os.Environment.getExternalStorageState();
        if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
            showSDCardRequiredDialog(fragment.getActivity());
        } else {
            Intent intent = prepareLaunchCameraIntent(callback);
            fragment.startActivityForResult(intent, RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO);
        }
    }
    
    public static void launchCamera(Activity activity, LaunchCameraCallback callback) {
        String state = android.os.Environment.getExternalStorageState();
        if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
            showSDCardRequiredDialog(activity);
        } else {
            Intent intent = prepareLaunchCameraIntent(callback);
            activity.startActivityForResult(intent, RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO);
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
                e.printStackTrace();
            }
        }
        return intent;
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
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        activity.startActivityForResult(intent, RequestCode.ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY);
    }
    
    public static void launchVideoLibrary(Fragment fragment) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        fragment.startActivityForResult(intent, RequestCode.ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY);
    }
    
    public static void launchVideoCamera(Activity activity) {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        activity.startActivityForResult(intent, RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO);
    }
    
    public static void launchVideoCamera(Fragment fragment) {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        fragment.startActivityForResult(intent, RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO);
    }

    public static boolean isLocalFile(String state) {
        if (state == null)
            return false;
        
        if (state.equals("queued") || state.equals("uploading") || state.equals("retry") || state.equals("failed"))
            return true;
        
        return false;
    }
    
    public static Uri getLastRecordedVideoUri(Activity activity) {
        String[] proj = { MediaStore.Video.Media._ID };
        Uri contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Video.VideoColumns.DATE_TAKEN + " DESC";
        CursorLoader loader = new CursorLoader(activity, contentUri, proj, null, null, sortOrder);
        Cursor cursor = loader.loadInBackground();
        cursor.moveToFirst();
        
        Uri uri = Uri.parse(contentUri.toString() + "/" + cursor.getLong(0));
        
        return uri;
    }
    
}
