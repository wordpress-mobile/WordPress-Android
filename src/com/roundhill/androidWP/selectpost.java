//by Dan Roundhill, danroundhill.com/wptogo
package com.roundhill.androidWP;

import java.util.ArrayList;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;



public class selectpost extends Activity {
private String id = "";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
		setContentView(R.layout.selectpost);
		
		Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
        }
		
		final Spinner postSpinner = (Spinner) findViewById(R.id.selectPost);
		
		savedPostsDB postsDB2 = new savedPostsDB(this);
		Vector postFields = postsDB2.getPosts(this, id);
		
		ArrayList<CharSequence> textArray = new ArrayList<CharSequence>();
		
		if (postFields.size() > 0)
		{
			for(int i = 0; i < postFields.size(); i++)
			{				
		    		textArray.add(postFields.get(i).toString()); 	
			}
			
			
	        ArrayAdapter<CharSequence> aspnPosts = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, textArray);
	        
	          aspnPosts.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	          
	          postSpinner.setAdapter(aspnPosts); 
		} 
		else
		{
			Bundle bundle = new Bundle();
  
                bundle.putString("selectedSaveName", "noPostsFound");
                Intent mIntent2 = new Intent();
                mIntent2.putExtras(bundle);
                setResult(RESULT_OK, mIntent2);
                finish();
		}
        
final customButton okButton = (customButton) findViewById(R.id.selectPostOk);
        
        okButton.setOnClickListener(new customButton.OnClickListener() {
            public void onClick(View v) {
            	
         	 Bundle bundle = new Bundle();
         	 
         	 String selectedPostId = postSpinner.getSelectedItem().toString();
         	 

                 bundle.putString("selectedSaveName", selectedPostId);
                 Intent mIntent2 = new Intent();
                 mIntent2.putExtras(bundle);
                 setResult(RESULT_OK, mIntent2);
                 finish();
            		
            }
        });
        
final customButton delPostsButton = (customButton) findViewById(R.id.deletePosts);
        
		delPostsButton.setOnClickListener(new customButton.OnClickListener() {
            public void onClick(View v) {
            	
            	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(selectpost.this);
  			  dialogBuilder.setTitle("Delete Saved Posts");
              dialogBuilder.setMessage("Are you sure you want to delete all saved posts?");
              dialogBuilder.setPositiveButton("Yes",  new
            		  DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // User clicked Yes so load the URL in the browser.
                	  
                  	savedPostsDB postsDB = new savedPostsDB(selectpost.this);
                    postsDB.clearPosts(selectpost.this, id);
                    Bundle bundle = new Bundle();
                    bundle.putString("selectedSaveName", "CANCEL");
                    Intent mIntent2 = new Intent();
                    mIntent2.putExtras(bundle);
                    setResult(RESULT_OK, mIntent2);
                    finish();

                    }
                });
              dialogBuilder.setNegativeButton("No", new
            		  DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // User clicked No so don't delete posts (do nothing).
                    }
                });
              dialogBuilder.setCancelable(true);
             dialogBuilder.create().show();
            		
            }
        });       
		
		
	}
	

}
