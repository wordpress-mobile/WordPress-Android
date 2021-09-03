package org.wordpress.android.ui.uploads;

import android.content.Context;

import androidx.annotation.NonNull;

import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.FileUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import static org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_VIDEO_OPTIMIZED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_VIDEO_OPTIMIZE_ERROR;

public abstract class VideoOptimizerBase implements VideoOptimizerProvider {
    protected final File mCacheDir;
    protected final MediaModel mMedia;
    protected final VideoOptimizationListener mListener;

    protected final String mFilename;
    protected final String mInputPath;
    protected String mOutputPath;
    protected long mStartTimeMS;
    protected float mLastProgress;

    public VideoOptimizerBase(@NonNull MediaModel media, @NonNull VideoOptimizationListener listener) {
        mCacheDir = getContext().getCacheDir();
        mListener = listener;
        mMedia = media;
        mInputPath = mMedia.getFilePath();
        mFilename = MediaUtils.generateTimeStampedFileName("video/mp4");
    }

    protected Context getContext() {
        return WordPress.getContext();
    }

    protected boolean arePathsValidated() {
        if (mInputPath == null) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > empty input path");
            mListener.onVideoOptimizationCompleted(mMedia);
            return false;
        }

        if (mCacheDir == null) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > null cache dir");
            mListener.onVideoOptimizationCompleted(mMedia);
            return false;
        }

        if (!mCacheDir.exists() && !mCacheDir.mkdirs()) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > cannot create cache dir");
            mListener.onVideoOptimizationCompleted(mMedia);
            return false;
        }

        mOutputPath = mCacheDir.getPath() + "/" + mFilename;

        return true;
    }

    protected void trackVideoProcessingEvents(boolean isError, Exception exception) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> inputVideoProperties =
                AnalyticsUtils.getMediaProperties(getContext(), true, null, mInputPath);
        putAllWithPrefix("input_video_", inputVideoProperties, properties);
        if (mOutputPath != null) {
            Map<String, Object> outputVideoProperties = AnalyticsUtils.getMediaProperties(
                    getContext(),
                    true,
                    null,
                    mOutputPath
            );
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
        properties.put("optimizer_lib", "mp4composer");

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

    protected void selectMediaAndSendCompletionToListener() {
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

    protected void sendProgressIfNeeded(float progress) {
        // this event fires quite often so we only call the listener when progress increases by 1% or more
        if (mLastProgress == 0 || (progress - mLastProgress > 0.01F)) {
            AppLog.d(AppLog.T.MEDIA, "VideoOptimizer > " + mMedia.getId() + " - progress: " + progress);
            mLastProgress = progress;
            mListener.onVideoOptimizationProgress(mMedia, progress);
        }
    }
}
