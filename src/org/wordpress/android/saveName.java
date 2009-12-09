//by Dan Roundhill, danroundhill.com/wptogo
package org.wordpress.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;



public class saveName extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
		setContentView(R.layout.savename);
		
		final customButton cancelButton = (customButton) findViewById(R.id.cancel);
        final customButton okButton = (customButton) findViewById(R.id.ok);
        
        okButton.setOnClickListener(new customButton.OnClickListener() {
            public void onClick(View v) {
            	
            	EditText linkTextET = (EditText)findViewById(R.id.linkText);
            	String linkText = linkTextET.getText().toString();
            	
            	
                Bundle bundle = new Bundle();
                
                bundle.putString("saveName", linkText);
                Intent mIntent = new Intent();
                mIntent.putExtras(bundle);
                setResult(RESULT_OK, mIntent);
                finish();
                
            }
        });   
        
        cancelButton.setOnClickListener(new customButton.OnClickListener() {
            public void onClick(View v) {
            	
            	 Bundle bundle = new Bundle();
                 
                 bundle.putString("saveName", "CANCEL");
                 Intent mIntent = new Intent();
                 mIntent.putExtras(bundle);
                 setResult(RESULT_OK, mIntent);
                 finish();
            }
        });
		
	}
	

}
