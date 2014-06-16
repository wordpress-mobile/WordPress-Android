package org.wordpress.android.util;

import java.util.Date;

public abstract class RateLimitedTask {
    private Date mLastUpdate;
    private int mMinRateInSeconds;

    public RateLimitedTask(int minRateInSeconds) {
        mMinRateInSeconds = minRateInSeconds;
    }

    public void forceLastUpdate() {
        mLastUpdate = new Date();
    }

    public synchronized boolean forceRun() {
        if (run()) {
            mLastUpdate = new Date();
            return true;
        }
        return false;
    }

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

    protected abstract boolean run();
}
