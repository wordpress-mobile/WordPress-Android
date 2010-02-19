package org.wordpress.android;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


public class wpAndroid extends Activity {
    /** Called when the activity is first created. */
	public Vector accounts;
	public Vector accountNames = new Vector();
	private String selectedID = "";
	
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
	//settings time!
    settingsDB settingsDB = new settingsDB(this);
	accounts = settingsDB.getAccounts(this);
	
	
	if (accounts.size() > 0){
		checkStats(accounts.size());
		ScrollView sv = new ScrollView(this);
		sv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
		sv.setBackgroundColor(Color.parseColor("#e8e8e8"));
		LinearLayout layout = new LinearLayout(this);
		layout.setPadding(10, 10, 10, 0);
		layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));

		layout.setOrientation(LinearLayout.VERTICAL);
        
        for (int i = 0; i < accounts.size(); i++) {
            
        	HashMap curHash = (HashMap) accounts.get(i);
        	String curBlogName = curHash.get("blogName").toString();
        	String curUsername = curHash.get("username").toString();
        	String accountID = curHash.get("id").toString();
        	accountNames.add(i, curBlogName);
        	layout.setBackgroundColor(Color.parseColor("#e8e8e8"));
            
            final customButton buttonView = new customButton(this);
            buttonView.setTextColor(Color.parseColor("#444444"));
            buttonView.setTextSize(18);
            buttonView.setText(escapeUtils.unescapeHtml(curBlogName) + "\n" + "(" + curUsername + ")");
            buttonView.setId(i);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams 
            (LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            //params.setMargins(0, 0, 0, 6);
            buttonView.setLayoutParams(params);

            
            buttonView.setOnClickListener(new customButton.OnClickListener() {
                public void onClick(View v) {
                	
                	for (int i = 0; i < accounts.size(); i++) {
                	HashMap btnHash = (HashMap) accounts.get(i);
                	String btnText = buttonView.getText().toString();
                	
                	if (i == buttonView.getId()){
                		
                		Bundle bundle = new Bundle();
                		bundle.putString("accountName", accountNames.get(i).toString());
                		bundle.putString("id", btnHash.get("id").toString());
                		Intent viewPostsIntent = new Intent(wpAndroid.this, tabView.class);
                		viewPostsIntent.putExtras(bundle);
                    	startActivityForResult(viewPostsIntent , 1);
                		
                	}
                	}
                	
                }
            });
            
            
            buttonView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

	               public void onCreateContextMenu(ContextMenu menu, View v,
						ContextMenuInfo menuInfo) {

	                   for (int i = 0; i < accounts.size(); i++) {
	                   	HashMap btnHash = (HashMap) accounts.get(i);
	                   	String btnText = buttonView.getText().toString();
	                   	
	                   	if (i == buttonView.getId()){
	                   		
	                   		selectedID = btnHash.get("id").toString();
	                   		
	                   	}
	                   	}
	                   
	             
	            	   menu.add(0, 0, 0, "Remove Account");
				}
	          });
            
            layout.addView(buttonView);  
        } 
        TextView textView = new TextView(this);
        textView.setTextColor(Color.parseColor("#444444"));
        textView.setTextSize(12);
        textView.setPadding(0, 20, 0, 0);
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setText(R.string.hint_menu_to_add_acc);
        
        
        
        layout.addView(textView);
        
        sv.addView(layout);
        
        setContentView(sv);
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
    menu.add(0, 0, 0, "Add Account");
    MenuItem menuItem1 = menu.findItem(0);
    menuItem1.setIcon(R.drawable.ic_menu_preferences);
    
    menu.add(0, 1, 0, "Notification Settings");
    MenuItem menuItem2 = menu.findItem(1);
    menuItem2.setIcon(R.drawable.ic_menu_notifications);
    
    return true;
}
//Menu actions
@Override
public boolean onOptionsItemSelected(final MenuItem item){
    switch (item.getItemId()) {
    case 0:
    	Intent i = new Intent(this, newAccount.class);

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
   		  dialogBuilder.setTitle("Remove Account");
         dialogBuilder.setMessage("Are you sure you want to remove this account?");
         dialogBuilder.setPositiveButton("Yes",  new
       		  DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int whichButton) {
                   // remove the account
               	settingsDB settingsDB = new settingsDB(wpAndroid.this);
                 boolean deleteSuccess = settingsDB.deleteAccount(wpAndroid.this, selectedID);
                 if (deleteSuccess)
                 {
               	  Toast.makeText(wpAndroid.this, "Account removed successfully",
                             Toast.LENGTH_SHORT).show();
               	  displayAccounts();
                 }
                 else
                 {
               	  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(wpAndroid.this);
         			  dialogBuilder.setTitle("Error");
                     dialogBuilder.setMessage("Could not remove account, you may need to reinstall WordPress");
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
         dialogBuilder.setNegativeButton("No", new
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


}




