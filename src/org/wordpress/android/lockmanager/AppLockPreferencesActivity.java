package org.wordpress.android.lockmanager;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.wordpress.android.R;

public class AppLockPreferencesActivity extends Activity implements OnClickListener {

    private EditText mPasswordField = null;
    private Button mSaveButton = null;
    private CheckBox mEnabledCheckBox = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_lock_preferences);
        mPasswordField = (EditText) findViewById(R.id.password);
        mSaveButton = (Button) findViewById(R.id.save);
        mSaveButton.setOnClickListener(this);
        mEnabledCheckBox = (CheckBox) findViewById(R.id.applock_checkbox);
        mEnabledCheckBox.setChecked(AppLockManager.getInstance().getCurrentAppLock().isPasswordLocked());
        
        InputFilter[] filters = new InputFilter[2];
        filters[0]= new InputFilter.LengthFilter(4);
        filters[1] = onlyNumber;
        
        mPasswordField.setFilters(filters);
    }

    
    private InputFilter onlyNumber = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            
            if( source.length() > 1 )
                return "";

            if( source.length() == 0 ) //erase
                return null;

            try {
                int number = Integer.parseInt(source.toString());
                if( ( number >= 0 ) && ( number <= 9 ) )
                    return String.valueOf(number);
                else
                    return "";
            } catch (NumberFormatException e) {
                return "";
            }
        }
    };
    
    /*
    @Override
    public void onBackPressed() {
        finish();
    }
*/
    @Override
    public void onClick(View arg0) {
        String pinCode = null;
        if( mEnabledCheckBox.isChecked() ) {
            String newPassword = mPasswordField.getText().toString().trim();
            if(newPassword.length() != 4 ) {
                Toast.makeText(AppLockPreferencesActivity.this, "4 numbers please!", Toast.LENGTH_SHORT).show();
                return;
            }
            pinCode = newPassword;
        }
        AppLockManager.getInstance().getCurrentAppLock().setPassword(pinCode);
        finish();
    }
}