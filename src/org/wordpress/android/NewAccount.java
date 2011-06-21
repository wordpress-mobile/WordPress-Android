package org.wordpress.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

public class NewAccount extends Activity {
	public boolean success = false;
	public String blogURL, xmlrpcURL;
	public ProgressDialog pd;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_account);	

        Button createAccountButton = (Button) findViewById(R.id.createWPAccount);
        Button dotComButton = (Button) findViewById(R.id.dotcomExisting);
        Button dotOrgButton = (Button) findViewById(R.id.dotorgExisting);
        
        createAccountButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	Intent signupIntent = new Intent(NewAccount.this, Signup.class);
            	startActivity(signupIntent);
            }
        });
        
        dotComButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	Intent i = new Intent(NewAccount.this, AddAccount.class); 
            	i.putExtra("wpcom", true);
            	startActivityForResult(i, 0);
 
            }
        });
        
        dotOrgButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	Intent i = new Intent(NewAccount.this, AddAccount.class);
            	startActivityForResult(i, 0);
 
            }
        });
        
         
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (data != null)
		{

		Bundle extras = data.getExtras();

		switch(requestCode) {
		case 0:
			String action = extras.getString("returnStatus");
			if (action.equals("SAVE")){
			Bundle bundle = new Bundle();
            
            bundle.putString("returnStatus", action);
            Intent mIntent = new Intent();
            mIntent.putExtras(bundle);
            setResult(RESULT_OK, mIntent);
            finish();
			}
		    break;
		}
	}//end null check

	}
	
	@Override public boolean onKeyDown(int i, KeyEvent event) {

		  // only intercept back button press
		  if (i == KeyEvent.KEYCODE_BACK) {
       	 Bundle bundle = new Bundle();
           
           bundle.putString("returnStatus", "CANCEL");
           Intent mIntent = new Intent();
           mIntent.putExtras(bundle);
           setResult(RESULT_OK, mIntent);
           finish();
		  }

		  return false; // propagate this keyevent
		}
	
}