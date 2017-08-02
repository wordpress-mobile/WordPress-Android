package org.wordpress.android.ui.uploads;

import android.content.Context;
import android.support.annotation.NonNull;

import org.m4m.MediaComposer;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.FileUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.WPVideoUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_VIDEO_CANT_OPTIMIZE;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_VIDEO_OPTIMIZED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_VIDEO_OPTIMIZE_ERROR;

public class VideoOptimizer implements org.m4m.IProgressListener {

    public interface VideoOptimizationListener {
        void onVideoOptimizationSuccess(@NonNull MediaModel media);
        void onVideoOptimizationFailed(@NonNull MediaModel media);
    }

    private final File mCacheDir;
    private final MediaModel mMedia;
    private final VideoOptimizationListener mListener;

    private String mInputPath;
    private String mOutputPath;
    private long mStartTimeMS;

    public VideoOptimizer(@NonNull MediaModel media, @NonNull VideoOptimizationListener listener) {
        mCacheDir = MediaUtils.getDiskCacheDir(getContext());
        mListener = listener;
        mMedia = media;
        mInputPath = mMedia.getFilePath();
    }

    private Context getContext() {
        return WordPress.getContext();
    }

    public void start() {
        if (mInputPath == null) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > empty input path");
            mListener.onVideoOptimizationFailed(mMedia);
            return;
        }

        if (mCacheDir == null) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > null cache dir");
            mListener.onVideoOptimizationFailed(mMedia);
            return;
        }

        if (!mCacheDir.exists() && !mCacheDir.mkdirs()) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > cannot create cache dir");
            mListener.onVideoOptimizationFailed(mMedia);
            return;
        }

        mOutputPath = mCacheDir.getPath()+ "/" + MediaUtils.generateTimeStampedFileName("video/mp4");

        MediaComposer mediaComposer = WPVideoUtils.getVideoOptimizationComposer(
                getContext(),
                mInputPath,
                mOutputPath,
                this,
                AppPrefs.getVideoOptimizeWidth(),
                AppPrefs.getVideoOptimizeQuality());

        if (mediaComposer == null) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > null composer");
            AnalyticsTracker.track(MEDIA_VIDEO_CANT_OPTIMIZE,
                    AnalyticsUtils.getMediaProperties(getContext(), true, null, mInputPath)
            );
            mListener.onVideoOptimizationFailed(mMedia);
            return;
        }

        // setup done. We're ready to optimize!
        try {
            mediaComposer.start();
        } catch(IllegalStateException e) {
            AppLog.e(AppLog.T.MEDIA, "VideoOptimizer > failed to start composer", e);
            CrashlyticsUtils.logException(e, AppLog.T.MEDIA);
            mListener.onVideoOptimizationFailed(mMedia);
            return;
        }
    }

    private void trackVideoProcessingEvents(boolean isError, Exception exception) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> inputVideoProperties = AnalyticsUtils.getMediaProperties(getContext(), true, null, mInputPath);
        putAllWithPrefix("input_video_", inputVideoProperties, properties);
        if (mOutputPath != null) {
            Map<String, Object> outputVideoProperties = AnalyticsUtils.getMediaProperties(getContext(), true, null,
                    mOutputPath);
            putAllWithPrefix("output_video_", outputVideoProperties, properties);
            String savedMegabytes = String.valueOf((FileUtils.length(mInputPath) - FileUtils.length(mOutputPath)) /  (1024 *
                    1024));
            properties.put("saved_megabytes", savedMegabytes);
        }

        long endTime = System.currentTimeMillis();
        properties.put("elapsed_time_ms", endTime - mStartTimeMS);
        if (isError) {
            properties.put("exception_name", exception.getClass().getCanonicalName());
            properties.put("exception_message",  exception.getMessage());
            // Track to CrashlyticsUtils where it's easier to keep track of errors
            CrashlyticsUtils.logException(exception, AppLog.T.MEDIA);
        }

        AnalyticsTracker.Stat currentStatToTrack = isError ? MEDIA_VIDEO_OPTIMIZE_ERROR : MEDIA_VIDEO_OPTIMIZED;
        AnalyticsTracker.track(currentStatToTrack, properties);
    }

    private void putAllWithPrefix(String prefix, Map<String, Object> inputMap, Map<String, Object> targetMap) {
        if (inputMap != null && targetMap != null) {
            for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
                targetMap.put(prefix + entry.getKey(), entry.getValue());
            }
        }
    }

    /*
     * IProgressListener handlers
     */
    @Override
    public void onMediaStart() {
        mStartTimeMS = System.currentTimeMillis();
    }

    @Override
    public void onMediaProgress(float v) {

    }

    @Override
    public void onMediaDone() {
        trackVideoProcessingEvents(false, null);

        // Make sure the resulting file is smaller than the original
        long originalFileSize = FileUtils.length(mInputPath);
        long optimizedFileSize = FileUtils.length(mOutputPath);
        if (optimizedFileSize > originalFileSize) {
            AppLog.w(AppLog.T.MEDIA, "Optimized video is larger than original file "
                    + optimizedFileSize + " > " + originalFileSize );
            mListener.onVideoOptimizationFailed(mMedia);
        } else {
            mMedia.setFilePath(mOutputPath);
            mListener.onVideoOptimizationSuccess(mMedia);
        }
    }

    @Override
    public void onMediaPause() {

    }

    @Override
    public void onMediaStop() {
        // This seems to be called called in 2 cases. Do not use to check if we've manually stopped the composer.
        // 1. When the encoding is done without errors, before onMediaDone
        // 2. When we call 'stop' on the media composer
    }

    @Override
    public void onError(Exception e) {
        AppLog.e(AppLog.T.MEDIA, "VideoOptimizer > Can't optimize the video", e);
        trackVideoProcessingEvents(true, e);
        mListener.onVideoOptimizationFailed(mMedia);
    }
}
