package org.wordpress.android;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

public class newPost extends Activity implements LocationListener{
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
	public String id = "";
	private Vector<Uri> selectedImageIDs = new Vector();
	private int selectedImageCtr = 0;
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
    LocationManager lm;
    Criteria criteria;
    String provider;
    Location curLocation;
    public boolean location = false;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Bundle extras = getIntent().getExtras();
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
        	WordPressDB settingsDB = new WordPressDB(this);
        	Vector accounts = settingsDB.getAccounts(this);
        	
        	if (accounts.size() > 0){
                
        		final String blogNames[] = new String[accounts.size()];
        		final String accountIDs[] = new String[accounts.size()];
        		
                for (int i = 0; i < accounts.size(); i++) {
                    
                	HashMap curHash = (HashMap) accounts.get(i);
                	blogNames[i] = escapeUtils.unescapeHtml(curHash.get("blogName").toString());
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
            	        lbsCheck();
            	        
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
        else{
        	//clear up some variables
            selectedImageIDs.clear();
            selectedImageCtr = 0;
            
            if (!isPage){
            	lbsCheck();
            }
            
        }

        if (accountName != null){
        	this.setTitle(accountName + " - " + getResources().getText((isPage) ? R.string.new_page : R.string.new_post));
        }
        else{
        	this.setTitle(getResources().getText((isPage) ? R.string.new_page : R.string.new_post));
        }
        
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
	        
	        final Button viewMap = (Button) findViewById(R.id.viewMap);   
	        
	        viewMap.setOnClickListener(new TextView.OnClickListener() {
	            public void onClick(View v) {
	            	 
	            	Double latitude = 0.0;
	            	try {
						latitude = curLocation.getLatitude();
					} catch (Exception e) {
						e.printStackTrace();
					}
	            	if (latitude != 0.0){
		            	String uri = "geo:"+ latitude + "," + curLocation.getLongitude();  
		            	startActivity(new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri))); 
	            	}
	            	else {
	            		Toast.makeText(newPost.this, getResources().getText(R.string.location_toast), Toast.LENGTH_SHORT).show();
	            	}
	            	  

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
            		
            	  WordPressDB lddb = new WordPressDB(newPost.this);
              	  int newID = -1;
              	  if (isPage){
              		  newID = lddb.getLatestPageDraftID(newPost.this, id);
              	  }
              	  else{
              		  newID = lddb.getLatestDraftID(newPost.this, id);
              	  }
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
			     	gridview.setVisibility(View.GONE);
			     	 
			     	clearPictureButton.setVisibility(View.GONE);
                	         	
                }
        });            
            
    }
    
	protected void lbsCheck() {
		WordPressDB settingsDB = new WordPressDB(newPost.this);
    	Vector settingsVector = settingsDB.loadSettings(newPost.this, id);   	
    	
		String sLocation = settingsVector.get(11).toString();
		
		if (sLocation.equals("1")){
			location = true;
		}
		if (location){
        	lm = (LocationManager) getSystemService(LOCATION_SERVICE);
    		criteria = new Criteria();
    		criteria.setAccuracy(Criteria.ACCURACY_FINE);
    		criteria.setAltitudeRequired(false);
    		criteria.setBearingRequired(false);
    		criteria.setCostAllowed(true);
    		criteria.setPowerRequirement(Criteria.POWER_HIGH);

    		provider = lm.getBestProvider(criteria, true);
    		RelativeLayout locationSection = (RelativeLayout) findViewById(R.id.section4);
        	locationSection.setVisibility(View.VISIBLE);
		}
		
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
        		contentET.setText(text);
        	}
        	else{   	
	        	//add link tag around URLs, trac #64
	            String [] parts = text.split("\\s");
	            String finalText = "";
	
	            // Attempt to convert each item into an URL.   
	            for( String item : parts ) try {
	                URL url = new URL(item);
	                // If possible then replace with anchor...
	                finalText+="<a href=\"" + url + "\">"+ url + "</a> ";    
	            } catch (MalformedURLException e) {
	                // If there was an URL that was not it!...
	                finalText+= item + " ";
	            }
	            
	            contentET.setText(finalText);
        	}
        	
        	
        }
        
        String type = intent.getType();
        Uri stream = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (stream != null && type != null) {
        	String imgPath = stream.getEncodedPath();
        	selectedImageIDs.add(selectedImageCtr, stream);
        	imageUrl.add(selectedImageCtr, imgPath);
	           	selectedImageCtr++;
	           	GridView gridview = (GridView) findViewById(R.id.gridView);
		     	  gridview.setVisibility(View.VISIBLE);
	           	gridview.setAdapter(new ImageAdapter(newPost.this));

	           	Button clearMedia = (Button) findViewById(R.id.clearPicture);
	           	clearMedia.setVisibility(View.VISIBLE);
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
        
        	//images
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
        	
        	//Geotagging
        	WordPressDB settingsDB = new WordPressDB(this);
        	Vector settingsVector = settingsDB.loadSettings(this, id);   	
        	
    		String sLocation = settingsVector.get(11).toString();
    		
    		boolean location = false;
    		if (sLocation.equals("1")){
    			location = true;
    		}
        	
    		Double latitude = 0.0;
        	Double longitude = 0.0;
            if (location){
            	         	
        		//attempt to get the device's location
        		// set up the LocationManager
            	
                try {
        			Location loc = lm.getLastKnownLocation(provider);
        			latitude = loc.getLatitude();
        			longitude = loc.getLongitude();
        		} catch (Exception e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}

        	}
        
        	//new feature, automatically save a post as a draft just in case the posting fails
            WordPressDB lDraftsDB = new WordPressDB(this);
        	if (isPage){
        		success = lDraftsDB.saveLocalPageDraft(this, id, title, content, images, publishThis);
        	}
        	else{
        		success = lDraftsDB.saveLocalDraft(this, id, title, content, images, tags, categories, publishThis, latitude, longitude);
        	}
        
        
        }// if/then for valid settings
        
		return success;
	}
	
	public boolean checkSettings(){
		//see if the user has any saved preferences
		 WordPressDB settingsDB = new WordPressDB(this);
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
	        
	        if (!tempURI.toString().contains("video")){
	     	   
	        String[] projection = new String[] {
	      		    Images.Thumbnails._ID,
	      		    Images.Thumbnails.DATA,
	      		    Images.Media.ORIENTATION
	      		};
	     	String orientation = "";
			Cursor cur = managedQuery(tempURI, projection, null, null, null);
			File jpeg = null;
			if (cur != null){
	     	  String thumbData = "";
	     	 
	     	  if (cur.moveToFirst()) {
	     		  
	     		int nameColumn, dataColumn, orientationColumn;
	     		
	     			nameColumn = cur.getColumnIndex(Images.Media._ID);
	     	        dataColumn = cur.getColumnIndex(Images.Media.DATA);
	     	        orientationColumn = cur.getColumnIndex(Images.Media.ORIENTATION);

	           thumbData = cur.getString(dataColumn);
	           orientation = cur.getString(orientationColumn);
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
	        
	        if ((orientation != null) && (orientation.equals("90") || orientation.equals("180") || orientation.equals("270"))){
	        	Matrix matrix = new Matrix();
		        // rotate the Bitmap
		        matrix.postRotate(Integer.valueOf(orientation));

		        // recreate the new Bitmap
		        Bitmap rotatedBitmap = Bitmap.createBitmap(resizedBitmap, 0, 0,
		        		resizedBitmap.getWidth(), resizedBitmap.getHeight(), matrix, true); 
		        
		        imageView.setImageBitmap(rotatedBitmap);
	        }
	        else{
	        	imageView.setImageBitmap(resizedBitmap);
	        }
	        
	        
	        }
	        else{
	        	imageView.setImageDrawable(getResources().getDrawable(R.drawable.video));
	        }
	        
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
			Button clearMedia = (Button) findViewById(R.id.clearPicture);
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
		    gridview.setVisibility(View.VISIBLE);
	     	  
	     	gridview.setAdapter(new ImageAdapter(this));

           	clearMedia.setVisibility(View.VISIBLE);
	     	break;
		case 4:
			if (resultCode == Activity.RESULT_OK) {

                // http://code.google.com/p/android/issues/detail?id=1480
				File f = null;
				int sdk_int = 0;
				try {
					sdk_int = Integer.valueOf(android.os.Build.VERSION.SDK);
				} catch (Exception e1) {
					sdk_int = 3; //assume they are on cupcake
				}
                if (data != null && (sdk_int <= 4)){ //Older HTC Sense Devices return different data for image capture
                	
                	try {
                		String[] projection; 
						Uri imagePath = data.getData();
						projection = new String[] {
							    Images.Media._ID,
							    Images.Media.DATA,
							    Images.Media.MIME_TYPE,
							    Images.Media.ORIENTATION
							};
						
						Cursor cur = this.managedQuery(imagePath, projection, null, null, null);
  		 	  String thumbData = "";
  		 	 
  		 	  if (cur.moveToFirst()) {
						  
						int nameColumn, dataColumn, heightColumn, widthColumn, mimeTypeColumn, orientationColumn;

							nameColumn = cur.getColumnIndex(Images.Media._ID);
						    dataColumn = cur.getColumnIndex(Images.Media.DATA);
							            

  		       thumbData = cur.getString(dataColumn);
  		       f = new File(thumbData);
  		 	  }
					} catch (Exception e) {
						// TODO Auto-generated catch block
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
	            		dialogBuilder.setTitle(getResources().getText(R.string.error));
	                    dialogBuilder.setMessage(e.getMessage());
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
                else{
                	f = new File(SD_CARD_TEMP_DIR);
                }
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
                    gridview.setVisibility(View.VISIBLE);
         	     	 gridview.setAdapter(new ImageAdapter(this));
         	     	 
    	           	clearMedia.setVisibility(View.VISIBLE);
       
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
		case 6:
			 
		    Uri videoUri = data.getData();
		    String videoPath = videoUri.getEncodedPath();
   
           selectedImageIDs.add(selectedImageCtr, videoUri);
           imageUrl.add(selectedImageCtr, videoPath);
           selectedImageCtr++;

           gridview.setVisibility(View.VISIBLE);
	     	 gridview.setAdapter(new ImageAdapter(this));
	     	 clearMedia.setVisibility(View.VISIBLE);
	     	 break;
		case 7:
			if (resultCode == Activity.RESULT_OK) {
                    Uri capturedVideo = data.getData();
                    
                    selectedImageIDs.add(selectedImageCtr, capturedVideo);
                    imageUrl.add(selectedImageCtr, capturedVideo.toString());
                    selectedImageCtr++;
                    gridview.setVisibility(View.VISIBLE);
         	     	gridview.setAdapter(new ImageAdapter(this));
         	     	clearMedia.setVisibility(View.VISIBLE);
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
			menu.add(0, 2, 0, getResources().getText(R.string.select_video));
			menu.add(0, 3, 0, getResources().getText(R.string.take_video));
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

				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newPost.this);
	    		dialogBuilder.setTitle(getResources().getText(R.string.sdcard_title));
	            dialogBuilder.setMessage(getResources().getText(R.string.sdcard_message));
	            dialogBuilder.setPositiveButton("OK",  new
	        		  DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                    // just close the dialog

	                }
	            });
	            dialogBuilder.setCancelable(true);
	            dialogBuilder.create().show();
	            break;		
            }

        	SD_CARD_TEMP_DIR = Environment.getExternalStorageDirectory() + File.separator + "wordpress" + File.separator + "wp-" + System.currentTimeMillis() + ".jpg";
        	Intent takePictureFromCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        	takePictureFromCameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(SD_CARD_TEMP_DIR)));
        	
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
		case 2:
	    	
	    	Intent videoPickerIntent = new Intent(Intent.ACTION_PICK);
        	videoPickerIntent.setType("video/*");
        	
        	startActivityForResult(videoPickerIntent, 6);
	    	
	    	return true;
		case 3:

        	Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        	startActivityForResult(takeVideoIntent, 7); 
		
		return true;
	}
	  return false;	
	}
	
	/** Register for the updates when Activity is in foreground */
	@Override
	protected void onResume() {
		super.onResume();
		if (!isPage && location){
			lm.requestLocationUpdates(
		            LocationManager.GPS_PROVIDER, 
		            60000, 
		            0, 
		            this
		    );
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 0, this);
		}
	}

	/** Stop the updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
		if (!isPage && location){
			lm.removeUpdates(this);
		}
	}

	@Override
	protected void onDestroy() {
		super.onPause();
		if (!isPage && location){
			lm.removeUpdates(this);
		}
	}

	public void onLocationChanged(Location location) {
		curLocation = location;
		new getAddressTask().execute(location.getLatitude(), location.getLongitude());
		lm.removeUpdates(this);

	}

	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	
	private class getAddressTask extends AsyncTask<Double, Void, String> {

	     protected void onProgressUpdate() {
	     }

	     protected void onPostExecute(String result) {
	    	 TextView map = (TextView) findViewById(R.id.locationText); 
	    	 map.setText(result);
	     }
	     @Override
		protected String doInBackground(Double... args) {
			Geocoder gcd = new Geocoder(newPost.this, Locale.getDefault());
	    	String finalText = "";
	    			List<Address> addresses;
	    		try {
	    			addresses = gcd.getFromLocation(args[0], args[1], 1);
	    			if (addresses.size() > 0) {
	    			    finalText = addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea();
	    			}
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		}
				return finalText;
		}

	 }
	
}