package org.wordpress.android;

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
import org.wordpress.android.newPost.ImageAdapter;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class editPost extends Activity {
    /** Called when the activity is first created. */
	public static long globalData = 0;
	public ProgressDialog pd;
	public boolean postStatus = false;
	String[] mFiles=null;
	public String thumbnailPath = null;
	public String imagePath = null;
	public String imageTitle = null;
	public Vector imageUrl = new Vector();
	public Vector thumbnailUrl = new Vector();
	private final Handler mHandler = new Handler();
	public String finalResult = null;
	Vector<String> selectedCategories = new Vector();
	public ArrayList<CharSequence> textArray = new ArrayList<CharSequence>();
	public ArrayList<CharSequence> loadTextArray = new ArrayList<CharSequence>();
	public Boolean newStart = true;
	public String categoryErrorMsg = "";
	private XMLRPCClient client;
	public String id = "";
	private Vector<Uri> selectedImageIDs = new Vector();
	private int selectedImageCtr = 0;
	private Vector imageURLs = new Vector();
    private String accountName = "";
    private String postID = "";
    private boolean localDraft = false;
    private int ID_DIALOG_POSTING = 1;
    public String newID, imgHTML, sMaxImageWidth, sImagePlacement;
    public Boolean centerThumbnail, xmlrpcError = false, isPage = false;
    public String SD_CARD_TEMP_DIR = "", categories = "";
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
      
        setContentView(R.layout.edit);	
        
        
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         accountName = extras.getString("accountName");
         postID = extras.getString("postID");
         localDraft = extras.getBoolean("localDraft", false); 
         isPage = extras.getBoolean("isPage", false);
        }
        
        if (isPage){  
        	setContentView(R.layout.edit_page);
        }
        else{
        	setContentView(R.layout.edit);
        }
        
        this.setTitle(accountName + " - " + getResources().getText((isPage) ? R.string.edit_page : R.string.edit_post));
        
        if (localDraft){
        	localDraftsDB lDraftsDB = new localDraftsDB(this);
        	Vector post;
        	if (isPage){
        		post = lDraftsDB.loadPageDraft(this, postID);
        	}
        	else{
        		post = lDraftsDB.loadPost(this, postID);
        	}
        	
        	HashMap postHashMap = (HashMap) post.get(0);
        	
        	EditText titleET = (EditText)findViewById(R.id.title);
        	EditText contentET = (EditText)findViewById(R.id.content);
        	
        	titleET.setText(postHashMap.get("title").toString());
        	contentET.setText(postHashMap.get("content").toString());
        	
        	String picturePaths = postHashMap.get("picturePaths").toString();
        	if (!picturePaths.equals("")){
        		String[] pPaths = picturePaths.split(",");
        		
        		for (int i = 0; i < pPaths.length; i++)
        		{
        			Uri imagePath = Uri.parse(pPaths[i]); 
        			selectedImageIDs.add(selectedImageCtr, imagePath);
        	        imageUrl.add(selectedImageCtr, pPaths[i]);
        	        selectedImageCtr++;
        	     	  
        	     	GridView gridview = (GridView) findViewById(R.id.gridView);
        	     	gridview.setAdapter(new ImageAdapter(this));
        		}
        		
        	}
        	
        	if (!isPage){
        		
		    	categories = postHashMap.get("categories").toString();
		    	if (!categories.equals("")){
		    		
		    		String[] aCategories = categories.split(",");
		    		
		    		for (int i=0; i < aCategories.length; i++)
		    		{
		    			selectedCategories.add(aCategories[i]);
		    		}
		    		
		    		TextView tvCategories = (TextView) findViewById(R.id.selectedCategories);
		    		tvCategories.setText("Selected categories: " + categories);
		    		
		    	}
		    	
		    	Button selectCategories = (Button) findViewById(R.id.selectCategories);   
    	        
    	        selectCategories.setOnClickListener(new Button.OnClickListener() {
    	            public void onClick(View v) {
    	            	 
    	            	Bundle bundle = new Bundle();
    					bundle.putString("id", id);
    					if (categories != ""){
    					bundle.putString("categoriesCSV", categories);
    					}
    			    	Intent i = new Intent(editPost.this, selectCategories.class);
    			    	i.putExtras(bundle);
    			    	startActivityForResult(i, 5);
    	            }
    	        });
		    	
		    	String tags = postHashMap.get("tags").toString();
		    	if (!tags.equals("")){
		    		EditText tagsET = (EditText) findViewById(R.id.tags);
		    		tagsET.setText(tags);
		    	}
        	}
        	
        	int publish = Integer.valueOf(postHashMap.get("publish").toString());
        	
        	CheckBox publishCB = (CheckBox) findViewById(R.id.publish);
        	if (publish == 1){
        		publishCB.setChecked(true);
        	}
        	
        	
        	
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
        
    	client = new XMLRPCClient(sURL);
    	
    	EditText titleET = (EditText)findViewById(R.id.title);
    	String setTitle = titleET.getText().toString();
    	if (setTitle.equals("")){
    	
    	pd = ProgressDialog.show(editPost.this,
    			getResources().getText((isPage) ? R.string.getting_page : R.string.getting_post), getResources().getText((isPage) ? R.string.please_wait_getting_page : R.string.please_wait_getting_post), true, false);
    	
    	XMLRPCMethod method = new XMLRPCMethod("metaWeblog.getPost", new XMLRPCMethodCallback() {
			public void callFinished(Object result) {
				String s = "done";
				s = result.toString();
				pd.dismiss();
				
				if (result == null){
					//prompt that something went wrong
				}
				else{
					HashMap contentHash = (HashMap) result;
					
					EditText titleET = (EditText)findViewById(R.id.title);
			        titleET.setText(escapeUtils.unescapeHtml(contentHash.get("title").toString()));
			        EditText contentET = (EditText)findViewById(R.id.content);
			        if (contentHash.get("mt_text_more").toString() != ""){
			        	contentET.setText(escapeUtils.unescapeHtml(contentHash.get("description").toString() + "<!--more-->\n" + contentHash.get("mt_text_more").toString()));
			        }
			        else{
			        	contentET.setText(escapeUtils.unescapeHtml(contentHash.get("description").toString()));
			        }
			      
			        
			        String status = contentHash.get("post_status").toString();
			        
			        if (!isPage){
				        EditText tagsET = (EditText)findViewById(R.id.tags);
				        tagsET.setText(escapeUtils.unescapeHtml(contentHash.get("mt_keywords").toString()));
				        TextView categoriesTV = (TextView)findViewById(R.id.selectedCategories);
			        
			        
				        Object categoriesArray[] = (Object[]) contentHash.get("categories");
				        
				        if (categoriesArray != null){
				        	int ctr = 0;
				        	categories = "";
						    for (Object item : categoriesArray){
						        String category = categoriesArray[ctr].toString();
						        if (!selectedCategories.contains(category))
			                	{
						        categories += category + ",";
			                	selectedCategories.add(category);
			                	}
						        ctr++;					    
				        }	
						    categories = categories.trim();
			            	if (categories.endsWith(",")){
			            		categories = categories.substring(0, categories.length() - 1);
			            	}
			            	if (categories != ""){
			            	categoriesTV.setText(getResources().getText(R.string.selected_categories) + " " + categories);
			            	}
				        }
				        
				        Button selectCategories = (Button) findViewById(R.id.selectCategories);   
		    	        
		    	        selectCategories.setOnClickListener(new Button.OnClickListener() {
		    	            public void onClick(View v) {
		    	            	 
		    	            	Bundle bundle = new Bundle();
		    					bundle.putString("id", id);
		    					if (categories != ""){
		    					bundle.putString("categoriesCSV", categories);
		    					}
		    			    	Intent i = new Intent(editPost.this, selectCategories.class);
		    			    	i.putExtras(bundle);
		    			    	startActivityForResult(i, 5);
		    	            }
		    	        });
			        }
			        
			        CheckBox publishCB = (CheckBox)findViewById(R.id.publish);
			        if (status.equals("publish")){
			        	publishCB.setChecked(true);
			        }
			        else{
			        	publishCB.setChecked(false);
			        }
			        
				}
			}
        });
        Object[] params = {
        		postID,
        		sUsername,
        		sPassword,
        };
        
        
        method.call(params);
    	
    	}
    	
        } 
        
        
        
        final Button postButton = (Button) findViewById(R.id.post);
        
        postButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            
            if(localDraft){
            	boolean result = savePost();
            	
            	if (result){
            		Bundle bundle = new Bundle();                   
                    bundle.putString("returnStatus", "OK");
                    Intent mIntent = new Intent();
                    mIntent.putExtras(bundle);
                    setResult(RESULT_OK, mIntent);
                    finish(); 
            	}
            }
            else{
            	
            	showDialog(ID_DIALOG_POSTING);
            		Thread t = new Thread() {
            			String resultCode = "";
        				public void run() {
							try {
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
            		
            }
        });
        
            final Button addPictureButton = (Button) findViewById(R.id.addPictureButton);   
            
            registerForContextMenu(addPictureButton);
            
            addPictureButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                	
                	addPictureButton.performLongClick();
                	 
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
                		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
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
                		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
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
                		Intent i = new Intent(editPost.this, link.class);

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
                		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
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
                		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
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
            
            
final Button cancelButton = (Button) findViewById(R.id.cancel);   
            
            cancelButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                	
                	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
      			  dialogBuilder.setTitle(getResources().getText(R.string.cancel_edit));
                    dialogBuilder.setMessage(getResources().getText((isPage) ? R.string.sure_to_cancel_edit_page : R.string.sure_to_cancel_edit));
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
    
    
	public String submitPost() throws IOException {
		
		
		//grab the form data
        EditText titleET = (EditText)findViewById(R.id.title);
        String title = titleET.getText().toString();
        EditText contentET = (EditText)findViewById(R.id.content);
        String content = contentET.getText().toString();
        CheckBox publishCB = (CheckBox)findViewById(R.id.publish);
        Boolean publishThis = false;
        String imageContent = "";
        //upload the images and return the HTML
        imageContent =  uploadImages();

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
    		String sImagePlacement = categoriesVector.get(4).toString();
    		String sCenterThumbnailString = categoriesVector.get(5).toString();
    		String sFullSizeImageString = categoriesVector.get(6).toString();
    		boolean sFullSizeImage  = false;
    		if (sFullSizeImageString.equals("1")){
    			sFullSizeImage = true;
    		}

    		boolean centerThumbnail = false;
    		if (sCenterThumbnailString.equals("1")){
    			centerThumbnail = true;
    		}

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
        contentStruct.put("title", escapeUtils.escapeHtml(title));
        contentStruct.put("description", escapeUtils.escapeHtml(content));
        if (!isPage){
        	EditText tagsET = (EditText)findViewById(R.id.tags);
            String tags = tagsET.getText().toString();
            // categoryID = getCategoryId(selectedCategory);
            String[] theCategories = categories.split(",");
        if (tags != ""){
        contentStruct.put("mt_keywords", escapeUtils.escapeHtml(tags));
        }
        if (theCategories.length > 0){
        contentStruct.put("categories", theCategories);
        }
        }
        

        
        client = new XMLRPCClient(sURL);
        
        Object[] params = {
        		postID,
        		sUsername,
        		sPassword,
        		contentStruct,
        		publishThis
        };
        
        Object result = null;
        try {
			result = (Object) client.call("metaWeblog.editPost", params);
		} catch (XMLRPCException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

				newID = result.toString();
				res = "OK";
			

        }// if/then for valid settings
        
		return res;
	}

	public String uploadImages(){
		
	    Vector<Object> myPictureVector = new Vector<Object> ();
	    String returnedImageURL = null;
	    String imageRes = null;
	    String content = "";
	    int thumbWidth = 0, thumbHeight = 0, finalHeight = 0;
	    
	    //images variables
	    String finalThumbnailUrl = null;
	    String finalImageUrl = null;
	    String uploadImagePath = "";
	    
	    //get the settings
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
			boolean sFullSizeImage  = false;
			if (sFullSizeImageString.equals("1")){
				sFullSizeImage = true;
			}

			boolean centerThumbnail = false;
			if (sCenterThumbnailString.equals("1")){
				centerThumbnail = true;
			}
			String sMaxImageWidth = categoriesVector.get(7).toString();
			
			String thumbnailURL = "";
	    //new loop for multiple images
	    
	    for (int it = 0; it < selectedImageCtr; it++){

	    //check for image, and upload it
	    if (imageUrl.get(it) != null)
	    {
	       client = new XMLRPCClient(sURL);
	 	   
	 	   String sXmlRpcMethod = "wp.uploadFile";
	 	   String curImagePath = "";
	 	   
	 	   for (int i = 0; i < 2; i++){
	 		   

	 		 curImagePath = imageUrl.get(it).toString();
	 		   
	 		if (i == 0 || sFullSizeImage)
	 		{
	 	   
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

		Cursor cur = this.managedQuery(imgPath, projection, null, null, null);
	 	  String thumbData = "";
	 	 
	 	  if (cur.moveToFirst()) {
	 		  
	 		int nameColumn, dataColumn, heightColumn, widthColumn;
	 			nameColumn = cur.getColumnIndex(Images.Media._ID);
	 	        dataColumn = cur.getColumnIndex(Images.Media.DATA);

	       String imgPath4 = imgPath.getEncodedPath();              	            
	       
	       thumbData = cur.getString(dataColumn);

	 	  }
	 	   
	 	   File jpeg = new File(thumbData);
	 	   
	 	   imageTitle = jpeg.getName();
	 	  
	 	   byte[] bytes = new byte[(int) jpeg.length()];
	 	   byte[] finalBytes;
	 	   
	 	   DataInputStream in = null;
		try {
			in = new DataInputStream(new FileInputStream(jpeg));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	 	   try {
			in.readFully(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	 	   try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (i == 0){
			  finalBytes = imageHelper.createThumbnail(bytes, sMaxImageWidth);
		   }
		   else{
			  finalBytes = bytes;
		   }
	 	   	
	        //try to upload the image
	        String contentType = "image/jpg";
	        Map<String, Object> m = new HashMap<String, Object>();

	        HashMap hPost = new HashMap();
	        m.put("name", imageTitle);
	        m.put("type", contentType);
	        m.put("bits", finalBytes);
	        m.put("overwrite", true);
	        
	        Object[] params = {
	        		1,
	        		sUsername,
	        		sPassword,
	        		m
	        };
	        
	        Object result = null;
	        
	        try {
				result = (Object) client.call("wp.uploadFile", params);
			} catch (XMLRPCException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				e.getMessage();
				xmlrpcError = true;
				break;
			}
					
					HashMap contentHash = new HashMap();
					    
					contentHash = (HashMap) result;

					String resultURL = contentHash.get("url").toString();
					
					if (i == 0){
		            	finalThumbnailUrl = resultURL;
		            }
		            else{
		            	if (sFullSizeImage){
		            	finalImageUrl = resultURL;
		            	}
		            	else
		            	{
		            		finalImageUrl = "";
		            	}
		            }

		           int finalWidth = 500;  //default to this if there's a problem
		           //Change dimensions of thumbnail
		           if (sMaxImageWidth.equals("Original Size")){
		           	finalWidth = thumbWidth;
		           	finalHeight = thumbHeight;
		           }
		           else
		           {
		              	finalWidth = Integer.parseInt(sMaxImageWidth);
		           	if (finalWidth > thumbWidth){
		           		//don't resize
		           		finalWidth = thumbWidth;
		           		finalHeight = thumbHeight;
		           	}
		           	else
		           	{
		           		float percentage = (float) finalWidth / thumbWidth;
		           		float proportionateHeight = thumbHeight * percentage;
		           		finalHeight = (int) Math.rint(proportionateHeight);
		           	}
		           }
					
					
					//prepare the centering css if desired from user
			           String centerCSS = " ";
			           if (centerThumbnail){
			        	   centerCSS = "style=\"display:block;margin-right:auto;margin-left:auto;\" ";
			           }
			           
			     	   
			           if (i != 0 && sFullSizeImage)
			           {
				           if (resultURL != null)
				           {

				   	        	if (sImagePlacement.equals("Above Text")){
				   	        		
				   	        		content = content + "<a alt=\"image\" href=\"" + finalImageUrl + "\"><img " + centerCSS + "alt=\"image\" src=\"" + finalThumbnailUrl + "\" /></a><br /><br />";
				   	        	}
				   	        	else{
				   	        		content = content + "<br /><a alt=\"image\" href=\"" + finalImageUrl + "\"><img " + centerCSS + "alt=\"image\" src=\"" + finalThumbnailUrl + "\" /></a>";
				   	        	}        		
				           	
				           		
				           }
			           }
			           else{
				           if (i == 0 && sFullSizeImage == false && resultURL != null)
				           {

				   	        	if (sImagePlacement.equals("Above Text")){
				   	        		
				   	        		content = content + "<img " + centerCSS + "alt=\"image\" src=\"" + finalThumbnailUrl + "\" /><br /><br />";
				   	        	}
				   	        	else{
				   	        		content = content + "<br /><img " + centerCSS + "alt=\"image\" src=\"" + finalThumbnailUrl + "\" />";
				   	        	}        		
				           	
				           		
				           }
			           }
	                
	 	   }  //end if statement
	 	   
	       
	       
	 	  }//end image check
	 	   
	    }//end image stuff
	    }//end new for loop

	    return content;
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
				e.printStackTrace();
				if (pd.isShowing()){
					pd.dismiss();
				}
				else{
				dismissDialog(editPost.this.ID_DIALOG_POSTING);
				}
						final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
						  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
			              dialogBuilder.setMessage(e.getFaultString());
			              dialogBuilder.setPositiveButton("OK",  new
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
			              dialogBuilder.setCancelable(true);
			              Thread action = new Thread() 
							{ 
							  public void run() 
							  {
								  dialogBuilder.create().show();
							  } 
							}; 
							runOnUiThread(action);
			             
			             
					
			} catch (final XMLRPCException e) {
				
				handler.post(new Runnable() {
					public void run() {
						
						Throwable couse = e.getCause();
						if (couse instanceof HttpHostConnectException) {
							if (pd.isShowing()){
								pd.dismiss();
							}
							else{
							dismissDialog(editPost.this.ID_DIALOG_POSTING);
							}

						} else {
							if (pd.isShowing()){
								pd.dismiss();
							}
							else{
							dismissDialog(editPost.this.ID_DIALOG_POSTING);
							}
							final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
							  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
				              dialogBuilder.setMessage(e.getMessage() + e.getLocalizedMessage());
				              dialogBuilder.setPositiveButton("OK",  new
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
				              dialogBuilder.setCancelable(true);
				              Thread action = new Thread() 
								{ 
								  public void run() 
								  {
									  dialogBuilder.create().show();
								  } 
								}; 
								runOnUiThread(action);
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
			
		}
		public void call() throws InterruptedException {
			call(null);
		}
		public void call(Object[] params) throws InterruptedException {		
			this.params = params;
			this.method = method;
			final Object result;
			try {
				result = (Object) client.call(method, params);
				callBack.callFinished(result);
			} catch (XMLRPCException e) {

				xmlrpcError = true;
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
						dismissDialog(editPost.this.ID_DIALOG_POSTING);
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
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
							dismissDialog(editPost.this.ID_DIALOG_POSTING);
							//status.setText("Cannot connect to " + uri.getHost() + "\nMake sure server.py on your development host is running !!!");
						} else {
							//pd.dismiss();
							dismissDialog(editPost.this.ID_DIALOG_POSTING);
							AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
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
		case 5:
			 
			extras = data.getExtras();
			String cats = extras.getString("selectedCategories");
			categories = cats;
			TextView selectedCategoriesTV = (TextView) findViewById(R.id.selectedCategories);
			selectedCategoriesTV.setText(getResources().getText(R.string.selected_categories) + " " + cats);
           
	     	break;
		}
		
	}//end null check
	}
	
	final Runnable mUpdateResults = new Runnable() {
		public void run() {
			if (finalResult.equals("invalidSettings")){
				dismissDialog(editPost.this.ID_DIALOG_POSTING);			
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
							  dialogBuilder.setTitle(getResources().getText(R.string.settings_not_found));
				              dialogBuilder.setMessage(getResources().getText(R.string.settings_not_found_load_now));
				              dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),  new
				            		  DialogInterface.OnClickListener() {
		                            public void onClick(DialogInterface dialog, int whichButton) {
		                                // User clicked Yes so delete the contexts.
		                            	Intent i = new Intent(editPost.this, settings.class);

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
			else if (finalResult.equals("emptyFields")){
				dismissDialog(editPost.this.ID_DIALOG_POSTING);				
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
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
			else if (finalResult.equals("OK"))
			{
				dismissDialog(editPost.this.ID_DIALOG_POSTING);	
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
				  dialogBuilder.setTitle(getResources().getText(R.string.post_edited));
				  if (xmlrpcError){
					  dialogBuilder.setMessage(getResources().getText((isPage) ? R.string.page_edited_image_error : R.string.post_edited_image_error));  
				  }
				  else{
	              dialogBuilder.setMessage(getResources().getText((isPage) ? R.string.page_edited_successfully : R.string.post_edited_successfully));
				  }
	              dialogBuilder.setPositiveButton("OK",  new
	            		  DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int whichButton) {
                    	  Bundle bundle = new Bundle();
                          
                          bundle.putString("returnStatus", "OK");
                          Intent mIntent = new Intent();
                          mIntent.putExtras(bundle);
                          setResult(RESULT_OK, mIntent);
                          finish(); 
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
	loadingDialog.setMessage(getResources().getText((isPage) ? R.string.attempting_edit_page : R.string.attempting_edit_post));
	loadingDialog.setIndeterminate(true);
	loadingDialog.setCancelable(true);
	return loadingDialog;
	}

	return super.onCreateDialog(id);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		
		super.onConfigurationChanged(newConfig); 
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
        boolean success = false;
        
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
        
        	//update the images
        	for (int it = 0; it < selectedImageCtr; it++){
           
        		images += selectedImageIDs.get(it).toString() + ",";

        	}
        	if (!isPage){
	        	/*String selectedCategory = "";

	        	// categoryID = getCategoryId(selectedCategory);
	        	String[] theCategories = new String[selectedCategories.size()];
	        
	        	int catSize = selectedCategories.size();
	        
	        	for(int i=0; i < selectedCategories.size(); i++)
	        	{
	        		categories += selectedCategories.get(i).toString() + ",";
	        		//theCategories[i] = selectedCategories.get(i).toString();
	        	}*/
	        	
        	}
        
        	if (publishCB.isChecked())
        	{
        		publishThis = true;
        	}
        
        	//new feature, automatically save a post as a draft just in case the posting fails
        	localDraftsDB lDraftsDB = new localDraftsDB(this);
        	if (isPage){
        		success = lDraftsDB.updateLocalPageDraft(this, id, postID, title, content, images, publishThis);
        	}
        	else{
        		success = lDraftsDB.updateLocalDraft(this, id, postID, title, content, images, tags, categories, publishThis);
        	}
        
        
        }// if/then for valid settings
        
		return success;
	}
	
	@Override public boolean onKeyDown(int i, KeyEvent event) {

		  // only intercept back button press
		  if (i == KeyEvent.KEYCODE_BACK) {
			  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
			  dialogBuilder.setTitle(getResources().getText(R.string.cancel_edit));
              dialogBuilder.setMessage(getResources().getText((isPage) ? R.string.sure_to_cancel_edit_page : R.string.sure_to_cancel_edit));
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





