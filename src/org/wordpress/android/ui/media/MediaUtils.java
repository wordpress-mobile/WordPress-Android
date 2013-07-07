package org.wordpress.android.ui.media;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;

import org.wordpress.android.R;

public class MediaUtils {

    private static final Set<String> sSupportedFiles = new HashSet<String>();
    static {
        sSupportedFiles.add(".jpg");
        sSupportedFiles.add(".jpeg");
        sSupportedFiles.add(".png");
        sSupportedFiles.add(".gif");
        sSupportedFiles.add(".doc");
        sSupportedFiles.add(".pdf");
        sSupportedFiles.add(".ppt");
        sSupportedFiles.add(".odt");
        sSupportedFiles.add(".pptx");
        sSupportedFiles.add(".docx");
        sSupportedFiles.add(".pps");
        sSupportedFiles.add(".ppsx");
        sSupportedFiles.add(".xls");
        sSupportedFiles.add(".xlsx");
        sSupportedFiles.add(".key");
    }
    
    
    public class RequestCode {
        public static final int ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY = 1000;
        public static final int ACTIVITY_REQUEST_CODE_TAKE_PHOTO = 1100;
        public static final int ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY = 1200;
        public static final int ACTIVITY_REQUEST_CODE_TAKE_VIDEO = 1300;
        public static final int ACTIVITY_REQUEST_CODE_BROWSE_FILES = 1400;
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
    
    /** E.g. Jul 2, 2013 @ 21:57 **/
    public static String getDate(long ms) {
        Date date = new Date(ms);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy '@' HH:mm");
        
        // The timezone on the website is at GMT
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        return sdf.format(date);
    }
 
    
    public static void launchPictureLibrary(Activity activity) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        activity.startActivityForResult(photoPickerIntent, RequestCode.ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY);
    }
    
    public static void launchPictureLibrary(Fragment fragment) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        fragment.startActivityForResult(photoPickerIntent, RequestCode.ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY);
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
            Intent takePictureFromCameraIntent = prepareLaunchCameraIntent(callback);
            activity.startActivityForResult(takePictureFromCameraIntent, RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO);
        }
    }

    private static Intent prepareLaunchCameraIntent(LaunchCameraCallback callback) {
        String dcimFolderName = Environment.DIRECTORY_DCIM;
        if (dcimFolderName == null)
            dcimFolderName = "DCIM";
        String mediaCapturePath = Environment.getExternalStorageDirectory() + File.separator + dcimFolderName + File.separator + "Camera"
                + File.separator + "wp-" + System.currentTimeMillis() + ".jpg";
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
        Intent videoPickerIntent = new Intent(Intent.ACTION_PICK);
        videoPickerIntent.setType("video/*");
        activity.startActivityForResult(videoPickerIntent, RequestCode.ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY);
    }
    
    public static void launchVideoLibrary(Fragment fragment) {
        Intent videoPickerIntent = new Intent(Intent.ACTION_PICK);
        videoPickerIntent.setType("video/*");
        fragment.startActivityForResult(videoPickerIntent, RequestCode.ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY);
    }
    
    public static void launchVideoCamera(Activity activity) {
        activity.startActivityForResult(new Intent(MediaStore.ACTION_VIDEO_CAPTURE), RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO);
    }
    
    public static void launchVideoCamera(Fragment fragment) {
        fragment.startActivityForResult(new Intent(MediaStore.ACTION_VIDEO_CAPTURE), RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO);
    }
    
    public static void launchBrowseFiles(Activity activity) {
        activity.startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT), RequestCode.ACTIVITY_REQUEST_CODE_BROWSE_FILES);
    }
    
    public static void launchBrowseFiles(Fragment fragment) {
        fragment.startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT), RequestCode.ACTIVITY_REQUEST_CODE_BROWSE_FILES);
    }
    
    public static boolean isSupportedFile(String filePath) {
        for(String extension : sSupportedFiles) {
            if(filePath.endsWith(extension))
                return true;
        }
        
        return false;
    }
    
}
