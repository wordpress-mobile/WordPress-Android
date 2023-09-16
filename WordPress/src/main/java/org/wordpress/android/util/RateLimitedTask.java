package org.wordpress.android.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;

public abstract class RateLimitedTask {
    @Nullable
    private Date mLastUpdate;
    @NonNull
    private int mMinRateInSeconds;

    public RateLimitedTask(@NonNull int minRateInSeconds) {
        mMinRateInSeconds = minRateInSeconds;
    }

    public void forceLastUpdate() {
        mLastUpdate = new Date();
    }

    @NonNull
    public synchronized boolean forceRun() {
        if (run()) {
            mLastUpdate = new Date();
            return true;
        }
        return false;
    }

    @NonNull
    public synchronized boolean runIfNotLimited() {
        Date now = new Date();
        if (mLastUpdate == null || DateTimeUtils.secondsBetween(now, mLastUpdate) >= mMinRateInSeconds) {
            if (run()) {
                mLastUpdate = now;
                return true;
            }
        }
        return false;
    }

    @NonNull
    protected abstract boolean run();
}
