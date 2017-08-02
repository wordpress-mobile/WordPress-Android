package org.wordpress.android.ui.uploads;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.m4m.MediaComposer;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.posts.EditPostActivityVideoHelper;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.WPVideoUtils;

import java.io.File;

import static org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_VIDEO_CANT_OPTIMIZE;

/**
 *
 */

public class VideoOptimizer {

    private final File mCacheDir;
    private final Uri mVideoUri;

    public VideoOptimizer(@NonNull Uri videoUri) {
        mCacheDir = MediaUtils.getDiskCacheDir(getContext());
        mVideoUri = videoUri;
    }

    private Context getContext() {
        return WordPress.getContext();
    }

    public boolean optimize() {
        if (mCacheDir == null) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > null cache dir");
            return false;
        }

        if (!mCacheDir.exists() && !mCacheDir.mkdirs()) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > cannot create cache dir");
            return false;
        }

        String inputPath = MediaUtils.getRealPathFromURI(getContext(), mVideoUri);
        if (inputPath == null) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > empty input path");
            return false;

        }
        String outputPath = mCacheDir.getPath()+ "/" + MediaUtils.generateTimeStampedFileName("video/mp4");

        EditPostActivityVideoHelper.VideoOptimizationProgressListener progressListener = new EditPostActivityVideoHelper.VideoOptimizationProgressListener();
        MediaComposer mediaComposer = WPVideoUtils.getVideoOptimizationComposer(
                getContext(),
                inputPath,
                outputPath,
                progressListener,
                AppPrefs.getVideoOptimizeWidth(),
                AppPrefs.getVideoOptimizeQuality());

        if (mediaComposer == null) {
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > null composer");
            AnalyticsTracker.track(MEDIA_VIDEO_CANT_OPTIMIZE,
                    AnalyticsUtils.getMediaProperties(getContext(), true, null, inputPath)
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
        } catch(IllegalStateException e) {
            AppLog.e(T.MEDIA, e);
            AppLog.w(AppLog.T.MEDIA, "VideoOptimizer > failed to start composer");
            CrashlyticsUtils.logException(e, AppLog.T.MEDIA);
            return false;
        }

        return true;
    }
}
