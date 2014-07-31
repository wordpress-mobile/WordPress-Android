package org.wordpress.android.util;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;

import java.util.List;

public class ActivityUtils {
    public static final String UNKNOWN = "unknown";

    /**
     * Requires android.permission.GET_TASKS app permission
     */
    public static String getTopActivityClassName(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        // Get the most recent running task
        List<RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (tasks != null && tasks.get(0) != null && tasks.get(0).topActivity != null) {
            return tasks.get(0).topActivity.getClassName();
        }
        return UNKNOWN;
    }
}
