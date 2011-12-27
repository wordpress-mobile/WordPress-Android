package org.wordpress.android;

import org.wordpress.android.models.Blog;
import org.wordpress.android.util.EscapeUtils;
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

import java.util.HashMap;
import java.util.Timer;
import java.util.Vector;

public class CommentService extends Service {

	public static final String response = "true";
	public static ServiceUpdateUIListener UI_UPDATE_LISTENER;
	public String accountName = "", updateInterval = "";
	public int accountID;
	private XMLRPCClient client;
	private Timer timer = new Timer();

	public static void setUpdateListener(ServiceUpdateUIListener l) {
		  UI_UPDATE_LISTENER = l;
		}

	
	public IBinder onBind(Intent intent) {
	  return null;
	}

	@Override
	public void onCreate() {
	  super.onCreate();
	  
	  if (WordPress.wpDB == null)
			WordPress.wpDB = new WordPressDB(this);
	  
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
    	
    	Vector<?> notificationAccounts = WordPress.wpDB.getNotificationAccounts(this);
    	
    	
    	if (notificationAccounts != null){
    		
    		if (notificationAccounts.size() == 0){

    			this.stopSelf(); //no accounts wanted notifications, bye!
    		}
    		else
    		{
    		for (int i = 0; i < notificationAccounts.size(); i++)
    		{
    			
    			accountID = (Integer)notificationAccounts.get(i);
                Blog blog;
				try {
					blog = new Blog(accountID, CommentService.this);
				} catch (Exception e1) {
					return;
				}
    			accountName = EscapeUtils.unescapeHtml(blog.getBlogName());
    			//Log.i("WordPressCommentService", "Checking Comments for " + accountName);

    	final int latestCommentID = blog.getLastCommentId();
    	
    	String sURL = "";
    	if (blog.getUrl().contains("xmlrpc.php"))
    	{
    		sURL = blog.getUrl();
    	}
    	else
    	{
    		sURL = blog.getUrl() + "xmlrpc.php";
    	}
		String sUsername = blog.getUsername();
		String sPassword = blog.getPassword();
		String sHttpuser = blog.getHttpuser();
		String sHttppassword = blog.getHttppassword();
		
		client = new XMLRPCClient(sURL, sHttpuser, sHttppassword);
    	
        HashMap<String, Object> hPost = new HashMap<String, Object>();
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
			@SuppressWarnings("unchecked")
			public void callFinished(Object[] result) {
				HashMap<?, ?> notificationOptions = WordPress.wpDB.getNotificationOptions(CommentService.this);
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
				
				HashMap<Object, Object> contentHash = new HashMap<Object, Object>();
				
				//loop this!
				    for (int ctr = 0; ctr < result.length; ctr++){
				        contentHash = (HashMap<Object, Object>) result[ctr];
				        ctr++;
				    }

				String commentID = contentHash.get("comment_id").toString();
				if (latestCommentID == 0){
					WordPress.wpDB.updateLatestCommentID(accountID, Integer.valueOf(commentID));
					////Log.i("WordPressCommentService", "comment was zero");
				}
				else if (Integer.valueOf(commentID) > latestCommentID){	
					final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					Intent notificationIntent = new Intent(CommentService.this, Comments.class);
    		  		notificationIntent.setData((Uri.parse("custom://wordpressNotificationIntent"+accountID)));
    		  		notificationIntent.putExtra("id", accountID);
    		  		notificationIntent.putExtra("fromNotification", true);
    		  		PendingIntent pendingIntent = PendingIntent.getActivity(CommentService.this, 0, notificationIntent, Intent.FLAG_ACTIVITY_CLEAR_TOP);
 			  		
    		  		String comment = contentHash.get("content").toString();
    		  		String author = contentHash.get("author").toString();
    		  		
    		  		Notification n = new Notification(R.drawable.notification_icon, author + ": " + comment, System.currentTimeMillis());
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
    		  		n.flags |= Notification.FLAG_AUTO_CANCEL;
 			  		n.setLatestEventInfo(CommentService.this, accountName, author + ": " + comment, pendingIntent);
 			  		nm.notify(22 + Integer.valueOf(accountID), n); //needs a unique id
					
 			  		WordPress.wpDB.updateLatestCommentID(accountID, Integer.valueOf(commentID));
					//Log.i("WordPressCommentService", "found a new comment!");
				}
				else{
					//Log.i("WordPressCommentService", "no new comments");
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
    	}

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
				final Object[] result;
				result = (Object[]) client.call(method, params);
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
						
						e.printStackTrace();
						
					}
				});
			}
		}
	}
}