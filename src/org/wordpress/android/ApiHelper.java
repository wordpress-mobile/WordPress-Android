package org.wordpress.android;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Vector;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;


public class ApiHelper extends Activity {
    /** Called when the activity is first created. */
	private static XMLRPCClient client;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }
    
    @SuppressWarnings("unchecked")
	static void refreshComments(final String id, final Context ctx) {

		Vector<Object> settings = new Vector<Object>();
		final WordPressDB settingsDB = new WordPressDB(ctx);
		settings = settingsDB.loadSettings(ctx, id); 

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

		HashMap<String, Object> hPost = new HashMap<String, Object>();
		hPost.put("status", "");
		hPost.put("post_id", "");
		hPost.put("number", 30);  


		Object[] params = {
				sBlogId,
				sUsername,
				sPassword,
				hPost
		};
		Object[] result = null;
		try {
			result = (Object[]) client.call("wp.getComments", params);
		} catch (XMLRPCException e) {
		}

		if (result != null){
			if (result.length > 0){
				String author, postID, commentID, comment, dateCreated, dateCreatedFormatted, status, authorEmail, authorURL, postTitle;
	
				HashMap<Object, Object> contentHash = new HashMap<Object, Object>();
				Vector<HashMap<String, String>> dbVector = new Vector<HashMap<String, String>>();
	
				Date d = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
				Calendar cal = Calendar.getInstance();
				TimeZone tz = cal.getTimeZone();
				String shortDisplayName = "";
				shortDisplayName = tz.getDisplayName(true, TimeZone.SHORT);
				//loop this!
				for (int ctr = 0; ctr < result.length; ctr++){
					HashMap<String, String> dbValues = new HashMap<String, String>();
					contentHash = (HashMap) result[ctr];
					comment = contentHash.get("content").toString();
					author = contentHash.get("author").toString();
					status = contentHash.get("status").toString();
					postID = contentHash.get("post_id").toString();
					commentID = contentHash.get("comment_id").toString();
					dateCreated = contentHash.get("date_created_gmt").toString();
					authorURL = contentHash.get("author_url").toString();
					authorEmail = contentHash.get("author_email").toString();
					postTitle = contentHash.get("post_title").toString();
	
					//make the date pretty
					String cDate = dateCreated.replace(tz.getID(), shortDisplayName);
					try{  
						d = sdf.parse(cDate);
						SimpleDateFormat sdfOut = new SimpleDateFormat("MMMM dd, yyyy hh:mm a"); 
						dateCreatedFormatted = sdfOut.format(d);
					} catch (ParseException pe){  
						pe.printStackTrace();
						dateCreatedFormatted = dateCreated;  //just make it the ugly date if it doesn't work
					} 
	
					dbValues.put("blogID", id);
					dbValues.put("postID", postID);
					dbValues.put("commentID", commentID);
					dbValues.put("author", author);
					dbValues.put("comment", comment);
					dbValues.put("commentDate", dateCreated);
					dbValues.put("commentDateFormatted", dateCreatedFormatted);
					dbValues.put("status", status);
					dbValues.put("url", authorURL);
					dbValues.put("email", authorEmail);
					dbValues.put("postTitle", postTitle);
					dbVector.add(ctr, dbValues);
				}
	
				settingsDB.saveComments(ctx, dbVector, false);
	
	
			}
	
		}
    }

}



