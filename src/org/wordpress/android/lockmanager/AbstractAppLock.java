package org.wordpress.android.lockmanager;

import android.app.Application;

public abstract class AbstractAppLock implements Application.ActivityLifecycleCallbacks {
    public static final int DEFAULT_TIMEOUT = 2; //2 seconds
    public static final int EXTENDED_TIMEOUT = 60; //60 seconds
    
    protected static final String APP_LOCK_PASSWORD_PREF_KEY = "wp_app_lock_password_key";
    
    protected int lockTimeOut = DEFAULT_TIMEOUT;
    
    /*
     * There are situations where an activity will start a different application with an intent.  
     * In these situations call this method right before leaving the app.
     */
    public void setOneTimeTimeout(int timeout) {
        this.lockTimeOut = timeout;
    }
    
    public abstract void enable();
    public abstract void disable();
    public abstract void forcePasswordLock();
    public abstract boolean verifyPassword( String password );
    public abstract boolean isPasswordLocked();
    public abstract boolean setPassword(String password);
}
