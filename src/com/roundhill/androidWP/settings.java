//by Dan Roundhill, danroundhill.com/wptogo
package com.roundhill.androidWP;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
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


public class settings extends Activity {
	private String id = "";
	private String xmlrpcPath;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
		setContentView(R.layout.settings);
		
		
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
        }
		
		Spinner spinner = (Spinner)this.findViewById(R.id.maxImageWidth);
	    ArrayAdapter spinnerArrayAdapter = new ArrayAdapter<Object>(this,
	        android.R.layout.simple_spinner_item,
	            new String[] { "Original Size", "100", "150", "200" , "250", "300", "350", "400", "450", "500", "550", "600", "650", "700", "750", "800", "850", "900", "950", "1000"});
	    spinner.setAdapter(spinnerArrayAdapter);

		TextView eulaTV = (TextView) this.findViewById(R.id.l_EULA);
		
		eulaTV.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
    		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(settings.this);
  			  dialogBuilder.setTitle("End User License Agreement");
              dialogBuilder.setMessage(R.string.EULA);
              dialogBuilder.setPositiveButton("OK",  new
            		  DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // just close the window.
    
                    }
                });
              dialogBuilder.setCancelable(true);
             dialogBuilder.create().show();
            }
        });
    	
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
    

        
        
        final Button cancelButton = (Button) findViewById(R.id.cancel);
        final Button saveButton = (Button) findViewById(R.id.save);
        
        saveButton.setOnClickListener(new Button.OnClickListener() {
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

                
                
               /* editor.putString("blogURL", blogURL);
                editor.putString("username", username);
                editor.putString("password", password);
                editor.putString("imagePlacement", buttonValue);
                editor.putString("maxImageWidth", maxImageWidth);
                editor.putLong("maxImageWidthId", maxImageWidthId);
                editor.putBoolean("fullSizeImage", fullSizeImageValue);
                editor.putBoolean("centerThumbnail", centerThumbnailValue);*/
                

                settingsDB settingsDB = new settingsDB(settings.this);
                settingsDB.saveSettings(settings.this, id, xmlrpcPath, username, password, buttonValue, centerThumbnailValue, fullSizeImageValue, maxImageWidth, maxImageWidthIdInt);
                
                
                // Don't forget to commit your edits!!!
               // editor.commit();
                
                
                Bundle bundle = new Bundle();
                
                bundle.putString("returnStatus", "SAVE");
                Intent mIntent = new Intent();
                mIntent.putExtras(bundle);
                setResult(RESULT_OK, mIntent);
                finish();
                
            }
        });   
        
        cancelButton.setOnClickListener(new Button.OnClickListener() {
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
