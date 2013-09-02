package org.wordpress.android.lockmanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.R;

public class PasscodePreferencesActivity extends Activity {
    
    static final int ENABLE_PASSLOCK = 0;
    static final int DISABLE_PASSLOCK = 1;
    static final int CHANGE_PASSWORD = 2;
    
    private TextView turnPasscodeOnOff = null;
    private TextView changePasscode = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_passcode_preferences);
        
        turnPasscodeOnOff = (TextView) findViewById(R.id.turn_passcode_on_off);
        changePasscode = (TextView) findViewById(R.id.change_passcode);
        
        if ( AppLockManager.getInstance().getCurrentAppLock().isPasswordLocked() ) { 
            turnPasscodeOnOff.setText(R.string.passcode_turn_off);
        } else {           
            turnPasscodeOnOff.setText(R.string.passcode_turn_on);   
            changePasscode.setEnabled(false);
        }
        
        turnPasscodeOnOff.setOnTouchListener(passcodeOnOffTouchListener);
        changePasscode.setOnTouchListener(changePasscodeTouchListener);
        
    }

    
    private OnTouchListener passcodeOnOffTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch (View v, MotionEvent event) {
            int type = AppLockManager.getInstance().getCurrentAppLock().isPasswordLocked() ? DISABLE_PASSLOCK : ENABLE_PASSLOCK;
            Intent i = new Intent(PasscodePreferencesActivity.this, PasscodeManagePasswordActivity.class);
            i.putExtra("type", type);
            startActivityForResult(i, type);
            return false;
        }
    };
    

    private OnTouchListener changePasscodeTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch (View v, MotionEvent event) {
            Intent i = new Intent(PasscodePreferencesActivity.this, PasscodeManagePasswordActivity.class);
            i.putExtra("type", CHANGE_PASSWORD);
            i.putExtra("message", getString(R.string.passcode_enter_old_passcode));
            startActivityForResult(i, CHANGE_PASSWORD);
            return false;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case DISABLE_PASSLOCK:
                if (resultCode == RESULT_OK) {
                    updateUI();
                } else  if (resultCode == RESULT_FIRST_USER ) {
                    Toast.makeText(PasscodePreferencesActivity.this, getString(R.string.passcode_wrong_passcode), Toast.LENGTH_SHORT).show();
                }
                break;
            case ENABLE_PASSLOCK:
            case CHANGE_PASSWORD:
                if (resultCode == RESULT_OK) {
                    Toast.makeText(PasscodePreferencesActivity.this, getString(R.string.passcode_set), Toast.LENGTH_SHORT).show();
                } 
                updateUI();
                break;
        }
    }
    
    private void updateUI() {
        if ( AppLockManager.getInstance().getCurrentAppLock().isPasswordLocked() ) { 
            turnPasscodeOnOff.setText(R.string.passcode_turn_off);
            changePasscode.setEnabled(true);
        } else {           
            turnPasscodeOnOff.setText(R.string.passcode_turn_on);   
            changePasscode.setEnabled(false);
        }
    }

}