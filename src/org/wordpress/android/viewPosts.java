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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
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
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.widget.AdapterView.OnItemClickListener;


public class viewPosts extends ListActivity{
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
	private int ID_DIALOG_DELETING = 1;
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
    public String submitResult = "", mediaErrorMsg = "";
    ProgressDialog loadingDialog;
    public int totalDrafts = 0;
    public boolean isPage = false;
    public Vector thumbnailUrl = new Vector();
    boolean largeScreen = false;
    int numRecords = 30;
    private ViewSwitcher switcher;
    private PostListAdapter pla;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.viewposts);
        
        Bundle extras = getIntent().getExtras();
        String action = null;
        if(extras !=null)
        {
         id = extras.getString("id");
         accountName = extras.getString("accountName");
         isPage = extras.getBoolean("viewPages");
         action = extras.getString("action");
        }
        
        //user came from action intent
        if (action != null && !isPage){
        	if (action.equals("upload")){
        		selectedID = extras.getInt("uploadID");
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
        	}
        	else{
        		boolean loadedPosts = loadPosts(false);
        		if (!loadedPosts){
                	refreshPosts(false);
                }
        	}
        }
        else{
        	
        	//add footer view
    	    if (!isPage){
    	    	//create the ViewSwitcher in the current context
    	        switcher = new ViewSwitcher(this);
    			  //footer Button: see XML1
    			  Button footer = (Button)View.inflate(this, R.layout.list_footer_btn, null);
    			  footer.setText(getResources().getText(R.string.load_more) + " " + getResources().getText(R.string.tab_posts));
    			  
    			  //progress View: see XML2
    			  View progress = View.inflate(this, R.layout.list_footer_progress, null);
    			  
    			  //add the views (first added will show first)
    			  switcher.addView(footer);
    			  switcher.addView(progress);  
    	    }
	        
	        //query for posts and refresh view
	        boolean loadedPosts = loadPosts(false);
	        
	        if (!loadedPosts){
	        	refreshPosts(false);
	        }
        }
        
        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        if (width > 480 || height > 480){
        	largeScreen = true;
        }
        

            final ImageButton addNewPost = (ImageButton) findViewById(R.id.newPost);   
            
            addNewPost.setOnClickListener(new ImageButton.OnClickListener() {
                public void onClick(View v) {
                	
                	Intent i = new Intent(viewPosts.this, newPost.class);
                	i.putExtra("accountName", accountName);
 	                i.putExtra("id", id);
 	                if (isPage){
 	                	i.putExtra("isPage", true);
 	                }
 	                startActivityForResult(i, 0);
                	 
                }
        });
            
final ImageButton refresh = (ImageButton) findViewById(R.id.refresh);   
            
            refresh.setOnClickListener(new ImageButton.OnClickListener() {
                public void onClick(View v) {
                	
                	refreshPosts(false);
                	 
                }
        });
        
    }
    
   public void onClick(View arg0) {
		//first view is showing, show the second progress view
		switcher.showNext();
		//get 30 more posts
		numRecords += 30;
		refreshPosts(true);
	}
    
    private void refreshPosts(final boolean loadMore){

    	if (!loadMore){
    		showProgressBar();
    	}

    	Vector settings = new Vector();
        WordPressDB settingsDB = new WordPressDB(this);
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
        	
        	XMLRPCMethod method = new XMLRPCMethod((isPage) ? "wp.getPageList" : "blogger.getRecentPosts", new XMLRPCMethodCallback() {
				public void callFinished(Object[] result) {
					String s = "done";
					s = result.toString();
					
					if (result.length == 0){
						if (!loadMore){
							closeProgressBar();
						}
						else{
				    		switcher.showPrevious();
				    	}
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
						  dialogBuilder.setTitle(getResources().getText((isPage) ? R.string.pages_not_found: R.string.posts_not_found));
			              dialogBuilder.setMessage(getResources().getText((isPage) ? R.string.pages_no_pages: R.string.posts_no_posts));
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
					String rParentID[] = new String[result.length];
					Vector dbVector = new Vector();
					WordPressDB postStoreDB = new WordPressDB(viewPosts.this);
					Date d = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
					Calendar cal = Calendar.getInstance();
					TimeZone tz = cal.getTimeZone();
					String shortDisplayName = "";
					shortDisplayName = tz.getDisplayName(true, TimeZone.SHORT);
					
					//loop this!
					    for (int ctr = 0; ctr < result.length; ctr++){
					    	HashMap<String, String> dbValues = new HashMap();
					        contentHash = (HashMap) result[ctr];
					        if (isPage){
					        	rTitles[ctr] = escapeUtils.unescapeHtml(contentHash.get("page_title").toString());
						        rPostIDs[ctr] = contentHash.get("page_id").toString();
						        rDateCreated[ctr] = contentHash.get("dateCreated").toString();
						        rParentID[ctr] = contentHash.get("page_parent_id").toString();	
					        }
					        else{
					        	rTitles[ctr] = escapeUtils.unescapeHtml(contentHash.get("content").toString().substring(contentHash.get("content").toString().indexOf("<title>") + 7, contentHash.get("content").toString().indexOf("</title>")));
					        	rPostIDs[ctr] = contentHash.get("postid").toString();
					        	rDateCreated[ctr] = contentHash.get("dateCreated").toString();
					        	
					        }
					        
					      //make the date pretty
					        
							
							

					        String cDate = rDateCreated[ctr].replace(tz.getID(), shortDisplayName);
					        try{  
					        	d = sdf.parse(cDate);
					        	SimpleDateFormat sdfOut = new SimpleDateFormat("MMMM dd, yyyy hh:mm a"); 
					        	rDateCreatedFormatted[ctr] = sdfOut.format(d);
					        } catch (ParseException pe){  
					            pe.printStackTrace();
					            rDateCreatedFormatted[ctr] = rDateCreated[ctr];  //just make it the ugly date if it doesn't work
					        } 
					        
					        
					        dbValues.put("blogID", id);
					        dbValues.put("title", rTitles[ctr]);
					        
					        if (isPage){	
						        dbValues.put("pageID", rPostIDs[ctr]);		        
						        dbValues.put("pageDate", rDateCreated[ctr]);
						        dbValues.put("pageDateFormatted", rDateCreatedFormatted[ctr]);
						        dbValues.put("parentID", rParentID[ctr]);
						        dbVector.add(ctr, dbValues);						        
					        }
					        else{
					        	dbValues.put("postID", rPostIDs[ctr]);
					        	dbValues.put("postDate", rDateCreated[ctr]);
					        	dbValues.put("postDateFormatted", rDateCreatedFormatted[ctr]);
					        	dbVector.add(ctr, dbValues);
					        	
					        }
     
					    }//end for loop
					    
					    if (isPage){
					    	postStoreDB.savePages(viewPosts.this, dbVector);	
					    }
					    else{
					    	postStoreDB.savePosts(viewPosts.this, dbVector);
					    }
					    

					   loadPosts(loadMore);
					   if (!loadMore){
							closeProgressBar();
						}
						else{
				    		switcher.showPrevious();
				    	}
					}
			        
				}
	        });
        	if (isPage){
        		Object[] params = {
    	        		sBlogId,
    	        		sUsername,
    	        		sPassword,
    	        };
        		method.call(params);
        	}
        	else{
        		Object[] params = {
        				"spacer",
        				sBlogId,
        				sUsername,
        				sPassword,
        				numRecords
        		};
        		method.call(params);
        	}
	        
	        
	        
	        
	        
    }
    
    public Map<String,?> createItem(String title, String caption) {  
        Map<String,String> item = new HashMap<String,String>();  
        item.put("title", title);  
        item.put("caption", caption);  
        return item;  
    } 
    
    private boolean loadPosts(boolean loadMore){ //loads posts from the db
   	
    	WordPressDB postStoreDB = new WordPressDB(this);
    	Vector loadedPosts;
    	if (isPage){
    		loadedPosts = postStoreDB.loadPages(viewPosts.this, id);	
    	}
    	else{
    		loadedPosts = postStoreDB.loadSavedPosts(viewPosts.this, id);
    	}
   	
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
					        if (isPage){
					        	postIDs[i] = contentHash.get("pageID").toString();
					        	dateCreated[i] = contentHash.get("pageDate").toString();	
						        dateCreatedFormatted[i] = contentHash.get("pageDateFormatted").toString();
					        }
					        else{
					        postIDs[i] = contentHash.get("postID").toString();
					        dateCreated[i] = contentHash.get("postDate").toString();	
					        dateCreatedFormatted[i] = contentHash.get("postDateFormatted").toString();
					        }
					        
					    }
					    
					    //add the header
					    List postIDList = Arrays.asList(postIDs);  
				    	List newPostIDList = new ArrayList();   
				    	newPostIDList.add("postsHeader");
				    	newPostIDList.addAll(postIDList);
				    	postIDs = (String[]) newPostIDList.toArray(new String[newPostIDList.size()]);
				    	
				    	List postTitleList = Arrays.asList(titles);  
				    	List newPostTitleList = new ArrayList();   
				    	newPostTitleList.add(getResources().getText((isPage) ? R.string.tab_pages : R.string.tab_posts));
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
					    ListView listView = (ListView) findViewById(android.R.id.list);

					    if (!isPage){
					    	listView.removeFooterView(switcher);
					    	listView.addFooterView(switcher);
					    }				    
					    
					   if (loadMore){
						   pla.notifyDataSetChanged();
					   }
					   else{
						   pla = new PostListAdapter(viewPosts.this);
						   setListAdapter(pla);
						   
						   listView.setOnItemClickListener(new OnItemClickListener() {
							   

								public void onItemClick(AdapterView<?> arg0, View arg1,
										int arg2, long arg3) {
									arg1.performLongClick();
									
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
				                	   
				                	   if (isPage){
				                		   menu.setHeaderTitle(getResources().getText(R.string.page_actions));
					                	   menu.add(2, 0, 0, getResources().getText(R.string.preview_page));
					                	   menu.add(2, 1, 0, getResources().getText(R.string.view_comments));
					                	   menu.add(2, 2, 0, getResources().getText(R.string.edit_page));
					                	   menu.add(2, 3, 0, getResources().getText(R.string.delete_page));
				                	   }
				                	   else{
				                		   menu.setHeaderTitle(getResources().getText(R.string.post_actions));
				                		   menu.add(0, 0, 0, getResources().getText(R.string.preview_post));
				                		   menu.add(0, 1, 0, getResources().getText(R.string.view_comments));
					                	   menu.add(0, 2, 0, getResources().getText(R.string.edit_post));
					                	   menu.add(0, 3, 0, getResources().getText(R.string.delete_post));
				                	   }
				                	   
				                	   
				                   }

				                   
							}
				          });
					   }
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
       	
    	WordPressDB lDraftsDB = new WordPressDB(this);
        Vector loadedPosts;
        if (isPage){
        	loadedPosts = lDraftsDB.loadPageDrafts(viewPosts.this, id);
        }
        else{
        	loadedPosts = lDraftsDB.loadPosts(viewPosts.this, id);
        }
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
                if (largeScreen){
                	wrapper.getTitle().setPadding(12, 0, 12, 3);
                }
                else{
                	wrapper.getTitle().setPadding(8, 0, 8, 2);
                }
                wrapper.getTitle().setTextScaleX(1.2f);
                wrapper.getTitle().setTextSize(17);
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
        		if (largeScreen){
        			wrapper.getTitle().setPadding(12, 12, 12, 0);
        		}
        		else{
        			wrapper.getTitle().setPadding(8, 8, 8, 0);
        		}
        		wrapper.getTitle().setTextColor(Color.parseColor("#444444"));
        		wrapper.getTitle().setShadowLayer(0, 0, 0, Color.parseColor("#444444"));
        		wrapper.getTitle().setTextScaleX(1.0f);
        		wrapper.getTitle().setTextSize(16);
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
						WordPressDB postStoreDB = new WordPressDB(viewPosts.this);
						postStoreDB.clearPosts(viewPosts.this, id);
						loadPosts(false);
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
    menuItem1.setIcon(R.drawable.ic_menu_prefs);
    menu.add(0, 1, 0, getResources().getText(R.string.remove_account));
    MenuItem menuItem2 = menu.findItem(1);
    menuItem2.setIcon(R.drawable.ic_menu_close_clear_cancel);
    
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
            	WordPressDB settingsDB = new WordPressDB(viewPosts.this);
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
	if (resultCode == RESULT_OK){
	Bundle extras = data.getExtras();
	String returnResult = extras.getString("returnStatus");
	
	if (returnResult != null){
		switch (requestCode) {
		case 0:
			if (returnResult.equals("OK")){
				boolean uploadNow = false;
				uploadNow = extras.getBoolean("upload");
				if (uploadNow){
					int uploadID = extras.getInt("newID");
					selectedID = uploadID;
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

				}
				else{
					loadPosts(false);
				}
			}
			break;
		case 1:
			if (returnResult.equals("OK")){
				refreshPosts(false);
			}
			break;
		}
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
          case 3:
        	  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
			  dialogBuilder.setTitle(getResources().getText(R.string.delete_post));
            dialogBuilder.setMessage(getResources().getText(R.string.delete_sure_post) + " '" + titles[rowID] + "'?");
            dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),  new
          		  DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	showDialog(ID_DIALOG_DELETING);
    	    		new Thread() {
    	                  public void run() {	    		
    	                	  deletePost();
    	                  }
    	    		}.start();
            
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
        	  
        	  return true;
     }
     
	}
	else if (item.getGroupId() == 2){
	     switch (item.getItemId()) {
	     	  case 0:
	     		 Intent i0 = new Intent(viewPosts.this, viewPost.class);
	             i0.putExtra("postID", String.valueOf(selectedID));
	             //i0.putExtra("postTitle", titles[selectedID]);
	             i0.putExtra("id", id);
	             i0.putExtra("accountName", accountName);
	             i0.putExtra("isPage", true);
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
	              i2.putExtra("isPage", true);
	              startActivityForResult(i2,1);
	        	  return true;
	          case 3:
	        	  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
				  dialogBuilder.setTitle(getResources().getText(R.string.delete_page));
	            dialogBuilder.setMessage(getResources().getText(R.string.delete_sure_page) + " '" + titles[rowID] + "'?");
	            dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),  new
	          		  DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	        	  showDialog(ID_DIALOG_DELETING);
		    		new Thread() {
		                  public void run() {	    		
		                	  deletePost();
		                  }
		    		}.start();
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
            if (isPage){
            	i2.putExtra("isPage", true);
            }
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
                  WordPressDB lDraftsDB = new WordPressDB(viewPosts.this);
              	  if (isPage){
              		  lDraftsDB.deletePageDraft(viewPosts.this, String.valueOf(selectedID)); 
              	  }
              	  else{
              		  lDraftsDB.deletePost(viewPosts.this, String.valueOf(selectedID));
              	  }
              	  loadPosts(false);
            
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

private void deletePost() {

		
    	Vector settings = new Vector();
        WordPressDB settingsDB = new WordPressDB(viewPosts.this);
    	settings = settingsDB.loadSettings(viewPosts.this, id);
        
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
		String selPostID = String.valueOf(selectedID);
    	client = new XMLRPCClient(sURL);
	    

    	Object[] postParams = {
        		"",
        		selPostID,
        		sUsername,
        		sPassword
        };
    	Object[] pageParams = {
	    		sBlogId,
	    		sUsername,
	    		sPassword,
	    		selPostID
    	};
    	
        Object result = null;
        try {
    		result = (Object) client.call((isPage) ? "wp.deletePage" : "blogger.deletePost", (isPage) ? pageParams: postParams);
    		dismissDialog(ID_DIALOG_DELETING);
    		Thread action = new Thread() 
			{ 
			  public void run() 
			  {
				  Toast.makeText(viewPosts.this, getResources().getText((isPage) ? R.string.page_deleted : R.string.post_deleted), Toast.LENGTH_SHORT).show();
			  } 
			}; 
			this.runOnUiThread(action);
			Thread action2 = new Thread() 
			{ 
			  public void run() 
			  {
				  refreshPosts(false);				  } 
			}; 
			this.runOnUiThread(action2);
			
    	} catch (final XMLRPCException e) {
    		dismissDialog(ID_DIALOG_DELETING);
    		Thread action3 = new Thread() 
			{ 
			  public void run() 
			  {
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
			  }; 
				this.runOnUiThread(action3);
    	}	
	
}

@Override
protected Dialog onCreateDialog(int id) {
if (id == ID_DIALOG_POSTING){
	loadingDialog = new ProgressDialog(this);
	loadingDialog.setTitle(getResources().getText(R.string.uploading_content));
	loadingDialog.setMessage(getResources().getText((isPage) ? R.string.page_attempt_upload : R.string.post_attempt_upload));
	loadingDialog.setCancelable(true);
	return loadingDialog;
	}
else if (id == ID_DIALOG_DELETING){
	loadingDialog = new ProgressDialog(this);
	loadingDialog.setTitle(getResources().getText((isPage) ? R.string.delete_page : R.string.delete_post));
	loadingDialog.setMessage(getResources().getText((isPage) ? R.string.attempt_delete_page : R.string.attempt_delete_post));
	loadingDialog.setCancelable(true);
	return loadingDialog;
	}

return super.onCreateDialog(id);
}


public String submitPost() throws IOException {
	
	
	//grab the form data
	final WordPressDB lDraftsDB = new WordPressDB(this);
	Vector post;
	if (isPage){
		post = lDraftsDB.loadPageDraft(this, String.valueOf(selectedID));
	}
	else{
		post = lDraftsDB.loadPost(this, String.valueOf(selectedID));
	}
	
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
	
	String tags = "";
	if (!isPage){
		String categories = postHashMap.get("categories").toString();
		if (!categories.equals("")){
			
			String[] aCategories = categories.split(",");
			
			for (int i=0; i < aCategories.length; i++)
			{
				selectedCategories.add(aCategories[i]);
			}
			
		}
		
		tags = postHashMap.get("tags").toString();
		
	}
	int publish = Integer.valueOf(postHashMap.get("publish").toString());
	
	
    Boolean publishThis = false;
    
    if (publish == 1){
    	publishThis = true;
    }
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
 		 loadingDialog.setMessage(getResources().getText((isPage) ? R.string.page_attempt_upload : R.string.post_attempt_upload));
 	  } 
 	}; 
 	this.runOnUiThread(updateDialog);
    
    // categoryID = getCategoryId(selectedCategory);
    String[] theCategories = new String[selectedCategories.size()];
    
    for(int i=0; i < selectedCategories.size(); i++)
    {
		theCategories[i] = selectedCategories.get(i).toString();
    }
    
  //
    WordPressDB settingsDB = new WordPressDB(this);
	Vector settingsVector = settingsDB.loadSettings(this, id);   	
	
    	String sURL = "";
    	if (settingsVector.get(0).toString().contains("xmlrpc.php"))
    	{
    		sURL = settingsVector.get(0).toString();
    	}
    	else
    	{
    		sURL = settingsVector.get(0).toString() + "xmlrpc.php";
    	}
		String sUsername = settingsVector.get(2).toString();
		String sPassword = settingsVector.get(3).toString();
		String sImagePlacement = settingsVector.get(4).toString();
		String sCenterThumbnailString = settingsVector.get(5).toString();
		String sFullSizeImageString = settingsVector.get(6).toString();
		String sLocation = settingsVector.get(11).toString();
		boolean sFullSizeImage  = false;
		if (sFullSizeImageString.equals("1")){
			sFullSizeImage = true;
		}

		boolean centerThumbnail = false;
		if (sCenterThumbnailString.equals("1")){
			centerThumbnail = true;
		}
		
		boolean location = false;
		if (sLocation.equals("1")){
			location = true;
		}
		
		int sBlogId = Integer.parseInt(settingsVector.get(10).toString());
    
    Map<String, Object> contentStruct = new HashMap<String, Object>();
  
    if(imageContent != ""){
    	if (sImagePlacement.equals("Above Text")){
    		content = imageContent + content;
    	}
    	else{
    		content = content + imageContent;
    	}
    }
    
    if (!isPage){
	    //add the tagline
		HashMap globalSettings = settingsDB.getNotificationOptions(this);
		boolean taglineValue = false;
		String tagline = "";
		
		if (globalSettings != null){
			if (globalSettings.get("tagline_flag").toString().equals("1")){
				taglineValue = true;
			}
			
			if (taglineValue){
				tagline = globalSettings.get("tagline").toString();
				if (!tagline.equals("")){
					content += "\n\n<span id=\"post_sig\">" + tagline + "</span>\n\n";
				}
			}
		}
    }
	
    contentStruct.put("post_type", (isPage) ? "page" : "post");
    contentStruct.put("title", title);
    //for trac #53, add <p> and <br /> tags
    content = content.replace("/\n\n/g", "</p><p>");
    content = content.replace("/\n/g", "<br />");
    contentStruct.put("description", content);
    if (!isPage){
	    if (tags != ""){
	    contentStruct.put("mt_keywords", tags);
	    }
	    if (theCategories.length > 0){
	    contentStruct.put("categories", theCategories);
	    }
    }
    Double latitude = 0.0;
    Double longitude = 0.0;
    if (!isPage){
    	latitude = (Double) postHashMap.get("latitude");
		longitude = (Double) postHashMap.get("longitude");
		
		if (latitude > 0){
		HashMap hLatitude = new HashMap();
		hLatitude.put("key", "geo_latitude");
		hLatitude.put("value", latitude);

		HashMap hLongitude = new HashMap();
		hLongitude.put("key", "geo_longitude");
		hLongitude.put("value", longitude);
		
		HashMap hPublic = new HashMap();
		hPublic.put("key", "geo_public");
		hPublic.put("value", 1);
		
		Object[] geo = {
				hLatitude,
				hLongitude,
				hPublic	
		};
		
		contentStruct.put("custom_fields", geo);
		}
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
    boolean success = false;
    try {
		result = (Object) client.call("metaWeblog.newPost", params);
		success = true;
	} catch (final XMLRPCException e) {
		//e.printStackTrace();
		Thread prompt = new Thread() 
		{ 
		  public void run() 
		  {
			dismissDialog(ID_DIALOG_POSTING);
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
			  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
              dialogBuilder.setMessage(e.getMessage());
              dialogBuilder.setPositiveButton("OK",  new
            		  DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                  clearCounters();
              	
              	}
              });
              dialogBuilder.setCancelable(true);
             dialogBuilder.create().show();
		  } 
		}; 
		this.runOnUiThread(prompt);
	}

			if (success){
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
					  dialogBuilder.setMessage(getResources().getText((isPage) ? R.string.page_id : R.string.post_id) + " " + newID + " " + getResources().getText(R.string.added_successfully_image_error) + ": " + mediaErrorMsg);  
				  }
				  else{
	              dialogBuilder.setMessage(getResources().getText((isPage) ? R.string.page_id : R.string.post_id) + " " + newID + " " + getResources().getText(R.string.added_successfully));
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
	                		
	                		clearCounters();
	                		  //post made it, so let's delete the draft
		              	      if (isPage){
		              	    	  lDraftsDB.deletePageDraft(viewPosts.this, String.valueOf(selectedID));
		              	      }
		              	      else {
		              	    	  lDraftsDB.deletePost(viewPosts.this, String.valueOf(selectedID));
		              	      }
	                	  refreshPosts(false);
	                	  }
	              
	                  }
	              });
	              dialogBuilder.setCancelable(true);
	             dialogBuilder.create().show();

			  } 
			}; 
			this.runOnUiThread(action);
			}
    }
    else {
    	Thread prompt = new Thread() 
		{ 
		  public void run() 
		  {
			dismissDialog(ID_DIALOG_POSTING);
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
			  dialogBuilder.setTitle(getResources().getText(R.string.sdcard_title));
              dialogBuilder.setMessage(getResources().getText(R.string.sdcard_message));
              dialogBuilder.setPositiveButton("OK",  new
            		  DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
            	  //start over
            	    clearCounters();
              	}

              });
              dialogBuilder.setCancelable(true);
             dialogBuilder.create().show();
		  } 
		}; 
		this.runOnUiThread(prompt);
    }
	return res;
}

private void clearCounters() {
	//resets counter variables
	selectedImageIDs.clear();
    imageUrl.clear();
    selectedImageCtr = 0;
    selectedCategories.clear();
    xmlrpcError = false;
	
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
    WordPressDB settingsDB = new WordPressDB(this);
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
    	final int printCtr = it;
    	Thread prompt = new Thread() 
     	{ 
     	  public void run() 
     	  {
     		  loadingDialog.setMessage("Uploading Media File #" + String.valueOf(printCtr + 1));
     	  } 
     	}; 
     	this.runOnUiThread(prompt);
    //check for image, and upload it
    if (imageUrl.get(it) != null)
    {
       client = new XMLRPCClient(sURL);
 	   
 	   String sXmlRpcMethod = "wp.uploadFile";
 	   String curImagePath = "";
 	   
 	  curImagePath = imageUrl.get(it).toString();
 	 boolean video = false;
 	  if (curImagePath.contains("video")){
 		  video = true;
 	  }
 	   
 	  if (video){ //upload the video
  	   
  	   Uri videoUri = Uri.parse(curImagePath);
  	   File fVideo = null;
  	   String mimeType = "", xRes = "", yRes = "";
  	   MediaFile mf = null;
  	  
  	   if (videoUri.toString().contains("content:")){ //file is in media library
 		 	   String imgID = videoUri.getLastPathSegment();
 		 	   
 		 	   long imgID2 = Long.parseLong(imgID);
 		 	   
 		 	  String[] projection; 
 		 	 Uri imgPath;
 		 	 
 		 	 	
 		 	  	  //imgPath = ContentUris.withAppendedId(Video.Media.EXTERNAL_CONTENT_URI, imgID2);

 			 	  projection = new String[] {
 			       		    Video.Media._ID,
 			       		    Video.Media.DATA,
 			       		    Video.Media.MIME_TYPE,
 			       		    Video.Media.RESOLUTION
 			       		};
 			 	  //imgPath = ContentUris.withAppendedId(Video.Media.EXTERNAL_CONTENT_URI, imgID2);
 			 	imgPath = videoUri;

 		 	  Cursor cur = this.managedQuery(imgPath, projection, null, null, null);
 		 	  String thumbData = "";
 		 	 
 		 	  if (cur.moveToFirst()) {
 		 		  
 		 		int nameColumn, dataColumn, heightColumn, widthColumn, mimeTypeColumn, resolutionColumn;

	 			nameColumn = cur.getColumnIndex(Video.Media._ID);
	 	        dataColumn = cur.getColumnIndex(Video.Media.DATA);
	 	        mimeTypeColumn = cur.getColumnIndex(Video.Media.MIME_TYPE);
	 	        resolutionColumn = cur.getColumnIndex(Video.Media.RESOLUTION);

 		       String imgPath4 = imgPath.getEncodedPath(); 
 		       mf = new MediaFile();
 		       
 		       thumbData = cur.getString(dataColumn);
 		       mimeType = cur.getString(mimeTypeColumn);
 		       fVideo = new File(thumbData);
 			   mf.setFilePath(fVideo.getPath());
 			   String resolution = cur.getString(resolutionColumn);
 			   if (resolution != null){
	 			   String[] resx = resolution.split("x");
	 			   xRes = resx[0];
	 			   yRes = resx[1];
 			   }
 			   else{
 				   // set the width of the video to the thumbnail width, else 640x480
 				   if (!sMaxImageWidth.equals("Original Size")){
 					  xRes = sMaxImageWidth;
 					  yRes = String.valueOf(Math.round(Integer.valueOf(sMaxImageWidth) * 0.75));
 				   }
 				   else{
 					   xRes = "640";
 					   yRes = "480";
 				   }
 				   
 			   }
 				

 		 	  }
  	   }
  	   else{ //file is not in media library
  		   fVideo = new File(videoUri.toString().replace("file://", ""));
  	   }
  	   
  	   imageTitle = fVideo.getName();

         //try to upload the video
         Map<String, Object> m = new HashMap<String, Object>();
         
         HashMap hPost = new HashMap();
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
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 			mediaErrorMsg = e.getMessage();
 			xmlrpcError = true;
 			break;
 		}
 				
 				HashMap contentHash = new HashMap();
 				    
 				contentHash = (HashMap) result;

 				String resultURL = contentHash.get("url").toString();
 				if (contentHash.containsKey("videopress_shortcode")){
 					resultURL = contentHash.get("videopress_shortcode").toString() + "\n";
 				}
 				else{
 					resultURL = "<object classid=\"clsid:02BF25D5-8C17-4B23-BC80-D3488ABDDC6B\" width=\"" + xRes + "\" height=\"" + (Integer.valueOf(yRes) + 16) + "\" codebase=\"http://www.apple.com/qtactivex/qtplugin.cab\"><param name=\"scale\" value=\"aspect\"><param name=\"src\" value=\"" + resultURL + "\" /><param name=\"autoplay\" value=\"false\" /><param name=\"controller\" value=\"true\" /><object type=\"video/quicktime\" data=\"" + resultURL + "\" width=\"" + xRes + "\" height=\"" + (Integer.valueOf(yRes) + 16) + "\"><param name=\"scale\" value=\"aspect\"><param name=\"autoplay\" value=\"false\" /><param name=\"controller\" value=\"true\" /></object></object>\n";
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
		 	   String imgID = imageUri.getLastPathSegment();
		 	   
		 	   long imgID2 = Long.parseLong(imgID);
		 	   
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
		 		  
		 		int nameColumn, dataColumn, heightColumn, widthColumn, mimeTypeColumn, orientationColumn;

		 			nameColumn = cur.getColumnIndex(Images.Media._ID);
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
 		   jpeg = new File(imageUri.toString().replace("file://", ""));
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
	
	if (i == 0){
		  finalBytes = imageHelper.createThumbnail(bytes, sMaxImageWidth, orientation);
	   }
	   else{
		  finalBytes = bytes;
	   }
 	   	

        //try to upload the image
        Map<String, Object> m = new HashMap<String, Object>();
        
        HashMap hPost = new HashMap();
        m.put("name", imageTitle);
        m.put("type", mimeType);
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
			   	        		content = content + "<a alt=\"image\" href=\"" + finalImageUrl + "\"><img " + centerCSS + "alt=\"image\" src=\"" + finalThumbnailUrl + "\" /></a>\n\n";
			   	        	}
			   	        	else{
			   	        		content = content + "\n<a alt=\"image\" href=\"" + finalImageUrl + "\"><img " + centerCSS + "alt=\"image\" src=\"" + finalThumbnailUrl + "\" /></a>";
			   	        	}        		
			           	
			           		
			           }
		           }
		           else{
			           if (i == 0 && sFullSizeImage == false && resultURL != null)
			           {

			   	        	if (sImagePlacement.equals("Above Text")){
			   	        		
			   	        		content = content + "<img " + centerCSS + "alt=\"image\" src=\"" + finalThumbnailUrl + "\" />\n\n";
			   	        	}
			   	        	else{
			   	        		content = content + "\n<img " + centerCSS + "alt=\"image\" src=\"" + finalThumbnailUrl + "\" />";
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
					e.printStackTrace();
		             
				
		} catch (final XMLRPCException e) {
			
			handler.post(new Runnable() {
				public void run() {

					e.printStackTrace();
					
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


