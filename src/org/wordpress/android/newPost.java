package org.wordpress.android;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import org.apache.http.conn.HttpHostConnectException;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.text.Editable;
import android.text.Selection;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;


public class newPost extends Activity {
    /** Called when the activity is first created. */
	public static long globalData = 0;
	public ProgressDialog pd, imagePD;
	public boolean postStatus = false;
	String[] mFiles=null;
	public String thumbnailPath = null;
	public String imagePath = null;
	public String imageTitle = null;
	public Vector imageUrl = new Vector();
	public Vector thumbnailUrl = new Vector();
	private final Handler mHandler = new Handler();
	public String finalResult = null;
	Vector selectedCategories = new Vector();
	public ArrayList<CharSequence> textArray = new ArrayList<CharSequence>();
	public ArrayList<CharSequence> loadTextArray = new ArrayList<CharSequence>();
	public Boolean newStart = true;
	public String categoryErrorMsg = "";
	private XMLRPCClient client;
	public String id = "";
	private Vector<Uri> selectedImageIDs = new Vector();
	private int selectedImageCtr = 0;
    private String newID = "";
    private String accountName = "";
    public int ID_DIALOG_POSTING = 1;
    public String sMaxImageWidth = "";
    public boolean centerThumbnail = false;
    public String sImagePlacement = "";
    public String content = "";
    public boolean sFullSizeImage  = false;
    String finalThumbURL = "";
    public int imgLooper;
    public String imageContent = "";
    public String imgHTML = "";
    public boolean thumbnailOnly, secondPass, xmlrpcError = false, isPage = false;
    public String SD_CARD_TEMP_DIR = "";
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
      
        setContentView(R.layout.main);	
        
        
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         accountName = extras.getString("accountName");
         isPage = extras.getBoolean("isPage", false);
        }
        
        this.setTitle(accountName + " - " + getResources().getText((isPage) ? R.string.new_page : R.string.new_post));
        
      //remove categories and tags if creating a page
        if (isPage){  
        	TextView tvTitle = (TextView) findViewById(R.id.l_title);
        	tvTitle.setText(getResources().getText(R.string.page_title));
        	TextView tvContent = (TextView) findViewById(R.id.l_content);
        	tvTitle.setText(getResources().getText(R.string.page_content));
        	TextView tvCategories = (TextView) findViewById(R.id.selectedCategories);
    		EditText tagsET = (EditText) findViewById(R.id.tags);
    		TextView tvCategoriesLabel = (TextView) findViewById(R.id.l_category);
    		Spinner spinner = (Spinner) findViewById(R.id.spinner1);
    		TextView tvTagsLabel = (TextView) findViewById(R.id.l_tags);
    		customImageButton btnRefresh = (customImageButton) findViewById(R.id.refreshCategoriesButton);
    		customButton btnClear = (customButton) findViewById(R.id.clearCategories);
    		tvCategories.setVisibility(View.GONE);
    		tagsET.setVisibility(View.GONE);
    		tvCategoriesLabel.setVisibility(View.GONE);
    		spinner.setVisibility(View.GONE);
    		tvTagsLabel.setVisibility(View.GONE);
    		btnRefresh.setVisibility(View.GONE);
    		btnClear.setVisibility(View.GONE);
        }
        
      //clear up some variables
        selectedImageIDs.clear();
        selectedImageCtr = 0;
        
        //loads the categories from the db if they exist
        if (!isPage){
	        loadCategories();
	
	        Spinner spinner = (Spinner) findViewById(R.id.spinner1);
	        
	       
	        spinner.setOnItemSelectedListener(new OnItemSelectedListener(){
	            public void onItemSelected(AdapterView parent, View v,
	                      int position, long id) {
	            	
	            	
	            	if (newStart != true)
	            	{
	                	String selectedItem = parent.getItemAtPosition(position).toString();	
	                	TextView selectedCategoriesTV = (TextView) findViewById(R.id.selectedCategories);
	                	if (!selectedCategories.contains(selectedItem))
	                	{
	                	selectedCategoriesTV.setText(selectedCategoriesTV.getText().toString() + selectedItem + ", ");
	                	selectedCategories.add(selectedItem);
	                	}
	            	}
	            	else
	            	{
	            		newStart = false;
	            	}
	            }
	
	            public void onNothingSelected(AdapterView arg0) {
	                 
	            }
	        }); 
        
	        final customImageButton refreshCategoriesButton = (customImageButton) findViewById(R.id.refreshCategoriesButton);
	        
	        refreshCategoriesButton.setOnClickListener(new customImageButton.OnClickListener() {
	            public void onClick(View v) {
	            	
	            	pd = ProgressDialog.show(newPost.this,
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
	        
	        final customButton clearCategories = (customButton) findViewById(R.id.clearCategories);   
	        
	        clearCategories.setOnClickListener(new customButton.OnClickListener() {
	            public void onClick(View v) {
	            	 
	            	TextView selectedCategoriesTV = (TextView) findViewById(R.id.selectedCategories);
	
	            	selectedCategoriesTV.setText("Selected categories: ");
	            	
	            	selectedCategories.clear();
	            }
	        });
	        
    }
        
        final customButton postButton = (customButton) findViewById(R.id.post);
        
        postButton.setOnClickListener(new customButton.OnClickListener() {
            public void onClick(View v) {

            	boolean result = savePost();
            	
            	if (result){

              	  	Toast.makeText(newPost.this, getResources().getText(R.string.saved_to_local_drafts), Toast.LENGTH_SHORT).show();
              	  Bundle bundle = new Bundle();
                  
                  bundle.putString("returnStatus", "OK");
                  Intent mIntent = new Intent();
                  mIntent.putExtras(bundle);
                  setResult(RESULT_OK, mIntent);
                  finish();
            	}
	
            }
        });
        
            final customButton addPictureButton = (customButton) findViewById(R.id.addPictureButton);
            
            registerForContextMenu(addPictureButton);
            
            addPictureButton.setOnClickListener(new customButton.OnClickListener() {
                public void onClick(View v) {
                	
                	addPictureButton.performLongClick();
                	 
                }
        });
            
final customButton boldButton = (customButton) findViewById(R.id.bold);   
            
            boldButton.setOnClickListener(new customButton.OnClickListener() {
                public void onClick(View v) {
                	 
                	TextView contentText = (TextView) findViewById(R.id.content);

                	int selectionStart = contentText.getSelectionStart();
                	
                	int selectionEnd = contentText.getSelectionEnd();
                	
                	if (selectionStart > selectionEnd){
                		int temp = selectionEnd;
                		selectionEnd = selectionStart;
                		selectionStart = temp;
                	}
                	
                	if (selectionStart == -1 || selectionStart == contentText.getText().toString().length() || (selectionStart == selectionEnd)){
                		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
                		dialogBuilder.setTitle(getResources().getText(R.string.no_text_selected));
                        dialogBuilder.setMessage(getResources().getText(R.string.select_text_to_bold) + " " + getResources().getText(R.string.howto_select_text));
                      dialogBuilder.setPositiveButton("OK",  new
                    		  DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // just close the dialog
                            	
                        
                            }
                        });
                      dialogBuilder.setCancelable(true);
                     dialogBuilder.create().show();
                	}
                	else
                	{
                		String textToBold = contentText.getText().toString().substring(selectionStart, selectionEnd); 
                		textToBold = "<strong>" + textToBold + "</strong>";
                		String firstHalf = contentText.getText().toString().substring(0, selectionStart);
                		String lastHalf = contentText.getText().toString().substring(selectionEnd, contentText.getText().toString().length());
                		contentText.setText(firstHalf + textToBold + lastHalf);
                		Editable etext = (Editable) contentText.getText();
                		Selection.setSelection(etext, selectionStart + textToBold.length());
                		
                	}
                }
        });

            final customButton linkButton = (customButton) findViewById(R.id.link);   
            
linkButton.setOnClickListener(new customButton.OnClickListener() {
                public void onClick(View v) {
                	
                	TextView contentText = (TextView) findViewById(R.id.content);

                	int selectionStart = contentText.getSelectionStart();
                	
                	int selectionEnd = contentText.getSelectionEnd();
                	
                	if (selectionStart > selectionEnd){
                		int temp = selectionEnd;
                		selectionEnd = selectionStart;
                		selectionStart = temp;
                	}
                	
                	if (selectionStart == -1 || selectionStart == contentText.getText().toString().length() || (selectionStart == selectionEnd)){
                		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
                		dialogBuilder.setTitle(getResources().getText(R.string.no_text_selected));
                        dialogBuilder.setMessage(getResources().getText(R.string.select_text_to_link) + " " + getResources().getText(R.string.howto_select_text));
                      dialogBuilder.setPositiveButton("OK",  new
                    		  DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // just close the dialog
                            	
                        
                            }
                        });
                      dialogBuilder.setCancelable(true);
                     dialogBuilder.create().show();
                	}
                	else
                	{
                		Intent i = new Intent(newPost.this, link.class);

                    	startActivityForResult(i, 2);
                	}    	
            
        	
        	
        	
               }
            });
            
            
final customButton emButton = (customButton) findViewById(R.id.em);   
            
            emButton.setOnClickListener(new customButton.OnClickListener() {
                public void onClick(View v) {
                	 
                	TextView contentText = (TextView) findViewById(R.id.content);

                	int selectionStart = contentText.getSelectionStart();
                	
                	int selectionEnd = contentText.getSelectionEnd();
                	
                	if (selectionStart > selectionEnd){
                		int temp = selectionEnd;
                		selectionEnd = selectionStart;
                		selectionStart = temp;
                	}
                	
                	if (selectionStart == -1 || selectionStart == contentText.getText().toString().length() || (selectionStart == selectionEnd)){
                		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
                		dialogBuilder.setTitle(getResources().getText(R.string.no_text_selected));
                        dialogBuilder.setMessage(getResources().getText(R.string.select_text_to_emphasize) + " " + getResources().getText(R.string.howto_select_text));
                      dialogBuilder.setPositiveButton("OK",  new
                    		  DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // just close the dialog
                            	
                        
                            }
                        });
                      dialogBuilder.setCancelable(true);
                     dialogBuilder.create().show();
                	}
                	else
                	{
                		String textToBold = contentText.getText().toString().substring(selectionStart, selectionEnd); 
                		textToBold = "<em>" + textToBold + "</em>";
                		String firstHalf = contentText.getText().toString().substring(0, selectionStart);
                		String lastHalf = contentText.getText().toString().substring(selectionEnd, contentText.getText().toString().length());
                		contentText.setText(firstHalf + textToBold + lastHalf);
                		Editable etext = (Editable) contentText.getText();
                		Selection.setSelection(etext, selectionStart + textToBold.length());
                	}
                }
        });
            
final customButton bquoteButton = (customButton) findViewById(R.id.bquote);   
            
            bquoteButton.setOnClickListener(new customButton.OnClickListener() {
                public void onClick(View v) {
                	 
                	TextView contentText = (TextView) findViewById(R.id.content);

                	int selectionStart = contentText.getSelectionStart();
                	
                	int selectionEnd = contentText.getSelectionEnd();
                	
                	if (selectionStart > selectionEnd){
                		int temp = selectionEnd;
                		selectionEnd = selectionStart;
                		selectionStart = temp;
                	}
                	
                	if (selectionStart == -1 || selectionStart == contentText.getText().toString().length() || (selectionStart == selectionEnd)){
                		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
                		dialogBuilder.setTitle(getResources().getText(R.string.no_text_selected));
                        dialogBuilder.setMessage(getResources().getText(R.string.select_text_to_blockquote) + " " + getResources().getText(R.string.howto_select_text));
                      dialogBuilder.setPositiveButton("OK",  new
                    		  DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // just close the dialog
                            	
                        
                            }
                        });
                      dialogBuilder.setCancelable(true);
                     dialogBuilder.create().show();
                	}
                	else
                	{
                		String textToBold = contentText.getText().toString().substring(selectionStart, selectionEnd); 
                		textToBold = "<blockquote>" + textToBold + "</blockquote>";
                		String firstHalf = contentText.getText().toString().substring(0, selectionStart);
                		String lastHalf = contentText.getText().toString().substring(selectionEnd, contentText.getText().toString().length());
                		contentText.setText(firstHalf + textToBold + lastHalf);
                		Editable etext = (Editable) contentText.getText();
                		Selection.setSelection(etext, selectionStart + textToBold.length());
                	}
                }
        });
            
final customButton clearPictureButton = (customButton) findViewById(R.id.clearPicture);   
            
			clearPictureButton.setOnClickListener(new customButton.OnClickListener() {
                public void onClick(View v) {
                	

			        imageUrl.clear();
			        thumbnailUrl.clear();
			        selectedImageIDs.clear();
			        selectedImageCtr = 0;
			        GridView gridview = (GridView) findViewById(R.id.gridView);
			     	 gridview.setAdapter(null);
                	         	
                }
        });            
          
            
            
    }
    
    public void loadCategories(){
    	//for loading categories from the DB
    	categoriesDB categoriesDB = new categoriesDB(this);
    	Vector categoriesVector = categoriesDB.loadCategories(this, id);
    	if (categoriesVector != null)
    	{

	    	for(int i=0; i < categoriesVector.size(); i++)
	        {
	    		loadTextArray.add(categoriesVector.get(i).toString());
	        }
	    	
	    	Spinner spinner = (Spinner) findViewById(R.id.spinner1);
	        ArrayAdapter<CharSequence> categories = new ArrayAdapter<CharSequence>(newPost.this, R.layout.spinner_textview, loadTextArray);
	        
	          categories.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	          
	          spinner.setAdapter(categories);    
    	}
    	
    }
    
    public String getCategories(){
    	
    	//gets the categories via xmlrpc call to wp blog
    	Vector res;
        String returnMessage = "";

        //check for the settings
        boolean enteredSettings = checkSettings();

        if (!enteredSettings){
        	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
			  dialogBuilder.setTitle(getResources().getText(R.string.settings_not_found));
            dialogBuilder.setMessage(getResources().getText(R.string.settings_not_found_load_now));
            dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),  new
          		  DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                      // User clicked Yes so delete the contexts.
                  	Intent i = new Intent(newPost.this, settings.class);

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
            newStart = true;
        
        
        } //end valid url
        return returnMessage;
    	
    }
    
    
	public boolean savePost() {
		
		
		//grab the form data
        EditText titleET = (EditText)findViewById(R.id.title);
        String title = titleET.getText().toString();
        EditText contentET = (EditText)findViewById(R.id.content);
        String content = contentET.getText().toString();
        String tags = "";
        if (!isPage){
        EditText tagsET = (EditText)findViewById(R.id.tags);
        tags = tagsET.getText().toString();
        }
        CheckBox publishCB = (CheckBox)findViewById(R.id.publish); 
        boolean publishThis = false;
        String images = "";
        String categories = "";
        boolean success = false;
        

        Integer blogID = 1;
        
        Vector<Object> myPostVector = new Vector<Object> ();
        String res = null;
        //before we do anything, validate that the user has entered settings
        boolean enteredSettings = checkSettings();
        
        if (!enteredSettings){
        	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
			  dialogBuilder.setTitle(getResources().getText(R.string.settings_not_found));
            dialogBuilder.setMessage(getResources().getText(R.string.settings_not_found_load_now));
            dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),  new
          		  DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                      // User clicked Yes so delete the contexts.
                  	Intent i = new Intent(newPost.this, settings.class);

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
        else if (title.equals("") || content.equals(""))
        {
        	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
			  dialogBuilder.setTitle(getResources().getText(R.string.empty_fields));
            dialogBuilder.setMessage(getResources().getText(R.string.title_post_required));
            dialogBuilder.setPositiveButton("OK",  new
          		  DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                      //Just close the window
                  }
              });
            dialogBuilder.setCancelable(true);
           dialogBuilder.create().show();
        }
        else {
        
        	//upload the images and return the HTML
        	for (int it = 0; it < selectedImageCtr; it++){
           
        		images += selectedImageIDs.get(it).toString() + ",";

        	}
        	if (!isPage){
        	Spinner spinner = (Spinner) findViewById(R.id.spinner1);
        
        	int itemCount = spinner.getCount();
        	String selectedCategory = "Uncategorized";
        	if (itemCount != 0){
        		selectedCategory = spinner.getSelectedItem().toString();
        	}
        
        	// categoryID = getCategoryId(selectedCategory);
        	String[] theCategories = new String[selectedCategories.size()];
        
        	int catSize = selectedCategories.size();
        
        	for(int i=0; i < selectedCategories.size(); i++)
        	{
        		categories += selectedCategories.get(i).toString() + ",";
        		//theCategories[i] = selectedCategories.get(i).toString();
        	}
        	}
        
        	if (publishCB.isChecked())
        	{
        		publishThis = true;
        	}
        
        	//new feature, automatically save a post as a draft just in case the posting fails
        	localDraftsDB lDraftsDB = new localDraftsDB(this);
        	if (isPage){
        		success = lDraftsDB.saveLocalPageDraft(this, id, title, content, images, publishThis);
        	}
        	else{
        		success = lDraftsDB.saveLocalDraft(this, id, title, content, images, tags, categories, publishThis);
        	}
        
        
        }// if/then for valid settings
        
		return success;
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
	

    
    class ImageFilter implements FilenameFilter
    {
    public boolean accept(File dir, String name)
    {
        return (name.endsWith(".jpg"));
    }
    }
    


	
	interface XMLRPCMethodCallback {
		void callFinished(Object result);
	}

	class XMLRPCMethod extends Thread {
		private String method;
		private Object[] params;
		private Handler handler;
		private XMLRPCMethodCallback callBack;
		public XMLRPCMethod(String method, XMLRPCMethodCallback callBack) {
			this.method = method;
			this.callBack = callBack;
			
			handler = new Handler();
			
		}
		public void call() {
			call(null);
		}
		public void call(Object[] params) {
			this.params = params;
			start();
		}
		@Override
		public void run() {
			
			try {
				final long t0 = System.currentTimeMillis();
				final Object result;
				result = (Object) client.call(method, params);
				final long t1 = System.currentTimeMillis();
				handler.post(new Runnable() {
					public void run() {

						callBack.callFinished(result);
					}
				});
			} catch (final XMLRPCFault e) {
						//pd.dismiss();
						dismissDialog(newPost.this.ID_DIALOG_POSTING);
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
						  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
			              dialogBuilder.setMessage(e.getFaultString());
			              dialogBuilder.setPositiveButton("OK",  new
			            		  DialogInterface.OnClickListener() {
	                          public void onClick(DialogInterface dialog, int whichButton) {
	                              // Just close the window.
	                      
	                          }
	                      });
			              dialogBuilder.setCancelable(true);
			             dialogBuilder.create().show();
			             
					
			} catch (final XMLRPCException e) {
				
				handler.post(new Runnable() {
					public void run() {
						
						Throwable couse = e.getCause();
						if (couse instanceof HttpHostConnectException) {
							//pd.dismiss();
							dismissDialog(newPost.this.ID_DIALOG_POSTING);
						} else {
							//pd.dismiss();
							AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
							  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
				              dialogBuilder.setMessage(e.getMessage() + e.getLocalizedMessage());
				              dialogBuilder.setPositiveButton("OK",  new
				            		  DialogInterface.OnClickListener() {
		                          public void onClick(DialogInterface dialog, int whichButton) {
		                              // Just close the window.
		                      
		                          }
		                      });
				              dialogBuilder.setCancelable(true);
				             dialogBuilder.create().show();
						}
						//Log.d("Test", "error", e);
						
					}
				});
			}
			
		}
	}
	
	class XMLRPCMethodImages extends Thread {
		private String method;
		private Object[] params;
		private Handler handler;
		private XMLRPCMethodCallback callBack;
		public XMLRPCMethodImages(String method, XMLRPCMethodCallback callBack) {
			this.method = method;
			this.callBack = callBack;
			
			//handler = new Handler();
			
		}
		public void call() throws InterruptedException {
			call(null);
		}
		public void call(Object[] params) throws InterruptedException {		
			this.params = params;
			this.method = method;
			//start();
			//join();
			final Object result;
			try {
				result = (Object) client.call(method, params);
				callBack.callFinished(result);
			} catch (XMLRPCException e) {
				xmlrpcError = true;
				e.printStackTrace();
			}
		}
		@Override
		public void run() {
			
			try {
				final long t0 = System.currentTimeMillis();
				final Object result;
				result = (Object) client.call(method, params);
				final long t1 = System.currentTimeMillis();
				handler.post(new Runnable() {
					public void run() {

						callBack.callFinished(result);
					
					
					}
				});
			} catch (final XMLRPCFault e) {
						//pd.dismiss();
						dismissDialog(newPost.this.ID_DIALOG_POSTING);
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
						  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
			              dialogBuilder.setMessage(e.getFaultString());
			              dialogBuilder.setPositiveButton("OK",  new
			            		  DialogInterface.OnClickListener() {
	                          public void onClick(DialogInterface dialog, int whichButton) {
	                              // Just close the window.
	                      
	                          }
	                      });
			              dialogBuilder.setCancelable(true);
			             dialogBuilder.create().show();
			             
					
			} catch (final XMLRPCException e) {
				
				handler.post(new Runnable() {
					public void run() {
						
						Throwable couse = e.getCause();
						if (couse instanceof HttpHostConnectException) {
							dismissDialog(newPost.this.ID_DIALOG_POSTING);
						} else {
							AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
							  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
				              dialogBuilder.setMessage(e.getMessage() + e.getLocalizedMessage());
				              dialogBuilder.setPositiveButton("OK",  new
				            		  DialogInterface.OnClickListener() {
		                          public void onClick(DialogInterface dialog, int whichButton) {
		                              // Just close the window.
		                      
		                          }
		                      });
				              dialogBuilder.setCancelable(true);
				             dialogBuilder.create().show();
						}
						//Log.d("Test", "error", e);
						
					}
				});
			}
			
		}
	}
	
	
	public class ImageAdapter extends BaseAdapter {
	    private Context mContext;

	    public ImageAdapter(Context c) {
	        mContext = c;
	    }

	    public int getCount() {
	        return selectedImageIDs.size();
	    }

	    public Object getItem(int position) {
	        return null;
	    }

	    public long getItemId(int position) {
	        return 0;
	    }

	    // create a new ImageView for each item referenced by the Adapter
	    public View getView(int position, View convertView, ViewGroup parent) {
	        ImageView imageView;
	        if (convertView == null) {  // if it's not recycled, initialize some attributes
	            imageView = new ImageView(mContext);
	            imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
	            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
	            imageView.setPadding(8, 8, 8, 8);
	        } else {
	            imageView = (ImageView) convertView;
	        }
	        Uri tempURI = (Uri) selectedImageIDs.get(position);
	     	   
	        String[] projection = new String[] {
	      		    Images.Thumbnails._ID,
	      		    Images.Thumbnails.DATA
	      		};
	     	   
			Cursor cur = managedQuery(tempURI, projection, null, null, null);
	     	  String thumbData = "";
	     	 
	     	  if (cur.moveToFirst()) {
	     		  
	     		int nameColumn, dataColumn, heightColumn, widthColumn;
	     		
	     			nameColumn = cur.getColumnIndex(Images.Media._ID);
	     	        dataColumn = cur.getColumnIndex(Images.Media.DATA);
	     		             	            
	           
	           thumbData = cur.getString(dataColumn);

	     	  }
	     	   
	     	   File jpeg = new File(thumbData);
	     	   
	     	   imageTitle = jpeg.getName();
	     	  
	     	   byte[] bytes = new byte[(int) jpeg.length()];
	     	   
	     	   DataInputStream in = null;
			try {
				in = new DataInputStream(new FileInputStream(jpeg));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	     	   try {
				in.readFully(bytes);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	     	   try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//create the thumbnail
			
			BitmapFactory.Options opts = new BitmapFactory.Options();
	        opts.inJustDecodeBounds = true;
	        Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
	        
	        int width = opts.outHeight;
	        int height = opts.outWidth;
	        
	        float percentage = (float) 100 / width;
       		float proportionateHeight = height * percentage;
       		int finalHeight = (int) Math.rint(proportionateHeight);

	        
	     // calculate the scale - in this case = 0.4f
	        float scaleWidth = ((float) 100) / width;
	        float scaleHeight = ((float) finalHeight) / height;
	       
	        float finWidth = 200;
    		int sample = 0;

    		float fWidth = width;
            sample= new Double(Math.ceil(fWidth / finWidth)).intValue();
            
    		if(sample == 3){
                sample = 4;
    		}
    		else if(sample > 4 && sample < 8 ){
                sample = 8;
    		}
	        
    		opts.inSampleSize = sample;
	        opts.inJustDecodeBounds = false;
	        
	        Bitmap resizedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts); 
	        
	        imageView.setImageBitmap(resizedBitmap);
	        
	        //resizedBitmap.recycle(); //free up memory
	        
	        return imageView;
	    }
	    
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (data != null || requestCode == 4)
		{
			Bundle extras;
			GridView gridview = (GridView) findViewById(R.id.gridView);

		switch(requestCode) {
		case 0:
			extras = data.getExtras();
		    String title = extras.getString("returnStatus");
		    //Toast.makeText(wpAndroid.this, title, Toast.LENGTH_SHORT).show();
		    break;
		case 1:
		    
		    break;
		case 2:
			extras = data.getExtras();
			String linkText = extras.getString("linkText");
			if (linkText.equals("http://") != true){
				
			
			if (linkText.equals("CANCEL") != true){

			TextView contentText = (TextView) findViewById(R.id.content);

        	int selectionStart = contentText.getSelectionStart();
        	
        	int selectionEnd = contentText.getSelectionEnd();
        	
        	if (selectionStart > selectionEnd){
        		int temp = selectionEnd;
        		selectionEnd = selectionStart;
        		selectionStart = temp;
        	}
        	
			String textToLink = contentText.getText().toString().substring(selectionStart, selectionEnd); 
    		textToLink = "<a href=\"" + linkText + "\">"+ textToLink + "</a>";
    		String firstHalf = contentText.getText().toString().substring(0, selectionStart);
    		String lastHalf = contentText.getText().toString().substring(selectionEnd, contentText.getText().toString().length());
    		contentText.setText(firstHalf + textToLink + lastHalf);
    		Editable etext = (Editable) contentText.getText(); 
    		Selection.setSelection(etext, selectionStart + textToLink.length());
			}
			}
			break;			
		case 3:
 
		    Uri imageUri = data.getData();
		    String imgPath = imageUri.getEncodedPath();
   
           selectedImageIDs.add(selectedImageCtr, imageUri);
           imageUrl.add(selectedImageCtr, imgPath);
           selectedImageCtr++;

	     	  
	     	 gridview.setAdapter(new ImageAdapter(this));
	     	 break;
		case 4:
			if (resultCode == Activity.RESULT_OK) {

                // http://code.google.com/p/android/issues/detail?id=1480

                // on activity return
                File f = new File(SD_CARD_TEMP_DIR);
                try {
                    Uri capturedImage =
                        Uri.parse(android.provider.MediaStore.Images.Media.insertImage(getContentResolver(),
                                        f.getAbsolutePath(), null, null));


                        Log.i("camera", "Selected image: " + capturedImage.toString());

                    //f.delete();
                    
                    Bundle bundle = new Bundle();
                    
                    bundle.putString("imageURI", capturedImage.toString());
                    
                    selectedImageIDs.add(selectedImageCtr, capturedImage);
                    imageUrl.add(selectedImageCtr, capturedImage.toString());
                    selectedImageCtr++;

         	     	 gridview.setAdapter(new ImageAdapter(this));
       
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

        }
        else {
                Log.i("Camera", "Result code was " + resultCode);

        }

		     	break;
		}
		
	}//end null check
	}
	
	final Runnable mUpdateResults = new Runnable() {
		public void run() {
			if (finalResult.equals("gotCategories"))
			{
		        Spinner spinner = (Spinner) findViewById(R.id.spinner1);
		        ArrayAdapter<CharSequence> categories = new ArrayAdapter<CharSequence>(newPost.this, R.layout.spinner_textview, textArray);
		        
		        textArray = new ArrayList<CharSequence>();
		        
		          categories.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		         

		          spinner.setAdapter(categories);
		          if (pd.isShowing()){
						pd.dismiss();
						}
		         
				Toast.makeText(newPost.this, getResources().getText(R.string.categories_refreshed), Toast.LENGTH_SHORT).show();
			}
			else if (finalResult.equals("categoryFault")){
				if (pd.isShowing()){
					pd.dismiss();
					}	
				
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
							  dialogBuilder.setTitle(getResources().getText(R.string.category_refresh_error));
				              dialogBuilder.setMessage(categoryErrorMsg);
				              dialogBuilder.setPositiveButton("Ok",  new
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
	
	@Override
	protected Dialog onCreateDialog(int id) {
	if(id == ID_DIALOG_POSTING){
	ProgressDialog loadingDialog = new ProgressDialog(this);
	loadingDialog.setMessage(getResources().getText((isPage) ? R.string.page_attempt_upload : R.string.post_attempt_upload));
	loadingDialog.setIndeterminate(true);
	loadingDialog.setCancelable(true);
	return loadingDialog;
	}

	return super.onCreateDialog(id);
	}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
      //ignore orientation change
      super.onConfigurationChanged(newConfig);
    } 
	
	@Override public boolean onKeyDown(int i, KeyEvent event) {

		  // only intercept back button press
		  if (i == KeyEvent.KEYCODE_BACK) {
			  
			  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
			  dialogBuilder.setTitle(getResources().getText(R.string.cancel_draft));
              dialogBuilder.setMessage(getResources().getText(R.string.sure_cancel_draft));
              dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),  new
            		  DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	Bundle bundle = new Bundle();
                        
                        bundle.putString("returnStatus", "CANCEL");
                        Intent mIntent = new Intent();
                        mIntent.putExtras(bundle);
                        setResult(RESULT_OK, mIntent);
                        finish();

                
                    }
                });
              dialogBuilder.setNegativeButton(getResources().getText(R.string.no),  new
            		  DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	//just close the dialog window

                    }
                });
              dialogBuilder.setCancelable(true);
             dialogBuilder.create().show();
     	 
		  }

		  return false; // propagate this keyevent
		}
	
	public void onCreateContextMenu(
			      ContextMenu menu, View v,ContextMenu.ContextMenuInfo menuInfo)
			   {
				menu.setHeaderTitle(getResources().getText(R.string.add_media));
				menu.add(0, 0, 0, getResources().getText(R.string.select_photo));
				menu.add(0, 1, 0, getResources().getText(R.string.take_photo));
			   }
	
	@Override
	public boolean onContextItemSelected(MenuItem item){
	    switch (item.getItemId()) {
	    case 0:
	    	
	    	Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        	photoPickerIntent.setType("image/*");
        	
        	startActivityForResult(photoPickerIntent, 3);
	    	
	    	return true;
		case 1:
			String state = android.os.Environment.getExternalStorageState();
            if(!state.equals(android.os.Environment.MEDIA_MOUNTED))  {
                try {
					throw new IOException("SD Card is not mounted.  It is " + state + ".");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }

        	SD_CARD_TEMP_DIR = Environment.getExternalStorageDirectory() + File.separator + "wordpress" + File.separator + "wp-" + System.currentTimeMillis() + ".jpg";
        	Intent takePictureFromCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        	takePictureFromCameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new
        	                File(SD_CARD_TEMP_DIR)));
        	
        	// make sure the directory we plan to store the recording in exists
            File directory = new File(SD_CARD_TEMP_DIR).getParentFile();
            if (!directory.exists() && !directory.mkdirs()) {
              try {
				throw new IOException("Path to file could not be created.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            }

        	startActivityForResult(takePictureFromCameraIntent, 4); 
		
		return true;
	}
	  return false;	
	}
	
}