package org.wordpress.android.editor;

import org.wordpress.android.util.AppLog;

public class TestingUtils {
    public static void waitFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            AppLog.e(AppLog.T.EDITOR, "Thread interrupted");
        }
    }
}
