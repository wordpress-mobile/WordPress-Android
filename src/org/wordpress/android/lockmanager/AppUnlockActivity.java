package org.wordpress.android.lockmanager;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.wordpress.android.R;

public class AppUnlockActivity extends Activity implements OnClickListener {

    private EditText pinCodeField = null;
    private Button mUnlockButton = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_unlock);
        pinCodeField = (EditText) findViewById(R.id.pincode);
        mUnlockButton = (Button) findViewById(R.id.unlock);
        mUnlockButton.setOnClickListener(this);
    }

    @Override
    public void onBackPressed() {
        AppLockManager.getInstance().getCurrentAppLock().forcePasswordLock();
        finish();
    }

    @Override
    public void onClick(View arg0) {
           String pinCode = pinCodeField.getText().toString().trim();
           if( AppLockManager.getInstance().getCurrentAppLock().verifyPassword(pinCode) ) {
               finish();
           } else {
               Thread shake = new Thread() {
                   public void run() {
                       Animation shake = AnimationUtils.loadAnimation(AppUnlockActivity.this, R.anim.shake);
                       findViewById(R.id.AppUnlockLinearLayout1).startAnimation(shake);
                       Toast.makeText(AppUnlockActivity.this, getString(R.string.invalid_login), Toast.LENGTH_SHORT).show();
                       pinCodeField.setText("");
                   }
               };
               runOnUiThread(shake);
           }
    }

}