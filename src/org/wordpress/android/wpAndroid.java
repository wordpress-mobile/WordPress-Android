package org.wordpress.android;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.wordpress.android.moderateCommentsTab.CommentAdapter;
import org.wordpress.android.viewPosts.ViewWrapper;

import com.commonsware.cwac.cache.SimpleWebImageCache;
import com.commonsware.cwac.thumbnail.ThumbnailAdapter;
import com.commonsware.cwac.thumbnail.ThumbnailBus;
import com.commonsware.cwac.thumbnail.ThumbnailMessage;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


public class wpAndroid extends ListActivity {
    /** Called when the activity is first created. */
	public Vector accounts;
	public Vector accountNames = new Vector();
	public String[] accountIDs;
	public String[] blogNames;
	public String[] accountUsers;
	public String[] blavatars;
	private String selectedID = "";
	private ThumbnailAdapter thumbs=null;
	private static final int[] IMAGE_IDS={R.id.blavatar};
	
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        //verify that the user has accepted the EULA
        boolean eula = checkEULA();
        if (eula == false){
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(wpAndroid.this);
			  dialogBuilder.setTitle(R.string.eula);
            dialogBuilder.setMessage(R.string.eula_content);
            dialogBuilder.setPositiveButton(R.string.accept,  new
          		  DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                      // User clicked Accept so set that they've agreed to the eula.
                  	eulaDB eulaDB = new eulaDB(wpAndroid.this);
                    eulaDB.setEULA(wpAndroid.this);
                    displayAccounts();
              
                  }
              });
            dialogBuilder.setNegativeButton(R.string.decline, new
          		  DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                      finish();  //goodbye!
                  }
              });
            dialogBuilder.setCancelable(false);
           dialogBuilder.create().show();	
        }
        else{
        displayAccounts();
        }  		
    }
    
    
    
    public boolean checkEULA(){
    	eulaDB eulaDB = new eulaDB(this);
        boolean sEULA = eulaDB.checkEULA(this);
        
    	return sEULA;
    	
    }
    
public void displayAccounts(){
	
	setContentView(R.layout.home);
	setTitle(getResources().getText(R.string.app_name) + " - " + getResources().getText(R.string.blogs));
    
	//settings time!
    settingsDB settingsDB = new settingsDB(this);
	accounts = settingsDB.getAccounts(this);
	
	//upload stats
	checkStats(accounts.size());
	
	 ListView listView = (ListView) findViewById(android.R.id.list);
	 
	/* TextView tv = new TextView(this);
     tv.setText("Blogs");
     tv.setBackgroundDrawable(getResources().getDrawable(R.drawable.list_header_bg));
 	 tv.setTextSize(20);
 	 tv.setPadding(4, 4, 4, 4);
     tv.setTextColor(Color.parseColor("#EEEEEE"));
     tv.setShadowLayer(1, 1, 1, Color.parseColor("#444444"));
     listView.addHeaderView(tv, null, false);*/
	 
	 ImageView iv = new ImageView(this);
	 iv.setBackgroundDrawable(getResources().getDrawable(R.drawable.list_divider));
	 listView.addFooterView(iv);
	 listView.setVerticalFadingEdgeEnabled(false);
	 listView.setVerticalScrollBarEnabled(false);


	   listView.setOnItemClickListener(new OnItemClickListener() {
		   
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}

			public void onItemClick(AdapterView<?> arg0, View row,int position, long id) {
				Bundle bundle = new Bundle();
        		bundle.putString("accountName", blogNames[position]);
        		bundle.putString("id", String.valueOf(row.getId()));
        		Intent viewPostsIntent = new Intent(wpAndroid.this, tabView.class);
        		viewPostsIntent.putExtras(bundle);
            	startActivityForResult(viewPostsIntent , 1);
				
			}
			
			

      });

	   listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

           public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo) {

        	   AdapterView.AdapterContextMenuInfo info =
                   (AdapterView.AdapterContextMenuInfo) menuInfo;
        	   
        	   View row = info.targetView;
        	   
        	   selectedID = String.valueOf(row.getId());
               
         
        	   menu.add(0, 0, 0, getResources().getText(R.string.remove_account));
		}
      });
	
	
	if (accounts.size() > 0){
		ScrollView sv = new ScrollView(this);
		sv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
		sv.setBackgroundColor(Color.parseColor("#e8e8e8"));
		LinearLayout layout = new LinearLayout(this);
		layout.setPadding(10, 10, 10, 0);
		layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));

		layout.setOrientation(LinearLayout.VERTICAL);
        
		blogNames = new String[accounts.size()];
		accountIDs = new String[accounts.size()];
		accountUsers = new String[accounts.size()];
		blavatars = new String[accounts.size()];
		
        for (int i = 0; i < accounts.size(); i++) {
            
        	HashMap curHash = (HashMap) accounts.get(i);
        	blogNames[i] = curHash.get("blogName").toString();
        	accountUsers[i] = curHash.get("username").toString();
        	accountIDs[i] = curHash.get("id").toString();
        	String url = curHash.get("url").toString();
        	url = url.replace("http://", "");
        	url = url.replace("https://", "");
        	String[] urlSplit = url.split("/");
        	url = urlSplit[0];
        	url = "http://gravatar.com/blavatar/" + moderateCommentsTab.getMd5Hash(url.trim()) + "?s=60&d=404";
        	blavatars[i] = url;
        	accountNames.add(i, blogNames[i]);
        	
        } 
        
        ThumbnailBus bus = new ThumbnailBus();
		thumbs=new ThumbnailAdapter(this, new HomeListAdapter(this),new SimpleWebImageCache<ThumbnailBus, ThumbnailMessage>(null, null, 101, bus),IMAGE_IDS);

        setListAdapter(thumbs);
	}
	else{
		//no account, load new account view
		Intent i = new Intent(wpAndroid.this, newAccount.class);

    	startActivityForResult(i, 0);

	}
}
private void checkStats(final int numBlogs) {
	
	
	eulaDB eulaDB = new eulaDB(this);
	long lastStatsDate = eulaDB.getStatsDate(this);
	long now = System.currentTimeMillis();

	if ((now - lastStatsDate) > 604800000){  //works for first check as well
		new Thread() {
	        public void run() { 	  
	        uploadStats(numBlogs);
	        }
	    }.start();
		eulaDB.setStatsDate(this);
	}
	
}



private void uploadStats(int numBlogs) {
	
	//gather all of the device info
	
    PackageManager pm = getPackageManager();
    String app_version = "";
    try {
		try {
			PackageInfo pi = pm.getPackageInfo("org.wordpress.android", 0);
			app_version = pi.versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			app_version = "N/A";
		}
		
		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String device_uuid = tm.getDeviceId();
		if (device_uuid == null){
			device_uuid = "N/A";
		}
		String device_language = getResources().getConfiguration().locale.getLanguage();
		String mobile_country_code = tm.getNetworkCountryIso();
		String mobile_network_number = tm.getNetworkOperator();
		int network_type = tm.getNetworkType();
		
		//get the network type string
		String mobile_network_type = "N/A";
		switch (network_type) {
		case 0: 
			mobile_network_type = "TYPE_UNKNOWN";
			break;
		case 1: 
			mobile_network_type = "GPRS";
			break;	
		case 2: 
			mobile_network_type = "EDGE";
			break;	
		case 3: 
			mobile_network_type = "UMTS";
			break;	
		case 4: 
			mobile_network_type = "CDMA";
			break;	
		case 5: 
			mobile_network_type = "EVDO_0";
			break;
		case 6: 
			mobile_network_type = "EVDO_A";
			break;
		case 7: 
			mobile_network_type = "1xRTT";
			break;
		case 8: 
			mobile_network_type = "HSDPA";
			break;
		case 9: 
			mobile_network_type = "HSUPA";
			break;
		case 10: 
			mobile_network_type = "HSPA";
			break;
		}
		
		String device_version = android.os.Build.VERSION.RELEASE;
		
		if (device_version == null){
			device_version = "N/A";
		}
		int num_blogs = numBlogs;
		
		//post the data
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost("http://api.wordpress.org/androidapp/update-check/1.0/");
		post.setHeader("Content-Type", "application/x-www-form-urlencoded");
		
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("device_uuid", device_uuid));
		pairs.add(new BasicNameValuePair("app_version", app_version));
		pairs.add(new BasicNameValuePair("device_language", device_language));
		pairs.add(new BasicNameValuePair("mobile_country_code", mobile_country_code));
		pairs.add(new BasicNameValuePair("mobile_network_number", mobile_network_number));
		pairs.add(new BasicNameValuePair("mobile_network_type", mobile_network_type));
		pairs.add(new BasicNameValuePair("device_version", device_version));
		pairs.add(new BasicNameValuePair("num_blogs", String.valueOf(num_blogs)));
		try {
			post.setEntity(new UrlEncodedFormEntity(pairs));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			HttpResponse response = client.execute(post);
			int responseCode = response.getStatusLine().getStatusCode();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	
}



//Add settings to menu
@Override
public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, 0, 0, getResources().getText(R.string.add_account));
    MenuItem menuItem1 = menu.findItem(0);
    menuItem1.setIcon(R.drawable.ic_menu_add);
    
    menu.add(0, 1, 0, getResources().getText(R.string.notification_settings));
    MenuItem menuItem2 = menu.findItem(1);
    menuItem2.setIcon(R.drawable.ic_menu_notifications);
    
    return true;
}
//Menu actions
@Override
public boolean onOptionsItemSelected(final MenuItem item){
    switch (item.getItemId()) {
    case 0:
    	Intent i = new Intent(this, addAccount.class);

    	startActivityForResult(i, 0);
    	
    	return true;
	case 1:
		Intent i2 = new Intent(this, notificationSettings.class);

		startActivity(i2);
	
		return true;
	}
    return false;
    	
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
		settingsDB settingsDB = new settingsDB(this);
		accounts = settingsDB.getAccounts(this);
		
		if (accounts.size() == 0){
			finish();
		}
		else{
		displayAccounts();
		}
	    //Toast.makeText(wpAndroid.this, title, Toast.LENGTH_SHORT).show();
	    break;
	}
}//end null check
	else{
		displayAccounts();
	}
}


@Override
public boolean onContextItemSelected(MenuItem item) {

     /* Switch on the ID of the item, to get what the user selected. */
     switch (item.getItemId()) {
     	  case 0:
     		 AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(wpAndroid.this);
   		  dialogBuilder.setTitle(getResources().getText(R.string.remove_account));
         dialogBuilder.setMessage(getResources().getText(R.string.sure_to_remove_account));
         dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),  new
       		  DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int whichButton) {
                   // remove the account
               	settingsDB settingsDB = new settingsDB(wpAndroid.this);
                 boolean deleteSuccess = settingsDB.deleteAccount(wpAndroid.this, selectedID);
                 if (deleteSuccess)
                 {
               	  Toast.makeText(wpAndroid.this, getResources().getText(R.string.account_removed_successfully),
                             Toast.LENGTH_SHORT).show();
               	  displayAccounts();
                 }
                 else
                 {
               	  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(wpAndroid.this);
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
         	dialogBuilder.setNegativeButton(getResources().getText(R.string.no),  new
         		  DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int whichButton) {
                   // Just close the window.
           
               }
           });
         dialogBuilder.setCancelable(false);
        dialogBuilder.create().show();
   	
   	
   	return true;      
     }
     return false;
}

private class HomeListAdapter extends BaseAdapter {
	private int usenameHeight;
    public HomeListAdapter(Context context) {
        mContext = context;
    }

    public int getCount() {
        return accounts.size();
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
    		pv=inflater.inflate(R.layout.home_row, parent, false);
    		wrapper=new ViewWrapper(pv);
    		if (position == 0){
    			usenameHeight = wrapper.getBlogUsername().getHeight();
    		}
    		pv.setTag(wrapper);
    	wrapper=new ViewWrapper(pv);
    	pv.setTag(wrapper);
    	}
    	else {
    	wrapper=(ViewWrapper)pv.getTag();      	
    	}
    	String username= accountUsers[position];
    	pv.setBackgroundDrawable(getResources().getDrawable(R.drawable.list_bg_selector));
		pv.setId(Integer.valueOf(accountIDs[position]));
		if (wrapper.getBlogUsername().getHeight() == 0){
			wrapper.getBlogUsername().setHeight((int) wrapper.getBlogName().getTextSize() + wrapper.getBlogUsername().getPaddingBottom());
		}

    	wrapper.getBlogName().setText(escapeUtils.unescapeHtml(blogNames[position]));
    	wrapper.getBlogUsername().setText(escapeUtils.unescapeHtml(username));
    	
    	if (wrapper.getBlavatar()!=null) {
			try {
				wrapper.getBlavatar().setImageResource(R.drawable.app_icon);
				wrapper.getBlavatar().setTag(blavatars[position]);
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
		}
    	
    	return pv;

    }

    private Context mContext;
    
}

class ViewWrapper {
	View base;
	TextView blogName=null;
	TextView blogUsername=null;
	ImageView blavatar=null;
	ViewWrapper(View base) {
	this.base=base;
	}
	TextView getBlogName() {
			if (blogName==null) {
			blogName=(TextView)base.findViewById(R.id.blogName);
			}
			return(blogName);
			}
		TextView getBlogUsername() {
			if (blogUsername==null) {
				blogUsername=(TextView)base.findViewById(R.id.blogUser);
			}
			return(blogUsername);
			}
		ImageView getBlavatar() {
			if (blavatar==null) {
				blavatar=(ImageView)base.findViewById(R.id.blavatar);
			}
			return(blavatar);
		}
}

@Override
public void onConfigurationChanged(Configuration newConfig) {
  //ignore orientation change
  super.onConfigurationChanged(newConfig);
} 

}




