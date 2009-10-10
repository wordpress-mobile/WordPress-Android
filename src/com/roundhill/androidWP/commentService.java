package com.roundhill.androidWP;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.http.conn.HttpHostConnectException;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import com.roundhill.androidWP.editPost.XMLRPCMethodCallback;

import android.R.integer;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class commentService extends Service {

	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// constants
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	public static final String ServletUri = "http://danroundhill.com/xmlrpc.php";
	public static final String response = "true";
	public static ServiceUpdateUIListener UI_UPDATE_LISTENER;
	public String accountID = "", accountName = "", updateInterval = "";
	private XMLRPCClient client;
	private Timer timer = new Timer();
	private static long UPDATE_INTERVAL = 360000;  //default to hourly

	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// hooks into other activities
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	/*public static void setMainActivity(MainActivity activity) {
	  MAIN_ACTIVITY = activity;
	}*/

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
	  
	  
	  // if (MAIN_ACTIVITY != null) AppUtils.showToastShort(MAIN_ACTIVITY, "MyService started");
	}
	
    @Override
    public void onStart(Intent intent, int startId) {
        //Log.i("ServiceStartArguments","Starting #" + startId + ": " + intent.getExtras());
        Bundle extras = intent.getExtras();
        if(extras !=null)
        {
         accountID = extras.getString("id");
         accountName = extras.getString("accountName");
         updateInterval = extras.getString("updateInterval");
         
         //configure time interval
         if (updateInterval.equals("5 Minutes")){
        	 UPDATE_INTERVAL = 300000;
         }
         else if (updateInterval.equals("10 Minutes")){
        	 UPDATE_INTERVAL = 600000;
         }
         else if (updateInterval.equals("15 Minutes")){
        	 UPDATE_INTERVAL = 900000;
         }
         else if (updateInterval.equals("30 Minutes")){
        	 UPDATE_INTERVAL = 1800000;
         }
         else if (updateInterval.equals("1 Hour")){
        	 UPDATE_INTERVAL = 3600000;
         }
         else if (updateInterval.equals("3 Hours")){
        	 UPDATE_INTERVAL = 10800000;
         }
         else if (updateInterval.equals("6 Hours")){
        	 UPDATE_INTERVAL = 21600000;
         }
         else if (updateInterval.equals("12 Hours")){
        	 UPDATE_INTERVAL = 43200000;
         }
         else if (updateInterval.equals("Daily")){
        	 UPDATE_INTERVAL = 86400000;
         }
         
         
        }
    }


	@Override
	public void onDestroy() {
	  super.onDestroy();

	  _shutdownService();

	 // if (MAIN_ACTIVITY != null) AppUtils.showToastShort(MAIN_ACTIVITY, "MyService stopped");
	}

	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// service business logic
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	private void _startService() {
	  timer.scheduleAtFixedRate(
	      new TimerTask() {
	        public void run() {
	        	
	          _getWeatherUpdate();
	        }
	      },
	      0,
	      UPDATE_INTERVAL);
	  //Log.i(getClass().getSimpleName(), "Timer started!!!");
	}

	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// weather data that the service gets...
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	//public static Hashtable<String, Hashtable<String, String>> DataFromServlet =
	//    new Hashtable<String, Hashtable<String, String>>();

	/** dont forget to fire update to the ui listener */
	private void _getWeatherUpdate() {
		
		
		//just notify for now
		/*Intent appIntent = new Intent(this, com.roundhill.androidWP.wpAndroid.class); 
		  
		  */
		

		//Log.i(getClass().getSimpleName(), "Timer Cycled!");
		
		settingsDB settingsDB = new settingsDB(this);
    	
    	Vector notificationAccounts = settingsDB.getNotificationAccounts(this);
    	
    	
    	if (notificationAccounts != null){

    		for (int i = 0; i < notificationAccounts.size(); i++)
    		{
    			
    			accountID = notificationAccounts.get(i).toString();
    			accountName = settingsDB.getAccountName(this, accountID);
    			Log.i("wpToGoCommentService", "Checking Comments for " + accountName);

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
		
    	//Log.i("wpToGoCommentService", "XMLRPCTIME!");

        Object[] params = {
        		1,
        		sUsername,
        		sPassword,
        		hPost
        };
		    
        XMLRPCMethodCallback callBack = new XMLRPCMethodCallback() {
			public void callFinished(Object[] result) {
				String s = "done";
				//Log.i(getClass().getSimpleName(), "made it to callback");
				settingsDB settingsDB = new settingsDB(commentService.this);
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
					Log.i("wpToGoCommentService", "comment was zero");
				}
				else if (Integer.valueOf(commentID) > latestCommentID){
					UI_UPDATE_LISTENER.updateUI(accountID, accountName); //new comment!
					settingsDB.updateLatestCommentID(commentService.this, accountID, Integer.valueOf(commentID));
					Log.i("wpToGoCommentService", "found a new comment!");
				}
				else{
					Log.i("wpToGoCommentService", "no new comments");
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
        
    		} //end for loop
    	}  // end if

	}

	private void _shutdownService() {
	  if (timer != null) timer.cancel();
	  //Log.i(getClass().getSimpleName(), "Timer stopped!!!");
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
				// TODO Auto-generated catch block
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
						//Log.d("Test", "error", e);
						
					}
				});
			}
			
		}
	}

	}//end class MyService


