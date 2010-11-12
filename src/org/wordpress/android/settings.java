package org.wordpress.android;
import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;


public class settings extends Activity {
	protected static Intent svc = null;
	private String id = "", originalUsername;
	private String xmlrpcPath;
	boolean isWPCom = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.settings);
		
		
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
        }
		
		Spinner spinner = (Spinner)this.findViewById(R.id.maxImageWidth);
	    ArrayAdapter<Object> spinnerArrayAdapter = new ArrayAdapter<Object>(this,
	    		R.layout.spinner_textview,
	            new String[] { "Original Size", "100", "200", "300", "400", "500", "600", "700", "800", "900", "1000"});
	    spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(spinnerArrayAdapter);
		
    	WordPressDB settingsDB = new WordPressDB(this);
    	Vector<?> categoriesVector = settingsDB.loadSettings(this, id);
    	if (categoriesVector != null)
    	{
    		xmlrpcPath = categoriesVector.get(0).toString();
    		//String savedBlogName = categoriesVector.get(1).toString();
    		String savedUsername = categoriesVector.get(2).toString();
    		originalUsername = savedUsername;
    		String savedPassword = categoriesVector.get(3).toString();
    		String imagePlacement = categoriesVector.get(4).toString();
    		String sCenterThumbnailString = categoriesVector.get(5).toString();
    		String sFullSizeImageString = categoriesVector.get(6).toString();
    		String location = categoriesVector.get(11).toString();
    		
    		String sWPCom = categoriesVector.get(12).toString();
    		if (sWPCom.equals("1")){
    			isWPCom = true;
    		}
    		
    			
    		boolean sFullSizeImage  = false;
    		if (sFullSizeImageString.equals("1")){
    			sFullSizeImage = true;
    		}
    		
    		boolean sCenterThumbnail = false;
    		if (sCenterThumbnailString.equals("1")){
    			sCenterThumbnail = true;
    		}
    		
    		boolean sLocation  = false;
    		if (location.equals("1")){
    			sLocation = true;
    		}
    		
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
            aboveTextRB.setTag(0);
            belowTextRB.setTag(1);
            
            CheckBox centerThumbnail = (CheckBox)findViewById(R.id.centerThumbnail);
            centerThumbnail.setChecked(sCenterThumbnail);
            
            CheckBox fullSize = (CheckBox)findViewById(R.id.fullSizeImage);
            fullSize.setChecked(sFullSizeImage);
            
            CheckBox locationCB = (CheckBox)findViewById(R.id.location);
            locationCB.setChecked(sLocation);
            
      
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
                
                //capture the entered fields *needs validation*
                EditText usernameET = (EditText)findViewById(R.id.username);
                String username = usernameET.getText().toString();
                EditText passwordET = (EditText)findViewById(R.id.password);
                String password = passwordET.getText().toString();
                
                // trac #55
                String buttonValue = ""; 
                RadioButton aboveTextRB = (RadioButton)findViewById(R.id.aboveText);
                if (aboveTextRB.isChecked()){
                	buttonValue = "Above Text"; 
                }
                else{
                	buttonValue = "Below Text";
                }
                
                CheckBox fullSize = (CheckBox)findViewById(R.id.fullSizeImage);
                boolean fullSizeImageValue = fullSize.isChecked();
                
                Spinner spinner = (Spinner)findViewById(R.id.maxImageWidth);
                String maxImageWidth = spinner.getSelectedItem().toString();
                long maxImageWidthId = spinner.getSelectedItemId();
                int maxImageWidthIdInt = (int) maxImageWidthId;
                CheckBox centerThumbnail = (CheckBox)findViewById(R.id.centerThumbnail);
                boolean centerThumbnailValue = centerThumbnail.isChecked();
                
                CheckBox locationCB = (CheckBox)findViewById(R.id.location);
                boolean locationValue = locationCB.isChecked();

                WordPressDB settingsDB = new WordPressDB(settings.this);

                settingsDB.saveSettings(settings.this, id, xmlrpcPath, username, password, buttonValue, centerThumbnailValue, fullSizeImageValue, maxImageWidth, maxImageWidthIdInt, locationValue, isWPCom, originalUsername);

        		//exit settings screen
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
                 setResult(RESULT_CANCELED, mIntent);
                 finish();
            }
        });
     
	}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
      //ignore orientation change
      super.onConfigurationChanged(newConfig);
    } 
	
}
