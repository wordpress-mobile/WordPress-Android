package org.wordpress.android.ui.stats.tasks;

import org.json.JSONObject;

/**
 * Created by nbradbury on 2/25/14.
 */
public abstract class StatsTask implements Runnable {
    abstract void parseResponse(JSONObject response);
}
