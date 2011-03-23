package org.wordpress.android;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.http.conn.HttpHostConnectException;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ParseException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.text.Editable;
import android.text.Html;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

public class editPost extends Activity implements LocationListener{
    /** Called when the activity is first created. */
	public static long globalData = 0;
	public ProgressDialog pd;
	public boolean postStatus = false;
	String[] mFiles=null;
	public String thumbnailPath = null;
	public String imagePath = null;
	public String imageTitle = null;
	public Vector<String> imageUrl = new Vector<String>();
	public Vector<Object> thumbnailUrl = new Vector<Object>();
	private final Handler mHandler = new Handler();
	public String finalResult = null;
	Vector<String> selectedCategories = new Vector<String>();
	public ArrayList<CharSequence> textArray = new ArrayList<CharSequence>();
	public ArrayList<CharSequence> loadTextArray = new ArrayList<CharSequence>();
	public Boolean newStart = true;
	public String categoryErrorMsg = "", id = "", accountName = "", postID = "", SD_CARD_TEMP_DIR = "", categories = "", mediaErrorMsg = "";
	private XMLRPCClient client;
	private Vector<Uri> selectedImageIDs = new Vector<Uri>();
	private int selectedImageCtr = 0;
    private int ID_DIALOG_POSTING = 1, ID_DIALOG_LOADING = 2, ID_DIALOG_DATE = 3, ID_DIALOG_TIME = 4;
    public String newID, imgHTML, sMaxImageWidth, sImagePlacement;
    public Boolean localDraft = false, centerThumbnail = false, xmlrpcError = false, isPage = false, isNew = false, 
    isAction = false, isUrl = false, location = false, locationActive = false, isLargeScreen = false, isCustomPubDate = false;
    public Vector<Object> imgThumbs = new Vector<Object>();
    LocationManager lm;
    Criteria criteria;
    String provider;
    Location curLocation;
    ProgressDialog postingDialog;
    int styleStart = -1, cursorLoc = 0, screenDensity = 0;
    //date holders
    private int mYear;
    private int mMonth;
    private int mDay;
    private int mHour;
    private int mMinute;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);	
        
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         accountName = escapeUtils.unescapeHtml(extras.getString("accountName"));
         postID = extras.getString("postID");
         localDraft = extras.getBoolean("localDraft", false); 
         isPage = extras.getBoolean("isPage", false);
         isNew = extras.getBoolean("isNew", false);
        }
        
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay(); 
		int width = display.getWidth();
		int height = display.getHeight();
		if (height > width){
			width = height;
		}	
		if (width > 480){
			isLargeScreen = true;
		}
		String[] items;
        if (isPage){  
        	setContentView(R.layout.edit_page);
        	items = new String[] {getResources().getString(R.string.draft), getResources().getString(R.string.post_private), getResources().getString(R.string.publish_post)};
        }
        else{
        	setContentView(R.layout.edit);
        	items = new String[] {getResources().getString(R.string.draft), getResources().getString(R.string.pending_review), getResources().getString(R.string.post_private), getResources().getString(R.string.publish_post)};
        }
        
        Spinner spinner = (Spinner) findViewById(R.id.status);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        
        String action = getIntent().getAction();
		if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)){ //this is from a share action!
			isAction = true;
			isNew = true;
			WordPressDB settingsDB = new WordPressDB(this);
			Vector<?> accounts = settingsDB.getAccounts(this);

			if (accounts.size() > 0){

				final String blogNames[] = new String[accounts.size()];
				final String accountIDs[] = new String[accounts.size()];

				for (int i = 0; i < accounts.size(); i++) {

					HashMap<?, ?> curHash = (HashMap<?, ?>) accounts.get(i);
					try {
						blogNames[i] = escapeUtils.unescapeHtml(curHash.get("blogName").toString());
					} catch (Exception e) {
						blogNames[i] = "(No Blog Title)";
					}
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
        
		if (isNew){
			setTitle(accountName + " - " + getResources().getText((isPage) ? R.string.new_page : R.string.new_post));
		}
		else {
        	setTitle(accountName + " - " + getResources().getText((isPage) ? R.string.edit_page : R.string.edit_post));
		}
        
        if (localDraft){
        	WordPressDB lDraftsDB = new WordPressDB(this);
        	Vector<?> post;
        	if (isPage){
        		post = lDraftsDB.loadPageDraft(this, postID);
        	}
        	else{
        		post = lDraftsDB.loadPost(this, postID);
        	}
        	
        	final HashMap<?, ?> postHashMap = (HashMap<?, ?>) post.get(0);
        	
        	EditText titleET = (EditText)findViewById(R.id.title);
        	EditText contentET = (EditText)findViewById(R.id.content);
        	EditText passwordET = (EditText)findViewById(R.id.post_password);
        	
        	titleET.setText(postHashMap.get("title").toString());
        	contentET.setText(Html.fromHtml(postHashMap.get("content").toString()));
        	
        	long pubDate = Long.parseLong(postHashMap.get("pubDate").toString());
        	if (pubDate != 0){
        		try {
					Date date = new Date(pubDate);
					SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a"); 
					String sPubDate = sdf.format(date);
					TextView tvPubDate = (TextView) findViewById(R.id.pubDate);
					tvPubDate.setText(sPubDate);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        	
        	if(postHashMap.get("password") != null)
        		passwordET.setText(postHashMap.get("password").toString());
	        
        	if (postHashMap.get("status") != null){
	        	String status = postHashMap.get("status").toString();
	        	
		        if (status.equals("draft")){
		        	spinner.setSelection(0, true);
		        }
		        else if (status.equals("pending")){
		        	spinner.setSelection(1, true);
		        }
		        else if (status.equals("private")){
		        	if (isPage){
		        		spinner.setSelection(1);
		        	}
		        	else{
		        		spinner.setSelection(2);
		        	}
		        }
		        else if (status.equals("publish")){
		        	if (isPage){
		        		spinner.setSelection(2);
		        	}
		        	else{
		        		spinner.setSelection(3);
		        	}
		        }
        	}
        	
        	String picturePaths = postHashMap.get("picturePaths").toString();
        	if (!picturePaths.equals("")){
        		String[] pPaths = picturePaths.split(",");
        		
        		for (int i = 0; i < pPaths.length; i++)
        		{
        			Uri imagePath = Uri.parse(pPaths[i]); 
        			addMedia(imagePath.getEncodedPath(), imagePath);
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
		    	
		    	WordPressDB settingsDB = new WordPressDB(this);
	        	Vector<?> settingsVector = settingsDB.loadSettings(this, id);   	
	        	
	    		String sLocation = settingsVector.get(13).toString();
	    		
	    		location = false;
	    		if (sLocation.equals("1")){
	    			location = true;
	    		}
	    		
	    		if (location){
	    			enableLBSButtons();
	    		}
	    		
	    		Double latitude = (Double) postHashMap.get("latitude");
	    		Double longitude = (Double) postHashMap.get("longitude");

	    		if (latitude != 0.0){
	    			new getAddressTask().execute(latitude, longitude);
	    		}
	    		
	    		if (location && latitude > 0){
	    			Button updateLocation = (Button) findViewById(R.id.updateLocation);
	    			
	    			updateLocation.setOnClickListener(new Button.OnClickListener() {
	    	            public void onClick(View v) {
	    	            	 
	    	            	lm = (LocationManager) getSystemService(LOCATION_SERVICE);
	    		    		
	    		    		lm.requestLocationUpdates(
	    				            LocationManager.GPS_PROVIDER, 
	    				            20000, 
	    				            0, 
	    				            editPost.this
	    				    );
	    					lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 20000, 0, editPost.this);
	    					locationActive = true;
	    	            }
	    	        });
	    			
		    		RelativeLayout locationSection = (RelativeLayout) findViewById(R.id.section4);
	            	locationSection.setVisibility(View.VISIBLE);
	    		}
	    		else if (location){
	    			lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		    		
		    		lm.requestLocationUpdates(
				            LocationManager.GPS_PROVIDER, 
				            20000, 
				            0, 
				            editPost.this
				    );
					lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 20000, 0, editPost.this);
					locationActive = true;
					
					RelativeLayout locationSection = (RelativeLayout) findViewById(R.id.section4);
	            	locationSection.setVisibility(View.VISIBLE);
	    		}
		    	
		    	String tags = postHashMap.get("tags").toString();
		    	if (!tags.equals("")){
		    		EditText tagsET = (EditText) findViewById(R.id.tags);
		    		tagsET.setText(tags);
		    	}
        	}
        }
        else if (isNew){
        	Button cancelBtn = (Button) findViewById(R.id.cancel);
        	cancelBtn.setText(getResources().getText(R.string.upload_now));
        	
        	if (!isAction){
	        	WordPressDB settingsDB = new WordPressDB(this);
	        	Vector<?> settingsVector = settingsDB.loadSettings(this, id);   	
	        	
	    		String sLocation = settingsVector.get(13).toString();
	    		if (!isPage){
		    		location = false;
		    		if (sLocation.equals("1")){
		    			location = true;
		    		}
		    		
		    		enableLBSButtons();
	    		}
        	}
        	
        }
        else{

        WordPressDB settingsDB = new WordPressDB(this);
    	Vector<?> categoriesVector = settingsDB.loadSettings(this, id);
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
		String sHttpuser = categoriesVector.get(4).toString();
		String sHttppassword = categoriesVector.get(5).toString();
		
    	client = new XMLRPCClient(sURL, sHttpuser, sHttppassword);
    	
    	EditText titleET = (EditText)findViewById(R.id.title);
    	String setTitle = titleET.getText().toString();
    	if (setTitle.equals("")){
    	
    	pd = ProgressDialog.show(editPost.this,
    			getResources().getText((isPage) ? R.string.getting_page : R.string.getting_post), getResources().getText((isPage) ? R.string.please_wait_getting_page : R.string.please_wait_getting_post), true, false);
    	
    	XMLRPCMethod method = new XMLRPCMethod("metaWeblog.getPost", new XMLRPCMethodCallback() {
			public void callFinished(Object result) {
				pd.dismiss();
				
				if (result == null){
					//prompt that something went wrong?
				}
				else{
					HashMap<?, ?> contentHash = (HashMap<?, ?>) result;
					
					EditText titleET = (EditText)findViewById(R.id.title);
			        titleET.setText(escapeUtils.unescapeHtml(contentHash.get("title").toString()));
			        EditText contentET = (EditText)findViewById(R.id.content);
			        String content = "";
			        if (contentHash.get("mt_text_more").toString() != ""){
			        	//removed toHtml function for trac ticket #68
			        	content = contentHash.get("description").toString() + "<!--more-->\n" + contentHash.get("mt_text_more").toString();
			        }
			        else{
			        	content = contentHash.get("description").toString();
			        }
			        
			        contentET.setText(content);
			        
			        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
			        Calendar cal = Calendar.getInstance();
			        TimeZone tz = cal.getTimeZone();
			        String shortDisplayName = "";
			        shortDisplayName = tz.getDisplayName(true, TimeZone.SHORT);
			        
			     // make the date pretty
					String cDate = contentHash.get("dateCreated").toString().replace(tz.getID(),
							shortDisplayName);
					try {
						Date d = sdf.parse(cDate);
						SimpleDateFormat sdfOut = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
						TextView tvPubDate = (TextView) findViewById(R.id.pubDate);
						tvPubDate.setText(sdfOut.format(d));
					} catch (ParseException pe) {
						pe.printStackTrace();
					} catch (java.text.ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			        
			        EditText passwordET = (EditText)findViewById(R.id.post_password);
			        if(contentHash.get("wp_password") != null)
			        	passwordET.setText(contentHash.get("wp_password").toString());
			        
			        Toast.makeText(editPost.this, getResources().getText(R.string.html), Toast.LENGTH_SHORT).show();
			        
			        String status = contentHash.get("post_status").toString();
			        
			        if (!isPage){
				        EditText tagsET = (EditText)findViewById(R.id.tags);
				        tagsET.setText(escapeUtils.unescapeHtml(contentHash.get("mt_keywords").toString()));
				        TextView categoriesTV = (TextView)findViewById(R.id.selectedCategories);
			        
				        Object categoriesArray[] = (Object[]) contentHash.get("categories");
				        
				        if (categoriesArray != null){
				        	
				        	categories = "";
						    for (int ctr = 0; ctr < categoriesArray.length; ctr++){
						        String category = categoriesArray[ctr].toString();
						        if (!selectedCategories.contains(category))
			                	{
						        categories += category + ",";
			                	selectedCategories.add(category);
			                	}					    
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
			        
			        Spinner spinner = (Spinner) findViewById(R.id.status);
			        
			        if (status.equals("draft")){
			        	spinner.setSelection(0, true);
			        }
			        else if (status.equals("pending")){
			        	spinner.setSelection(1, true);
			        }
			        else if (status.equals("private")){
			        	if (isPage){
			        		spinner.setSelection(1);
			        	}
			        	else{
			        		spinner.setSelection(2);
			        	}
			        }
			        else if (status.equals("publish")){
			        	if (isPage){
			        		spinner.setSelection(2);
			        	}
			        	else{
			        		spinner.setSelection(3);
			        	}
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
        
        if ((localDraft || isNew) && !isPage){
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
        
        final Button postButton = (Button) findViewById(R.id.post);
        
        postButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            
            if(localDraft || isNew) {
            	boolean result = savePost();
            	
            	if (result) {
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
        				public void run() {
							try {
								finalResult = submitPost();

								mHandler.post(mUpdateResults);
								
							} catch (IOException e) {
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
            
            final EditText contentEdit = (EditText) findViewById(R.id.content);
            contentEdit.addTextChangedListener(new TextWatcher() { 
                public void afterTextChanged(Editable s) { 
                	if (localDraft || isNew){
	                	//add style as the user types if a toggle button is enabled
	                	ToggleButton boldButton = (ToggleButton) findViewById(R.id.bold);
	                	ToggleButton emButton = (ToggleButton) findViewById(R.id.em);
	                	ToggleButton bquoteButton = (ToggleButton) findViewById(R.id.bquote);
	                	ToggleButton underlineButton = (ToggleButton) findViewById(R.id.underline);
	                	ToggleButton strikeButton = (ToggleButton) findViewById(R.id.strike);
	                	int position = Selection.getSelectionStart(contentEdit.getText());
	            		if (position < 0){
	            			position = 0;
	            		}
	                	
	            		if (position > 0){
	            			
	            			if (styleStart > position || position > (cursorLoc + 1)){
	    						//user changed cursor location, reset
	    						if (position - cursorLoc > 1){
	    							//user pasted text
	    							styleStart = cursorLoc;
	    						}
	    						else{
	    							styleStart = position - 1;
	    						}
	    					}
	            			
		                	if (boldButton.isChecked()){  
		                		StyleSpan[] ss = s.getSpans(styleStart, position, StyleSpan.class);
	
		                		for (int i = 0; i < ss.length; i++) {
		                			if (ss[i].getStyle() == android.graphics.Typeface.BOLD){
		                				s.removeSpan(ss[i]);
		                			}
		                        }
		                		s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		                	}
		                	if (emButton.isChecked()){
		                		StyleSpan[] ss = s.getSpans(styleStart, position, StyleSpan.class);
		                		
		                		for (int i = 0; i < ss.length; i++) {
		                			if (ss[i].getStyle() == android.graphics.Typeface.ITALIC){
		                				s.removeSpan(ss[i]);
		                			}
		                        }
		                		s.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		                	}
		                	if (bquoteButton.isChecked()){
		                		
		                		QuoteSpan[] ss = s.getSpans(styleStart, position, QuoteSpan.class);
	
		                		for (int i = 0; i < ss.length; i++) {
		                				s.removeSpan(ss[i]);
		                        }
		                		s.setSpan(new QuoteSpan(), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		                	}
		                	if (underlineButton.isChecked()){
		                		UnderlineSpan[] ss = s.getSpans(styleStart, position, UnderlineSpan.class);
	
		                		for (int i = 0; i < ss.length; i++) {
		                				s.removeSpan(ss[i]);
		                        }
		                		s.setSpan(new UnderlineSpan(), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		                	}
		                	if (strikeButton.isChecked()){
		                		StrikethroughSpan[] ss = s.getSpans(styleStart, position, StrikethroughSpan.class);
	
		                		for (int i = 0; i < ss.length; i++) {
		                				s.removeSpan(ss[i]);
		                        }
		                		s.setSpan(new StrikethroughSpan(), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		                	}
	            		}
	            		
	            		cursorLoc = Selection.getSelectionStart(contentEdit.getText());
                	}
                } 
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { 
                        //unused
                } 
                public void onTextChanged(CharSequence s, int start, int before, int count) { 
                        //unused
                } 
                
});
            
            final ToggleButton boldButton = (ToggleButton) findViewById(R.id.bold);   
            
            boldButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                	 
                	formatBtnClick(boldButton, "strong");
                }
        });

            final Button linkButton = (Button) findViewById(R.id.link);   
            
            linkButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                	
                	TextView contentText = (TextView) findViewById(R.id.content);

                	int selectionStart = contentText.getSelectionStart();
                	
                	styleStart = selectionStart;
                	
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
            
            
            final ToggleButton emButton = (ToggleButton) findViewById(R.id.em);   
            
            emButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                	 
                	formatBtnClick(emButton, "em");
                }
        });
            
            final ToggleButton underlineButton = (ToggleButton) findViewById(R.id.underline);   
            
            underlineButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                	 
                	formatBtnClick(underlineButton, "u");
                }
        });
            
            final ToggleButton strikeButton = (ToggleButton) findViewById(R.id.strike);   
            
            strikeButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                	 
                	formatBtnClick(strikeButton, "strike");
                }
        });
            
            final ToggleButton bquoteButton = (ToggleButton) findViewById(R.id.bquote);   
            
            bquoteButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                	
                	formatBtnClick(bquoteButton, "blockquote");

                }
        });
            
            
            final Button cancelButton = (Button) findViewById(R.id.cancel);   
            
            cancelButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                	if (isNew){
                		boolean result = savePost();

        				if (result){

        					WordPressDB lddb = new WordPressDB(editPost.this);
        					int newID = -1;
        					if (isPage){
        						newID = lddb.getLatestPageDraftID(editPost.this, id);
        					}
        					else{
        						newID = lddb.getLatestDraftID(editPost.this, id);
        					}
        					Bundle bundle = new Bundle();
        					if (newID != -1){

        						if (isAction){
        							Intent mIntent = new Intent(editPost.this, tabView.class);
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
                	else {
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
                }
                
        });
            
            final Button clearPictureButton = (Button) findViewById(R.id.clearPicture);   
            
			clearPictureButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                	

			        imageUrl.clear();
			        thumbnailUrl.clear();
			        selectedImageIDs = new Vector<Uri>();
			        selectedImageCtr = 0;
			        imgThumbs.clear();
			        Gallery gallery = (Gallery) findViewById(R.id.gallery);
			        gallery.setVisibility(View.GONE);
			     	gallery.setAdapter(null);
			     	clearPictureButton.setVisibility(View.GONE);
                	         	
                }
        });            
          
			Button pubDate = (Button) findViewById(R.id.pubDateButton);
	        pubDate.setOnClickListener(new TextView.OnClickListener() {
	            public void onClick(View v) {
	            	
	            	// get the current date
	                Calendar c = Calendar.getInstance();
	                mYear = c.get(Calendar.YEAR);
	                mMonth = c.get(Calendar.MONTH);
	                mDay = c.get(Calendar.DAY_OF_MONTH);
	                mHour = c.get(Calendar.HOUR_OF_DAY);
	                mMinute = c.get(Calendar.MINUTE);
	            	
	            	showDialog(ID_DIALOG_DATE);
	            	         	
	            }
	    });   
            
}
    
    
	private void enableLBSButtons() {

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
	            		Toast.makeText(editPost.this, getResources().getText(R.string.location_toast), Toast.LENGTH_SHORT).show();
	            	}
	            	  

	            }
	        });
		
	        if (isNew && location){
	        	
	        	Button updateLocation = (Button) findViewById(R.id.updateLocation);
	        	updateLocation.setVisibility(View.GONE);
	        	
				lm = (LocationManager) getSystemService(LOCATION_SERVICE);
	    		
	    		lm.requestLocationUpdates(
			            LocationManager.GPS_PROVIDER, 
			            20000, 
			            0, 
			            editPost.this
			    );
				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 20000, 0, editPost.this);
				locationActive = true;
				
				RelativeLayout locationSection = (RelativeLayout) findViewById(R.id.section4);
	        	locationSection.setVisibility(View.VISIBLE);
	        }
	}


	protected void formatBtnClick(ToggleButton toggleButton, String tag) {
		EditText contentText = (EditText) findViewById(R.id.content);

    	int selectionStart = contentText.getSelectionStart();
    	
    	String startTag = "<" + tag + ">";
    	String endTag = "</" + tag + ">";
    	
    	styleStart = selectionStart;
    	
    	int selectionEnd = contentText.getSelectionEnd();
    	
    	if (selectionStart > selectionEnd){
    		int temp = selectionEnd;
    		selectionEnd = selectionStart;
    		selectionStart = temp;
    	}
    	
    	if (localDraft || isNew){             	
        	if (selectionEnd > selectionStart)
        	{
        		Spannable str = contentText.getText();
        		if (tag.equals("blockquote")){
        			

            		QuoteSpan[] ss = str.getSpans(selectionStart, selectionEnd, QuoteSpan.class);
        			
        			boolean exists = false;
            		for (int i = 0; i < ss.length; i++) {
            				str.removeSpan(ss[i]);
            				exists = true;
                    }
            		
            		if (!exists){
            			str.setSpan(new QuoteSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            		}
            		
            		toggleButton.setChecked(false);
        		}
        		else if (tag.equals("strong")){
        			StyleSpan[] ss = str.getSpans(selectionStart, selectionEnd, StyleSpan.class);
        			
        			boolean exists = false;
            		for (int i = 0; i < ss.length; i++) {
            				int style = ((StyleSpan) ss[i]).getStyle();
            				if (style == android.graphics.Typeface.BOLD)
            				{
            					str.removeSpan(ss[i]);
            					exists = true;
            				}
                    }
            		
            		if (!exists){
    					str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            		}
            		toggleButton.setChecked(false);
        		}
        		else if (tag.equals("em")){
        			StyleSpan[] ss = str.getSpans(selectionStart, selectionEnd, StyleSpan.class);
        			
        			boolean exists = false;
            		for (int i = 0; i < ss.length; i++) {
            				int style = ((StyleSpan) ss[i]).getStyle();
            				if (style == android.graphics.Typeface.ITALIC)
            				{
            					str.removeSpan(ss[i]);
            					exists = true;
            				}
                    }
            		
            		if (!exists){
    					str.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            		}
            		toggleButton.setChecked(false);
        		}
        		else if (tag.equals("u")){
        			

            		UnderlineSpan[] ss = str.getSpans(selectionStart, selectionEnd, UnderlineSpan.class);
        			
        			boolean exists = false;
            		for (int i = 0; i < ss.length; i++) {
            				str.removeSpan(ss[i]);
            				exists = true;
                    }
            		
            		if (!exists){
            			str.setSpan(new UnderlineSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            		}
            		
            		toggleButton.setChecked(false);
        		}
        		else if (tag.equals("strike")){
        			

            		StrikethroughSpan[] ss = str.getSpans(selectionStart, selectionEnd, StrikethroughSpan.class);
        			
        			boolean exists = false;
            		for (int i = 0; i < ss.length; i++) {
            				str.removeSpan(ss[i]);
            				exists = true;
                    }
            		
            		if (!exists){
            			str.setSpan(new StrikethroughSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            		}
            		
            		toggleButton.setChecked(false);
        		}
        	}
    	}
    	else{
    		String content = contentText.getText().toString();
    		if (selectionEnd > selectionStart)
        	{                		
        		contentText.setText(content.substring(0, selectionStart) + startTag + content.substring(selectionStart, selectionEnd) + endTag + content.substring(selectionEnd, content.length()));
        		
        		toggleButton.setChecked(false);
        		contentText.setSelection(selectionStart + content.substring(selectionStart, selectionEnd).length() + startTag.length() + endTag.length());
        	}
        	else if (toggleButton.isChecked()){
        		contentText.setText(content.substring(0, selectionStart) + startTag + content.substring(selectionStart, content.length()));
        		contentText.setSelection(selectionEnd + startTag.length());
        	}
        	else if (!toggleButton.isChecked()){
        		contentText.setText(content.substring(0, selectionStart) + endTag + content.substring(selectionStart, content.length()));
        		contentText.setSelection(selectionEnd + endTag.length());
        	}
    	}
		
	}
	
	private void addMedia(String imgPath, Uri curStream) {
		selectedImageIDs.add(selectedImageCtr, curStream);
		imageUrl.add(selectedImageCtr, imgPath);
		selectedImageCtr++;
		
		if (!imgPath.contains("video")){
		
		String[] projection = new String[] {
				Images.Thumbnails._ID,
				Images.Thumbnails.DATA,
				Images.Media.ORIENTATION
		};
		String orientation = "", path = "";
		Cursor cur = managedQuery(curStream, projection, null, null, null);
		File jpeg = null;
		if (cur != null){
			String thumbData = "";

			if (cur.moveToFirst()) {

				int dataColumn, orientationColumn;

				dataColumn = cur.getColumnIndex(Images.Media.DATA);
				orientationColumn = cur.getColumnIndex(Images.Media.ORIENTATION);

				thumbData = cur.getString(dataColumn);
				orientation = cur.getString(orientationColumn);
			}


			jpeg = new File(thumbData);
			path = thumbData;
		}
		else{
			path = curStream.toString().replace("file://", "");
			jpeg = new File(curStream.toString().replace("file://", ""));
			
		}
		
		imageTitle = jpeg.getName();
	 	   
	 	   byte[] finalBytes = null;
	 	  
	 	   byte[] bytes = new byte[(int) jpeg.length()];
	 	   
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
		
		imageHelper ih = imageHelper.getInstance();
		
		if (orientation == ""){
			orientation = ih.getExifOrientation(path, orientation);
		}

		imageTitle = jpeg.getName();
		
		finalBytes = ih.createThumbnail(bytes, "150", orientation, true);

		Bitmap resizedBitmap = BitmapFactory.decodeByteArray(finalBytes, 0, finalBytes.length); 
		imgThumbs.add(resizedBitmap);
		
		}
		else {
			imgThumbs.add("video");
		}

		Gallery gallery = (Gallery) findViewById(R.id.gallery);
		gallery.setVisibility(View.VISIBLE);
		gallery.setAdapter(new ImageAdapter(editPost.this));
		Button clearMedia = (Button) findViewById(R.id.clearPicture);
		clearMedia.setVisibility(View.VISIBLE);
		
	}


	public String submitPost() throws IOException {
		
		
		//grab the form data
        EditText titleET = (EditText)findViewById(R.id.title);
        String title = titleET.getText().toString();
        EditText contentET = (EditText)findViewById(R.id.content);
        String content;
        if (localDraft){
        	content = escapeUtils.unescapeHtml(Html.toHtml(contentET.getText()));
        }
        else{
        	content = contentET.getText().toString();
        }
        
        content = StringHelper.convertHTMLTagsForUpload(content);
        Boolean publishThis = false;
        String imageContent = "";
        boolean mediaError = false;
        if (selectedImageCtr > 0){  //did user add media to post?
        	//upload the images and return the HTML
        	String state = android.os.Environment.getExternalStorageState();
        	if(!state.equals(android.os.Environment.MEDIA_MOUNTED))  {
                //we need an SD card to submit media, stop this train!
        		mediaError = true;
            }
        	else{
        		imageContent =  uploadImages();
        	}

        }

        String res = "";
        if (!mediaError){
        	
    	Thread updateDialog = new Thread() 
     	{ 
     	  public void run() 
     	  {
     		 postingDialog.setMessage(getResources().getText((isPage) ? R.string.page_attempt_upload : R.string.post_attempt_upload));
     	  } 
     	}; 
     	this.runOnUiThread(updateDialog);
     	
        //before we do anything, validate that the user has entered settings
        boolean enteredSettings = checkSettings();
        
        if (!enteredSettings){
        	res = "invalidSettings";
        }
        else if (title.equals("") || (content.equals("") && selectedImageIDs.size() == 0))
        {
        	res = "emptyFields";
        }
        else {
        WordPressDB settingsDB = new WordPressDB(this);
    	Vector<?> categoriesVector = settingsDB.loadSettings(this, id);   	
        
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
    		String sHttpuser = categoriesVector.get(4).toString();
    		String sHttppassword = categoriesVector.get(5).toString();
    		String sImagePlacement = categoriesVector.get(6).toString();
    		String sCenterThumbnailString = categoriesVector.get(7).toString();

    		if (sCenterThumbnailString.equals("1")){
    			centerThumbnail = true;
    		}

    		Spinner spinner = (Spinner) findViewById(R.id.status);
            int selectedStatus = spinner.getSelectedItemPosition();
            String status = "";
            switch (selectedStatus){
            	case 0:
            		status = "draft";
            		break;
            	case 1:
            		if (isPage){
            			status = "private";
            		}
            		else {
            			status = "pending";
            		}
            		break;
            	case 2:
            		if (isPage) {
            			status = "publish";
            		}
            		else {
            			status = "private";
            		}
            		break;
            	case 3:
            		status = "publish";
            		break;
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
        
        EditText passwordET = (EditText)findViewById(R.id.post_password);
        String wpPassword = passwordET.getText().toString();
        
        contentStruct.put("post_type", "post");
        contentStruct.put("title", title);
        contentStruct.put("description", content);
        contentStruct.put("post_status", status);
        
        TextView tvPubDate = (TextView) findViewById(R.id.pubDate);
        String pubDate = tvPubDate.getText().toString();

        if (!pubDate.equals(getResources().getText(R.string.immediately))){
        	SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
        	Date d = new Date();
        	try {
				d = sdf.parse(pubDate);
				contentStruct.put("date_created_gmt", d);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (java.text.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        if(wpPassword != null && !"".equals(wpPassword)) {
        	contentStruct.put("wp_password", wpPassword); 
        }
        if (!isPage){
        	EditText tagsET = (EditText)findViewById(R.id.tags);
            String tags = tagsET.getText().toString();
            // categoryID = getCategoryId(selectedCategory);
            String[] theCategories = categories.split(",");
        if (tags != ""){
        contentStruct.put("mt_keywords", tags);
        }
        if (theCategories.length > 0){
        contentStruct.put("categories", theCategories);
        }
        }
      
        client = new XMLRPCClient(sURL, sHttpuser, sHttppassword);
        
        Object[] params = {
        		postID,
        		sUsername,
        		sPassword,
        		contentStruct,
        		publishThis
        };
        
        Object result = null;
        boolean success = false;
        try {
			result = (Object) client.call("metaWeblog.editPost", params);
			success = true;
		} catch (final XMLRPCException e) {
			Thread prompt = new Thread() 
			{ 
			  public void run() 
			  {
				dismissDialog(ID_DIALOG_POSTING);
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
				  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
	              dialogBuilder.setMessage(e.getMessage());
	              dialogBuilder.setPositiveButton("OK",  new
	            		  DialogInterface.OnClickListener() {
	              public void onClick(DialogInterface dialog, int whichButton) {
	                  // Just close the window.
	              	}
	              });
	              dialogBuilder.setCancelable(true);
	              if (!isFinishing()){
	            	  dialogBuilder.create().show();
	              }
			  } 
			}; 
			this.runOnUiThread(prompt);
		}
		
		if (success){

				newID = result.toString();
				res = "OK";
		}
		else{
			res = "FAIL";
		}
			

        }// if/then for valid settings
        
		
        }
        else {
        	Thread prompt = new Thread() 
    		{ 
    		  public void run() 
    		  {
    			dismissDialog(ID_DIALOG_POSTING);
    			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
    			  dialogBuilder.setTitle(getResources().getText(R.string.sdcard_title));
                  dialogBuilder.setMessage(getResources().getText(R.string.sdcard_message));
                  dialogBuilder.setPositiveButton("OK",  new
                		  DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                	  //just close the dialog
                  	}

                  });
                  dialogBuilder.setCancelable(true);
                 dialogBuilder.create().show();
    		  } 
    		}; 
    		this.runOnUiThread(prompt);
    		res = "mediaError";
        }
        return res;
	}

	@SuppressWarnings("unchecked")
	public String uploadImages(){
		
	    String content = "";
	    
	    //images variables
	    String finalThumbnailUrl = null;
	    String finalImageUrl = null;
	    
	    //get the settings
	    WordPressDB settingsDB = new WordPressDB(this);
		Vector<?> categoriesVector = settingsDB.loadSettings(this, id);   	
		
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
			String sHttpuser = categoriesVector.get(4).toString();
			String sHttppassword = categoriesVector.get(5).toString();
			String sImagePlacement = categoriesVector.get(6).toString();
			String sCenterThumbnailString = categoriesVector.get(7).toString();
			String sFullSizeImageString = categoriesVector.get(8).toString();
			boolean sFullSizeImage  = false;
			if (sFullSizeImageString.equals("1")){
				sFullSizeImage = true;
			}

			boolean centerThumbnail = false;
			if (sCenterThumbnailString.equals("1")){
				centerThumbnail = true;
			}
			String sMaxImageWidth = categoriesVector.get(9).toString();
			
	    //new loop for multiple images
	    
	    for (int it = 0; it < selectedImageCtr; it++){
	    	final int printCtr = it;
	    	Thread prompt = new Thread() 
	     	{ 
	     	  public void run() 
	     	  {
	     		  postingDialog.setMessage("Uploading Media File #" + String.valueOf(printCtr + 1));
	     		  postingDialog.setProgress(postingDialog.getProgress() + (100 / (selectedImageCtr + 1)));
	     	  } 
	     	}; 
	     	this.runOnUiThread(prompt);
	    //check for image, and upload it
	    if (imageUrl.get(it) != null)
	    {
	       client = new XMLRPCClient(sURL, sHttpuser, sHttppassword);
	 	   
	 	   String curImagePath = "";
	 	   
	 	  curImagePath = imageUrl.get(it).toString();
	 	 boolean video = false;
	 	  if (curImagePath.contains("video")){
	 		  video = true;
	 	  }
	 	   
	 	  if (video){ //upload the video
	  	   
	  	   Uri videoUri = Uri.parse(curImagePath);
	  	   File fVideo = null;
	  	   String mimeType = "";
	  	   MediaFile mf = null;
	  	  
	  	   if (videoUri.toString().contains("content:")){ //file is in media library
	 		 	   
	 		 	  String[] projection; 
	 		 	 Uri imgPath;

	 			 	  projection = new String[] {
	 			       		    Video.Media._ID,
	 			       		    Video.Media.DATA,
	 			       		    Video.Media.MIME_TYPE
	 			       		};
	 			 	  imgPath = videoUri;

	 		 	  Cursor cur = this.managedQuery(imgPath, projection, null, null, null);
	 		 	  String thumbData = "";
	 		 	 
	 		 	  if (cur.moveToFirst()) {
	 		 		  
	 		 		int dataColumn, mimeTypeColumn;

		 	        dataColumn = cur.getColumnIndex(Video.Media.DATA);
		 	        mimeTypeColumn = cur.getColumnIndex(Video.Media.MIME_TYPE);
          	            
	 		       mf = new MediaFile();
	 		       
	 		       thumbData = cur.getString(dataColumn);
	 		       mimeType = cur.getString(mimeTypeColumn);
	 		       fVideo = new File(thumbData);
	 				mf.setFilePath(fVideo.getPath());

	 		 	  }
	  	   }
	  	   else{ //file is not in media library
	  		   fVideo = new File(videoUri.toString().replace("file://", ""));
	  	   }
	  	   
	  	   imageTitle = fVideo.getName();

	         //try to upload the video
	         Map<String, Object> m = new HashMap<String, Object>();
	         
	         m.put("name", imageTitle);
	         m.put("type", mimeType);
	         m.put("bits", mf);
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
	 			mediaErrorMsg = e.getMessage();
	 			xmlrpcError = true;
	 			break;
	 		}
	 				
	 				HashMap<Object, Object> contentHash = (HashMap<Object, Object>) result;

	 				String resultURL = contentHash.get("url").toString();
	 				if (contentHash.containsKey("videopress_shortcode")){
	 					resultURL = contentHash.get("videopress_shortcode").toString() + "<br />";
	 				}
	 				else{
	 					resultURL = "<a type=\"" + mimeType + "\" href=\"" + resultURL + "\">View Video</a><br />";
	 				}
	 				
	 				content = content + resultURL;

	 	  } //end video
	 	  else{
	 	   for (int i = 0; i < 2; i++){
	 
	 		 curImagePath = imageUrl.get(it).toString();
	 		   
	 		if (i == 0 || sFullSizeImage)
	 		{
	 	   
	 	   Uri imageUri = Uri.parse(curImagePath);
	 	   File jpeg = null;
	 	   String mimeType = "", orientation = "";
	 	   MediaFile mf = null;
	 	 
	 	   if (imageUri.toString().contains("content:")){ //file is in media library
			 	   
			 	 String[] projection; 
			 	 Uri imgPath;

				 	  projection = new String[] {
				       		    Images.Media._ID,
				       		    Images.Media.DATA,
				       		    Images.Media.MIME_TYPE,
				       		    Images.Media.ORIENTATION
				       		};
				 	  //imgPath = ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, imgID2);
				 	  imgPath = imageUri;

			 	  Cursor cur = this.managedQuery(imgPath, projection, null, null, null);
			 	  String thumbData = "";
			 	 
			 	  if (cur.moveToFirst()) {
			 		  
			 		int dataColumn, mimeTypeColumn, orientationColumn;

			 	        dataColumn = cur.getColumnIndex(Images.Media.DATA);
			 	        mimeTypeColumn = cur.getColumnIndex(Images.Media.MIME_TYPE);
			 	       orientationColumn = cur.getColumnIndex(Images.Media.ORIENTATION);
	              	            
			       mf = new MediaFile();
			       orientation = cur.getString(orientationColumn);
			       thumbData = cur.getString(dataColumn);
			       mimeType = cur.getString(mimeTypeColumn);
			       jpeg = new File(thumbData);
				   mf.setFilePath(jpeg.getPath());

			 	  }
	 	   }
	 	   else{ //file is not in media library
	 		   String path = imageUri.toString().replace("file://", "");
	 		   jpeg = new File(path);
	 		   mf = new MediaFile();
	 		   mf.setFilePath(path);
	 	   }
	 	   
	 	   imageTitle = jpeg.getName();
	 	   
	 	   byte[] finalBytes = null;
	 	   
	 	   if (i == 0){
			 	byte[] bytes = new byte[(int) jpeg.length()];
			 	   
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
				
				imageHelper ih = imageHelper.getInstance();
				finalBytes = ih.createThumbnail(bytes, sMaxImageWidth, orientation, false);
	 	   }

	        //try to upload the image
	        Map<String, Object> m = new HashMap<String, Object>();
	
	        m.put("name", imageTitle);
	        m.put("type", mimeType);
	        if (i == 0){
	        	m.put("bits", finalBytes);
	        }
	        else {
	        	m.put("bits", mf);
	        }
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
	 	   
	 	  }
	 	   
	    }//end image stuff
	    }//end new for loop

	    return content;
	}

	
	public boolean checkSettings(){
		//see if the user has any saved preferences
		 WordPressDB settingsDB = new WordPressDB(this);
	    	Vector<?> categoriesVector = settingsDB.loadSettings(this, id);
	    	String sURL = null, sUsername = null, sPassword = null;
	    	if (categoriesVector != null){
	    		sURL = categoriesVector.get(0).toString();
	    		sUsername = categoriesVector.get(2).toString();
	    		sPassword = categoriesVector.get(3).toString();
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
				final Object result;
				result = (Object) client.call(method, params);
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
								  if (!isFinishing()){
									  dialogBuilder.create().show();
								  }
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
									  if (!isFinishing()) {
										  dialogBuilder.create().show();
									  }
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
				final Object result;
				result = (Object) client.call(method, params);
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
			              if (!isFinishing()) {
			            	  dialogBuilder.create().show();
			              }
			             
					
			} catch (final XMLRPCException e) {
				
				handler.post(new Runnable() {
					public void run() {
						
						Throwable couse = e.getCause();
						if (couse instanceof HttpHostConnectException) {
							//pd.dismiss();
							dismissDialog(editPost.this.ID_DIALOG_POSTING);
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
				              if (!isFinishing()) {
				            	  dialogBuilder.create().show();
				              }
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
			boolean isVideo = false;
			ViewHolder holder;
			if (convertView == null) {  // if it's not recycled, initialize some attributes
				convertView = new ImageView(mContext);
				holder = new ViewHolder();
				
				holder.imageView = (ImageView) convertView;
				
				int width, height;
				if (isLargeScreen){
					width =  240;
					height = 160;
				}
				else{
					width = 125;
					height = 100;
				}
				holder.imageView.setLayoutParams(new Gallery.LayoutParams(width,height));
				holder.imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
				holder.imageView.setBackgroundResource(R.drawable.wordpress_gallery_background);
	    		
				Uri tempURI = (Uri) selectedImageIDs.get(position);

				if (!tempURI.toString().contains("video")){

					
				}
				else{
					holder.imageView.setImageDrawable(getResources().getDrawable(R.drawable.video));
					isVideo = true;
				}
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			if (!isVideo){
				holder.imageView.setImageBitmap((Bitmap) imgThumbs.get(position));
			}
			
			//holder.imageView.setImageDrawable(getResources().getDrawable(R.drawable.video));
			
			return convertView;
			
		}
		
		class ViewHolder {
            ImageView imageView;
        }

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (data != null || requestCode == 4)
		{
			Bundle extras;
		switch(requestCode) {
		case 0:
			extras = data.getExtras();
		    break;
		case 1:
		    break;
		case 2:
			extras = data.getExtras();
			String linkText = extras.getString("linkText");
			if (linkText.equals("http://") != true){
				
			
			if (linkText.equals("CANCEL") != true){

			EditText contentText = (EditText) findViewById(R.id.content);

        	int selectionStart = contentText.getSelectionStart();
        	
        	int selectionEnd = contentText.getSelectionEnd();
        	
        	if (selectionStart > selectionEnd){
        		int temp = selectionEnd;
        		selectionEnd = selectionStart;
        		selectionStart = temp;
        	}
        	
    		Spannable str = contentText.getText();
			str.setSpan(new URLSpan(linkText),  selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			}
			break;			
		case 3:
 
		    Uri imageUri = data.getData();
		    String imgPath = imageUri.toString();
   
		    addMedia(imgPath, imageUri);
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
						  
						int dataColumn;

						    dataColumn = cur.getColumnIndex(Images.Media.DATA);
							            

  		       thumbData = cur.getString(dataColumn);
  		       f = new File(thumbData);
  		 	  }
					} catch (Exception e) {
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
	            		dialogBuilder.setTitle(getResources().getText(R.string.error));
	                    dialogBuilder.setMessage(e.getMessage());
	                  dialogBuilder.setPositiveButton("OK",  new
	                		  DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog, int whichButton) {
	                            // just close the dialog
	                        }
	                    });
	                  dialogBuilder.setCancelable(true);
	                  if (!isFinishing()) {
	                	  dialogBuilder.create().show();
	                  }
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
                    
                    addMedia(capturedImage.toString(), capturedImage);
       
                } catch (FileNotFoundException e) {
                	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
            		dialogBuilder.setTitle(getResources().getText(R.string.file_error));
                    dialogBuilder.setMessage(getResources().getText(R.string.file_error_encountered));
                  dialogBuilder.setPositiveButton("OK",  new
                		  DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // just close the dialog
                        }
                    });
                  dialogBuilder.setCancelable(true);
                  if (!isFinishing()) {
                	  dialogBuilder.create().show();
                  }
                }

        }
        else {
        	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
    		dialogBuilder.setTitle(getResources().getText(R.string.file_error));
            dialogBuilder.setMessage(getResources().getText(R.string.file_error_encountered));
          dialogBuilder.setPositiveButton("OK",  new
        		  DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // just close the dialog
                }
            });
          dialogBuilder.setCancelable(true);
          if (!isFinishing()) {
        	  dialogBuilder.create().show();
          }
        }

		     	break;
		     	
		case 5:
			extras = data.getExtras();
			String cats = extras.getString("selectedCategories");
			categories = cats;
			TextView selectedCategoriesTV = (TextView) findViewById(R.id.selectedCategories);
			selectedCategoriesTV.setText(getResources().getText(R.string.selected_categories) + " " + cats);
	     	break;
		case 6:
			 
		    Uri videoUri = data.getData();
		    String videoPath = videoUri.toString();
   
		    addMedia(videoPath, videoUri);
		    
	     	 break;
		case 7:
			if (resultCode == Activity.RESULT_OK) {
                Uri capturedVideo = data.getData();
                
                addMedia(capturedVideo.toString(), capturedVideo);
    }
    else {
    	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
		dialogBuilder.setTitle(getResources().getText(R.string.file_error));
        dialogBuilder.setMessage(getResources().getText(R.string.file_error_encountered));
      dialogBuilder.setPositiveButton("OK",  new
    		  DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // just close the dialog
            }
        });
      dialogBuilder.setCancelable(true);
      if (!isFinishing()) {
    	  dialogBuilder.create().show();
      }
    }

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
				  dialogBuilder.setTitle(getResources().getText((isPage) ? R.string.page_edited : R.string.post_edited));
				  if (xmlrpcError){
					  dialogBuilder.setMessage(getResources().getText((isPage) ? R.string.page_edited_image_error : R.string.post_edited_image_error) + ": " + mediaErrorMsg);  
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
	              if (!isFinishing()) {
	            	  dialogBuilder.create().show();
	              }
			}
			else if (finalResult.equals("FAIL")){
				dismissDialog(editPost.this.ID_DIALOG_POSTING);	
			}
		}
	};
	
	@Override
	protected Dialog onCreateDialog(int id) {
	if(id == ID_DIALOG_POSTING){
		postingDialog = new ProgressDialog(this);
		postingDialog.setTitle(getResources().getText(R.string.uploading_content));
		postingDialog.setMessage(getResources().getText((isPage) ? R.string.attempting_edit_page : R.string.attempting_edit_post));
		postingDialog.setIndeterminate(true);
		postingDialog.setCancelable(true);
		return postingDialog;
	}
	else if(id == ID_DIALOG_LOADING){
		ProgressDialog loadingDialog = new ProgressDialog(this);
		loadingDialog.setMessage(getResources().getText(R.string.loading));
		loadingDialog.setIndeterminate(true);
		loadingDialog.setCancelable(true);
		return loadingDialog;
	}
	else if (id == ID_DIALOG_DATE){
		DatePickerDialog dpd = new DatePickerDialog(this,
                mDateSetListener,
                mYear, mMonth, mDay);
		dpd.setTitle("");
		return dpd;
	}
	else if (id == ID_DIALOG_TIME){
		TimePickerDialog tpd = new TimePickerDialog(this,
                mTimeSetListener,
                mHour, mMinute, false);
		tpd.setTitle("");
		return tpd;
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
        String content = escapeUtils.unescapeHtml(Html.toHtml(contentET.getText()));
        EditText passwordET = (EditText)findViewById(R.id.post_password);
        String password = passwordET.getText().toString();
        //replace duplicate <p> tags so there's not duplicates, trac #86
        content = content.replace("<p><p>", "<p>");
        content = content.replace("</p></p>", "</p>");
        content = content.replace("<br><br>", "<br>");
        
        TextView tvPubDate = (TextView) findViewById(R.id.pubDate);
        String pubDate = tvPubDate.getText().toString();

    	long pubDateTimestamp = 0;
        if (!pubDate.equals(getResources().getText(R.string.immediately))){
        	SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
        	Date d = new Date();
        	try {
				d = sdf.parse(pubDate);
				pubDateTimestamp = d.getTime();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (java.text.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
        }
        
        String tags = "";
        if (!isPage){
        	EditText tagsET = (EditText)findViewById(R.id.tags);
        	tags = tagsET.getText().toString();
        }

        String images = "";
        boolean success = false;
        
        //before we do anything, validate that the user has entered settings
        boolean enteredSettings = checkSettings();
        
        if (!enteredSettings){
        	finalResult = "invalidSettings";
        }
        else if (title.equals("") || (content.equals("") && selectedImageIDs.size() == 0))
        {
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
        else {
        
        	//update the images
        	for (int it = 0; it < selectedImageCtr; it++){
           
        		images += selectedImageIDs.get(it).toString() + ",";

        	}
        	
            Spinner spinner = (Spinner) findViewById(R.id.status);
            int selectedStatus = spinner.getSelectedItemPosition();
            String status = "";
            switch (selectedStatus){
            	case 0:
            		status = "draft";
            		break;
            	case 1:
            		if (isPage){
            			status = "private";
            		}
            		else {
            			status = "pending";
            		}
            		break;
            	case 2:
            		if (isPage) {
            			status = "publish";
            		}
            		else {
            			status = "private";
            		}
            		break;
            	case 3:
            		status = "publish";
            		break;
            }
        
        	//Geotagging
        	WordPressDB settingsDB = new WordPressDB(this);
        	Vector<?> settingsVector = settingsDB.loadSettings(this, id);   	
        	
    		String sLocation = settingsVector.get(13).toString();
    		
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
        			e.printStackTrace();
        		}

        	}
        
        	//new feature, automatically save a post as a draft just in case the posting fails
            WordPressDB lDraftsDB = new WordPressDB(this);
        	if (isPage){
        		if (isNew){
        			success = lDraftsDB.saveLocalPageDraft(this, id, title, content, images, status, password, pubDateTimestamp);
        		}
        		else {
        			success = lDraftsDB.updateLocalPageDraft(this, id, postID, title, content, images, status, password, pubDateTimestamp);
        		}
        	}
        	else {
        		if (isNew){
        			success = lDraftsDB.saveLocalDraft(this, id, title, content, images, tags, categories, status, latitude, longitude, password, pubDateTimestamp);
        		}
        		else {
        			success = lDraftsDB.updateLocalDraft(this, id, postID, title, content, images, tags, categories, status, latitude, longitude, password, pubDateTimestamp);
        		}
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
            	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(editPost.this);
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

	}

	/** Stop the updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
		if (!isPage && location && locationActive){
			lm.removeUpdates(this);
		}
	}

	@Override
	protected void onDestroy() {
		super.onPause();
		if (!isPage && location && locationActive){
			lm.removeUpdates(this);
		}
	}

	public void onLocationChanged(Location location) {
		curLocation = location;
		new getAddressTask().execute(location.getLatitude(), location.getLongitude());
		lm.removeUpdates(this);
	}

	public void onProviderDisabled(String provider) {
		
	}

	public void onProviderEnabled(String provider) {
		
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		
	}
	
	private class getAddressTask extends AsyncTask<Double, Void, String> {

	     protected void onPostExecute(String result) {
	    	 TextView map = (TextView) findViewById(R.id.locationText); 
	    	 map.setText(result);
	     }
	     @Override
		protected String doInBackground(Double... args) {
			Geocoder gcd = new Geocoder(editPost.this, Locale.getDefault());
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
	
	protected void setContent() {
		Intent intent = getIntent();
		String text = intent.getStringExtra(Intent.EXTRA_TEXT);
		String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
		if (text != null) {
			EditText titleET = (EditText) findViewById(R.id.title);
			
			if (title != null){
				titleET.setText(title);
			}
			
			EditText contentET = (EditText) findViewById(R.id.content);
			//It's a youtube video link! need to strip some parameters so the embed will work
			if (text.contains("youtube_gdata")){
				text = text.replace("feature=youtube_gdata", "");
				text = text.replace("&", "");
				text = text.replace("_player", "");
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
					finalText+="<a href=\"" + url + "\">"+ url + "</a> ";  
					contentET.setText(Html.fromHtml(finalText));
					isUrl = true;
				} catch (MalformedURLException e) {
					finalText+= item + " ";
					contentET.setText(finalText);
				}
			}
		}
		else {
			String action = intent.getAction();
			final String type = intent.getType();
			final ArrayList<Uri> multi_stream;
			if (Intent.ACTION_SEND_MULTIPLE.equals(action)){
				multi_stream = intent.getParcelableArrayListExtra((Intent.EXTRA_STREAM));
			}
			else{
				multi_stream = new ArrayList<Uri>();
				multi_stream.add((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
			}
	
			Vector<Serializable> params = new Vector<Serializable>();
			params.add(multi_stream);
			params.add(type);
			new processAttachmentsTask().execute(params);
		}
		

	}
	
private class processAttachmentsTask extends AsyncTask<Vector<?>, Void, Boolean> {
		
		protected void onPreExecute(){

			showDialog(ID_DIALOG_LOADING);
		}

		protected void onPostExecute(Boolean result) {
			dismissDialog(ID_DIALOG_LOADING);
			Gallery gallery = (Gallery) findViewById(R.id.gallery);
			gallery.setVisibility(View.VISIBLE);
			gallery.setAdapter(new ImageAdapter(editPost.this));
			Button clearMedia = (Button) findViewById(R.id.clearPicture);
			clearMedia.setVisibility(View.VISIBLE);
		}
		@Override
		protected Boolean doInBackground(Vector<?>... args) {
			ArrayList<?> multi_stream = (ArrayList<?>) args[0].get(0);
			String type = (String) args[0].get(1);
			for (int i=0; i < multi_stream.size(); i++) {
			    Uri curStream = (Uri) multi_stream.get(i);
			    if (curStream != null && type != null) {
					String imgPath = curStream.getEncodedPath();
					
					addMedia(imgPath, curStream, true);
					
				}
			}
			return true;
		}
	}

private void addMedia(String imgPath, Uri curStream, boolean noUI) {
	selectedImageIDs.add(selectedImageCtr, curStream);
	imageUrl.add(selectedImageCtr, imgPath);
	selectedImageCtr++;
	
	if (!imgPath.contains("video")){
	
	String[] projection = new String[] {
			Images.Thumbnails._ID,
			Images.Thumbnails.DATA,
			Images.Media.ORIENTATION
	};
	String orientation = "", path = "";
	Cursor cur = managedQuery(curStream, projection, null, null, null);
	File jpeg = null;
	if (cur != null){
		String thumbData = "";

		if (cur.moveToFirst()) {

			int dataColumn, orientationColumn;

			dataColumn = cur.getColumnIndex(Images.Media.DATA);
			orientationColumn = cur.getColumnIndex(Images.Media.ORIENTATION);

			thumbData = cur.getString(dataColumn);
			orientation = cur.getString(orientationColumn);
		}


		jpeg = new File(thumbData);
		path = thumbData;
	}
	else{
		path = curStream.toString().replace("file://", "");
		jpeg = new File(curStream.toString().replace("file://", ""));
		
	}
	
	imageTitle = jpeg.getName();
 	   
 	   byte[] finalBytes = null;
 	  
 	   byte[] bytes = new byte[(int) jpeg.length()];
 	   
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
	
	imageHelper ih = imageHelper.getInstance();
	
	orientation = ih.getExifOrientation(path, orientation);

	imageTitle = jpeg.getName();
	
	finalBytes = ih.createThumbnail(bytes, "150", orientation, true);

	Bitmap resizedBitmap = BitmapFactory.decodeByteArray(finalBytes, 0, finalBytes.length); 
	imgThumbs.add(resizedBitmap);
	
	}
	else {
		imgThumbs.add("video");
	}
	if (!noUI){
	Gallery gallery = (Gallery) findViewById(R.id.gallery);
	gallery.setVisibility(View.VISIBLE);
	gallery.setAdapter(new ImageAdapter(editPost.this));
	Button clearMedia = (Button) findViewById(R.id.clearPicture);
	clearMedia.setVisibility(View.VISIBLE);
	}
	
}

protected void lbsCheck() {
	WordPressDB settingsDB = new WordPressDB(editPost.this);
	Vector<?> settingsVector = settingsDB.loadSettings(editPost.this, id);   	

	String sLocation = settingsVector.get(13).toString();

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
		
		if (isAction){
			enableLBSButtons();
		}
	}

}

private DatePickerDialog.OnDateSetListener mDateSetListener =
    new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			mYear = year;
			mMonth = monthOfYear;
			mDay = dayOfMonth;
			
			showDialog(ID_DIALOG_TIME);
			
		}
    };
    
    
	
private TimePickerDialog.OnTimeSetListener mTimeSetListener =
    new TimePickerDialog.OnTimeSetListener() {

		public void onTimeSet(TimePicker view, int hour, int minute) {
			mHour = hour;
			mMinute = minute;
			String AMPM = "AM";
			if (mHour >= 12){
				AMPM = "PM";
				if (mHour > 12){
					mHour -= 12;
				}
			}
			TextView pubDate = (TextView) findViewById(R.id.pubDate);
			String[] shortMonths = new DateFormatSymbols().getShortMonths();
			pubDate.setText(shortMonths[mMonth] + " " + String.format("%02d", mDay) + ", " + mYear + " " + String.format("%02d", mHour) + ":" + String.format("%02d", mMinute) + " " + AMPM);
			
			isCustomPubDate = true;
		}

    };
    
}





