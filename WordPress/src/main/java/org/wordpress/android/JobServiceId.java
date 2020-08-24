package org.wordpress.android;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;

import org.wordpress.android.util.AppLog;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class JobServiceId {
    public static final int JOB_INSTALL_REFERRER_SERVICE_ID = 9000;
    public static final int JOB_STATS_SERVICE_ID = 8000;
    public static final int JOB_NOTIFICATIONS_UPDATE_SERVICE_ID = 7000;
    public static final int JOB_READER_SEARCH_SERVICE_ID = 5000;
    public static final int JOB_PUBLICIZE_UPDATE_SERVICE_ID = 3000;
    public static final int JOB_READER_UPDATE_SERVICE_ID = 2000;
    public static final int JOB_READER_DISCOVER_SERVICE_ID = 10000;
    public static final int JOB_GCM_REG_SERVICE_ID = 1000;

    /*
     * This method checks that a bundle for a given JobService matches perfectly (all extras and all of its
     * values match) to check if it is already scheduled or not.
     * TODO IMPORTANT: note that this particular method checks for int[] and compares within the array, as that's
     * a case we needed to implement. In the future, if you need to compare other kind of arrays, you'll need to
     * implement each case as you see fit.
     */

    public static boolean isJobServiceWithSameParamsPending(Context context, ComponentName componentName,
                                                            PersistableBundle bundleCompare, String exceptKey) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        boolean jobAlreadyScheduled = false;

        List<JobInfo> jobs = scheduler.getAllPendingJobs();
        if (jobs == null) {
            return jobAlreadyScheduled;
        }
        for (JobInfo jobInfo : jobs) {
            // check this is the same Service we are looking for
            if (jobInfo.getService().getClassName().compareTo(componentName.getClassName()) == 0) {
                PersistableBundle extras = jobInfo.getExtras();
                if (extras != null) {
                    // check the keySet for both bundles are the exact same
                    Set<String> keySetOne = extras.keySet();
                    Set<String> keySetTwo = bundleCompare.keySet();

                    // don't check `exceptKey` if it's null
                    if ((exceptKey == null && (keySetOne.size() == keySetTwo.size()))
                        || ((exceptKey != null && (keySetOne.size() - 1 == keySetTwo.size()))
                        && extras.keySet().containsAll(bundleCompare.keySet()))) {
                        // compare all parameters
                        jobAlreadyScheduled = true;
                        for (String key : bundleCompare.keySet()) {
                            // this is contained, check the value is the same now
                            Object one = extras.get(key);
                            Object two = bundleCompare.get(key);

                            if ((one != null && two == null) || (one == null && two != null)) {
                                jobAlreadyScheduled = false;
                                break;
                            }
                            if (one == two) {
                                continue;
                            }
                            if (one instanceof int[] && two instanceof int[]) {
                                if (Arrays.equals((int[]) one, (int[]) two)) {
                                    continue;
                                } else {
                                    jobAlreadyScheduled = false;
                                    break;
                                }
                                // TODO here implement other cases, i.e.
                                // if (one instanceof StringArray && two instanceof StringArray) {
                                //   ...
                                // }
                            } else {
                                if (!one.equals(two)) {
                                    jobAlreadyScheduled = false;
                                    break;
                                }
                            }
                        }
                        // if all is good, we found it
                        if (jobAlreadyScheduled) {
                            AppLog.i(AppLog.T.STATS, "Job was already scheduled");
                            return jobAlreadyScheduled;
                        }
                    }
                }
            }
        }
        return jobAlreadyScheduled;
    }
}
