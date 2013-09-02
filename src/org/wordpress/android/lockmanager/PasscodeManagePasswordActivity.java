package org.wordpress.android.lockmanager;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.R;

public class PasscodeManagePasswordActivity extends AbstractPasscodeKeyboardActivity {
    private int type = -1;
    private String unverifiedPasscode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            type = extras.getInt("type", -1);
        }
        
    }
    
    @Override
    protected void onPinLockInserted() {
        String passLock = pinCodeField1.getText().toString() + pinCodeField2.getText().toString() +
                pinCodeField3.getText().toString() + pinCodeField4.getText();
        
        pinCodeField1.setText("");
        pinCodeField2.setText("");
        pinCodeField3.setText("");
        pinCodeField4.setText("");
        pinCodeField1.requestFocus();
        
        switch (type) {
            
            case PasscodePreferencesActivity.DISABLE_PASSLOCK:
                if( AppLockManager.getInstance().getCurrentAppLock().verifyPassword(passLock) ) {
                    setResult(RESULT_OK);
                    AppLockManager.getInstance().getCurrentAppLock().setPassword(null);
                } else {
                    setResult(RESULT_FIRST_USER);
                }
                finish();
                break;
                
            case PasscodePreferencesActivity.ENABLE_PASSLOCK:
                if( unverifiedPasscode == null ) {
                    ((TextView) findViewById(R.id.top_message)).setText(R.string.passcode_re_enter_passcode);
                    unverifiedPasscode = passLock;
                } else {
                    if( passLock.equals(unverifiedPasscode)) {
                        setResult(RESULT_OK);
                        AppLockManager.getInstance().getCurrentAppLock().setPassword(passLock);
                        finish();
                    } else {
                        unverifiedPasscode = null;
                        topMessage.setText(R.string.passcode_enter_passcode);
                        Toast.makeText(PasscodeManagePasswordActivity.this, getString(R.string.passcode_wrong_passcode), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
                
            case PasscodePreferencesActivity.CHANGE_PASSWORD:
                //verify old password
                if( AppLockManager.getInstance().getCurrentAppLock().verifyPassword(passLock) ) {
                    topMessage.setText(R.string.passcode_enter_passcode);
                    type = PasscodePreferencesActivity.ENABLE_PASSLOCK;
                } else {
                    Toast.makeText(PasscodeManagePasswordActivity.this, getString(R.string.passcode_wrong_passcode), Toast.LENGTH_SHORT).show();
                } 
                break;
                
            default:
                break;
        }
    }
}