package org.wordpress.android;

import java.util.ArrayList;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

public class addCategory extends Activity {
	String id = "";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.add_category);
		
		Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
        }
        
        loadCategories();
		
		final Button cancelButton = (Button) findViewById(R.id.cancel);
        final Button okButton = (Button) findViewById(R.id.ok);
        
        okButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	EditText categoryNameET = (EditText)findViewById(R.id.category_name);
            	String category_name = categoryNameET.getText().toString();
            	EditText categorySlugET = (EditText)findViewById(R.id.category_slug);
            	String category_slug = categorySlugET.getText().toString();
            	EditText categoryDescET = (EditText)findViewById(R.id.category_desc);
            	String category_desc = categoryDescET.getText().toString();
            	Spinner sCategories = (Spinner) findViewById(R.id.parent_category);
            	String parent_category = sCategories.getSelectedItem().toString();
            	int parent_id = 0;
            	if (sCategories.getSelectedItemPosition() != 0){
            		categoriesDB categoriesDB = new categoriesDB(addCategory.this);
                	parent_id = categoriesDB.getCategoryId(addCategory.this, id, parent_category);
            	}
            	
            	if (category_name.replaceAll(" ", "").equals("")) {
            		//	Name field cannot be empty
            		
            	  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(addCategory.this);
  				  dialogBuilder.setTitle(getResources().getText(R.string.required_field));
  	              dialogBuilder.setMessage(getResources().getText(R.string.cat_name_required));
  	              dialogBuilder.setPositiveButton("OK",  new
  	            		  DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Just close the window.
                    
                        }
                    });
  	              dialogBuilder.setCancelable(true);
  	             dialogBuilder.create().show();
            	}
            	else {
	                Bundle bundle = new Bundle();
	                
	                bundle.putString("category_name", category_name);
	                bundle.putString("category_slug", category_slug);
	                bundle.putString("category_desc", category_desc);
	                bundle.putInt("parent_id", parent_id);
	                bundle.putString("continue", "TRUE");
	                Intent mIntent = new Intent();
	                mIntent.putExtras(bundle);
	                setResult(RESULT_OK, mIntent);
	                finish();
            	}
                
            }
        });   
        
        cancelButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	 Bundle bundle = new Bundle();
                 
                 bundle.putString("continue", "FALSE");
                 Intent mIntent = new Intent();
                 mIntent.putExtras(bundle);
                 setResult(RESULT_OK, mIntent);
                 finish();
            }
        });
		
	}
	
	private void loadCategories() {
		ArrayList<CharSequence> loadTextArray = new ArrayList<CharSequence>();
        loadTextArray.clear();
        categoriesDB categoriesDB = new categoriesDB(this);
    	Vector categoriesVector = categoriesDB.loadCategories(this, id);
    	if (categoriesVector.size() > 0)
    	{
    		
    		loadTextArray.add(getResources().getText(R.string.none));
    		
	    	for(int i=0; i < categoriesVector.size(); i++)
	        {
	    		loadTextArray.add(categoriesVector.get(i).toString());
	        }
	    	
	        ArrayAdapter<CharSequence> categories = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_dropdown_item_1line, loadTextArray);
	        categories.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        
	        Spinner sCategories = (Spinner) findViewById(R.id.parent_category);

	        
	        sCategories.setAdapter(categories);

    	}
		
	}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
      //ignore orientation change
      super.onConfigurationChanged(newConfig);
    } 	

}
