package org.wordpress.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

public class link extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
		setContentView(R.layout.link);
		
		final Button cancelButton = (Button) findViewById(R.id.cancel);
        final Button okButton = (Button) findViewById(R.id.ok);
        
        okButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	EditText linkTextET = (EditText)findViewById(R.id.linkText);
            	String linkText = linkTextET.getText().toString();
            	
                Bundle bundle = new Bundle();
                
                bundle.putString("linkText", linkText);
                Intent mIntent = new Intent();
                mIntent.putExtras(bundle);
                setResult(RESULT_OK, mIntent);
                finish();
                
            }
        });   
        
        cancelButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	 Bundle bundle = new Bundle();
                 
                 bundle.putString("linkText", "CANCEL");
                 Intent mIntent = new Intent();
                 mIntent.putExtras(bundle);
                 setResult(RESULT_OK, mIntent);
                 finish();
            }
        });
		
	}
	

}
