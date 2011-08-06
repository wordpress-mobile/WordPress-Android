package org.wordpress.android;
import org.wordpress.android.models.Blog;

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
import android.widget.TextView;


public class Settings extends Activity {
	protected static Intent svc = null;
	private String originalUsername;
	private String xmlrpcPath;
	private Blog blog;
	private int id;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.settings);
		
		
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getInt("id");
         blog = new Blog(id, this);
        }
		
		Spinner spinner = (Spinner)this.findViewById(R.id.maxImageWidth);
	    ArrayAdapter<Object> spinnerArrayAdapter = new ArrayAdapter<Object>(this,
	    		R.layout.spinner_textview,
	            new String[] { "Original Size", "100", "200", "300", "400", "500", "600", "700", "800", "900", "1000"});
	    spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(spinnerArrayAdapter);

    	EditText usernameET = (EditText)findViewById(R.id.username);
    	usernameET.setText(blog.getUsername());
    	originalUsername = blog.getUsername();

    	EditText passwordET = (EditText)findViewById(R.id.password);
    	passwordET.setText(blog.getPassword());



    	EditText httpUserET = (EditText)findViewById(R.id.httpuser);
    	httpUserET.setText(blog.getHttpuser());

    	EditText httpPasswordET = (EditText)findViewById(R.id.httppassword);
    	httpPasswordET.setText(blog.getHttppassword());
    	if (blog.isDotcomFlag()){
    		TextView httpPasswordLabel = (TextView) findViewById(R.id.l_httppassword);
    		TextView httpUserLabel = (TextView) findViewById(R.id.l_httpuser);

    		httpPasswordLabel.setVisibility(View.GONE);
    		httpPasswordET.setVisibility(View.GONE);

    		httpUserLabel.setVisibility(View.GONE);
    		httpUserET.setVisibility(View.GONE);
    	}

    	//radio buttons for image placement

    	RadioButton aboveTextRB = (RadioButton)findViewById(R.id.aboveText);
    	RadioButton belowTextRB = (RadioButton)findViewById(R.id.belowText);
    	aboveTextRB.setTag(0);
    	belowTextRB.setTag(1);

    	CheckBox centerThumbnail = (CheckBox)findViewById(R.id.centerThumbnail);
    	centerThumbnail.setChecked(blog.isCenterThumbnail());

    	CheckBox fullSize = (CheckBox)findViewById(R.id.fullSizeImage);
    	fullSize.setChecked(blog.isFullSizeImage());

    	CheckBox locationCB = (CheckBox)findViewById(R.id.location);
    	locationCB.setChecked(blog.isLocation());


    	spinner.setSelection(blog.getMaxImageWidthId());


    	if (blog.getImagePlacement() != null){
    		if (blog.getImagePlacement().equals("Above Text")){
    			aboveTextRB.setChecked(true);
    		}
    		else
    		{
    			belowTextRB.setChecked(true);
    		}
    	}

        final Button cancelButton = (Button) findViewById(R.id.cancel);
        final Button saveButton = (Button) findViewById(R.id.save);
        
        saveButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                
                //capture the entered fields *needs validation*
                EditText usernameET = (EditText)findViewById(R.id.username);
                blog.setUsername(usernameET.getText().toString());
                EditText passwordET = (EditText)findViewById(R.id.password);
                blog.setPassword(passwordET.getText().toString());
                EditText httpuserET = (EditText)findViewById(R.id.httpuser);
                blog.setHttpuser(httpuserET.getText().toString());
                EditText httppasswordET = (EditText)findViewById(R.id.httppassword);
                blog.setHttppassword(httppasswordET.getText().toString());
                
                // trac #55
                String buttonValue = ""; 
                RadioButton aboveTextRB = (RadioButton)findViewById(R.id.aboveText);
                if (aboveTextRB.isChecked()){
                	buttonValue = "Above Text"; 
                }
                else{
                	buttonValue = "Below Text";
                }
                
                blog.setImagePlacement(buttonValue);
                
                CheckBox fullSize = (CheckBox)findViewById(R.id.fullSizeImage);
                blog.setFullSizeImage(fullSize.isChecked());
                
                Spinner spinner = (Spinner)findViewById(R.id.maxImageWidth);
                blog.setMaxImageWidth(spinner.getSelectedItem().toString());
                
                long maxImageWidthId = spinner.getSelectedItemId();
                int maxImageWidthIdInt = (int) maxImageWidthId;
                
                blog.setMaxImageWidthId(maxImageWidthIdInt);
                
                CheckBox centerThumbnail = (CheckBox)findViewById(R.id.centerThumbnail);
                blog.setCenterThumbnail(centerThumbnail.isChecked());
                
                CheckBox locationCB = (CheckBox)findViewById(R.id.location);
                blog.setLocation(locationCB.isChecked());

                blog.save(Settings.this, originalUsername);
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
