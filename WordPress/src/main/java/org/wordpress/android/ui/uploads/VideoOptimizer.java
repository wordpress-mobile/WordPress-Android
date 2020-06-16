package org.wordpress.android.ui.uploads;

import android.content.Context;

import androidx.annotation.NonNull;

import org.m4m.MediaComposer;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.FileUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.WPVideoUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import static org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_VIDEO_CANT_OPTIMIZE;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_VIDEO_OPTIMIZED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_VIDEO_OPTIMIZE_ERROR;

public class VideoOptimizer implements org.m4m.IProgressListener {
    public interface VideoOptimizationListener {
        void onVideoOptimizationCompleted(@NonNull MediaModel media);

        void onVideoOptimizationProgress(@NonNull MediaModel media, float progress);
    }

    public static class ProgressEvent {
        public final MediaModel media;
        public final float progress;

        public ProgressEvent(@NonNull MediaModel media, float progress) {
            this.media = media;
            this.progress = progress;
        }
    }

    private final File mCacheDir;
    private final MediaModel mMedia;
    private final VideoOptimizationListener mListener;

    private final String mFilename;
    private final String mInputPath;
    private String mOutputPath;
    private long mStartTimeMS;
    private float mLastProgress;

    public VideoOptimizer(@NonNull MediaModel media, @NonNull VideoOptimizationListener listener) {
        mCacheDir = getContext().getCacheDir();
        mListener = listener;
        mMedia = media;
        mInputPath = mMedia.getFilePath();
        mFilename = MediaUtils.generateTimeStampedFileName("video/mp4");
    }

    private Context getContext() {
        return WordPress.getContext();
    }

    public void start() {
        if (mInputPath == null) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > empty input path");
            mListener.onVideoOptimizationCompleted(mMedia);
            return;
        }

        if (mCacheDir == null) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > null cache dir");
            mListener.onVideoOptimizationCompleted(mMedia);
            return;
        }

        if (!mCacheDir.exists() && !mCacheDir.mkdirs()) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > cannot create cache dir");
            mListener.onVideoOptimizationCompleted(mMedia);
            return;
        }

        mOutputPath = mCacheDir.getPath() + "/" + mFilename;

        MediaComposer mediaComposer = WPVideoUtils.getVideoOptimizationComposer(
                getContext(),
                mInputPath,
                mOutputPath,
                this,
                AppPrefs.getVideoOptimizeWidth(),
                AppPrefs.getVideoOptimizeQuality());

        if (mediaComposer == null) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > null composer");
            AnalyticsTracker.track(MEDIA_VIDEO_CANT_OPTIMIZE, AnalyticsUtils.getMediaProperties(getContext(), true,
                    null, mInputPath));
            mListener.onVideoOptimizationCompleted(mMedia);
            return;
        }

        // setup done. We're ready to optimize!
        try {
            mediaComposer.start();
            AppLog.d(AppLog.T.MEDIA, "VideoOptimizer > composer started");
        } catch (IllegalStateException e) {
            AppLog.e(AppLog.T.MEDIA, "VideoOptimizer > failed to start composer", e);
            mListener.onVideoOptimizationCompleted(mMedia);
        }
    }

    private void trackVideoProcessingEvents(boolean isError, Exception exception) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> inputVideoProperties =
                AnalyticsUtils.getMediaProperties(getContext(), true, null, mInputPath);
        putAllWithPrefix("input_video_", inputVideoProperties, properties);
        if (mOutputPath != null) {
            Map<String, Object> outputVideoProperties = AnalyticsUtils.getMediaProperties(getContext(), true, null,
                                                                                          mOutputPath);
            putAllWithPrefix("output_video_", outputVideoProperties, properties);
            String savedMegabytes =
                    String.valueOf((FileUtils.length(mInputPath) - FileUtils.length(mOutputPath)) / (1024 * 1024));
            properties.put("saved_megabytes", savedMegabytes);
        }

        long endTime = System.currentTimeMillis();
        properties.put("elapsed_time_ms", endTime - mStartTimeMS);
        if (isError) {
            properties.put("exception_name", exception.getClass().getCanonicalName());
            properties.put("exception_message", exception.getMessage());
            AppLog.e(T.MEDIA, exception);
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
    public void onMediaProgress(float progress) {
        // this event fires quite often so we only call the listener when progress increases by 1% or more
        if (mLastProgress == 0 || (progress - mLastProgress > 0.01F)) {
            AppLog.d(AppLog.T.MEDIA, "VideoOptimizer > " + mMedia.getId() + " - progress: " + progress);
            mLastProgress = progress;
            mListener.onVideoOptimizationProgress(mMedia, progress);
        }
    }

    @Override
    public void onMediaDone() {
        trackVideoProcessingEvents(false, null);

        long originalFileSize = FileUtils.length(mInputPath);
        long optimizedFileSize = FileUtils.length(mOutputPath);
        long savings = originalFileSize - optimizedFileSize;

        double savingsKb = Math.abs(savings) / 1024;
        String strSavingsKb = new DecimalFormat("0.00").format(savingsKb).concat("KB");

        // make sure the resulting file is smaller than the original
        if (savings <= 0) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > no savings, optimized file is " + strSavingsKb + " larger");
            // no savings, so use original unoptimized media
            mListener.onVideoOptimizationCompleted(mMedia);
        } else {
            AppLog.d(AppLog.T.MEDIA, "VideoOptimizer > reduced by " + strSavingsKb);
            // update media object to point to optimized video
            mMedia.setFilePath(mOutputPath);
            mMedia.setFileName(mFilename);
            mListener.onVideoOptimizationCompleted(mMedia);
        }
    }

    @Override
    public void onMediaPause() {
        AppLog.d(AppLog.T.MEDIA, "VideoOptimizer > paused");
    }

    @Override
    public void onMediaStop() {
        // This seems to be called called in 2 cases. Do not use to check if we've manually stopped the composer.
        // 1. When the encoding is done without errors, before onMediaDone
        // 2. When we call 'stop' on the media composer
        AppLog.d(AppLog.T.MEDIA, "VideoOptimizer > stopped");
    }

    @Override
    public void onError(Exception e) {
        AppLog.e(AppLog.T.MEDIA, "VideoOptimizer > Can't optimize the video", e);
        trackVideoProcessingEvents(true, e);
        mListener.onVideoOptimizationCompleted(mMedia);
    }
}
