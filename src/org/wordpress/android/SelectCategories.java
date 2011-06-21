package org.wordpress.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.wordpress.android.models.Blog;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;


public class SelectCategories extends ListActivity {
    /** Called when the activity is first created. */
	String id = "", categoriesCSV = "";
	long[] checkedCategories;
	private XMLRPCClient client;
	String finalResult = "", addCategoryResult = "";
	ProgressDialog pd;
	public String categoryErrorMsg = "";
	public ArrayList<CharSequence> textArray = new ArrayList<CharSequence>();
	public ArrayList<CharSequence> loadTextArray = new ArrayList<CharSequence>();
	private final Handler mHandler = new Handler();
	private Blog blog;
	
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
      
        setContentView(R.layout.select_categories);
        setTitle(getResources().getString(R.string.select_categories));
        final ListView lv = getListView();
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE); 
        lv.setItemsCanFocus(false);
        
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         blog = new Blog(id, this);
         checkedCategories = extras.getLongArray("checkedCategories");
         categoriesCSV = extras.getString("categoriesCSV");
        }
      
        loadCategories();
        
//    	Button to add a Category
        final ImageButton addCategory = (ImageButton) findViewById(R.id.newCategory);   
        addCategory.setOnClickListener(new ImageButton.OnClickListener() {
        	public void onClick(View v) {
        		
        		Bundle bundle = new Bundle();
            	bundle.putString("id", id);
                Intent i = new Intent(SelectCategories.this, AddCategory.class);
        		i.putExtras(bundle);
        		startActivityForResult(i, 0);
        	}
        });
        
    	Button categoriesDone = (Button) findViewById(R.id.categories_done);
    	
    	categoriesDone.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	String selectedCategories = "";
            	
            	SparseBooleanArray selectedItems = lv.getCheckedItemPositions();
            	Vector<Integer> rCheckedItems = new Vector<Integer>();
            	
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

}
    
    private void loadCategories() {
        loadTextArray.clear();
        WordPressDB categoriesDB = new WordPressDB(this);
    	Vector<?> categoriesVector = categoriesDB.loadCategories(this, id);
    	if (categoriesVector.size() > 0)
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
    	else{
    		//go get the categories!
    		pd = ProgressDialog.show(SelectCategories.this,
        			getResources().getText(R.string.refreshing_categories), getResources().getText(R.string.attempting_categories_refresh), true, true);
        	Thread th = new Thread() {
				public void run() {					
				    finalResult = getCategories();	
				    
				    mHandler.post(mUpdateResults);
				    
				}
			};
			th.start();
    	}
		
	}

	final Runnable mUpdateResults = new Runnable() {
		public void run() {
			if (finalResult.equals("addCategory_success")){
				if (pd.isShowing())
				{
					pd.dismiss();
				}
				
				loadCategories();
				
				Toast.makeText(SelectCategories.this, getResources().getText(R.string.adding_cat_success), Toast.LENGTH_SHORT).show();
			}
			if (finalResult.equals("addCategory_failed")){
				if (pd.isShowing())
				{
					pd.dismiss();
				}
				
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SelectCategories.this);
				  dialogBuilder.setTitle(getResources().getText(R.string.adding_cat_failed));
	              dialogBuilder.setMessage(getResources().getText(R.string.adding_cat_failed_check));
	              dialogBuilder.setPositiveButton("OK",  new
	            		  DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int whichButton) {
                          // Just close the window.
                  
                      }
                  });
	              dialogBuilder.setCancelable(true);
	             dialogBuilder.create().show();
			}
			else if (finalResult.equals("gotCategories")){
		          if (pd.isShowing()){
						pd.dismiss();
						}
		        loadCategories(); 
				Toast.makeText(SelectCategories.this, getResources().getText(R.string.categories_refreshed), Toast.LENGTH_SHORT).show();
			}
			else if (finalResult.equals("FAIL")){
				if (pd.isShowing())
					{
					pd.dismiss();
					}	
				
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SelectCategories.this);
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
        String returnMessage = "";

        
        Object result[] = null;

        Object[] params = {
        		blog.getBlogId(),
        		blog.getUsername(),
        		blog.getPassword(),
        };

        client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog.getHttppassword());

        boolean success = false;

        try {
        	result = (Object[]) client.call("wp.getCategories", params);
        	success = true;
        } catch (XMLRPCException e) {
        	//e.getMessage();
        	e.printStackTrace();
        }

        //Vector categoryIds = (Vector) result;
        if (success){
        	int size = result.length;

        	//initialize database
        	WordPressDB categoriesDB = new WordPressDB(this);
        	//wipe out the categories table
        	categoriesDB.clearCategories(this, id);

        	for(int i=0; i<size; i++)
        	{
        		HashMap<?, ?> curHash = (HashMap<?, ?>) result[i];

        		String categoryName = curHash.get("categoryName").toString();
        		String categoryID = curHash.get("categoryId").toString();

        		int convertedCategoryID = Integer.parseInt(categoryID);

        		categoriesDB.insertCategory(this, id, convertedCategoryID, categoryName);

        		//populate the spinner with the category names

        		textArray.add(categoryName);

        	}

        	returnMessage = "gotCategories";
        }
        else{
        	returnMessage = "FAIL";
        }


        return returnMessage;
    	
    }
    
    /**
     * function addCategory
     * @param String category_name
     * @return
     * @description Adds a new category
     */
    public String addCategory(String category_name, String category_slug, String category_desc, int parent_id) {
    	//	Return string
    	String returnString = "";
    
		//	Store the parameters for wp.addCategory
	    Map<String, Object> struct = new HashMap<String, Object>();
	    struct.put("name", category_name);
	    struct.put("slug", category_slug);
	    struct.put("description", category_desc);
	    struct.put("parent_id", parent_id);

	    client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog.getHttppassword());
	    
	    Object[] params = {
	    		blog.getBlogId(),
	    		blog.getUsername(),
	    		blog.getPassword(),
	    		struct
	    };
	    
	    Object result = null;
	    try {
			result = client.call("wp.newCategory", params);
		} catch (XMLRPCException e) {
			e.printStackTrace();
		}
    	
		if (result == null) {
			returnString = "addCategory_failed";
		}
		else {	//	Category successfully created. "result" is the ID of the new category.
			//	Initialize the category database
			WordPressDB categoriesDB = new WordPressDB(this);
            //	Convert "result" (= category_id) from type Object to int
            int category_id = Integer.parseInt(result.toString());
            //	Insert the new category into database
            categoriesDB.insertCategory(this, id, category_id, category_name);
			
			returnString = "addCategory_success";
		}
		
    	return returnString;
    }
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (data != null)
		{

		final Bundle extras = data.getExtras();

		switch(requestCode) {
		case 0:

			//	Add category
			
			//	Does the user want to continue, or did he press "dismiss"?
			if (extras.getString("continue").equals("TRUE")) {
				//	Get name, slug and desc from Intent
				final String category_name = extras.getString("category_name");
				final String category_slug = extras.getString("category_slug");
				final String category_desc = extras.getString("category_desc");
				final int parent_id = extras.getInt("parent_id");
				
				if (loadTextArray.contains(category_name)) {
					//	A category with the specified name does already exist.
				}
				else {
					//	Add the category
					pd = ProgressDialog.show(SelectCategories.this,
		        			getResources().getText(R.string.cat_adding_category), getResources().getText(R.string.cat_attempt_add_category), true, true);
					Thread th = new Thread() {
	    				public void run() {					
	    				    finalResult = addCategory(category_name, category_slug, category_desc, parent_id);
	    				    
					if (finalResult.equals("addCategory_success")) {
						//	Add category to spinner
						loadTextArray.add(category_name);
						
					}
					
					mHandler.post(mUpdateResults);
					
					}
	    			};
	    			th.start();
	    			
			}

			break;
		}
	}//end null check
	}
	
}
	
	//Add settings to menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);
	    menu.add(0, 0, 0, getResources().getText(R.string.refresh_categories));
	    MenuItem menuItem1 = menu.findItem(0);
	    menuItem1.setIcon(R.drawable.ic_menu_rotate);
	    
	    return true;
	}
	//Menu actions
	@Override
	public boolean onOptionsItemSelected(final MenuItem item){
	    switch (item.getItemId()) {
	    case 0:
	    	pd = ProgressDialog.show(SelectCategories.this,
        			getResources().getText(R.string.refreshing_categories), getResources().getText(R.string.attempting_categories_refresh), true, true);
        	Thread th = new Thread() {
				public void run() {					
				    finalResult = getCategories();	
				    
				    mHandler.post(mUpdateResults);
				    
				}
			};
			th.start();
	    	
	    	return true;
		}
	    return false;
	    	
	}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
      //ignore orientation change
      super.onConfigurationChanged(newConfig);
    } 
	
}


