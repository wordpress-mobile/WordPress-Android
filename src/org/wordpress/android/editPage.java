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
import android.os.Handler;
import android.provider.MediaStore.Images;
import android.text.Editable;
import android.text.Selection;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class editPage extends Activity {
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
    public Boolean centerThumbnail, xmlrpcError = false;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
      
        setContentView(R.layout.editpage);	
        
        
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         accountName = extras.getString("accountName");
         postID = extras.getString("postID");
         localDraft = extras.getBoolean("localDraft", false);
        }
        
        this.setTitle(accountName + " - " + getResources().getText(R.string.edit_page));
        
        if (localDraft){
        	localDraftsDB lDraftsDB = new localDraftsDB(this);
        	Vector post = lDraftsDB.loadPageDraft(this, postID);
        	
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
    	
    	pd = ProgressDialog.show(editPage.this,
        			getResources().getText(R.string.getting_page), getResources().getText(R.string.please_wait_getting_page), true, false);
    	
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
        
        
        
        final customButton postButton = (customButton) findViewById(R.id.post);
        
        postButton.setOnClickListener(new customButton.OnClickListener() {
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
        
            final customButton addPictureButton = (customButton) findViewById(R.id.addPictureButton);   
            
            addPictureButton.setOnClickListener(new customButton.OnClickListener() {
                public void onClick(View v) {
                	
                	Intent photoPickerIntent = new
                	Intent(Intent.ACTION_PICK);
                	photoPickerIntent.setType("image/*");
                	
                	startActivityForResult(photoPickerIntent, 1); 
                	 
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
                		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPage.this);
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
                		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPage.this);
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
                		Intent i = new Intent(editPage.this, link.class);

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
                		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPage.this);
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
                		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPage.this);
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
            
            
final customButton cancelButton = (customButton) findViewById(R.id.cancel);   
            
            cancelButton.setOnClickListener(new customButton.OnClickListener() {
                public void onClick(View v) {
                	
                	Bundle bundle = new Bundle();
                    
                    bundle.putString("returnStatus", "CANCEL");
                    Intent mIntent = new Intent();
                    mIntent.putExtras(bundle);
                    setResult(RESULT_OK, mIntent);
                    finish();          	
                	}          	
                
        });
            
            
final customButton clearPictureButton = (customButton) findViewById(R.id.clearPicture);   
            
			clearPictureButton.setOnClickListener(new customButton.OnClickListener() {
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
        for (int it = 0; it < selectedImageCtr; it++){
            
            imageContent +=  uploadImage(selectedImageIDs.get(it).toString());

            }

        Integer blogID = 1; //never changes with WordPress, so far
        
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
        
        contentStruct.put("post_type", "page");
        contentStruct.put("title", escapeUtils.escapeHtml(title));
        contentStruct.put("description", escapeUtils.escapeHtml(content));
        

        
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

	public String uploadImage(String imageURL){
        
        //get the settings
        settingsDB settingsDB = new settingsDB(editPage.this);
    	Vector categoriesVector = settingsDB.loadSettings(editPage.this, id);   	
    	
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
    		
    		int sBlogId = Integer.parseInt(categoriesVector.get(10).toString());

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
		byte[] finalBytes = imageHelper.createThumbnail(bytes, sMaxImageWidth);

            //attempt to upload the image
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
	        		sBlogId,
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
				e.printStackTrace();
				if (pd.isShowing()){
					pd.dismiss();
				}
				else{
				dismissDialog(editPage.this.ID_DIALOG_POSTING);
				}
						final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPage.this);
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
							dismissDialog(editPage.this.ID_DIALOG_POSTING);
							}
							//status.setText("Cannot connect to " + uri.getHost() + "\nMake sure server.py on your development host is running !!!");
						} else {
							if (pd.isShowing()){
								pd.dismiss();
							}
							else{
							dismissDialog(editPage.this.ID_DIALOG_POSTING);
							}
							final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPage.this);
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
						dismissDialog(editPage.this.ID_DIALOG_POSTING);
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPage.this);
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
							dismissDialog(editPage.this.ID_DIALOG_POSTING);
							//status.setText("Cannot connect to " + uri.getHost() + "\nMake sure server.py on your development host is running !!!");
						} else {
							//pd.dismiss();
							dismissDialog(editPage.this.ID_DIALOG_POSTING);
							AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPage.this);
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
		    break;
		case 1:
			Uri imagePath = data.getData();   
		    String imgPath2 = imagePath.getEncodedPath();

	        selectedImageIDs.add(selectedImageCtr, imagePath);
	        imageUrl.add(selectedImageCtr, imgPath2);
	        selectedImageCtr++;
	     	  
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
		}
	}//end null check
	}
	
	final Runnable mUpdateResults = new Runnable() {
		public void run() {
			if (finalResult.equals("invalidSettings")){
				dismissDialog(editPage.this.ID_DIALOG_POSTING);			
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPage.this);
							  dialogBuilder.setTitle(getResources().getText(R.string.settings_not_found));
				              dialogBuilder.setMessage(getResources().getText(R.string.settings_not_found_load_now));
				              dialogBuilder.setPositiveButton("Yes",  new
				            		  DialogInterface.OnClickListener() {
		                            public void onClick(DialogInterface dialog, int whichButton) {
		                                
		                            	Intent i = new Intent(editPage.this, settings.class);

		                            	startActivityForResult(i, 0);
		                        
		                            }
		                        });
				              dialogBuilder.setNegativeButton("No", new
				            		  DialogInterface.OnClickListener() {
		                            public void onClick(DialogInterface dialog, int whichButton) {
		                                //just close the window
		                            }
		                        });
				              dialogBuilder.setCancelable(true);
				             dialogBuilder.create().show();
			
			}
			else if (finalResult.equals("emptyFields")){
				dismissDialog(editPage.this.ID_DIALOG_POSTING);				
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPage.this);
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
				dismissDialog(editPage.this.ID_DIALOG_POSTING);	
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPage.this);
				  dialogBuilder.setTitle(getResources().getText(R.string.page_edited));
				  if (xmlrpcError){
					  dialogBuilder.setMessage(getResources().getText(R.string.page_edited_image_error));  
				  }
				  else{
	              dialogBuilder.setMessage(getResources().getText(R.string.page_edited_successfully));
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
	loadingDialog.setMessage(getResources().getText(R.string.attempting_edit_page));
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
        CheckBox publishCB = (CheckBox)findViewById(R.id.publish);
        boolean publishThis = false;
        String images = "";
        boolean success = false;
        

        Integer blogID = 1;
        
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
        		//imageContent +=  uploadImage(selectedImageIDs.get(it).toString());

        	}
        
        
        	if (publishCB.isChecked())
        	{
        		publishThis = true;
        	}
        
        	//new feature, automatically save a post as a draft just in case the posting fails
        	localDraftsDB lDraftsDB = new localDraftsDB(this);
        	success = lDraftsDB.updateLocalPageDraft(this, id, postID, title, content, images, publishThis);
        
        
        }// if/then for valid settings
        
		return success;
	}
	
	@Override public boolean onKeyDown(int i, KeyEvent event) {

		  // only intercept back button press
		  if (i == KeyEvent.KEYCODE_BACK) {
			  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPage.this);
			  dialogBuilder.setTitle(getResources().getText(R.string.cancel_edit));
              dialogBuilder.setMessage(getResources().getText(R.string.sure_to_cancel_edit_page));
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
    
}





