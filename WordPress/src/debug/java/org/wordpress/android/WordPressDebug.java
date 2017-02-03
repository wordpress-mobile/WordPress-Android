package org.wordpress.android;

import com.facebook.stetho.Stetho;

public class WordPressDebug extends WordPress {
    @Override
    public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);
    }
}
