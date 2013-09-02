package org.wordpress.android.lockmanager;

import java.util.Date;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import org.wordpress.android.util.StringUtils;

public class DefaultAppLock extends AbstractAppLock {

    private Application currentApp; //Keep a reference to the app that invoked the locker
    private SharedPreferences settings;
    private Date lostFocusDate;
    private static final String PASSWORD_SALT = "sadasauidhsuyeuihdahdiauhs";

    public DefaultAppLock(Application currentApp) {
        super();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(currentApp); 
        this.settings = settings;
        this.currentApp = currentApp;
    }

    public void enable(){
        if( isPasswordLocked() ) {
            currentApp.unregisterActivityLifecycleCallbacks(this);
            currentApp.registerActivityLifecycleCallbacks(this);
        }
    }
    
    public void disable( ){
        currentApp.unregisterActivityLifecycleCallbacks(this);
    }
    
    public void forcePasswordLock(){
        lostFocusDate = null;
    }
    
    public boolean verifyPassword( String password ){
        String storedPassword = settings.getString(APP_LOCK_PASSWORD_PREF_KEY, "");
        password = PASSWORD_SALT + password + PASSWORD_SALT;
        password = StringUtils.getMd5Hash(password);
        if( password.equalsIgnoreCase(storedPassword) ) {
            lostFocusDate = new Date();
            return true;
        } else {
            return false;
        }
    }
    
    public boolean setPassword(String password){
        SharedPreferences.Editor editor = settings.edit();

        if(password == null) {
            editor.remove(APP_LOCK_PASSWORD_PREF_KEY);
            editor.commit();
            this.disable();
        } else {
            password = PASSWORD_SALT + password + PASSWORD_SALT;
            editor.putString(APP_LOCK_PASSWORD_PREF_KEY, StringUtils.getMd5Hash(password));
            editor.commit();
            this.enable();
        }
        
        return true;
    }
        
    public boolean isPasswordLocked(){
        //Check if we need to show the lock screen at startup
       if( settings.getString(APP_LOCK_PASSWORD_PREF_KEY, "").equals("") ) 
           return false;
          
        return true;
    }
    
    private boolean mustShowUnlockSceen() {

        if( isPasswordLocked() == false)
            return false;
        
        if( lostFocusDate == null )
            return true; //first startup or when we forced to show the password
        
        int currentTimeOut = lockTimeOut; //get a reference to the current password timeout and reset it to default
        lockTimeOut = DEFAULT_TIMEOUT; 
        Date now = new Date();
        long now_ms = now.getTime();
        long lost_focus_ms = lostFocusDate.getTime();
        int secondsPassed = (int) (now_ms - lost_focus_ms)/(1000);
        if (secondsPassed >= currentTimeOut) {         
            lostFocusDate = null;
            return true;
        }

        return false;
    }

    @Override
    public void onActivityPaused(Activity arg0) {
        
        if( arg0.getClass() == PasscodeUnlockActivity.class )
            return;
        
        lostFocusDate = new Date();
        
    }

    @Override
    public void onActivityResumed(Activity arg0) {
        
        if( arg0.getClass() == PasscodeUnlockActivity.class )
            return;

        if(mustShowUnlockSceen()) {
            //uhhh ohhh!
            Intent i = new Intent(arg0.getApplicationContext(), PasscodeUnlockActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            arg0.getApplication().startActivity(i);
            return;
        }
        
    }

    @Override
    public void onActivityCreated(Activity arg0, Bundle arg1) {
    }
    
    @Override
    public void onActivityDestroyed(Activity arg0) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity arg0, Bundle arg1) {
    }

    @Override
    public void onActivityStarted(Activity arg0) {
    }

    @Override
    public void onActivityStopped(Activity arg0) {
    }
}
