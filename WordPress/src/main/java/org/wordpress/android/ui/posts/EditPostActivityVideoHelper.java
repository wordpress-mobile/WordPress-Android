package org.wordpress.android.ui.posts;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;

import org.m4m.MediaComposer;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.FileUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.WPVideoUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import static org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_VIDEO_CANT_OPTIMIZE;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_VIDEO_OPTIMIZED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_VIDEO_OPTIMIZE_ERROR;

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
            AnalyticsTracker.track(MEDIA_VIDEO_CANT_OPTIMIZE,
                    AnalyticsUtils.getMediaProperties(parentActivity, true, null, mOriginalPath)
            );
            return false;
        }

        // setup done. We're ready to optimize!

        final ProgressDialog progressDialog = ProgressDialog.show(parentActivity, "", parentActivity.getString(org.wordpress.android.R.string.video_optimization_in_progress),
                true, true, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                progressListener.setStopped(true);
                try {
                    mediaComposer.stop();
                } catch(IllegalStateException err) {
                    AppLog.e(AppLog.T.MEDIA, "Can't stop the media composer.", err);
                }

            }
        });
        progressListener.setProgressDialog(progressDialog);

        try {
            mediaComposer.start();
        } catch(IllegalStateException err) {
            AppLog.e(AppLog.T.MEDIA, "Can't start the media composer. Using the original file", err);
            CrashlyticsUtils.logException(err, AppLog.T.MEDIA);
            return false;
        }

        return true;
    }

    private class VideoOptimizationProgressListener implements org.m4m.IProgressListener {
        private WeakReference<ProgressDialog> mWeakProgressDialog;
        private boolean isStopped = false;
        private long startTime;

        private boolean putAllWithPrefix(String prefix, Map<String, Object> inputMap, Map<String, Object> targetMap) {
            if (inputMap == null || targetMap == null) {
                return false;
            }

            for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
                targetMap.put(prefix + entry.getKey(),  entry.getValue());
            }

            return true;
        }

        /**
         * Analytics about video being uploaded
         */
        private void trackVideoProcessingEvents(boolean isError, Exception exception) {
            final EditPostActivity parentActivity = mEditPostActivityWeakReference.get();
            if (parentActivity == null) {
                return;
            }

            Map<String, Object> properties = new HashMap<>();
            Map<String, Object> inputVideoProperties = AnalyticsUtils.getMediaProperties(parentActivity, true, null, mOriginalPath);
            putAllWithPrefix("input_video_", inputVideoProperties, properties);
            if (mOutFilePath != null) {
                Map<String, Object> outputVideoProperties = AnalyticsUtils.getMediaProperties(parentActivity, true, null, mOutFilePath);
                putAllWithPrefix("output_video_", outputVideoProperties, properties);
                String savedMegabytes = String.valueOf((FileUtils.length(mOriginalPath) - FileUtils.length(mOutFilePath)) /  (1024 * 1024));
                properties.put("saved_megabytes", savedMegabytes);
            }

            long endTime = System.currentTimeMillis();
            properties.put("elapsed_time_ms", endTime - startTime);
            if (isError) {
                properties.put("exception_name", exception.getClass().getCanonicalName());
                properties.put("exception_message",  exception.getMessage());
                // Track to CrashlyticsUtils where it's easier to keep track of errors
                CrashlyticsUtils.logException(exception, AppLog.T.MEDIA);
            }

            AnalyticsTracker.Stat currentStatToTrack = isError ? MEDIA_VIDEO_OPTIMIZE_ERROR : MEDIA_VIDEO_OPTIMIZED;
            AnalyticsTracker.track(currentStatToTrack, properties);
        }

        public void setProgressDialog(ProgressDialog progressDialog) {
            this.mWeakProgressDialog = new WeakReference<ProgressDialog>(progressDialog);;
        }

        public void setStopped(boolean stopped) {
            isStopped = stopped;
        }

        @Override
        public void onMediaStart() {
            startTime = System.currentTimeMillis();
        }

        @Override
        public void onMediaProgress(float progress) {

        }

        @Override
        public void onMediaDone() {
            ProgressDialog pd = mWeakProgressDialog.get();
            dismissProgressDialog(pd);

            trackVideoProcessingEvents(false, null);

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
            trackVideoProcessingEvents(true, exception);

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
