package org.wordpress.android;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
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
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Images;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


public class viewPosts extends ListActivity {
    /** Called when the activity is first created. */
	private XMLRPCClient client;
	private String[] postIDs, titles, dateCreated, dateCreatedFormatted, draftIDs, draftTitles, publish;
	private Integer[] uploaded;
	private String id = "";
	private String accountName = "";
	private String newID = "";
	Vector postNames = new Vector();
	int selectedID = 0;
	int rowID = 0;
	private int ID_DIALOG_POSTING = 2;
	Vector selectedCategories = new Vector();
	private boolean inDrafts = false;
	private Vector<Uri> selectedImageIDs = new Vector();
	private int selectedImageCtr = 0;
    public String imgHTML = "";
    public String sImagePlacement = "";
    public String sMaxImageWidth = "";
    public boolean centerThumbnail = false;
    public Vector imageUrl = new Vector();
    public String imageTitle = null;
    public boolean thumbnailOnly, secondPass, xmlrpcError = false;
    public String submitResult = "";
    public int totalDrafts = 0;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
      
        setContentView(R.layout.viewposts);
        
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         accountName = extras.getString("accountName");
        }
        
        //query for posts and refresh view
        boolean loadedPosts = loadPosts();
        
        if (!loadedPosts){
        	refreshPosts();
        }
        

            final customMenuButton addNewPost = (customMenuButton) findViewById(R.id.newPost);   
            
            addNewPost.setOnClickListener(new customMenuButton.OnClickListener() {
                public void onClick(View v) {
                	
                	Intent i = new Intent(viewPosts.this, newPost.class);
                	i.putExtra("accountName", accountName);
 	                i.putExtra("id", id);
 	                startActivityForResult(i, 0);
                	 
                }
        });
            
final customMenuButton refresh = (customMenuButton) findViewById(R.id.refresh);   
            
            refresh.setOnClickListener(new customMenuButton.OnClickListener() {
                public void onClick(View v) {
                	
                	refreshPosts();
                	 
                }
        });
        
        
    }
    
    private void refreshPosts(){
    	showProgressBar();
    	Vector settings = new Vector();
        settingsDB settingsDB = new settingsDB(this);
    	settings = settingsDB.loadSettings(this, id);
        
    	
    	String sURL = "";
    	if (settings.get(0).toString().contains("xmlrpc.php"))
    	{
    		sURL = settings.get(0).toString();
    	}
    	else
    	{
    		sURL = settings.get(0).toString() + "xmlrpc.php";
    	}
		String sUsername = settings.get(2).toString();
		String sPassword = settings.get(3).toString();
		int sBlogId = Integer.parseInt(settings.get(10).toString());
        
        	
        	client = new XMLRPCClient(sURL);
        	
        	XMLRPCMethod method = new XMLRPCMethod("blogger.getRecentPosts", new XMLRPCMethodCallback() {
				public void callFinished(Object[] result) {
					String s = "done";
					s = result.toString();
					
					if (result.length == 0){
						closeProgressBar();
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
						  dialogBuilder.setTitle(getResources().getText(R.string.posts_not_found));
			              dialogBuilder.setMessage(getResources().getText(R.string.posts_no_posts));
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

					HashMap contentHash = new HashMap();
					    
					String rTitles[] = new String[result.length];
					String rPostIDs[] = new String[result.length];
					String rDateCreated[] = new String[result.length];
					String rDateCreatedFormatted[] = new String[result.length];
					Vector dbVector = new Vector();
					
					//loop this!
					    for (int ctr = 0; ctr < result.length; ctr++){
					    	HashMap<String, String> dbValues = new HashMap();
					        contentHash = (HashMap) result[ctr];
					        rTitles[ctr] = escapeUtils.unescapeHtml(contentHash.get("content").toString().substring(contentHash.get("content").toString().indexOf("<title>") + 7, contentHash.get("content").toString().indexOf("</title>")));
					        rPostIDs[ctr] = contentHash.get("postid").toString();
					        rDateCreated[ctr] = contentHash.get("dateCreated").toString();
					        
					      //make the date pretty
					        Date d = new Date();
							SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy"); 
					        String cDate = rDateCreated[ctr].replace("America/Los_Angeles", "PST");
					        try{  
					        	d = sdf.parse(cDate);
					        	SimpleDateFormat sdfOut = new SimpleDateFormat("MMMM dd, yyyy hh:mm a"); 
					        	rDateCreatedFormatted[ctr] = sdfOut.format(d);
					        } catch (ParseException pe){  
					            pe.printStackTrace();
					            rDateCreatedFormatted[ctr] = rDateCreated[ctr];  //just make it the ugly date if it doesn't work
					        } 
					        
					        dbValues.put("blogID", id);
					        dbValues.put("postID", rPostIDs[ctr]);
					        dbValues.put("title", rTitles[ctr]);
					        dbValues.put("postDate", rDateCreated[ctr]);
					        dbValues.put("postDateFormatted", rDateCreatedFormatted[ctr]);
					        dbVector.add(ctr, dbValues);
					        
					    }
					    
					    postStoreDB postStoreDB = new postStoreDB(viewPosts.this);
					    postStoreDB.savePosts(viewPosts.this, dbVector);
					    
					    
					   closeProgressBar();
					   loadPosts();
					}
			        
				}
	        });
	        Object[] params = {
	        		"spacer",
	        		sBlogId,
	        		sUsername,
	        		sPassword,
	        		30
	        };
	        
	        
	        method.call(params);
	        
	        
    }
    
    public Map<String,?> createItem(String title, String caption) {  
        Map<String,String> item = new HashMap<String,String>();  
        item.put("title", title);  
        item.put("caption", caption);  
        return item;  
    } 
    
    private boolean loadPosts(){ //loads posts from the db
   	
    	postStoreDB postStoreDB = new postStoreDB(this);
    	Vector loadedPosts = postStoreDB.loadPosts(viewPosts.this, id);
   	
    	if (loadedPosts != null){
    	titles = new String[loadedPosts.size()];
    	postIDs = new String[loadedPosts.size()];
    	dateCreated = new String[loadedPosts.size()];
    	dateCreatedFormatted = new String[loadedPosts.size()];
    	}
    	else{
    		titles = new String[0];
        	postIDs = new String[0];
        	dateCreated = new String[0];
        	dateCreatedFormatted = new String[0];
    	}
    	if (loadedPosts != null){
					    for (int i=0; i < loadedPosts.size(); i++){
					        HashMap contentHash = (HashMap) loadedPosts.get(i);
					        titles[i] = escapeUtils.unescapeHtml(contentHash.get("title").toString());
					        postIDs[i] = contentHash.get("postID").toString();
					        dateCreated[i] = contentHash.get("postDate").toString();	
					        dateCreatedFormatted[i] = contentHash.get("postDateFormatted").toString();
					    }
					    
					    //add the header
					    List postIDList = Arrays.asList(postIDs);  
				    	List newPostIDList = new ArrayList();   
				    	newPostIDList.add("postsHeader");
				    	newPostIDList.addAll(postIDList);
				    	postIDs = (String[]) newPostIDList.toArray(new String[newPostIDList.size()]);
				    	
				    	List postTitleList = Arrays.asList(titles);  
				    	List newPostTitleList = new ArrayList();   
				    	newPostTitleList.add(getResources().getText(R.string.tab_posts));
				    	newPostTitleList.addAll(postTitleList);
				    	titles = (String[]) newPostTitleList.toArray(new String[newPostTitleList.size()]);
				    	
				    	List dateList = Arrays.asList(dateCreated);  
				    	List newDateList = new ArrayList();   
				    	newDateList.add("postsHeader");
				    	newDateList.addAll(dateList);
				    	dateCreated = (String[]) newDateList.toArray(new String[newDateList.size()]);
					    
				    	List dateFormattedList = Arrays.asList(dateCreatedFormatted);  
				    	List newDateFormattedList = new ArrayList();   
				    	newDateFormattedList.add("postsHeader");
				    	newDateFormattedList.addAll(dateFormattedList);
				    	dateCreatedFormatted = (String[]) newDateFormattedList.toArray(new String[newDateFormattedList.size()]);
   	}
					    //load drafts
					    boolean drafts = loadDrafts();
					    
					    if (drafts){
					    	
					    	List draftIDList = Arrays.asList(draftIDs);  
					    	List newDraftIDList = new ArrayList();   
					    	newDraftIDList.add("draftsHeader");
					    	newDraftIDList.addAll(draftIDList);
					    	draftIDs = (String[]) newDraftIDList.toArray(new String[newDraftIDList.size()]);
					    	
					    	List titleList = Arrays.asList(draftTitles);  
					    	List newTitleList = new ArrayList();   
					    	newTitleList.add(getResources().getText(R.string.local_drafts));
					    	newTitleList.addAll(titleList);
					    	draftTitles = (String[]) newTitleList.toArray(new String[newTitleList.size()]);
					    	
					    	List publishList = Arrays.asList(publish);  
					    	List newPublishList = new ArrayList();   
					    	newPublishList.add("draftsHeader");
					    	newPublishList.addAll(publishList);
					    	publish = (String[]) newPublishList.toArray(new String[newPublishList.size()]);
					    	
					    	postIDs = StringHelper.mergeStringArrays(draftIDs, postIDs);
					    	titles = StringHelper.mergeStringArrays(draftTitles, titles);
					    	dateCreatedFormatted = StringHelper.mergeStringArrays(publish, dateCreatedFormatted);
					    }
					    
					    if (loadedPosts != null || drafts == true )
					    {
					   setListAdapter(new PostListAdapter(viewPosts.this));

					   ListView listView = (ListView) findViewById(android.R.id.list);
					   listView.setSelector(R.layout.list_selector);
					   
					   listView.setOnItemClickListener(new OnItemClickListener() {
						   
							public void onNothingSelected(AdapterView<?> arg0) {
								
							}

							public void onItemClick(AdapterView<?> arg0, View arg1,
									int arg2, long arg3) {
								//don't do anything
								
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
			                   
			                   if (totalDrafts > 0 && rowID <= totalDrafts && rowID != 0){
			                	   menu.clear();
			                	   menu.setHeaderTitle(getResources().getText(R.string.draft_actions));
			                	   menu.add(1, 0, 0, getResources().getText(R.string.edit_draft));
			                	   menu.add(1, 1, 0, getResources().getText(R.string.upload));
			                	   menu.add(1, 2, 0, getResources().getText(R.string.delete_draft));            	             
			                   }
			                   else if(rowID == 1 || ((rowID != (totalDrafts + 1)) && rowID != 0)){
			                	   menu.clear();
			                	   menu.setHeaderTitle(getResources().getText(R.string.post_actions));
			                	   menu.add(0, 0, 0, getResources().getText(R.string.preview_post));
			                	   menu.add(0, 1, 0, getResources().getText(R.string.view_comments));
			                	   menu.add(0, 2, 0, getResources().getText(R.string.edit_post));
			                   }

			                   
						}
			          });
   
		        return true;
		    }
			else{
				return false;
			}
	
   }
    
    class ViewWrapper {
    	View base;
    	TextView title=null;
    	TextView date=null;
    	ViewWrapper(View base) {
    	this.base=base;
    	}
    	TextView getTitle() {
    		if (title==null) {
    		title=(TextView)base.findViewById(R.id.title);
    		}
    		return(title);
    		}
    		TextView getDate() {
    		if (date==null) {
    		date=(TextView)base.findViewById(R.id.date);
    		}
    		return(date);
    		}
    		}
    
    
    private boolean loadDrafts(){ //loads drafts from the db
       	
        localDraftsDB lDraftsDB = new localDraftsDB(this);
    	Vector loadedPosts = lDraftsDB.loadPosts(viewPosts.this, id);
    	if (loadedPosts != null){
    	draftIDs = new String[loadedPosts.size()];
    	draftTitles = new String[loadedPosts.size()];
    	publish = new String[loadedPosts.size()];
    	uploaded = new Integer[loadedPosts.size()];
    	totalDrafts = loadedPosts.size();
    	
 					    for (int i=0; i < loadedPosts.size(); i++){
 					        HashMap contentHash = (HashMap) loadedPosts.get(i);
 					        draftIDs[i] = contentHash.get("id").toString();
 					        draftTitles[i] = escapeUtils.unescapeHtml(contentHash.get("title").toString());
 					        publish[i] = contentHash.get("publish").toString();
 					        uploaded[i] = (Integer) contentHash.get("uploaded");
 					    }
    	
 	return true;
     }
    	else{
    		totalDrafts = 0;
    		return false;
    	}
    }


    private class PostListAdapter extends BaseAdapter {
    	private int dateHeight;
        public PostListAdapter(Context context) {
            mContext = context;
        }

        public int getCount() {
            return postIDs.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
        	View pv=convertView;
        	ViewWrapper wrapper=null;
        	if (pv==null) {
        		LayoutInflater inflater=getLayoutInflater();
        		pv=inflater.inflate(R.layout.row_post_page, parent, false);
        		wrapper=new ViewWrapper(pv);
        		if (position == 0){
        		dateHeight = wrapper.getDate().getHeight();
        		}
        		pv.setTag(wrapper);
        	wrapper=new ViewWrapper(pv);
        	pv.setTag(wrapper);
        	}
        	else {
        	wrapper=(ViewWrapper)pv.getTag();      	
        	}
        	String date = dateCreatedFormatted[position];
        	if (date.equals("postsHeader") || date.equals("draftsHeader")){

            	pv.setBackgroundDrawable(getResources().getDrawable(R.drawable.list_header_bg));
            	
                wrapper.getTitle().setTextColor(Color.parseColor("#EEEEEE"));
                wrapper.getTitle().setShadowLayer(1, 1, 1, Color.parseColor("#444444"));
                wrapper.getDate().setHeight(0);
                
                if (date.equals("draftsHeader")){
                	inDrafts = true;
                	date = "";
                }
                else if (date.equals("postsHeader")){
                	inDrafts = false;
                	date = "";
                }
            }
        	else{
        		pv.setBackgroundDrawable(getResources().getDrawable(R.drawable.list_bg_selector));
        		wrapper.getTitle().setTextColor(Color.parseColor("#444444"));
        		wrapper.getTitle().setShadowLayer(0, 0, 0, Color.parseColor("#444444"));
        		wrapper.getDate().setTextColor(Color.parseColor("#888888"));
        		pv.setId(Integer.valueOf(postIDs[position]));
        		if (wrapper.getDate().getHeight() == 0){
        			wrapper.getDate().setHeight((int) wrapper.getTitle().getTextSize() + wrapper.getDate().getPaddingBottom());
        		}
        		String customDate = date;
                
                if (customDate.equals("1")){
                	customDate = getResources().getText(R.string.publish_yes).toString();
                	wrapper.getDate().setTextColor(Color.parseColor("#006505"));
                }
                else if (customDate.equals("0")){
                	customDate = getResources().getText(R.string.publish_no).toString();
                }
                date = customDate;
        		
        	}
        	wrapper.getTitle().setText(titles[position]);
        	wrapper.getDate().setText(date);
        	
        	return pv;

        }

        private Context mContext;
        
    }
   
interface XMLRPCMethodCallback {
	void callFinished(Object[] result);
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
			final Object[] result = (Object[]) client.call(method, params);
			final long t1 = System.currentTimeMillis();
			handler.post(new Runnable() {
				public void run() {

					callBack.callFinished(result);
				}
			});
		} catch (final XMLRPCFault e) {
			handler.post(new Runnable() {
				public void run() {
					closeProgressBar();
					if (e.getFaultCode() != 500){
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
					  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
		              dialogBuilder.setMessage(e.getMessage());
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
						postStoreDB postStoreDB = new postStoreDB(viewPosts.this);
						postStoreDB.clearPosts(viewPosts.this, id);
						loadPosts();
					}
				}
			});
		} catch (final XMLRPCException e) {
			handler.post(new Runnable() {
				public void run() {
					closeProgressBar();
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
					  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
		              dialogBuilder.setMessage(e.getMessage());
		              dialogBuilder.setPositiveButton("OK",  new
		            		  DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Just close the window.
                    
                        }
                    });
		              dialogBuilder.setCancelable(true);
		             dialogBuilder.create().show();
				}
			});
		}
	}
}
//Add settings to menu
@Override
public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, 0, 0, getResources().getText(R.string.blog_settings));
    MenuItem menuItem1 = menu.findItem(0);
    menuItem1.setIcon(R.drawable.ic_menu_preferences);
    menu.add(0, 1, 0, getResources().getText(R.string.remove_account));
    MenuItem menuItem2 = menu.findItem(1);
    menuItem2.setIcon(R.drawable.ic_notification_clear_all);
    
    return true;
}
//Menu actions
@Override
public boolean onOptionsItemSelected(final MenuItem item){
    switch (item.getItemId()) {
    case 0:
    	
    	Bundle bundle = new Bundle();
		bundle.putString("id", id);
		bundle.putString("accountName", accountName);
    	Intent i = new Intent(this, settings.class);
    	i.putExtras(bundle);
    	startActivityForResult(i, 0);
    	
    	return true;
	case 1:
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
		  dialogBuilder.setTitle(getResources().getText(R.string.remove_account));
      dialogBuilder.setMessage(getResources().getText(R.string.sure_to_remove_account));
      dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),  new
    		  DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // User clicked Accept so set that they've agreed to the eula.
            	settingsDB settingsDB = new settingsDB(viewPosts.this);
              boolean deleteSuccess = settingsDB.deleteAccount(viewPosts.this, id);
              if (deleteSuccess)
              {
            	  Toast.makeText(viewPosts.this, getResources().getText(R.string.blog_removed_successfully),
                          Toast.LENGTH_SHORT).show();
            	  finish();
              }
              else
              {
            	  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
      			  dialogBuilder.setTitle(getResources().getText(R.string.error));
                  dialogBuilder.setMessage(getResources().getText(R.string.could_not_remove_account));
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
        });
      dialogBuilder.setNegativeButton(getResources().getText(R.string.no), new
    		  DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	//just close the window
            }
        });
      dialogBuilder.setCancelable(false);
     dialogBuilder.create().show();
	
	
	return true;
}
  return false;	
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	// TODO Auto-generated method stub
	super.onActivityResult(requestCode, resultCode, data);
	Bundle extras = data.getExtras();
	String returnResult = extras.getString("returnStatus");
	
	if (returnResult != null){
		switch (requestCode) {
		case 0:
			if (returnResult.equals("OK")){
				loadPosts();
			}
			break;
		case 1:
			if (returnResult.equals("OK")){
				refreshPosts();
			}
			break;
		}
	}

}

@Override
public boolean onContextItemSelected(MenuItem item) {

	
	
     /* Switch on the ID of the item, to get what the user selected. */
	if (item.getGroupId() == 0){
     switch (item.getItemId()) {
     	  case 0:
     		 Intent i0 = new Intent(viewPosts.this, viewPost.class);
             i0.putExtra("postID", String.valueOf(selectedID));
             //i0.putExtra("postTitle", titles[selectedID]);
             i0.putExtra("id", id);
             i0.putExtra("accountName", accountName);
             startActivity(i0);
             return true;
          case 1:     
        	  Intent i = new Intent(viewPosts.this, viewComments.class);
              i.putExtra("postID", String.valueOf(selectedID));
              //i.putExtra("postTitle", titles[selectedID]);
              i.putExtra("id", id);
              i.putExtra("accountName", accountName);
              startActivity(i);
              return true; 
          case 2:
        	  Intent i2 = new Intent(viewPosts.this, editPost.class);
              i2.putExtra("postID", String.valueOf(selectedID));
              //i2.putExtra("postTitle", titles[selectedID]);
              i2.putExtra("id", id);
              i2.putExtra("accountName", accountName);
              startActivityForResult(i2,1);
        	  return true;
     }
     
	}
	else{
		switch (item.getItemId()) {
        case 0:
      	  Intent i2 = new Intent(viewPosts.this, editPost.class);
            i2.putExtra("postID", String.valueOf(selectedID));
            //i2.putExtra("postTitle", titles[rowID]);
            i2.putExtra("id", id);
            i2.putExtra("accountName", accountName);
            i2.putExtra("localDraft", true);
            startActivityForResult(i2,0);
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
      	  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
			  dialogBuilder.setTitle(getResources().getText(R.string.delete_draft));
            dialogBuilder.setMessage(getResources().getText(R.string.delete_sure) + " '" + titles[rowID] + "'?");
            dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),  new
          		  DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
              	  localDraftsDB lDraftsDB = new localDraftsDB(viewPosts.this);
              	  
              	  lDraftsDB.deletePost(viewPosts.this, String.valueOf(selectedID));
              	  loadPosts();
            
                }
            });
            dialogBuilder.setNegativeButton(getResources().getText(R.string.no),  new
          		  DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Just close the window.
            
                }
            });
            dialogBuilder.setCancelable(true);
           dialogBuilder.create().show();
      	  
		}
	}
	
	
	return false;
}

@Override
protected Dialog onCreateDialog(int id) {
if (id == ID_DIALOG_POSTING){
	ProgressDialog loadingDialog = new ProgressDialog(this);
	loadingDialog.setMessage(getResources().getText(R.string.post_attempt_upload));
	loadingDialog.setIndeterminate(true);
	loadingDialog.setCancelable(true);
	return loadingDialog;
	}

return super.onCreateDialog(id);
}


public String submitPost() throws IOException {
	
	
	//grab the form data
	final localDraftsDB lDraftsDB = new localDraftsDB(this);
	Vector post = lDraftsDB.loadPost(this, String.valueOf(selectedID));
	
	HashMap postHashMap = (HashMap) post.get(0);
	
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
		
		int sBlogId = Integer.parseInt(categoriesVector.get(10).toString());
    
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
    		sBlogId,
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
				  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
	  			  dialogBuilder.setTitle(getResources().getText(R.string.success));
	  			if (xmlrpcError){
					  dialogBuilder.setMessage(getResources().getText(R.string.post_id) + " " + newID + " " + getResources().getText(R.string.added_successfully_image_error));  
				  }
				  else{
	              dialogBuilder.setMessage(getResources().getText(R.string.post_id) + " " + newID + " " + getResources().getText(R.string.added_successfully));
				  }
	              dialogBuilder.setPositiveButton("OK",  new
	            		  DialogInterface.OnClickListener() {
	                  public void onClick(DialogInterface dialog, int whichButton) {
	                      // Just close the window.
	                	  
	                	  boolean isInteger = false;
	                	  
	                	  try {
							int i = Integer.parseInt(newID);
							isInteger = true;
						} catch (NumberFormatException e) {
							
						}
	                	  
	                	  if (isInteger)
	                	  {
	                		
	                		//reset variables
	                		selectedImageIDs.clear();
	              	        imageUrl.clear();
	              	        selectedImageCtr = 0;
	              	        selectedCategories.clear();
	              	        xmlrpcError = false;
	                		  //post made it, so let's delete the draft
	                	  lDraftsDB.deletePost(viewPosts.this, String.valueOf(selectedID));
	                	  refreshPosts();
	                	  }
	              
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

public String uploadImage(String imageURL){
	
    //get the settings
    settingsDB settingsDB = new settingsDB(viewPosts.this);
	Vector categoriesVector = settingsDB.loadSettings(viewPosts.this, id);   	
	
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

    //check for image, and upload it
		
		int sBlogId = Integer.parseInt(categoriesVector.get(10).toString());

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
    	
    	XMLRPCMethodImages method = new XMLRPCMethodImages("wp.uploadFile", new XMLRPCMethodCallbackImages() {
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

interface XMLRPCMethodCallbackImages {
	void callFinished(Object result);
}

class XMLRPCMethodImages extends Thread {
	private String method;
	private Object[] params;
	private Handler handler;
	private XMLRPCMethodCallbackImages callBack;
	public XMLRPCMethodImages(String method, XMLRPCMethodCallbackImages callBack) {
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

public void showProgressBar() {
	AnimationSet set = new AnimationSet(true);

    Animation animation = new AlphaAnimation(0.0f, 1.0f);
    animation.setDuration(500);
    set.addAnimation(animation);

    animation = new TranslateAnimation(
        Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, -1.0f,Animation.RELATIVE_TO_SELF, 0.0f
    );
    animation.setDuration(500);
    set.addAnimation(animation);

    LayoutAnimationController controller =
            new LayoutAnimationController(set, 0.5f);
    RelativeLayout loading = (RelativeLayout) findViewById(R.id.loading);       
    loading.setVisibility(View.VISIBLE);
    loading.setLayoutAnimation(controller);
}

public void closeProgressBar() {

    AnimationSet set = new AnimationSet(true);

    Animation animation = new AlphaAnimation(0.0f, 1.0f);
    animation.setDuration(500);
    set.addAnimation(animation);

    animation = new TranslateAnimation(
        Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, -1.0f
    );
    animation.setDuration(500);
    set.addAnimation(animation);

    LayoutAnimationController controller =
            new LayoutAnimationController(set, 0.5f);
    RelativeLayout loading = (RelativeLayout) findViewById(R.id.loading);       
    
    loading.setLayoutAnimation(controller);
    
    loading.setVisibility(View.INVISIBLE);
}

}


