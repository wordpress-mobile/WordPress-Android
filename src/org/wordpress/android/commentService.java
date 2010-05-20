package org.wordpress.android;

import java.util.HashMap;
import java.util.Timer;
import java.util.Vector;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

public class commentService extends Service {

	public static final String response = "true";
	public static ServiceUpdateUIListener UI_UPDATE_LISTENER;
	public String accountID = "", accountName = "", updateInterval = "";
	private XMLRPCClient client;
	private Timer timer = new Timer();
	private static long UPDATE_INTERVAL = 360000;  //default to hourly

	public static void setUpdateListener(ServiceUpdateUIListener l) {
		  UI_UPDATE_LISTENER = l;
		}

	
	/** not using ipc... dont care about this method */
	public IBinder onBind(Intent intent) {
	  return null;
	}

	@Override
	public void onCreate() {
	  super.onCreate();
	  // init the service here
	  _startService();

	}
	
    @Override
    public void onStart(Intent intent, int startId) {
       
    }


	@Override
	public void onDestroy() {
	  super.onDestroy();

	  _shutdownService();

	}


	private void _startService() {

		Thread t = new Thread() {
			public void run() {
	          _getUpdatedComments();
			}	
		};
		t.start();
	
	}



	/** dont forget to fire update to the ui listener */
	private void _getUpdatedComments() {
		
		//need to create eulaDB first in case the user reboots their device before launching the app
		WordPressDB settingsDB = new WordPressDB(this);
    	
    	Vector notificationAccounts = settingsDB.getNotificationAccounts(this);
    	
    	
    	if (notificationAccounts != null){
    		
    		if (notificationAccounts.size() == 0){

    			this.stopSelf(); //no accounts wanted notifications, bye!
    		}
    		else
    		{
    		for (int i = 0; i < notificationAccounts.size(); i++)
    		{
    			
    			accountID = notificationAccounts.get(i).toString();
    			accountName = settingsDB.getAccountName(this, accountID);
    			Log.i("WordPressCommentService", "Checking Comments for " + accountName);

    	Vector settings = settingsDB.loadSettings(this, accountID);
    	final int latestCommentID = settingsDB.getLatestCommentID(this, accountID);
    	
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
		
		client = new XMLRPCClient(sURL);
    	
        HashMap hPost = new HashMap();
        hPost.put("status", "");
        hPost.put("post_id", "");
        hPost.put("number", 1);

        Object[] params = {
        		1,
        		sUsername,
        		sPassword,
        		hPost
        };
		    
        XMLRPCMethodCallback callBack = new XMLRPCMethodCallback() {
			public void callFinished(Object[] result) {
				String s = "done";
				WordPressDB settingsDB = new WordPressDB(commentService.this);
				HashMap notificationOptions = settingsDB.getNotificationOptions(commentService.this);
				boolean sound = false, vibrate = false, light = false;
				
				//there must be a less dorky way of pulling a boolean value from a db?
				if (notificationOptions != null){
					if (notificationOptions.get("sound").toString().equals("1")){
						sound = true;
					}
					if (notificationOptions.get("vibrate").toString().equals("1")){
						vibrate = true;
					}
					if (notificationOptions.get("light").toString().equals("1")){
						light = true;
					}
				}
				if (result.length == 0){

				}
				else{
				s = result.toString();
				
				HashMap contentHash = new HashMap();
			    
				int ctr = 0;
				
				//loop this!
				    for (Object item : result){
				        contentHash = (HashMap) result[ctr];

				        ctr++;
				    }
				

				String commentID = contentHash.get("comment_id").toString();
				if (latestCommentID == 0){
					settingsDB.updateLatestCommentID(commentService.this, accountID, Integer.valueOf(commentID));
					Log.i("WordPressCommentService", "comment was zero");
				}
				else if (Integer.valueOf(commentID) > latestCommentID){
					final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					Intent notificationIntent = new Intent(commentService.this, tabView.class);
    		  		notificationIntent.setData((Uri.parse("custom://wordpressNotificationIntent"+accountID)));
    		  		notificationIntent.putExtra("id", accountID);
    		  		notificationIntent.putExtra("accountName", accountName);
    		  		notificationIntent.putExtra("fromNotification", true);
    		  		PendingIntent pendingIntent = PendingIntent.getActivity(commentService.this, 0, notificationIntent, Intent.FLAG_ACTIVITY_CLEAR_TOP);
 			  			  		
    		  		Notification n = new Notification(R.drawable.wp_logo, getResources().getText(R.string.new_comment), System.currentTimeMillis());
    		  		if (sound){
    		  		n.defaults |= Notification.DEFAULT_SOUND;
    		  		}
    		  		if (vibrate){
    		  			n.defaults |= Notification.DEFAULT_VIBRATE;
    		  		}
    		  		if (light){
    		  		n.ledARGB = 0xff0000ff;
    		  		n.ledOnMS = 1000;
    		  		n.ledOffMS = 5000;
    		  		n.flags |= Notification.FLAG_SHOW_LIGHTS;
    		  		}
 			  		n.setLatestEventInfo(commentService.this, accountName, getResources().getText(R.string.new_comment), pendingIntent);
 			  		nm.notify(22 + Integer.valueOf(accountID), n); //needs a unique id
					
					settingsDB.updateLatestCommentID(commentService.this, accountID, Integer.valueOf(commentID));
					Log.i("WordPressCommentService", "found a new comment!");
				}
				else{
					Log.i("WordPressCommentService", "no new comments");
				}

				}  
	        

			}
        };
        final Object[] result;
		try {
			result = (Object[]) client.call("wp.getComments", params);
			callBack.callFinished(result);
		} catch (XMLRPCException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    		} 
    		}
    	}  // end if

	}

	private void _shutdownService() {
	  if (timer != null) timer.cancel();
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
			Looper.prepare();
			try {
				handler = new Handler();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
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
				final Object[] result;
				result = (Object[]) client.call(method, params);
				final long t1 = System.currentTimeMillis();
				handler.post(new Runnable() {
					public void run() {

						callBack.callFinished(result);
						Looper.myLooper().quit();
					}
				});
			} catch (final XMLRPCFault e) {
				e.printStackTrace();
			             
					
			} catch (final XMLRPCException e) {
				
				handler.post(new Runnable() {
					public void run() {
						
						Throwable couse = e.getCause();
						e.printStackTrace();
						
					}
				});
			}
			
		}
	}

}