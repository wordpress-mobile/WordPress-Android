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
import android.graphics.Color;
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
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


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
	public String finalResult = null, selectedCategories = "";
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
    public boolean thumbnailOnly, secondPass, xmlrpcError = false, isPage = false, isAction=false;
    public String SD_CARD_TEMP_DIR = "";
    public long checkedCategories[];
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Bundle extras = getIntent().getExtras();
        final Intent intent = getIntent();
        if(extras !=null)
        {
         id = extras.getString("id");
         accountName = escapeUtils.unescapeHtml(extras.getString("accountName"));
         isPage = extras.getBoolean("isPage", false);
        }
        
        if (isPage){  
        	setContentView(R.layout.main_page);
        }
        else{
        	setContentView(R.layout.main);
        }
        
        String action = getIntent().getAction();
        if (Intent.ACTION_SEND.equals(action)){ //this is from a share action!
        	isAction = true;
        	settingsDB settingsDB = new settingsDB(this);
        	Vector accounts = settingsDB.getAccounts(this);
        	
        	if (accounts.size() > 0){
                
        		final String blogNames[] = new String[accounts.size()];
        		final String accountIDs[] = new String[accounts.size()];
        		String accountUsers[] = new String[accounts.size()];
        		
                for (int i = 0; i < accounts.size(); i++) {
                    
                	HashMap curHash = (HashMap) accounts.get(i);
                	blogNames[i] = escapeUtils.unescapeHtml(curHash.get("blogName").toString());
                	accountUsers[i] = curHash.get("username").toString();
                	accountIDs[i] = curHash.get("id").toString();
                	
                } 

                //Don't prompt if they have one blog only
                if (accounts.size() != 1){
            	AlertDialog.Builder builder = new AlertDialog.Builder(this);
            	builder.setTitle(getResources().getText(R.string.select_a_blog));
            	builder.setItems(blogNames, new DialogInterface.OnClickListener() {
            	    public void onClick(DialogInterface dialog, int item) {
            	        id = accountIDs[item];
            	        accountName = blogNames[item];
            	        setTitle(accountName + " - " + getResources().getText((isPage) ? R.string.new_page : R.string.new_post)); 
            	        setContent();
            	    }
            	});
            	AlertDialog alert = builder.create();
            	alert.show();
                }
                else{
                	id = accountIDs[0];
        	        accountName = blogNames[0];
        	        setTitle(accountName + " - " + getResources().getText((isPage) ? R.string.new_page : R.string.new_post));
        	        setContent();
                }
                
    	        
                
        	}
        	else{
        		//no account, load main view to load new account view
        		Intent i = new Intent(this, wpAndroid.class);
        		Toast.makeText(getApplicationContext(), getResources().getText(R.string.no_account), Toast.LENGTH_LONG).show();
            	startActivity(i);
            	finish();

        	}
        	
        	
        }

        if (accountName != null){
        	this.setTitle(accountName + " - " + getResources().getText((isPage) ? R.string.new_page : R.string.new_post));
        }
        else{
        	this.setTitle(getResources().getText((isPage) ? R.string.new_page : R.string.new_post));
        }

      //clear up some variables
        selectedImageIDs.clear();
        selectedImageCtr = 0;
        
        //loads the categories from the db if they exist
        if (!isPage){
	        //loadCategories();
	       
	       final Button selectCategories = (Button) findViewById(R.id.selectCategories);   
	        
	        selectCategories.setOnClickListener(new Button.OnClickListener() {
	            public void onClick(View v) {
	            	 
	            	Bundle bundle = new Bundle();
					bundle.putString("id", id);
					if (checkedCategories != null){
					bundle.putLongArray("checkedCategories", checkedCategories);
					}
			    	Intent i = new Intent(newPost.this, selectCategories.class);
			    	i.putExtras(bundle);
			    	startActivityForResult(i, 5);
	            }
	        });
	        
    }
        
        final Button postButton = (Button) findViewById(R.id.post);
        
        postButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

            	boolean result = savePost();
            	
            	if (result){

              	  Toast.makeText(newPost.this, getResources().getText(R.string.saved_to_local_drafts), Toast.LENGTH_SHORT).show();
              	  if (isAction){
              		  Bundle bundle = new Bundle();
	                  Intent mIntent = new Intent(newPost.this, tabView.class);
	                  bundle.putString("activateTab", "posts");
	                  bundle.putString("id", id);
	                  bundle.putString("accountName", accountName);
	                  bundle.putString("action", "save");
	                  mIntent.putExtras(bundle);
	                  startActivity(mIntent);
	                  finish();
              	  }
              	  else{
	              	  Bundle bundle = new Bundle();
	                  bundle.putString("returnStatus", "OK");
	                  Intent mIntent = new Intent();
	                  mIntent.putExtras(bundle);
	                  setResult(RESULT_OK, mIntent);
	                  finish();
              	  }
            	}
	
            }
        });
        
final Button uploadButton = (Button) findViewById(R.id.upload);
        
        uploadButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

            	boolean result = savePost();
            	
            	if (result){
            		
            	  localDraftsDB lddb = new localDraftsDB(newPost.this);
              	  
              	  int newID = lddb.getLatestDraftID(newPost.this, id);
              	  Bundle bundle = new Bundle();
              	  if (newID != -1){
            	  
            	  if (isAction){
            		  Intent mIntent = new Intent(newPost.this, tabView.class);
	                  bundle.putString("activateTab", "posts");
	                  bundle.putString("id", id);
	                  bundle.putInt("uploadID", newID);
	                  bundle.putString("accountName", accountName);
	                  bundle.putString("action", "upload");
	                  mIntent.putExtras(bundle);
	                  startActivity(mIntent);
            	  }
            	  else{
                      bundle.putString("returnStatus", "OK");
                      bundle.putBoolean("upload", true);
                      bundle.putInt("newID", newID);
                      Intent mIntent = new Intent();
                      mIntent.putExtras(bundle);
                      setResult(RESULT_OK, mIntent); 
            	  }

                  finish();
            	  }
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
            
final Button clearPictureButton = (Button) findViewById(R.id.clearPicture);   
            
			clearPictureButton.setOnClickListener(new Button.OnClickListener() {
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
    

    
	protected void setContent() {
		Intent intent = getIntent();
		String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (text != null) {
        	EditText contentET = (EditText) findViewById(R.id.content);
        	//It's a youtube video link! need to strip some parameters so the embed will work
        	if (text.contains("youtube_gdata")){
        		text = text.replace("&feature=youtube_gdata", "");
        		text = text.replace("watch?v=", "v/");
        		text = "<object width=\"480\" height=\"385\"><param name=\"movie\" value=\"" + text + "\"></param><param name=\"allowFullScreen\" value=\"true\"></param><param name=\"allowscriptaccess\" value=\"always\"></param><embed src=\"" + text + "\" type=\"application/x-shockwave-flash\" allowscriptaccess=\"always\" allowfullscreen=\"true\" width=\"480\" height=\"385\"></embed></object>";
        	}
        	
        	contentET.setText(text);
        }
        
        String type = intent.getType();
        Uri stream = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (stream != null && type != null) {
        	String imgPath = stream.getEncodedPath();
        	selectedImageIDs.add(selectedImageCtr, stream);
        	imageUrl.add(selectedImageCtr, imgPath);
	           	selectedImageCtr++;
	           	GridView gridview = (GridView) findViewById(R.id.gridView);
		     	  
	           	gridview.setAdapter(new ImageAdapter(newPost.this));
        }
		
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
        else if (title.equals("") || (content.equals("") && selectedImageIDs.size() == 0))
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

        	categories = selectedCategories;

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
			File jpeg = null;
			if (cur != null){
	     	  String thumbData = "";
	     	 
	     	  if (cur.moveToFirst()) {
	     		  
	     		int nameColumn, dataColumn, heightColumn, widthColumn;
	     		
	     			nameColumn = cur.getColumnIndex(Images.Media._ID);
	     	        dataColumn = cur.getColumnIndex(Images.Media.DATA);

	           thumbData = cur.getString(dataColumn);

	     	  }
	     	  
	     	   
	     	   jpeg = new File(thumbData);
			}
			else{
				jpeg = new File(tempURI.toString().replace("file://", ""));
			}
	     	   
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

                    f.delete();
                    
                    Bundle bundle = new Bundle();
                    
                    bundle.putString("imageURI", capturedImage.toString());
                    
                    selectedImageIDs.add(selectedImageCtr, capturedImage);
                    imageUrl.add(selectedImageCtr, capturedImage.toString());
                    selectedImageCtr++;

         	     	 gridview.setAdapter(new ImageAdapter(this));
       
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
            		dialogBuilder.setTitle(getResources().getText(R.string.file_error));
                    dialogBuilder.setMessage(getResources().getText(R.string.file_error_encountered));
                  dialogBuilder.setPositiveButton("OK",  new
                		  DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // just close the dialog
                        }
                    });
                  dialogBuilder.setCancelable(true);
                 dialogBuilder.create().show();
                }

        }
        else {
        	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
    		dialogBuilder.setTitle(getResources().getText(R.string.file_error));
            dialogBuilder.setMessage(getResources().getText(R.string.file_error_encountered));
          dialogBuilder.setPositiveButton("OK",  new
        		  DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // just close the dialog
                }
            });
          dialogBuilder.setCancelable(true);
         dialogBuilder.create().show();
        }

		     	break;
		     	
		case 5:
			if (resultCode == RESULT_OK){
			extras = data.getExtras();
			String cats = extras.getString("selectedCategories");
			long[] checkedCats = extras.getLongArray("checkedItems");
			selectedCategories = cats;
			checkedCategories = checkedCats;
			TextView selectedCategoriesTV = (TextView) findViewById(R.id.selectedCategories);
			selectedCategoriesTV.setText(getResources().getText(R.string.selected_categories) + " " + cats);
			}
	     	break;
		}
		
		
		
	}//end null check
	}
	
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