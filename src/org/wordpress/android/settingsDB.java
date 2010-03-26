package org.wordpress.android;

import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.View.OnClickListener;

public class settingsDB {

	private static final String CREATE_TABLE_SETTINGS = "create table if not exists accounts (id integer primary key autoincrement, "
			+ "url text, blogName text, username text, password text, imagePlacement text, centerThumbnail boolean, fullSizeImage boolean, maxImageWidth text, maxImageWidthId integer, lastCommentId integer, runService boolean);";
	private static final String SETTINGS_TABLE = "accounts";
	private static final String DATABASE_NAME = "wordpress";
	
	private static final int DATABASE_VERSION = 3;
	
	//for capturing blogID, trac ticket #
	private static final String ADD_BLOGID = "alter table accounts add blogId integer;";
	private static final String UPDATE_BLOGID = "update accounts set blogId = 1;"; //set them all to 1 if updating
	
	//add notification options
	private static final String ADD_SOUND_OPTION = "alter table eula add sound boolean default false;";
	private static final String ADD_VIBRATE_OPTION = "alter table eula add vibrate boolean default false;";
	private static final String ADD_LIGHT_OPTION = "alter table eula add light boolean default false;";
	
	
	private SQLiteDatabase db;

	public settingsDB(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		//db.execSQL("DROP TABLE IF EXISTS "+ SETTINGS_TABLE);
		db.execSQL(CREATE_TABLE_SETTINGS);
		int test = db.getVersion();
		
		if (db.getVersion() <= 1){ //user is new install or running v1.0.0 or v1.0.1
			db.execSQL(ADD_BLOGID);
			db.execSQL(UPDATE_BLOGID);
			db.execSQL(ADD_SOUND_OPTION);
			db.execSQL(ADD_VIBRATE_OPTION);
			db.execSQL(ADD_LIGHT_OPTION);
		}
		else if (db.getVersion()  == 2){
			db.execSQL(ADD_SOUND_OPTION);
			db.execSQL(ADD_VIBRATE_OPTION);
			db.execSQL(ADD_LIGHT_OPTION);
		}

		db.setVersion(DATABASE_VERSION); //set to latest revision

		db.close();
		
	}

	
	public boolean addAccount(Context ctx, String url, String blogName, String username, String password, String imagePlacement, boolean centerThumbnail, boolean fullSizeImage, String maxImageWidth, int maxImageWidthId, boolean runService, int blogId) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		ContentValues values = new ContentValues();
		values.put("url", url);
		values.put("blogName", blogName);
		values.put("username", username);
		values.put("password", password);
		values.put("imagePlacement", imagePlacement);
		values.put("centerThumbnail", centerThumbnail);
		values.put("fullSizeImage", fullSizeImage);
		values.put("maxImageWidth", maxImageWidth);
		values.put("maxImageWidthId", maxImageWidthId);
		values.put("runService", runService);
		values.put("blogId", blogId);
		boolean returnValue = db.insert(SETTINGS_TABLE, null, values) > 0;
		db.close();
		return (returnValue);
	}	
	public Vector getAccounts(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Cursor c = db.query(SETTINGS_TABLE, new String[] { "id", "blogName", "username", "runService", "blogId", "url"}, null, null, null, null, null);
		String id;
		String blogName, username, url;
		int blogId;
		int runService;
		int numRows = c.getCount();
		c.moveToFirst();
		Vector accounts = new Vector();
		for (int i = 0; i < numRows; i++) {
			
			id = c.getString(0);
			blogName = c.getString(1);
			username = c.getString(2);
			runService = c.getInt(3);
			blogId = c.getInt(4);
			url = c.getString(5);
			if (id != null)
			{	
				HashMap thisHash = new HashMap();
				
				thisHash.put("id", id);
				thisHash.put("blogName", blogName);
				thisHash.put("username", username);
				thisHash.put("runService", runService);
				thisHash.put("blogId", blogId);
				thisHash.put("url", url);
				accounts.add(thisHash);
			}
			c.moveToNext();
		}
		c.close();
		db.close();
		
		return accounts;
	}
	
	public boolean checkMatch(Context ctx, String blogName, String blogURL, String username) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Cursor c = db.query(SETTINGS_TABLE, new String[] {"blogName", "url"}, "blogName='" + addSlashes(blogName) + "' AND url='" + addSlashes(blogURL) + "'" + " AND username='" + username + "'", null, null, null, null);
		int numRows = c.getCount();
		boolean result = false;
		
		if (numRows > 0){
			//this account is already saved, yo!
			result = true;
		}
		
		c.close();
		db.close();
		
		return result;
	}
	
	public static String addSlashes( String text ){    	
        final StringBuffer sb                   = new StringBuffer( text.length() * 2 );
        final StringCharacterIterator iterator  = new StringCharacterIterator( text );
        
  	  	char character = iterator.current();
        
        while( character != StringCharacterIterator.DONE ){
            if( character == '"' ) sb.append( "\\\"" );
            else if( character == '\'' ) sb.append( "\'\'" );
            else if( character == '\\' ) sb.append( "\\\\" );
            else if( character == '\n' ) sb.append( "\\n" );
            else if( character == '{'  ) sb.append( "\\{" );
            else if( character == '}'  ) sb.append( "\\}" );
            else sb.append( character );
            
            character = iterator.next();
        }
        
        return sb.toString();
    }

	public boolean saveSettings(Context ctx, String id, String url, String username, String password, String imagePlacement, boolean centerThumbnail, boolean fullSizeImage, String maxImageWidth, int maxImageWidthId) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		ContentValues values = new ContentValues();
		values.put("url", url);
		values.put("username", username);
		values.put("password", password);
		values.put("imagePlacement", imagePlacement);
		values.put("centerThumbnail", centerThumbnail);
		values.put("fullSizeImage", fullSizeImage);
		values.put("maxImageWidth", maxImageWidth);
		values.put("maxImageWidthId", maxImageWidthId);
		boolean returnValue = db.update(SETTINGS_TABLE, values, "id=" + id, null) > 0;
		db.close();
		return (returnValue);
	}
	
	public boolean deleteAccount(Context ctx, String id) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		int rowsAffected = db.delete(SETTINGS_TABLE, "id=" + id, null);
		
		boolean returnValue = false;
		if (rowsAffected > 0){
			returnValue = true;
		}
		db.close();
		return (returnValue);
	}

	public Vector loadSettings(Context ctx, String id) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		Cursor c = db.query(SETTINGS_TABLE, new String[] { "url", "blogName", "username", "password", "imagePlacement", "centerThumbnail", "fullSizeImage", "maxImageWidth", "maxImageWidthId", "runService", "blogId"}, "id=" + id, null, null, null, null);
		
		int numRows = c.getCount();
		c.moveToFirst();

		Vector returnVector = new Vector();
		if (c.getString(0) != null){
		returnVector.add(c.getString(0));
		returnVector.add(c.getString(1));
		returnVector.add(c.getString(2));
		returnVector.add(c.getString(3));
		returnVector.add(c.getString(4));
		returnVector.add(c.getInt(5));
		returnVector.add(c.getString(6));
		returnVector.add(c.getString(7));
		returnVector.add(c.getInt(8));
		returnVector.add(c.getInt(9));
		returnVector.add(c.getInt(10));
		}
		else
		{
			returnVector = null;
		}
		c.close();
		db.close();
		
		return returnVector;
	}


	public int getLatestCommentID(Context ctx, String id) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		int returnInt = 0;
		Cursor c = db.query(SETTINGS_TABLE, new String[] { "lastCommentId"  }, "id=" + id, null, null, null, null);
		int blah = c.getCount();
		c.moveToFirst();
		if (c.getString(0) != null){
			returnInt = Integer.valueOf(c.getString(0));
		}
		c.close();
		db.close();
		return returnInt;
	}


	public boolean updateLatestCommentID(Context ctx, String id, Integer newCommentID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		ContentValues values = new ContentValues();
		values.put("lastCommentId", newCommentID);

		boolean returnValue = db.update(SETTINGS_TABLE, values, "id=" + id, null) > 0;
		db.close();
		return (returnValue);
		
	}


	public Vector getNotificationAccounts(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		Cursor c = null;
		try {
			c = db.query(SETTINGS_TABLE, new String[] { "id" }, "runService=1", null, null, null, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int numRows = c.getCount();
		c.moveToFirst();
		
		Vector returnVector = new Vector();
		for (int i = 0; i < numRows; ++i) {
			int tempID = c.getInt(0);	
			returnVector.add(tempID);
			c.moveToNext();
		}
		
		c.close();
		db.close();
		return returnVector;
	}


	public String getAccountName(Context ctx, String accountID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		int returnInt = 0;
		String accountName = "";
		Cursor c = db.query(SETTINGS_TABLE, new String[] { "blogName"  }, "id=" + accountID, null, null, null, null);
		int blah = c.getCount();
		c.moveToFirst();
		if (c.getString(0) != null){
			accountName = c.getString(0);
		}
		c.close();
		db.close();
		return accountName;
	}


	public void updateNotificationFlag(Context ctx, int id, boolean flag) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		ContentValues values = new ContentValues();
		int iFlag = 0;
		if (flag){
			iFlag = 1;
		}
		values.put("runService", iFlag);

		boolean returnValue = db.update(SETTINGS_TABLE, values, "id=" + String.valueOf(id), null) > 0;
		
		db.close();
		
	}
	
	public void updateNotificationSettings(Context ctx, String interval, boolean sound, boolean vibrate, boolean light) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		ContentValues values = new ContentValues();
		values.put("interval", interval);
		values.put("sound", sound);
		values.put("vibrate", vibrate);
		values.put("light", light);

		boolean returnValue = db.update("eula", values, null, null) > 0;
		
		db.close();
		
	}
	
	public String getInterval(Context ctx) {
db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		Cursor c = db.query("eula", new String[] { "interval" }, "id=0", null, null, null, null);
		int numRows = c.getCount();
		c.moveToFirst();
		String returnValue = "";
		if (numRows == 1){
			if (c.getString(0) != null){
			returnValue = c.getString(0);
			}
		}
		c.close();
		db.close();

		return returnValue;
		
	}
	
	public HashMap getNotificationOptions(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Cursor c = db.query("eula", new String[] { "id", "sound", "vibrate", "light"}, "id=0", null, null, null, null);
		int sound, vibrate, light;
		HashMap thisHash = new HashMap();
		int numRows = c.getCount();
		if (numRows >= 1){
		c.moveToFirst();
			
		sound = c.getInt(1);
		vibrate = c.getInt(2);
		light = c.getInt(3);
		
		thisHash.put("sound", sound);
		thisHash.put("vibrate", vibrate);
		thisHash.put("light", light);
		
		}

		c.close();
		db.close();
		
		return thisHash;
	}

}

