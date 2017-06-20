package org.wordpress.android.ui.posts;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;

import org.m4m.MediaComposer;
import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.FileUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.WPVideoUtils;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * This class is just a helper class to offload all the temporary video encoding logic out of EditPostActivity.
 * It will be soon removed when video encoding is moved to async.
 */
public class EditPostActivityVideoHelper {

    public interface IVideoOptimizationListener {
        void done(String path);
    }

    private final WeakReference<EditPostActivity> mEditPostActivityWeakReference;
    private final String mOriginalPath;
    private final IVideoOptimizationListener mVideoOptimizationListener;
    String mOutFilePath;

    EditPostActivityVideoHelper(@NonNull final EditPostActivity activity, @NonNull final IVideoOptimizationListener listener, @NonNull final String inputPath){
        mEditPostActivityWeakReference = new WeakReference<EditPostActivity>(activity);
        mOriginalPath = inputPath;
        mVideoOptimizationListener = listener;
    }

    public boolean startVideoOptimization() {
        if (!WPMediaUtils.isVideoOptimizationAvailable()) {
            // Video optimization -> API18 or higher
            return false;
        }

        EditPostActivity parentActivity = mEditPostActivityWeakReference.get();
        if (parentActivity == null || parentActivity.isFinishing()) {
            return false;
        }

        // create the destination file
        File cacheDir = MediaUtils.getDiskCacheDir(parentActivity);
        if (cacheDir != null && !cacheDir.exists()) {
            boolean directoryCreated = cacheDir.mkdirs();
            if (!directoryCreated) {
                return  false;
            }
        }
        mOutFilePath = cacheDir.getPath()+ "/" + MediaUtils.generateTimeStampedFileName("video/mp4");

        // Setup video optimization objects
        final VideoOptimizationProgressListener progressListener = new VideoOptimizationProgressListener();
        final MediaComposer mediaComposer = WPVideoUtils.getVideoOptimizationComposer(parentActivity, mOriginalPath, mOutFilePath, progressListener);
        if (mediaComposer == null) {
            AppLog.w(AppLog.T.MEDIA, "Can't optimize this video. Using the original file");
            return false;
        }

        // setup done. We're ready to optimize!

        final ProgressDialog progressDialog = ProgressDialog.show(parentActivity, "", parentActivity.getString(org.wordpress.android.R.string.video_optimization_in_progress),
                true, true, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                progressListener.setStopped(true);
                mediaComposer.stop();
            }
        });
        progressListener.setProgressDialog(progressDialog);

        mediaComposer.start();
        return true;
    }

    private class VideoOptimizationProgressListener implements org.m4m.IProgressListener {
        private WeakReference<ProgressDialog> mWeakProgressDialog;
        private boolean isStopped = false;

        public void setProgressDialog(ProgressDialog progressDialog) {
            this.mWeakProgressDialog = new WeakReference<ProgressDialog>(progressDialog);;
        }

        public void setStopped(boolean stopped) {
            isStopped = stopped;
        }

        @Override
        public void onMediaStart() {

        }

        @Override
        public void onMediaProgress(float progress) {

        }

        @Override
        public void onMediaDone() {
            ProgressDialog pd = mWeakProgressDialog.get();
            dismissProgressDialog(pd);
            if (isStopped) {
                return;
            }
            final EditPostActivity parentActivity = mEditPostActivityWeakReference.get();
            if (parentActivity == null || parentActivity.isFinishing()) {
                return;
            }
            parentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isStopped || parentActivity.isFinishing()) {
                        return;
                    }
                    // Make sure the resulting file is smaller than the original
                    long originalFileSize = FileUtils.length(mOriginalPath);
                    long optimizedFileSize = FileUtils.length(mOutFilePath);
                    String pathToUse = mOutFilePath;
                    if (optimizedFileSize > originalFileSize) {
                        AppLog.w(AppLog.T.MEDIA, "Optimized video is larger than original file "
                                + optimizedFileSize + " > " + originalFileSize );
                        pathToUse = mOriginalPath;
                    }
                    // Upload the file
                    mVideoOptimizationListener.done(pathToUse);
                }
            });
        }

        @Override
        public void onMediaPause() {

        }

        @Override
        public void onMediaStop() {
            // This seems to be called called in 2 cases. Do not use to check if we've manually stopped the composer.
            // 1. When the encoding is done without errors, before onMediaDone!
            // 2. When we call 'stop' on the media composer.
        }

        @Override
        public void onError(Exception exception) {
            AppLog.e(AppLog.T.MEDIA, "Can't optimize the video", exception);
            if (isStopped) {
                return;
            }
            final EditPostActivity parentActivity = mEditPostActivityWeakReference.get();
            if (parentActivity == null || parentActivity.isFinishing()) {
                return;
            }
            ProgressDialog pd = mWeakProgressDialog.get();
            dismissProgressDialog(pd);
            ToastUtils.showToast(parentActivity, R.string.video_optimization_generic_error_message, ToastUtils.Duration.LONG);
            // Upload the original file
            mVideoOptimizationListener.done(mOriginalPath);
        }
    }

    private void dismissProgressDialog(ProgressDialog progressDialog) {
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (IllegalArgumentException e) {
                // dialog doesn't exist
            }
        }
    }
}
