//by Dan Roundhill, danroundhill.com/wptogo
package com.roundhill.androidWP;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.http.conn.HttpHostConnectException;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore.Images;
import android.text.Editable;
import android.text.Selection;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
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
    public boolean thumbnailOnly, secondPass, xmlrpcError = false;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
      
        setContentView(R.layout.main);	
        
        
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         accountName = extras.getString("accountName");
        }
        
        this.setTitle(accountName + " - New Post");
        
        //loads the categories from the db if they exist
        loadCategories();
        
        //clear up some variables
        selectedImageIDs.clear();
        selectedImageCtr = 0;
        
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
        
        
        
        final Button postButton = (Button) findViewById(R.id.post);
        
        postButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            
            	
            	//pd = ProgressDialog.show(newPost.this,
                 //       "Adding Post", "Please wait while attempting to add post...", true, false);
            	showDialog(ID_DIALOG_POSTING);
            		Thread t = new Thread() {
            			String resultCode = "";
        				public void run() {
							try {
								Looper.prepare();
								finalResult = submitPost();
								
								mHandler.post(mUpdateResults);
								
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

        				}
        			};
        			t.start();
            		
            }
        });
        final ImageButton refreshCategoriesButton = (ImageButton) findViewById(R.id.refreshCategoriesButton);
        
        refreshCategoriesButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	pd = ProgressDialog.show(newPost.this,
                        "Refreshing Categories", "Attempting to refresh categories from the wordpress site...", true, true);
            	Thread th = new Thread() {
    				public void run() {					
    				    finalResult = getCategories();	
    				    
    				    mHandler.post(mUpdateResults);
    				    
    				}
    			};
    			th.start();
            }
    });
        
            final Button addPictureButton = (Button) findViewById(R.id.addPictureButton);   
            
            addPictureButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                	
                	Intent photoPickerIntent = new
                	Intent(Intent.ACTION_PICK);
                	photoPickerIntent.setType("image/*");
                	
                	startActivityForResult(photoPickerIntent, 1); 
                	 
                }
        });
            
            final Button clearCategories = (Button) findViewById(R.id.clearCategories);   
            
            clearCategories.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                	 
                	TextView selectedCategoriesTV = (TextView) findViewById(R.id.selectedCategories);

                	selectedCategoriesTV.setText("Selected categories: ");
                	
                	selectedCategories.clear();
                }
        });
            
final Button boldButton = (Button) findViewById(R.id.bold);   
            
            boldButton.setOnClickListener(new Button.OnClickListener() {
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
          			  dialogBuilder.setTitle("No text selected");
                      dialogBuilder.setMessage("Please select some text first in order to bold it. You can select text by holding the shift key and scrolling.");
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

            final Button linkButton = (Button) findViewById(R.id.link);   
            
linkButton.setOnClickListener(new Button.OnClickListener() {
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
          			  dialogBuilder.setTitle("No text selected");
                      dialogBuilder.setMessage("Please select some text first in order to link it. You can select text by holding the shift key and scrolling.");
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
            
            
final Button emButton = (Button) findViewById(R.id.em);   
            
            emButton.setOnClickListener(new Button.OnClickListener() {
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
          			  dialogBuilder.setTitle("No text selected");
                      dialogBuilder.setMessage("Please select some text in order to emphasize it. You can select text by holding the shift key and scrolling.");
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
            
final Button bquoteButton = (Button) findViewById(R.id.bquote);   
            
            bquoteButton.setOnClickListener(new Button.OnClickListener() {
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
          			  dialogBuilder.setTitle("No text selected");
                      dialogBuilder.setMessage("Please select some text in order to place it in a blockquote. You can select text by holding the shift key and scrolling.");
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
            
            
final Button saveButton = (Button) findViewById(R.id.saveLocally);   
            
            saveButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                	
                	EditText titleET = (EditText)findViewById(R.id.title);
    		        String postTitle = titleET.getText().toString();
    		        EditText contentET = (EditText)findViewById(R.id.content);
    		        String postContent = contentET.getText().toString();
    		        
                	 if (postTitle.equals("") || postContent.equals(""))
     		        {
     		        	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
     					  dialogBuilder.setTitle("Missing Fields");
     		              dialogBuilder.setMessage("You must enter something in the title and content fields before saving.");
     		              dialogBuilder.setPositiveButton("OK",  new
     		            		  DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface dialog, int whichButton) {
                                   // Just close the window.
                           
                               }
                           });
     		              dialogBuilder.setCancelable(true);
     		             dialogBuilder.create().show();	
     		        }
                	 else{
                	
                	Intent i = new Intent(newPost.this, saveName.class);

                	startActivityForResult(i, 3);              	
                	 }          	
                }
        });
            
final Button clearPictureButton = (Button) findViewById(R.id.clearPicture);   
            
			clearPictureButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                	

			        imageUrl.clear();
			        thumbnailUrl.clear();
			        selectedImageIDs = new Vector();
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
	        ArrayAdapter<CharSequence> aspnCountries = new ArrayAdapter<CharSequence>(newPost.this, android.R.layout.simple_spinner_item, loadTextArray);
	        
	          aspnCountries.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	          
	          spinner.setAdapter(aspnCountries);    
    	}
    	
    }
    
    public String getCategories(){
    	
    	//gets the categories via xmlrpc call to wp blog
    	Vector res;
        String returnMessage = "";

        //check for the settings
        boolean enteredSettings = checkSettings();

        if (!enteredSettings){
        	returnMessage = "invalidSettings";
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
    
    
	public String submitPost() throws IOException {
		
		
		//grab the form data
        EditText titleET = (EditText)findViewById(R.id.title);
        String title = titleET.getText().toString();
        EditText contentET = (EditText)findViewById(R.id.content);
        String content = contentET.getText().toString();
        EditText tagsET = (EditText)findViewById(R.id.tags);
        String tags = tagsET.getText().toString();
        CheckBox publishCB = (CheckBox)findViewById(R.id.publish);
        Boolean publishThis = false;
        
        

        Integer blogID = 1; //never changes with wordpress, so far
        
        Vector<Object> myPostVector = new Vector<Object> ();
        String res = null;
        //before we do anything, validate that the user has entered settings
        boolean enteredSettings = checkSettings();
        
        if (!enteredSettings){
        	res = "invalidSettings";
        }
        else if (title.equals("") || content.equals(""))
        {
        	res = "emptyFields";
        }
        else {
        
        	//upload the images and return the HTML
        for (int it = 0; it < selectedImageCtr; it++){
                
         imageContent +=  uploadImage(selectedImageIDs.get(it).toString());

        }
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
    		theCategories[i] = selectedCategories.get(i).toString();
        }
        
      //
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
    		String sBlogName = categoriesVector.get(1).toString();
    		String sUsername = categoriesVector.get(2).toString();
    		String sPassword = categoriesVector.get(3).toString();
    		String sImagePlacement = categoriesVector.get(4).toString();
    		String sCenterThumbnailString = categoriesVector.get(5).toString();
    		String sFullSizeImageString = categoriesVector.get(6).toString();
    		

    		boolean centerThumbnail = false;
    		if (sCenterThumbnailString.equals("1")){
    			centerThumbnail = true;
    		}
    		sMaxImageWidth = categoriesVector.get(7).toString();

        
        
        if (publishCB.isChecked())
        {
        	publishThis = true;
        }
        
        Map<String, Object> contentStruct = new HashMap<String, Object>();
      
        if(imageContent != ""){
        	if (sImagePlacement.equals("Above Text")){
        		content = imageContent + content;
        	}
        	else{
        		content = content + imageContent;
        	}
        }
        
        contentStruct.put("post_type", "post");
        contentStruct.put("title", title);
        contentStruct.put("description", content);
        if (tags != ""){
        contentStruct.put("mt_keywords", tags);
        }
        if (theCategories.length > 0){
        contentStruct.put("categories", theCategories);
        }
        

        
        client = new XMLRPCClient(sURL);
        
        Object[] params = {
        		1,
        		sUsername,
        		sPassword,
        		contentStruct,
        		publishThis
        };
        
        Object result = null;
        try {
			result = (Object) client.call("metaWeblog.newPost", params);
		} catch (XMLRPCException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

				newID = result.toString();
				res = "OK";
			

        }// if/then for valid settings
        
		return res;
	}

	public String uploadImage(String imageURL){
        
        int finalHeight = 0;
        
        //images variables

        
        //get the settings
        settingsDB settingsDB = new settingsDB(newPost.this);
    	Vector categoriesVector = settingsDB.loadSettings(newPost.this, id);   	
    	
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
    		sImagePlacement = categoriesVector.get(4).toString();
    		String sCenterThumbnailString = categoriesVector.get(5).toString();
    		
    		//removed this as a quick fix to get rid of full size upload option
    		/*if (sFullSizeImageString.equals("1")){
    			sFullSizeImage = true;
    		}*/  

    		
    		if (sCenterThumbnailString.equals("1")){
    			centerThumbnail = true;
    		}
    		sMaxImageWidth = categoriesVector.get(7).toString();

        //new loop for multiple images
        
        

        //check for image, and upload it

           client = new XMLRPCClient(sURL);

     	   String curImagePath = "";
     	   
     	   
     		curImagePath = imageURL;
 
     	   Uri imageUri = Uri.parse(curImagePath);
     	   
     	   String imgID = imageUri.getLastPathSegment();
     	   long imgID2 = Long.parseLong(imgID);
     	   
     	  String[] projection; 

     	  projection = new String[] {
           		    Images.Media._ID,
           		    Images.Media.DATA
           		};
     	  
     	  
     	   Uri imgPath;

     	   imgPath = ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, imgID2);
     	   
     	   
     	   
		Cursor cur = managedQuery(imgPath, projection, null, null, null);
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
        
        int width = opts.outWidth;
        int height = opts.outHeight; 
        
        int finalWidth = 500;  //default to this if there's a problem
        //Change dimensions of thumbnail
        
        byte[] finalBytes;
        
        if (sMaxImageWidth.equals("Original Size")){
        	
        	if (bytes.length > 1000000) //it's a biggie! don't want out of memory crash
        	{
        		float finWidth = 1000;
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
        		
        		float percentage = (float) finalWidth / width;
        		float proportionateHeight = height * percentage;
        		finalHeight = (int) Math.rint(proportionateHeight);
        	
                bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
                
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();  
                bm.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                
                bm.recycle(); //free up memory
                
                finalBytes = baos.toByteArray();
        	}
        	else
        	{
        	finalBytes = bytes;
        	}   	
        	
        }
        else
        {
           	finalWidth = Integer.parseInt(sMaxImageWidth);
        	if (finalWidth > width){
        		//don't resize
        		finalBytes = bytes;
        	}
        	else
        	{
        		float finWidth = finalWidth;
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
        		
        		float percentage = (float) finalWidth / width;
        		float proportionateHeight = height * percentage;
        		finalHeight = (int) Math.rint(proportionateHeight);
        	
                bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
                
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();  
                bm.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                
                bm.recycle(); //free up memory
                
                finalBytes = baos.toByteArray();
        	}
        	
            
        }

            //try and upload the freakin' image
            //imageRes = service.ping(sURL + "/xmlrpc.php", sXmlRpcMethod, myPictureVector);
            String contentType = "image/jpg";
            Map<String, Object> m = new HashMap<String, Object>();

            HashMap hPost = new HashMap();

            	
            m.put("name", imageTitle);
            m.put("type", contentType);
            m.put("bits", finalBytes);
            m.put("overwrite", true);

			client = new XMLRPCClient(sURL);
        	
        	XMLRPCMethodImages method = new XMLRPCMethodImages("wp.uploadFile", new XMLRPCMethodCallback() {
				public void callFinished(Object result) {
					
					imgHTML = ""; //start fresh
					//Looper.myLooper().quit();
					HashMap contentHash = new HashMap();
					    
					contentHash = (HashMap) result;

					String resultURL = contentHash.get("url").toString();
					
					String finalImageUrl = "";
					

		            finalImageUrl = resultURL;
					
					//prepare the centering css if desired from user
			           String centerCSS = " ";
			           if (centerThumbnail){
			        	   centerCSS = "style=\"display:block;margin-right:auto;margin-left:auto;\" ";
			           }
			           
			     	   
				           if (resultURL != null)
				           {

				   	        	if (sImagePlacement.equals("Above Text")){
				   	        		
				   	        		imgHTML +=  "<img " + centerCSS + "alt=\"image\" src=\"" + finalImageUrl + "\" /><br /><br />";
				   	        	}
				   	        	else{
				   	        		imgHTML +=  "<br /><img " + centerCSS + "alt=\"image\" src=\"" + finalImageUrl + "\" />";
				   	        	}        		
				           	
				           		
				           }
					
					
			           
				}
	        });
        	
        	Object[] params = {
	        		1,
	        		sUsername,
	        		sPassword,
	        		m
	        };
        	
        	try {
				method.call(params);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
					        //titles[ctr] = contentHash.get("content").toString().substring(contentHash.get("content").toString().indexOf("<title>") + 7, contentHash.get("content").toString().indexOf("</title>"));
					       // postIDs[ctr] = contentHash.get("postid").toString();
        	
        	
     	  
     	   


        
		
        return imgHTML;
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
						  dialogBuilder.setTitle("Connection Error");
			              dialogBuilder.setMessage(e.getFaultString());
			              dialogBuilder.setPositiveButton("Ok",  new
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
							//status.setText("Cannot connect to " + uri.getHost() + "\nMake sure server.py on your development host is running !!!");
						} else {
							//pd.dismiss();
							AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
							  dialogBuilder.setTitle("Connection Error");
				              dialogBuilder.setMessage(e.getMessage() + e.getLocalizedMessage());
				              dialogBuilder.setPositiveButton("Ok",  new
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
						  dialogBuilder.setTitle("Connection Error");
			              dialogBuilder.setMessage(e.getFaultString());
			              dialogBuilder.setPositiveButton("Ok",  new
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
							//status.setText("Cannot connect to " + uri.getHost() + "\nMake sure server.py on your development host is running !!!");
						} else {
							//pd.dismiss();
							AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
							  dialogBuilder.setTitle("Connection Error");
				              dialogBuilder.setMessage(e.getMessage() + e.getLocalizedMessage());
				              dialogBuilder.setPositiveButton("Ok",  new
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
		if (data != null)
		{

		Bundle extras = data.getExtras();

		switch(requestCode) {
		case 0:
		    String title = extras.getString("returnStatus");
		    //Toast.makeText(wpAndroid.this, title, Toast.LENGTH_SHORT).show();
		    break;
		case 1:
		    Uri imagePath = data.getData();   
		    String imgPath2 = imagePath.getEncodedPath();
		   
	           
	           //for gridview
	           selectedImageIDs.add(selectedImageCtr, imagePath);
	           //for submission
	           imageUrl.add(selectedImageCtr, imgPath2);
	           //thumbnailUrl.add(selectedImageCtr, Images.Thumbnails.EXTERNAL_CONTENT_URI.toString() + "/" + thumbIdString);
	           //new
	           //thumbnailUrl.add(selectedImageCtr, thumbPath);
	           selectedImageCtr++;
	           //thumbData = cur.getString(dataColumn);
	     	 // }
	     	  
	     	 GridView gridview = (GridView) findViewById(R.id.gridView);
	     	 gridview.setAdapter(new ImageAdapter(this));

		    
		    break;
		case 2:
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
			String saveName = extras.getString("saveName");
			
			if (saveName.equals("CANCEL") != true && saveName.equals("") != true && saveName.equals(null) != true){
				
		        EditText titleET = (EditText)findViewById(R.id.title);
		        String postTitle = titleET.getText().toString();
		        EditText contentET = (EditText)findViewById(R.id.content);
		        String postContent = contentET.getText().toString();

		        TextView categoriesTV = (TextView)findViewById(R.id.selectedCategories);
			    String categoriesValue = categoriesTV.getText().toString();
		        	
		        CheckBox publishCB = (CheckBox)findViewById(R.id.publish);
		        boolean publish = publishCB.isChecked();
			        
                savedPostsDB postsDB = new savedPostsDB(newPost.this);
                boolean savePostResult = postsDB.savePost(this, saveName, id, postTitle, postContent, categoriesValue, publish);	        	
		        if (savePostResult == false){
		        	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
					  dialogBuilder.setTitle("Post Not Saved");
		              dialogBuilder.setMessage("Duplicate post save names were found. Please save your posts with unique names.");
		              dialogBuilder.setPositiveButton("OK",  new
		            		  DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //Just close the window

                        }
                    });
		              dialogBuilder.setCancelable(true);
		             dialogBuilder.create().show();
		        }
					
			}
			break;
		case 4:
			String selectedPostID = extras.getString("selectedSaveName");
			
				
				if (selectedPostID.equals("noPostsFound") != true && selectedPostID.equals("CANCEL") != true){
					savedPostsDB postsDB2 = new savedPostsDB(newPost.this);
					Vector postFields = postsDB2.loadPost(this, selectedPostID, id);
					EditText titleET = (EditText)findViewById(R.id.title);
			        EditText contentET = (EditText)findViewById(R.id.content);
			        TextView categoriesTV = (TextView)findViewById(R.id.selectedCategories);
			        	
			        CheckBox publishCB = (CheckBox)findViewById(R.id.publish);
			        
			        titleET.setText(postFields.get(0).toString());
			        contentET.setText(postFields.get(1).toString());
			        if (postFields.get(2) != null)
			        {
			        	categoriesTV.setText(postFields.get(2).toString());
			        }
			        else
			        {
			        	categoriesTV.setText("Selected categories: ");
			        }
			        
			        boolean publish  = false;
			        if (postFields.get(3) != null)
			        {
		    		if (postFields.get(3).equals("1")){
		    			publish = true;
		    		}
			        }
			        publishCB.setChecked(publish);
	
				    	//get rid of the thumbnail
			        
			        selectedImageIDs = new Vector();
   
				}
				else if (selectedPostID.equals("CANCEL"))
				{
					//don't do anything
				}
				else
				{
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
					  dialogBuilder.setTitle("No Posts Found");
		              dialogBuilder.setMessage("No saved posts were found.");
		              dialogBuilder.setPositiveButton("OK",  new
		            		  DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog, int whichButton) {
                              //Just close the window

                          }
                      });
		              dialogBuilder.setCancelable(true);
		             dialogBuilder.create().show();
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
		        ArrayAdapter<CharSequence> categories = new ArrayAdapter<CharSequence>(newPost.this, android.R.layout.simple_spinner_item, textArray);
		        
		        textArray = new ArrayList<CharSequence>();
		        
		          categories.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		         

		          spinner.setAdapter(categories);
		          if (pd.isShowing()){
						pd.dismiss();
						}
		         
				Toast.makeText(newPost.this, "Categories refreshed!", Toast.LENGTH_SHORT).show();
			}
			else if (finalResult.equals("categoryFault")){
				if (pd.isShowing()){
					pd.dismiss();
					}	
				
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
							  dialogBuilder.setTitle("Category Refresh Error");
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
			else if (finalResult.equals("invalidSettings")){
				/*if (pd.isShowing()){
					pd.dismiss();
					}*/
				dismissDialog(newPost.this.ID_DIALOG_POSTING);
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
							  dialogBuilder.setTitle("Settings not found!");
				              dialogBuilder.setMessage("Some required settings were not found.  Load settings now?");
				              dialogBuilder.setPositiveButton("Yes",  new
				            		  DialogInterface.OnClickListener() {
		                            public void onClick(DialogInterface dialog, int whichButton) {
		                                // User clicked Yes so delete the contexts.
		                            	Intent i = new Intent(newPost.this, settings.class);

		                            	startActivityForResult(i, 0);
		                        
		                            }
		                        });
				              dialogBuilder.setNegativeButton("No", new
				            		  DialogInterface.OnClickListener() {
		                            public void onClick(DialogInterface dialog, int whichButton) {
		                                // User clicked No so don't delete (do nothing).
		                            }
		                        });
				              dialogBuilder.setCancelable(true);
				             dialogBuilder.create().show();
			
			}
			else if (finalResult.equals("emptyFields")){
				/*if (pd.isShowing()){
					pd.dismiss();
					}*/
				dismissDialog(newPost.this.ID_DIALOG_POSTING);
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
							  dialogBuilder.setTitle("Empty Fields");
				              dialogBuilder.setMessage("Title and Post are required fields. Please enter something to your post before submitting!");
				              dialogBuilder.setPositiveButton("OK",  new
				            		  DialogInterface.OnClickListener() {
		                            public void onClick(DialogInterface dialog, int whichButton) {
		                                //Just close the window

		                        
		                            }
		                        });
				              dialogBuilder.setCancelable(true);
				             dialogBuilder.create().show();
			
			}
			else if (finalResult.equals("OK"))
			{
				/*if (pd.isShowing()){
					pd.dismiss();
					}*/
				dismissDialog(newPost.this.ID_DIALOG_POSTING);
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
				  dialogBuilder.setTitle("Post Added");
				  if (xmlrpcError){
					  dialogBuilder.setMessage("Post ID# " + newID + " added successfully, but an error was encountered when uploading images");  
				  }
				  else{
	              dialogBuilder.setMessage("Post ID# " + newID + " added successfully");
				  }
	              dialogBuilder.setPositiveButton("OK",  new
	            		  DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int whichButton) {
                          finish();
                      }
                  });
	              dialogBuilder.setCancelable(true);
	             dialogBuilder.create().show();
			}
		}
	};
	
	//Add settings to menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);
	    menu.add(0, 0, 0, "Load Post");
	    MenuItem menuItem1 = menu.findItem(0);
	    menuItem1.setIcon(R.drawable.ic_menu_preferences);
	    
	    return true;
	}
	
	//Menu actions
	@Override
	public boolean onOptionsItemSelected(final MenuItem item){
	    switch (item.getItemId()) {
	    case 0:
	    	
	    	Bundle savedPosts = new Bundle();
	    	savedPosts.putString("id", id);
	    	Intent in = new Intent(this, selectpost.class);
	    	in.putExtras(savedPosts);
        	startActivityForResult(in, 4);
        	
	    	
	    	return true;
	}
	  return false;	
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
	if(id == ID_DIALOG_POSTING){
	ProgressDialog loadingDialog = new ProgressDialog(this);
	loadingDialog.setMessage("Attempting to add post...");
	loadingDialog.setIndeterminate(true);
	loadingDialog.setCancelable(true);
	return loadingDialog;
	}

	return super.onCreateDialog(id);
	}
	
}





