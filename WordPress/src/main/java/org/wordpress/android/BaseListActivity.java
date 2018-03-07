package org.wordpress.android;

import android.app.ListActivity;
import android.content.Context;

import org.wordpress.android.util.LocaleManager;

public class BaseListActivity extends ListActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }
}
