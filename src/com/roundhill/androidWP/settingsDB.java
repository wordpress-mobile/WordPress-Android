//by Dan Roundhill, danroundhill.com/wptogo
package com.roundhill.androidWP;

import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class settingsDB {

	private static final String CREATE_TABLE_SETTINGS = "create table if not exists accounts (id integer primary key autoincrement, "
			+ "url text, blogName text, username text, password text, imagePlacement text, centerThumbnail boolean, fullSizeImage boolean, maxImageWidth text, maxImageWidthId integer);";


	private static final String SETTINGS_TABLE = "accounts";
	private static final String DATABASE_NAME = "wpToGo";
	private static final int DATABASE_VERSION = 2;

	private SQLiteDatabase db;

	public settingsDB(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		//db.execSQL("DROP TABLE IF EXISTS "+ SETTINGS_TABLE);
		db.execSQL(CREATE_TABLE_SETTINGS);
		
		db.close();
	}
	
	public boolean addAccount(Context ctx, String url, String blogName, String username, String password, String imagePlacement, boolean centerThumbnail, boolean fullSizeImage, String maxImageWidth, int maxImageWidthId) {
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
		boolean returnValue = db.insert(SETTINGS_TABLE, null, values) > 0;
		db.close();
		return (returnValue);
	}	
	public Vector getAccounts(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Cursor c = db.query(SETTINGS_TABLE, new String[] { "id", "blogName", "username"}, null, null, null, null, null);
		String id;
		String blogName, username;
		int numRows = c.getCount();
		c.moveToFirst();
		Vector accounts = new Vector();
		for (int i = 0; i < numRows; i++) {
			
			id = c.getString(0);
			blogName = c.getString(1);
			username = c.getString(2);
			if (id != null)
			{	
				HashMap thisHash = new HashMap();
				
				thisHash.put("id", id);
				thisHash.put("blogName", blogName);
				thisHash.put("username", username);
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
		
		Cursor c = db.query(SETTINGS_TABLE, new String[] { "url", "blogName", "username", "password", "imagePlacement", "centerThumbnail", "fullSizeImage", "maxImageWidth", "maxImageWidthId" }, "id=" + id, null, null, null, null);
		
		int numRows = c.getCount();
		c.moveToFirst();
		/*Vector returnVector = new Vector();
		for (int i = 0; i < numRows; ++i) {
			String category_name = c.getString(2);
			if (category_name != null)
			{	
			returnVector.add(category_name);
			}
			c.moveToNext();
		}*/
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
		}
		else
		{
			returnVector = null;
		}
		c.close();
		db.close();
		
		return returnVector;
	}
	
	public void clearCategories(Context ctx){

	}

}

