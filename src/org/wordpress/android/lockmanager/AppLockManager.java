package org.wordpress.android.lockmanager;

import android.app.Application;

public class AppLockManager {

    private static AppLockManager instance;
    private AbstractAppLock currentAppLocker;
    
    public static AppLockManager getInstance() {
        if (instance == null) {
            instance = new AppLockManager();
        }
        return instance;
    }
        
    public void enableDefaultAppLockIfAvailable(Application currentApp) {
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            currentAppLocker = new DefaultAppLock(currentApp);
            currentAppLocker.enable();
        }
    }

    /*
    public void stopAppLock() {
        if ( currentAppLocker == null )
            return;
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            currentAppLocker.disable();
            currentAppLocker = null;
        }
    }
  */  
    
    /**
     * App lock is available on Android-v14 or higher.
     * @return True if the Passcode Lock feature is available on the device
     */
    public boolean isAppLockFeatureEnabled(){
        return (android.os.Build.VERSION.SDK_INT >= 14) && ( currentAppLocker != null );
    }
    
    public AbstractAppLock getCurrentAppLock() {
        return currentAppLocker;
    }
    
    /*
     * Convenience method used to extend the default timeout.
     * 
     * There are situations where an activity will start a different application with an intent.  
     * In these situations call this method right before leaving the app.
     */
    public void setExtendedTimeout(){
        if ( currentAppLocker == null )
            return;
        currentAppLocker.setOneTimeTimeout(AbstractAppLock.EXTENDED_TIMEOUT);
    }
}
