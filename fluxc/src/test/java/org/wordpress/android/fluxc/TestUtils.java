package org.wordpress.android.fluxc;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class TestUtils {
    public static final int DEFAULT_TIMEOUT_MS = 30000;

    public static void waitFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            AppLog.e(T.API, "Thread interrupted");
        }
    }

    public static void waitForNetworkCall() {
        waitFor(DEFAULT_TIMEOUT_MS);
    }
}

