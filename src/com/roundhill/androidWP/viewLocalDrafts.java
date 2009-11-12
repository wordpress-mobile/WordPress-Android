//by Dan Roundhill, danroundhill.com/wptogo
package com.roundhill.androidWP;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.http.conn.HttpHostConnectException;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore.Images;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


public class viewLocalDrafts extends ListActivity {
    /** Called when the activity is first created. */
	private XMLRPCClient client;
	private Integer[] postIDs;
	private String[] titles;
	private Integer[] publish;
	private Integer[] uploaded;
	private String id = "";
	private String accountName = "";
	Vector postNames = new Vector();
	int selectedID = 0;
	int rowID = 0;
	private int ID_DIALOG_POSTING = 1;
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
	private Vector<Uri> selectedImageIDs = new Vector();
	private int selectedImageCtr = 0;
    private String newID = "";
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
    public String submitResult = "";
	
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
      
        setContentView(R.layout.viewlocaldrafts);
        
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         accountName = extras.getString("accountName");
        }
        
        this.setTitle(escapeUtils.unescapeHtml(accountName) + " - Local Drafts");
        //query for posts and refresh view


        boolean loadedPosts = loadPosts();
        

         /*   final customImageButton addNewPost = (customImageButton) findViewById(R.id.newPost);   
            
            addNewPost.setOnClickListener(new customImageButton.OnClickListener() {
                public void onClick(View v) {
                	
                	Intent i = new Intent(viewLocalDrafts.this, newPost.class);
                	i.putExtra("accountName", accountName);
 	                i.putExtra("id", id);
 	                startActivityForResult(i, 0);
                	 
                }
        });
            
            
final customImageButton moderate = (customImageButton) findViewById(R.id.moderate);   
            
            moderate.setOnClickListener(new customImageButton.OnClickListener() {
                public void onClick(View v) {
                	
                	Intent i = new Intent(viewLocalDrafts.this, moderateComments.class);
                	i.putExtra("accountName", accountName);
 	                i.putExtra("id", id);
 	                startActivityForResult(i, 0);
                	 
                }
        });*/
        
        
    }
    
    private boolean loadPosts(){ //loads posts from the db
   	
       localDraftsDB lDraftsDB = new localDraftsDB(this);
   	Vector loadedPosts = lDraftsDB.loadPosts(viewLocalDrafts.this, id);
   	if (loadedPosts != null){
   	postIDs = new Integer[loadedPosts.size()];
   	titles = new String[loadedPosts.size()];
   	publish = new Integer[loadedPosts.size()];
   	uploaded = new Integer[loadedPosts.size()];
   	
					    for (int i=0; i < loadedPosts.size(); i++){
					        HashMap contentHash = (HashMap) loadedPosts.get(i);
					        postIDs[i] = (Integer) contentHash.get("id");
					        titles[i] = escapeUtils.unescapeHtml(contentHash.get("title").toString());
					        publish[i] = (Integer) contentHash.get("publish");
					        uploaded[i] = (Integer) contentHash.get("uploaded");
					    }
					   
			        
					   setListAdapter(new CommentListAdapter(viewLocalDrafts.this));
					
					   ListView listView = (ListView) findViewById(android.R.id.list);
					   listView.setSelector(R.layout.list_selector);
					   
					   listView.setOnItemClickListener(new OnItemClickListener() {

							public void onNothingSelected(AdapterView<?> arg0) {
								
							}

							public void onItemClick(AdapterView<?> arg0, View arg1,
									int arg2, long arg3) {
								Intent intent = new Intent(viewLocalDrafts.this, editPost.class);
			                    intent.putExtra("postID", postIDs[(int) arg3]);
			                    intent.putExtra("postTitle", titles[(int) arg3]);
			                    intent.putExtra("id", id);
			                    intent.putExtra("accountName", accountName);
			                    intent.putExtra("localDraft", true);
			                    startActivity(intent);
								
							}

			            });
					   
	        listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

	               public void onCreateContextMenu(ContextMenu menu, View v,
						ContextMenuInfo menuInfo) {
					// TODO Auto-generated method stub
	            	   AdapterView.AdapterContextMenuInfo info;
	                   try {
	                        info = (AdapterView.AdapterContextMenuInfo) menuInfo;
	                   } catch (ClassCastException e) {
	                       //Log.e(TAG, "bad menuInfo", e);
	                       return;
	                   }
	                   
	                   
	                   
	                   selectedID = info.targetView.getId();
	                   rowID = info.position;
	                   
				 menu.setHeaderTitle("Post Actions");
                 menu.add(0, 0, 0, "Edit Draft");
                 menu.add(0, 1, 0, "Upload Draft to Blog");
                 menu.add(0, 2, 0, "Delete Draft");
				}
	          });
   	
	return true;
    }
   	else{
   		return false;
   	}
   }

    private class CommentListAdapter extends BaseAdapter {
        public CommentListAdapter(Context context) {
            mContext = context;
        }

        /**
         * The number of items in the list is determined by the number of speeches
         * in our array.
         * 
         * @see android.widget.ListAdapter#getCount()
         */
        public int getCount() {
            return postIDs.length;
        }

        /**
         * Since the data comes from an array, just returning the index is
         * sufficent to get at the data. If we were using a more complex data
         * structure, we would return whatever object represents one row in the
         * list.
         * 
         * @see android.widget.ListAdapter#getItem(int)
         */
        public Object getItem(int position) {
            return position;
        }

        /**
         * Use the array index as a unique id.
         * 
         * @see android.widget.ListAdapter#getItemId(int)
         */
        public long getItemId(int position) {
            return position;
        }
        
        public long getListId(int position){
        	return postIDs[position];
        }

        /**
         * Make a SpeechView to hold each row.
         * 
         * @see android.widget.ListAdapter#getView(int, android.view.View,
         *      android.view.ViewGroup)
         */
        public View getView(int position, View convertView, ViewGroup parent) {
        	
            CommentView cv;
            if (convertView == null) {
                cv = new CommentView(mContext, postIDs[position], titles[position], publish[position]);
            } else {
                cv = (CommentView) convertView;
                cv.setId(postIDs[position]);
                cv.setTitle(titles[position]);
                cv.setPublished(publish[position]);
            }

            return cv;
        }

        /**
         * Remember our context so we can use it when constructing views.
         */
        private Context mContext;
        
        /**
         * Our data, part 1.
         */
    }

    private class CommentView extends RelativeLayout {
        public CommentView(Context context, final Integer postID, String title, Integer published) {
            super(context);

            
            this.setPadding(4, 4, 4, 4);
            this.setId(postID);

            // Here we build the child views in code. They could also have
            // been specified in an XML file.
            
            tvTitle = new TextView(context);
            tvTitle.setText(title);
            tvTitle.setTextSize(20);
            tvTitle.setTextColor(Color.parseColor("#444444"));
            tvTitle.setGravity(Gravity.LEFT);
            tvTitle.setId(100001);
            
            RelativeLayout.LayoutParams param2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);  
            //param2.addRule(RelativeLayout.LEFT_OF,100000);
            //param2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            
            addView(tvTitle, param2);
            
            tvPublish = new TextView(context);
            if (published == 1){
            tvPublish.setTextColor(Color.parseColor(("#73B646")));
            tvPublish.setText("Publish");
            }
            else{
            	tvPublish.setTextColor(Color.parseColor(("#ABABAB")));
            	tvPublish.setText("Will not publish");
            }
            
            tvPublish.setTextSize(12);
            
            RelativeLayout.LayoutParams param3 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);  
            param3.addRule(RelativeLayout.BELOW,100001);
            
            addView(tvPublish, param3);
            
            
       
        }

        /**
         * Convenience method to set the title of a postView
         */
        public void setTitle(String titleName) {
            tvTitle.setText(titleName);
        }
        
        public void setPublished(Integer published) {
        	if (published == 1){
                tvPublish.setTextColor(Color.parseColor(("#73B646")));
                tvPublish.setText("Published");
                }
                else{
                	tvPublish.setTextColor(Color.parseColor(("#ABABAB")));
                	tvPublish.setText("Unpublished");
                }
        }

        private TextView tvTitle;
        private TextView tvPublish;
    }
    

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	// TODO Auto-generated method stub
	super.onActivityResult(requestCode, resultCode, data);

	switch(requestCode) {
	case 0:
	    //displayAccounts();
	    //Toast.makeText(wpAndroid.this, title, Toast.LENGTH_SHORT).show();
		//refreshPosts();
	    break;
	}

}

@Override
public boolean onContextItemSelected(MenuItem item) {

     /* Switch on the ID of the item, to get what the user selected. */
     switch (item.getItemId()) {
          case 0:
        	  Intent i2 = new Intent(viewLocalDrafts.this, editPost.class);
              i2.putExtra("postID", String.valueOf(selectedID));
              i2.putExtra("postTitle", titles[rowID]);
              i2.putExtra("id", id);
              i2.putExtra("accountName", accountName);
              i2.putExtra("localDraft", true);
              startActivity(i2);
        	  return true;
          case 1:
        	  showDialog(ID_DIALOG_POSTING);
        	  
        	  
        	  new Thread() {
                  public void run() { 	  
        	  
		try {
			submitResult = submitPost();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
                  }
              }.start(); 
              	
              
              
              
        	  return true;
          case 2:
        	  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewLocalDrafts.this);
  			  dialogBuilder.setTitle("Delete Post?");
              dialogBuilder.setMessage("Are you sure you want to delete the draft '" + titles[rowID] + "'?");
              dialogBuilder.setPositiveButton("OK",  new
            		  DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                	  localDraftsDB lDraftsDB = new localDraftsDB(viewLocalDrafts.this);
                	  
                	  lDraftsDB.deletePost(viewLocalDrafts.this, String.valueOf(selectedID));
                	  loadPosts();
              
                  }
              });
              dialogBuilder.setNegativeButton("Cancel",  new
            		  DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                      // Just close the window.
              
                  }
              });
              dialogBuilder.setCancelable(true);
             dialogBuilder.create().show();
        	  
     }
     return false;
}

public String submitPost() throws IOException {
	
	
	//grab the form data
	localDraftsDB lDraftsDB = new localDraftsDB(this);
	Vector post = lDraftsDB.loadPost(this, String.valueOf(selectedID));
	
	HashMap postHashMap = (HashMap) post.get(0);
	
	EditText titleET = (EditText)findViewById(R.id.title);
	EditText contentET = (EditText)findViewById(R.id.content);
	
	String title = postHashMap.get("title").toString();
	String content = postHashMap.get("content").toString();
	
	String picturePaths = postHashMap.get("picturePaths").toString();
	
	if (!picturePaths.equals("")){
		String[] pPaths = picturePaths.split(",");
		
		for (int i = 0; i < pPaths.length; i++)
		{
			Uri imagePath = Uri.parse(pPaths[i]); 
			selectedImageIDs.add(selectedImageCtr, imagePath);
	        imageUrl.add(selectedImageCtr, pPaths[i]);
	        selectedImageCtr++;
		}
		
	}
	
	String categories = postHashMap.get("categories").toString();
	if (!categories.equals("")){
		
		String[] aCategories = categories.split(",");
		
		for (int i=0; i < aCategories.length; i++)
		{
			selectedCategories.add(aCategories[i]);
		}
		
	}
	
	String tags = postHashMap.get("tags").toString();
	
	int publish = Integer.valueOf(postHashMap.get("publish").toString());
	
	
    Boolean publishThis = false;
    
    if (publish == 1){
    	publishThis = true;
    }
    String imageContent = "";
    //upload the images and return the HTML
    for (int it = 0; it < selectedImageCtr; it++){
        
        imageContent +=  uploadImage(selectedImageIDs.get(it).toString());

        }

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
    if (tags != ""){
    contentStruct.put("mt_keywords", escapeUtils.escapeHtml(tags));
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
			dismissDialog(ID_DIALOG_POSTING);
			
			Thread action = new Thread() 
			{ 
			  public void run() 
			  {
				  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewLocalDrafts.this);
	  			  dialogBuilder.setTitle("Success");
	              dialogBuilder.setMessage("Post #" + newID + " added successfully");
	              dialogBuilder.setPositiveButton("OK",  new
	            		  DialogInterface.OnClickListener() {
	                  public void onClick(DialogInterface dialog, int whichButton) {
	                      // Just close the window.
	              
	                  }
	              });
	              dialogBuilder.setCancelable(true);
	             dialogBuilder.create().show();

			  } 
			}; 
			this.runOnUiThread(action);
          	  
            

    }// if/then for valid settings
    
	return res;
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

@Override
protected Dialog onCreateDialog(int id) {
if(id == ID_DIALOG_POSTING){
ProgressDialog loadingDialog = new ProgressDialog(this);
loadingDialog.setMessage("Attempting to submit post...");
loadingDialog.setIndeterminate(true);
loadingDialog.setCancelable(false);
return loadingDialog;
}

return super.onCreateDialog(id);
}
public String uploadImage(String imageURL){
    
    int finalHeight = 0;
    
    //images variables

    
    //get the settings
    settingsDB settingsDB = new settingsDB(viewLocalDrafts.this);
	Vector categoriesVector = settingsDB.loadSettings(viewLocalDrafts.this, id);   	
	
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
        		int sample = 0;

        		float fWidth = width;
                sample= new Double(Math.ceil(fWidth / 1200)).intValue();
                
        		if(sample == 3){
                    sample = 4;
        		}
        		else if(sample > 4 && sample < 8 ){
                    sample = 8;
        		}
        		
        		opts.inSampleSize = sample;
        		opts.inJustDecodeBounds = false;
        		
                bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
                
                float percentage = (float) finalWidth / bm.getWidth();
        		float proportionateHeight = bm.getHeight() * percentage;
        		finalHeight = (int) Math.rint(proportionateHeight);
        		
        		float scaleWidth = ((float) finalWidth) / bm.getWidth(); 
    	        float scaleHeight = ((float) finalHeight) / bm.getHeight(); 

                
    	        float scaleBy = Math.min(scaleWidth, scaleHeight);
    	        
    	        // Create a matrix for the manipulation 
    	        Matrix matrix = new Matrix(); 
    	        // Resize the bitmap 
    	        matrix.postScale(scaleBy, scaleBy); 

    	        Bitmap resized = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();  
                resized.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                
                bm.recycle(); //free up memory
                resized.recycle();
                
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
				dismissDialog(ID_DIALOG_POSTING);
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewLocalDrafts.this);
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
						dismissDialog(ID_DIALOG_POSTING);
						//status.setText("Cannot connect to " + uri.getHost() + "\nMake sure server.py on your development host is running !!!");
					} else {
						dismissDialog(ID_DIALOG_POSTING);
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewLocalDrafts.this);
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
					e.printStackTrace();
		             
				
		} catch (final XMLRPCException e) {
			
			handler.post(new Runnable() {
				public void run() {
					
					Throwable couse = e.getCause();
					e.printStackTrace();
					
					//Log.d("Test", "error", e);
					
				}
			});
		}
		
	}
}

}


