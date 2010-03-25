package org.wordpress.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;


public class selectCategories extends ListActivity {
    /** Called when the activity is first created. */
	String id = "", categoriesCSV = "";
	long[] checkedCategories;
	private XMLRPCClient client;
	String finalResult = "";
	ProgressDialog pd;
	public String categoryErrorMsg = "";
	public ArrayList<CharSequence> textArray = new ArrayList<CharSequence>();
	public ArrayList<CharSequence> loadTextArray = new ArrayList<CharSequence>();
	private final Handler mHandler = new Handler();
	
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
      
        setContentView(R.layout.select_categories);
        setTitle(getResources().getString(R.string.select_categories));
        final ListView lv = getListView();
        lv.setItemsCanFocus(false);
        
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         checkedCategories = extras.getLongArray("checkedCategories");
         categoriesCSV = extras.getString("categoriesCSV");
        }
      
        loadCategories();
        
    	customButton done = (customButton) findViewById(R.id.categories_done);
    	
    	done.setOnClickListener(new customButton.OnClickListener() {
            public void onClick(View v) {
            	String selectedCategories = "";
            	long checkedItems[] = lv.getCheckItemIds();
            	
            	SparseBooleanArray selectedItems = lv.getCheckedItemPositions();
            	int ctr = 0;
            	Vector rCheckedItems = new Vector();
            	
            	for (int i=0; i<selectedItems.size();i++){
            		if (selectedItems.get(selectedItems.keyAt(i)) == true){
            			rCheckedItems.add(selectedItems.keyAt(i));
            			selectedCategories += loadTextArray.get(selectedItems.keyAt(i)).toString() + ",";
            		}
            	}
            	
            	long finalCheckedItems[] = new long[rCheckedItems.size()];
            	
            	for (int x=0;x<rCheckedItems.size();x++){
            		finalCheckedItems[x] = Long.parseLong(rCheckedItems.get(x).toString());
            	}
            	
            	Bundle bundle = new Bundle();
                
            	selectedCategories = selectedCategories.trim();
            	if (selectedCategories.endsWith(",")){
            		selectedCategories = selectedCategories.substring(0, selectedCategories.length() - 1);
            	}
            	
                bundle.putString("selectedCategories", selectedCategories);
                bundle.putLongArray("checkedItems", finalCheckedItems);
                Intent mIntent = new Intent();
                mIntent.putExtras(bundle);
                setResult(RESULT_OK, mIntent);
                finish();
            	
            }
            
            
        });
    	
customButton cancel = (customButton) findViewById(R.id.categories_cancel);
    	
    	cancel.setOnClickListener(new customButton.OnClickListener() {
            public void onClick(View v) {

            	
            	Bundle bundle = new Bundle();

                Intent mIntent = new Intent();
                mIntent.putExtras(bundle);
                setResult(RESULT_OK, mIntent);
                finish();
            	
            }
            
            
        });
    	
    	final customImageButton refreshCategoriesButton = (customImageButton) findViewById(R.id.refreshCategoriesButton);
        
        refreshCategoriesButton.setOnClickListener(new customImageButton.OnClickListener() {
            public void onClick(View v) {
            	
            	pd = ProgressDialog.show(selectCategories.this,
            			getResources().getText(R.string.refreshing_categories), getResources().getText(R.string.attempting_categories_refresh), true, true);
            	Thread th = new Thread() {
    				public void run() {					
    				    finalResult = getCategories();	
    				    
    				    mHandler.post(mUpdateResults);
    				    
    				}
    			};
    			th.start();
            }
        });
        
        

}
    
    private void loadCategories() {
        loadTextArray.clear();
        categoriesDB categoriesDB = new categoriesDB(this);
    	Vector categoriesVector = categoriesDB.loadCategories(this, id);
    	if (categoriesVector != null)
    	{

	    	for(int i=0; i < categoriesVector.size(); i++)
	        {
	    		loadTextArray.add(categoriesVector.get(i).toString());
	        }
	    	
	        ArrayAdapter<CharSequence> categories = new ArrayAdapter<CharSequence>(this, R.layout.categories_row, loadTextArray);
	        
	          //categories.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	          
	        this.setListAdapter(categories);
	        
	        if (checkedCategories != null){
	        	ListView lv = getListView();
	        	for (int i=0;i<checkedCategories.length;i++){
	        		
	        		lv.setItemChecked((int) checkedCategories[i], true);
	        	
	        	}
	        	
	        }
	        else if (categoriesCSV != null){
	        	String catsArray[] = categoriesCSV.split(",");
	        	ListView lv = getListView();
	        	for (int i=0;i<loadTextArray.size();i++){
	        		
	        		for (int x=0;x<catsArray.length;x++){
	        			if (catsArray[x].equals(loadTextArray.get(i).toString())){
	        				lv.setItemChecked(i, true);
	        			}
	        		}
	        	
	        	}
	        }
    	}
		
	}

	final Runnable mUpdateResults = new Runnable() {
		public void run() {
			if (finalResult.equals("gotCategories"))
			{
		          if (pd.isShowing()){
						pd.dismiss();
						}
		        loadCategories(); 
				Toast.makeText(selectCategories.this, getResources().getText(R.string.categories_refreshed), Toast.LENGTH_SHORT).show();
			}
			else if (finalResult.equals("categoryFault")){
				if (pd.isShowing()){
					pd.dismiss();
					}	
				
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(selectCategories.this);
							  dialogBuilder.setTitle(getResources().getText(R.string.category_refresh_error));
				              dialogBuilder.setMessage(categoryErrorMsg);
				              dialogBuilder.setPositiveButton("OK",  new
				            		  DialogInterface.OnClickListener() {
		                            public void onClick(DialogInterface dialog, int whichButton) {
		                                // Just close the window.
		                        
		                            }
		                        });
				              dialogBuilder.setCancelable(true);
				             dialogBuilder.create().show();
			
			}
		}
	};
	
    public String getCategories(){
    	
    	//gets the categories via xmlrpc call to wp blog
    	Vector res;
        String returnMessage = "";

        //check for the settings
        boolean enteredSettings = checkSettings();

        if (!enteredSettings){
        	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(selectCategories.this);
			  dialogBuilder.setTitle(getResources().getText(R.string.settings_not_found));
            dialogBuilder.setMessage(getResources().getText(R.string.settings_not_found_load_now));
            dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),  new
          		  DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                      // User clicked Yes so delete the contexts.
                  	Intent i = new Intent(selectCategories.this, settings.class);

                  	startActivityForResult(i, 0);
              
                  }
              });
            dialogBuilder.setNegativeButton(getResources().getText(R.string.no), new
          		  DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                      // User clicked No so don't delete (do nothing).
                  }
              });
            dialogBuilder.setCancelable(true);
           dialogBuilder.create().show();
        }
        else{
        	settingsDB settingsDB = new settingsDB(this);
        	Vector categoriesVector = settingsDB.loadSettings(this, id);
        	
        	
	        	String sURL = "";
	        	if (categoriesVector.get(0).toString().contains("xmlrpc.php"))
	        	{
	        		sURL = categoriesVector.get(0).toString();
	        	}
	        	else
	        	{
	        		sURL = categoriesVector.get(0).toString() + "xmlrpc.php";
	        	}
        		String sUsername = categoriesVector.get(2).toString();
        		String sPassword = categoriesVector.get(3).toString();
        	

        
        	Object result[] = null;
        	
        	Object[] params = {
            		1,
            		sUsername,
            		sPassword,
            };
        	
            client = new XMLRPCClient(sURL);
            
            try {
				result = (Object[]) client.call("wp.getCategories", params);
			} catch (XMLRPCException e) {
				// TODO Auto-generated catch block
				e.getMessage();
				e.printStackTrace();
				res = null;
			}
								   
        
        
       // HashMap categoryNames = (HashMap) result[0];
        
            //Vector categoryIds = (Vector) result;
            
            int size = result.length;
            
            //initialize database
            categoriesDB categoriesDB = new categoriesDB(this);
            //wipe out the categories table
            categoriesDB.clearCategories(this, id);
            
            for(int i=0; i<size; i++)
            {
              HashMap curHash = (HashMap) result[i];
              
              String categoryName = curHash.get("categoryName").toString();
              String categoryID = curHash.get("categoryId").toString();
              
              int convertedCategoryID = Integer.parseInt(categoryID);
              
              categoriesDB.insertCategory(this, id, convertedCategoryID, categoryName);
              
              //populate the spinner with the category names
              
              textArray.add(categoryName);
              
            }
            
            returnMessage = "gotCategories";
        
        } //end valid url
        return returnMessage;
    	
    }
    
    public boolean checkSettings(){
		//see if the user has any saved preferences
		 settingsDB settingsDB = new settingsDB(this);
	    	Vector categoriesVector = settingsDB.loadSettings(this, id);
	    	String sURL = null, sUsername = null, sPassword = null;
	    	if (categoriesVector != null){
	    		sURL = categoriesVector.get(0).toString();
	    		sUsername = categoriesVector.get(1).toString();
	    		sPassword = categoriesVector.get(2).toString();
	    	}
 
        boolean validSettings = false;
        
        if ((sURL != "" && sUsername != "" && sPassword != "") && (sURL != null && sUsername != null && sPassword != null)){
        	validSettings = true;
        }
        
        return validSettings;
	}
}


