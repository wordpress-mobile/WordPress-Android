package org.wordpress.android;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;

import org.wordpress.android.util.LocaleManager;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }
}
