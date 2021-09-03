package org.wordpress.android.ui.uploads;

import androidx.annotation.NonNull;

import org.m4m.MediaComposer;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.WPVideoUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.util.Map;

import static org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_VIDEO_CANT_OPTIMIZE;

public class M4mVideoOptimizer extends VideoOptimizerBase implements org.m4m.IProgressListener {
    public M4mVideoOptimizer(
            @NonNull MediaModel media,
            @NonNull VideoOptimizationListener listener) {
        super(media, listener);
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
        sendProgressIfNeeded(progress);
    }

    @Override
    public void onMediaDone() {
        trackVideoProcessingEvents(false, null);
        selectMediaAndSendCompletionToListener();
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

    @Override
    public void start() {
        if (!arePathsValidated()) return;

        MediaComposer mediaComposer = null;
        boolean wasNpeDetected = false;

        try {
            mediaComposer = WPVideoUtils.getVideoOptimizationComposer(
                    getContext(),
                    mInputPath,
                    mOutputPath,
                    this,
                    AppPrefs.getVideoOptimizeWidth(),
                    AppPrefs.getVideoOptimizeQuality());
        } catch (NullPointerException npe) {
            AppLog.w(
                    AppLog.T.MEDIA,
                    "VideoOptimizer > NullPointerException while getting composer " + npe.getMessage()
            );
            wasNpeDetected = true;
        }

        if (mediaComposer == null) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > null composer");
            Map<String, Object> properties = AnalyticsUtils.getMediaProperties(getContext(), true,
                    null, mInputPath);
            properties.put("was_npe_detected", wasNpeDetected);
            properties.put("optimizer_lib", "m4m");
            AnalyticsTracker.track(MEDIA_VIDEO_CANT_OPTIMIZE, properties);
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
}
