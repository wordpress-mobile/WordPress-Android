package org.wordpress.android.ui.uploads;

import androidx.annotation.NonNull;

import com.daasuu.mp4compose.composer.ComposerInterface;
import com.daasuu.mp4compose.composer.Listener;

import org.jetbrains.annotations.NotNull;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.WPVideoUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.util.Map;

import static org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_VIDEO_CANT_OPTIMIZE;

public class Mp4ComposerVideoOptimizer extends VideoOptimizerBase implements Listener {
    public Mp4ComposerVideoOptimizer(
            @NonNull MediaModel media,
            @NonNull VideoOptimizationListener listener) {
        super(media, listener);
    }

    @Override
    public void onStart() {
        mStartTimeMS = System.currentTimeMillis();
    }

    @Override
    public void onProgress(double progress) {
        // this event fires quite often so we only call the listener when progress increases by 1% or more
        // NOTE: progress can be -1 with Mp4Composer library
        if (progress < 0) return;

        sendProgressIfNeeded((float) progress);
    }

    @Override
    public void onCompleted() {
        trackVideoProcessingEvents(false, null);
        selectMediaAndSendCompletionToListener();
    }

    @Override
    public void onCanceled() {
        AppLog.d(AppLog.T.MEDIA, "VideoOptimizer > stopped");
    }

    @Override
    public void onFailed(@NotNull Exception exception) {
        AppLog.e(AppLog.T.MEDIA, "VideoOptimizer > Can't optimize the video", exception);
        trackVideoProcessingEvents(true, exception);
        mListener.onVideoOptimizationCompleted(mMedia);
    }

    @Override public void start() {
        if (!arePathsValidated()) return;

        ComposerInterface composer = null;

        try {
            composer = WPVideoUtils.getVideoOptimizationComposer(
                    mInputPath,
                    mOutputPath,
                    this,
                    AppPrefs.getVideoOptimizeWidth(),
                    AppPrefs.getVideoOptimizeQuality());
        } catch (Exception e) {
            AppLog.w(
                    AppLog.T.MEDIA,
                    "VideoOptimizer > Exception while getting composer " + e.getMessage()
            );
            composer = null;
        }

        if (composer == null) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > null composer");
            Map<String, Object> properties = AnalyticsUtils.getMediaProperties(getContext(), true,
                    null, mInputPath);
            properties.put("optimizer_lib", "mp4composer");
            AnalyticsTracker.track(MEDIA_VIDEO_CANT_OPTIMIZE, properties);
            mListener.onVideoOptimizationCompleted(mMedia);
            return;
        }

        // setup done. We're ready to optimize!
        composer.start();
    }
}
