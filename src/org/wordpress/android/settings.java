//by Dan Roundhill, danroundhill.com/wptogo
package org.wordpress.android;
import java.util.Vector;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class settings extends Activity {
	protected static Intent svc = null;
	private String id = "", accountName = "";
	private String xmlrpcPath;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.settings);
		
		
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         accountName = extras.getString("accountName");
        }
		
		Spinner spinner = (Spinner)this.findViewById(R.id.maxImageWidth);
	    ArrayAdapter spinnerArrayAdapter = new ArrayAdapter<Object>(this,
	    		R.layout.spinner_textview,
	            new String[] { "Original Size", "100", "200", "300", "400", "500", "600", "700", "800", "900", "1000"});
	    spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(spinnerArrayAdapter);
		
    	settingsDB settingsDB = new settingsDB(this);
    	Vector categoriesVector = settingsDB.loadSettings(this, id);
    	if (categoriesVector != null)
    	{
    		xmlrpcPath = categoriesVector.get(0).toString();
    		//String savedBlogName = categoriesVector.get(1).toString();
    		String savedUsername = categoriesVector.get(2).toString();
    		String savedPassword = categoriesVector.get(3).toString();
    		String imagePlacement = categoriesVector.get(4).toString();
    		String sCenterThumbnailString = categoriesVector.get(5).toString();
    		String sFullSizeImageString = categoriesVector.get(6).toString();
    			
    		boolean sFullSizeImage  = false;
    		if (sFullSizeImageString.equals("1")){
    			sFullSizeImage = true;
    		}
    		
    		boolean sCenterThumbnail = false;
    		if (sCenterThumbnailString.equals("1")){
    			sCenterThumbnail = true;
    		}
    		
    		String maxImageWidth = categoriesVector.get(7).toString();
    		
    		String maxImageWidthId = categoriesVector.get(8).toString();
    		int maxImageWidthIdInt = Integer.parseInt(maxImageWidthId);
    		//int maxImageWidthId = Integer.parseInt(maxImageWidthIdString);
            
            EditText usernameET = (EditText)findViewById(R.id.username);
            usernameET.setText(savedUsername);
            
            EditText passwordET = (EditText)findViewById(R.id.password);
            passwordET.setText(savedPassword);
            //radio buttons for image placement

            RadioButton aboveTextRB = (RadioButton)findViewById(R.id.aboveText);
            RadioButton belowTextRB = (RadioButton)findViewById(R.id.belowText);
            
            CheckBox centerThumbnail = (CheckBox)findViewById(R.id.centerThumbnail);
            centerThumbnail.setChecked(sCenterThumbnail);
            
      
            spinner.setSelection(maxImageWidthIdInt);
            
            
            if (imagePlacement != null){
            if (imagePlacement.equals("Above Text")){
            	aboveTextRB.setChecked(true);
            }
            else
            {
            	belowTextRB.setChecked(true);
            }
            }
    		
    	}
    

        
        
        final customButton cancelButton = (customButton) findViewById(R.id.cancel);
        final customButton saveButton = (customButton) findViewById(R.id.save);
        
        saveButton.setOnClickListener(new customButton.OnClickListener() {
            public void onClick(View v) {
               // SharedPreferences settings = getSharedPreferences("wpAndroidSettings", 0);
               // SharedPreferences.Editor editor = settings.edit();
                
                //capture the entered fields *needs validation*
                EditText usernameET = (EditText)findViewById(R.id.username);
                String username = usernameET.getText().toString();
                EditText passwordET = (EditText)findViewById(R.id.password);
                String password = passwordET.getText().toString();
                
                
                RadioGroup imageRG = (RadioGroup)findViewById(R.id.imagePlacement);
                RadioButton checkedRB = (RadioButton)findViewById(imageRG.getCheckedRadioButtonId());
                String buttonValue = checkedRB.getText().toString();
                boolean fullSizeImageValue = false;
                
                Spinner spinner = (Spinner)findViewById(R.id.maxImageWidth);
                String maxImageWidth = spinner.getSelectedItem().toString();
                long maxImageWidthId = spinner.getSelectedItemId();
                int maxImageWidthIdInt = (int) maxImageWidthId;
                CheckBox centerThumbnail = (CheckBox)findViewById(R.id.centerThumbnail);
                boolean centerThumbnailValue = centerThumbnail.isChecked();

                settingsDB settingsDB = new settingsDB(settings.this);
                settingsDB.saveSettings(settings.this, id, xmlrpcPath, username, password, buttonValue, centerThumbnailValue, fullSizeImageValue, maxImageWidth, maxImageWidthIdInt);
                
        		//exit settings screen
                Bundle bundle = new Bundle();
                
                bundle.putString("returnStatus", "SAVE");
                Intent mIntent = new Intent();
                mIntent.putExtras(bundle);
                setResult(RESULT_OK, mIntent);
                finish();
                
            }
        });   
        
        cancelButton.setOnClickListener(new customButton.OnClickListener() {
            public void onClick(View v) {
            	
            	 Bundle bundle = new Bundle();
                 
                 bundle.putString("returnStatus", "CANCEL");
                 Intent mIntent = new Intent();
                 mIntent.putExtras(bundle);
                 setResult(RESULT_OK, mIntent);
                 finish();
            }
        });
        
        
        
     
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
