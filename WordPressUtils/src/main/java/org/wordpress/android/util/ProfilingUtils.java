package org.wordpress.android.util;

import android.os.SystemClock;

import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;

/**
 * forked from android.util.TimingLogger to use AppLog instead of Log + new static interface.
 */
public class ProfilingUtils {
    private static ProfilingUtils sInstance;

    private String mLabel;
    private ArrayList<Long> mSplits;
    private ArrayList<String> mSplitLabels;

    public static void start(String label) {
        getInstance().reset(label);
    }

    public static void split(String splitLabel) {
        getInstance().addSplit(splitLabel);
    }

    public static void dump() {
        getInstance().dumpToLog();
    }

    public static void stop() {
        getInstance().reset(null);
    }

    private static ProfilingUtils getInstance() {
        if (sInstance == null) {
            sInstance = new ProfilingUtils();
        }
        return sInstance;
    }

    public ProfilingUtils() {
        reset("init");
    }

    public void reset(String label) {
        mLabel = label;
        reset();
    }

    public void reset() {
        if (mSplits == null) {
            mSplits = new ArrayList<Long>();
            mSplitLabels = new ArrayList<String>();
        } else {
            mSplits.clear();
            mSplitLabels.clear();
        }
        addSplit(null);
    }

    public void addSplit(String splitLabel) {
        if (mLabel == null) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        mSplits.add(now);
        mSplitLabels.add(splitLabel);
    }

    public void dumpToLog() {
        if (mLabel == null) {
            return;
        }
        AppLog.d(T.PROFILING, mLabel + ": begin");
        final long first = mSplits.get(0);
        long now = first;
        for (int i = 1; i < mSplits.size(); i++) {
            now = mSplits.get(i);
            final String splitLabel = mSplitLabels.get(i);
            final long prev = mSplits.get(i - 1);
            AppLog.d(T.PROFILING, mLabel + ":      " + (now - prev) + " ms, " + splitLabel);
        }
        AppLog.d(T.PROFILING, mLabel + ": end, " + (now - first) + " ms");
    }
}

