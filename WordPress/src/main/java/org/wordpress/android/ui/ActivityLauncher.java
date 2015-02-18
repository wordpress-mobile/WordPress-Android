package org.wordpress.android.ui;

import android.app.Activity;
import android.content.Intent;

public class ActivityLauncher {

    public static void showSitePickerForResult(Activity activity, boolean visibleAccountsOnly) {
        Intent intent = new Intent(activity, SitePickerActivity.class);
        intent.putExtra(SitePickerActivity.ARG_VISIBLE_ONLY, visibleAccountsOnly);
        activity.startActivityForResult(intent, RequestCodes.SITE_PICKER);
    }

}
