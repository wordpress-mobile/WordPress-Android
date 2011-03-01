package org.wordpress.android;

import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Vector;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class WordPressDB {

	private static final int DATABASE_VERSION = 10;
	
	private static final String CREATE_TABLE_SETTINGS = "create table if not exists accounts (id integer primary key autoincrement, "
			+ "url text, blogName text, username text, password text, imagePlacement text, centerThumbnail boolean, fullSizeImage boolean, maxImageWidth text, maxImageWidthId integer, lastCommentId integer, runService boolean);";
	private static final String CREATE_TABLE_EULA = "create table if not exists eula (id integer primary key autoincrement, "
		+ "read integer not null, interval text, statsdate integer);";
	private static final String SETTINGS_TABLE = "accounts";
	private static final String DATABASE_NAME = "wordpress";
	
	//localDrafts
	private static final String CREATE_TABLE_LOCALDRAFTS = "create table if not exists localdrafts (id integer primary key autoincrement, blogID text, uploaded boolean, title text,content text, picturePaths text, tags text, categories text, publish boolean);";
	private static final String CREATE_TABLE_LOCALPAGEDRAFTS = "create table if not exists localpagedrafts (id integer primary key autoincrement, blogID text, uploaded boolean, title text,content text, picturePaths text, publish boolean);";

	private static final String LOCALDRAFTS_TABLE = "localdrafts";
	private static final String LOCALPAGEDRAFTS_TABLE = "localpagedrafts";
	
	private static final String ADD_LATITUDE = "alter table localdrafts add latitude real";
	private static final String ADD_LONGITUDE = "alter table localdrafts add longitude real";
	
	private static final String ADD_STATUS = "alter table localdrafts add status text";
	private static final String ADD_PAGE_STATUS = "alter table localpagedrafts add status text";
	
	private static final String ADD_LOCALDRAFT_DATE = "alter table localdrafts add date integer;";
	private static final String ADD_LOCALPAGEDRAFT_DATE = "alter table localpagedrafts add date integer;";
	
	//postStore
	private static final String CREATE_TABLE_POSTSTORE = "create table if not exists poststore (blogID text, postID text, title text, postDate text, postDateFormatted text);";
	private static final String CREATE_TABLE_PAGES = "create table if not exists pages (blogID text, pageID text, parentID text, title text, pageDate text, pageDateFormatted text);";
	private static final String CREATE_TABLE_COMMENTS = "create table if not exists comments (blogID text, postID text, iCommentID integer, author text, comment text, commentDate text, commentDateFormatted text, status text, url text, email text, postTitle text);";
	private static final String POSTSTORE_TABLE = "poststore";
	private static final String PAGES_TABLE = "pages";
	private static final String COMMENTS_TABLE = "comments";
	
	//eula
	private static final String EULA_TABLE = "eula";
	
	//categories
	private static final String CREATE_TABLE_CATEGORIES = "create table if not exists cats (id integer primary key autoincrement, "
		+ "blog_id text, wp_id integer, category_name text not null);";
	private static final String CATEGORIES_TABLE = "cats";
	
	//for capturing blogID, trac ticket #
	private static final String ADD_BLOGID = "alter table accounts add blogId integer;";
	private static final String UPDATE_BLOGID = "update accounts set blogId = 1;"; //set them all to 1 if updating
	
	//add notification options
	private static final String ADD_SOUND_OPTION = "alter table eula add sound boolean default false;";
	private static final String ADD_VIBRATE_OPTION = "alter table eula add vibrate boolean default false;";
	private static final String ADD_LIGHT_OPTION = "alter table eula add light boolean default false;";
	private static final String ADD_TAGLINE = "alter table eula add tagline text;";
	private static final String ADD_TAGLINE_FLAG = "alter table eula add tagline_flag boolean default false;";
	
	//for capturing blogID, trac ticket #
	private static final String ADD_LOCATION_FLAG = "alter table accounts add location boolean default false;";
	
	//fix commentID data type
	private static final String ADD_NEW_COMMENT_ID = "ALTER TABLE comments ADD iCommentID INTEGER;";
	private static final String COPY_COMMENT_IDS = "UPDATE comments SET iCommentID = commentID;";
	
	//add wordpress.com stats login info
	private static final String ADD_DOTCOM_USERNAME = "alter table accounts add dotcom_username text;";
	private static final String ADD_DOTCOM_PASSWORD = "alter table accounts add dotcom_password text;";
	private static final String ADD_API_KEY = "alter table accounts add api_key text;";
	private static final String ADD_API_BLOGID = "alter table accounts add api_blogid text;";
	
	//add wordpress.com flag and version column
	private static final String ADD_DOTCOM_FLAG = "alter table accounts add dotcomFlag boolean default false;";
	private static final String ADD_WP_VERSION = "alter table accounts add wpVersion text;";
	
	//add httpuser and httppassword
	private static final String ADD_HTTPUSER = "alter table accounts add httpuser text;";
	private static final String ADD_HTTPPASSWORD = "alter table accounts add httppassword text;";
	
	//add new unique identifier to no longer use device imei
	private static final String ADD_UNIQUE_ID = "alter table eula add uuid text;";
	
	//add new table for QuickPress homescreen shortcuts
	private static final String CREATE_TABLE_QUICKPRESS_SHORTCUTS = "create table if not exists quickpress_shortcuts (id integer primary key autoincrement, accountId text, name text);";
	private static final String QUICKPRESS_SHORTCUTS_TABLE = "quickpress_shortcuts";
	
	//add password column to poststore, localdrafts, pages and localpagedrafts
	private static final String ADD_PASSWORD_TO_POSTSTORE = "alter table poststore add password text;";
	private static final String ADD_PASSWORD_TO_LOCALDRAFTS = "alter table localdrafts add password text;";
	private static final String ADD_PASSWORD_TO_PAGES = "alter table pages add password text;";
	private static final String ADD_PASSWORD_TO_LOCALPAGEDRAFTS = "alter table localpagedrafts add password text;";
	
	private SQLiteDatabase db;
	
	protected static final String PASSWORD_SECRET = "makemetopsecretforrelease!";

	public WordPressDB(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		//db.execSQL("DROP TABLE IF EXISTS "+ SETTINGS_TABLE);
		db.execSQL(CREATE_TABLE_SETTINGS);
		//added eula to this class to fix trac #49
		db.execSQL(CREATE_TABLE_EULA);
		//int test = db.getVersion();

		db.execSQL(CREATE_TABLE_LOCALDRAFTS);
		db.execSQL(CREATE_TABLE_LOCALPAGEDRAFTS);
		
		db.execSQL(CREATE_TABLE_POSTSTORE);
		db.execSQL(CREATE_TABLE_PAGES);
		db.execSQL(CREATE_TABLE_COMMENTS);
		
		db.execSQL(CREATE_TABLE_CATEGORIES);
		
		db.execSQL(CREATE_TABLE_QUICKPRESS_SHORTCUTS);
		
		try {
			if (db.getVersion() < 1){ //user is new install
				db.execSQL(ADD_BLOGID);
				db.execSQL(UPDATE_BLOGID);
				db.execSQL(ADD_SOUND_OPTION);
				db.execSQL(ADD_VIBRATE_OPTION);
				db.execSQL(ADD_LIGHT_OPTION);
				db.execSQL(ADD_LOCATION_FLAG);
				db.execSQL(ADD_LATITUDE);
				db.execSQL(ADD_LONGITUDE);
				db.execSQL(ADD_TAGLINE);
				db.execSQL(ADD_TAGLINE_FLAG);
				db.execSQL(ADD_DOTCOM_USERNAME);
				db.execSQL(ADD_DOTCOM_PASSWORD);
				db.execSQL(ADD_API_KEY);
				db.execSQL(ADD_API_BLOGID);
				db.execSQL(ADD_DOTCOM_FLAG);
				db.execSQL(ADD_WP_VERSION);
				db.execSQL(ADD_UNIQUE_ID);
				db.execSQL(ADD_STATUS);
				db.execSQL(ADD_PAGE_STATUS);
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_PASSWORD_TO_POSTSTORE);
				db.execSQL(ADD_PASSWORD_TO_LOCALDRAFTS);
				db.execSQL(ADD_PASSWORD_TO_PAGES);
				db.execSQL(ADD_PASSWORD_TO_LOCALPAGEDRAFTS);
				db.execSQL(ADD_LOCALDRAFT_DATE);
				db.execSQL(ADD_LOCALPAGEDRAFT_DATE);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION); //set to latest revision
			}
			else if (db.getVersion() == 1){ //v1.0 or v1.0.1
				db.execSQL(ADD_BLOGID);
				db.execSQL(UPDATE_BLOGID);
				db.execSQL(ADD_SOUND_OPTION);
				db.execSQL(ADD_VIBRATE_OPTION);
				db.execSQL(ADD_LIGHT_OPTION);
				db.execSQL(ADD_LOCATION_FLAG);
				db.execSQL(ADD_LATITUDE);
				db.execSQL(ADD_LONGITUDE);
				db.execSQL(ADD_TAGLINE);
				db.execSQL(ADD_TAGLINE_FLAG);
				db.execSQL(ADD_NEW_COMMENT_ID);
				db.execSQL(COPY_COMMENT_IDS);
				db.execSQL(ADD_DOTCOM_USERNAME);
				db.execSQL(ADD_DOTCOM_PASSWORD);
				db.execSQL(ADD_API_KEY);
				db.execSQL(ADD_API_BLOGID);
				db.execSQL(ADD_DOTCOM_FLAG);
				db.execSQL(ADD_WP_VERSION);
				db.execSQL(ADD_UNIQUE_ID);
				db.execSQL(ADD_STATUS);
				db.execSQL(ADD_PAGE_STATUS);
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_PASSWORD_TO_POSTSTORE);
				db.execSQL(ADD_PASSWORD_TO_LOCALDRAFTS);
				db.execSQL(ADD_PASSWORD_TO_PAGES);
				db.execSQL(ADD_PASSWORD_TO_LOCALPAGEDRAFTS);
				db.execSQL(ADD_LOCALDRAFT_DATE);
				db.execSQL(ADD_LOCALPAGEDRAFT_DATE);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION); //set to latest revision
			}
			else if (db.getVersion()  == 2){
				db.execSQL(ADD_SOUND_OPTION);
				db.execSQL(ADD_VIBRATE_OPTION);
				db.execSQL(ADD_LIGHT_OPTION);
				db.execSQL(ADD_LOCATION_FLAG);
				db.execSQL(ADD_LATITUDE);
				db.execSQL(ADD_LONGITUDE);
				db.execSQL(ADD_TAGLINE);
				db.execSQL(ADD_TAGLINE_FLAG);
				db.execSQL(ADD_NEW_COMMENT_ID);
				db.execSQL(COPY_COMMENT_IDS);
				db.execSQL(ADD_DOTCOM_USERNAME);
				db.execSQL(ADD_DOTCOM_PASSWORD);
				db.execSQL(ADD_API_KEY);
				db.execSQL(ADD_API_BLOGID);
				db.execSQL(ADD_DOTCOM_FLAG);
				db.execSQL(ADD_WP_VERSION);
				db.execSQL(ADD_UNIQUE_ID);
				db.execSQL(ADD_STATUS);
				db.execSQL(ADD_PAGE_STATUS);
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_PASSWORD_TO_POSTSTORE);
				db.execSQL(ADD_PASSWORD_TO_LOCALDRAFTS);
				db.execSQL(ADD_PASSWORD_TO_PAGES);
				db.execSQL(ADD_PASSWORD_TO_LOCALPAGEDRAFTS);
				db.execSQL(ADD_LOCALDRAFT_DATE);
				db.execSQL(ADD_LOCALPAGEDRAFT_DATE);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION); 
			}
			else if (db.getVersion() == 3){
				db.execSQL(ADD_LOCATION_FLAG);
				db.execSQL(ADD_LATITUDE);
				db.execSQL(ADD_LONGITUDE);
				db.execSQL(ADD_TAGLINE);
				db.execSQL(ADD_TAGLINE_FLAG);
				db.execSQL(ADD_NEW_COMMENT_ID);
				db.execSQL(COPY_COMMENT_IDS);
				db.execSQL(ADD_DOTCOM_USERNAME);
				db.execSQL(ADD_DOTCOM_PASSWORD);
				db.execSQL(ADD_API_KEY);
				db.execSQL(ADD_API_BLOGID);
				db.execSQL(ADD_DOTCOM_FLAG);
				db.execSQL(ADD_WP_VERSION);
				db.execSQL(ADD_UNIQUE_ID);
				db.execSQL(ADD_STATUS);
				db.execSQL(ADD_PAGE_STATUS);
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_PASSWORD_TO_POSTSTORE);
				db.execSQL(ADD_PASSWORD_TO_LOCALDRAFTS);
				db.execSQL(ADD_PASSWORD_TO_PAGES);
				db.execSQL(ADD_PASSWORD_TO_LOCALPAGEDRAFTS);
				db.execSQL(ADD_LOCALDRAFT_DATE);
				db.execSQL(ADD_LOCALPAGEDRAFT_DATE);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION); 
			}
			else if (db.getVersion() == 4){
				db.execSQL(ADD_LOCATION_FLAG);
				db.execSQL(ADD_LATITUDE);
				db.execSQL(ADD_LONGITUDE);
				db.execSQL(ADD_TAGLINE);
				db.execSQL(ADD_TAGLINE_FLAG);
				db.execSQL(ADD_NEW_COMMENT_ID);
				db.execSQL(COPY_COMMENT_IDS);
				db.execSQL(ADD_DOTCOM_USERNAME);
				db.execSQL(ADD_DOTCOM_PASSWORD);
				db.execSQL(ADD_API_KEY);
				db.execSQL(ADD_API_BLOGID);
				db.execSQL(ADD_DOTCOM_FLAG);
				db.execSQL(ADD_WP_VERSION);
				db.execSQL(ADD_UNIQUE_ID);
				db.execSQL(ADD_STATUS);
				db.execSQL(ADD_PAGE_STATUS);
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_PASSWORD_TO_POSTSTORE);
				db.execSQL(ADD_PASSWORD_TO_LOCALDRAFTS);
				db.execSQL(ADD_PASSWORD_TO_PAGES);
				db.execSQL(ADD_PASSWORD_TO_LOCALPAGEDRAFTS);
				db.execSQL(ADD_LOCALDRAFT_DATE);
				db.execSQL(ADD_LOCALPAGEDRAFT_DATE);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION);
			}
			else if (db.getVersion() == 5){
				db.execSQL(ADD_TAGLINE);
				db.execSQL(ADD_TAGLINE_FLAG);
				db.execSQL(ADD_NEW_COMMENT_ID);
				db.execSQL(COPY_COMMENT_IDS);
				db.execSQL(ADD_DOTCOM_USERNAME);
				db.execSQL(ADD_DOTCOM_PASSWORD);
				db.execSQL(ADD_API_KEY);
				db.execSQL(ADD_API_BLOGID);
				db.execSQL(ADD_DOTCOM_FLAG);
				db.execSQL(ADD_WP_VERSION);
				db.execSQL(ADD_UNIQUE_ID);
				db.execSQL(ADD_STATUS);
				db.execSQL(ADD_PAGE_STATUS);
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_PASSWORD_TO_POSTSTORE);
				db.execSQL(ADD_PASSWORD_TO_LOCALDRAFTS);
				db.execSQL(ADD_PASSWORD_TO_PAGES);
				db.execSQL(ADD_PASSWORD_TO_LOCALPAGEDRAFTS);
				db.execSQL(ADD_LOCALDRAFT_DATE);
				db.execSQL(ADD_LOCALPAGEDRAFT_DATE);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION);
			}
			else if (db.getVersion() == 6){
				db.execSQL(ADD_NEW_COMMENT_ID);
				db.execSQL(COPY_COMMENT_IDS);
				db.execSQL(ADD_DOTCOM_USERNAME);
				db.execSQL(ADD_DOTCOM_PASSWORD);
				db.execSQL(ADD_API_KEY);
				db.execSQL(ADD_API_BLOGID);
				db.execSQL(ADD_DOTCOM_FLAG);
				db.execSQL(ADD_WP_VERSION);
				db.execSQL(ADD_UNIQUE_ID);
				db.execSQL(ADD_STATUS);
				db.execSQL(ADD_PAGE_STATUS);
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_PASSWORD_TO_POSTSTORE);
				db.execSQL(ADD_PASSWORD_TO_LOCALDRAFTS);
				db.execSQL(ADD_PASSWORD_TO_PAGES);
				db.execSQL(ADD_PASSWORD_TO_LOCALPAGEDRAFTS);
				db.execSQL(ADD_LOCALDRAFT_DATE);
				db.execSQL(ADD_LOCALPAGEDRAFT_DATE);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION);
			}
			else if (db.getVersion() == 7){
				db.execSQL(ADD_UNIQUE_ID);
				db.execSQL(ADD_STATUS);
				db.execSQL(ADD_PAGE_STATUS);
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_PASSWORD_TO_POSTSTORE);
				db.execSQL(ADD_PASSWORD_TO_LOCALDRAFTS);
				db.execSQL(ADD_PASSWORD_TO_PAGES);
				db.execSQL(ADD_PASSWORD_TO_LOCALPAGEDRAFTS);
				db.execSQL(ADD_LOCALDRAFT_DATE);
				db.execSQL(ADD_LOCALPAGEDRAFT_DATE);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION);
			}
			else if (db.getVersion() == 8){
				db.execSQL(ADD_STATUS);
				db.execSQL(ADD_PAGE_STATUS);
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_PASSWORD_TO_POSTSTORE);
				db.execSQL(ADD_PASSWORD_TO_LOCALDRAFTS);
				db.execSQL(ADD_PASSWORD_TO_PAGES);
				db.execSQL(ADD_PASSWORD_TO_LOCALPAGEDRAFTS);
				db.execSQL(ADD_LOCALDRAFT_DATE);
				db.execSQL(ADD_LOCALPAGEDRAFT_DATE);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION);
			}
			else if (db.getVersion() == 9){
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_PASSWORD_TO_POSTSTORE);
				db.execSQL(ADD_PASSWORD_TO_LOCALDRAFTS);
				db.execSQL(ADD_PASSWORD_TO_PAGES);
				db.execSQL(ADD_PASSWORD_TO_LOCALPAGEDRAFTS);
				db.execSQL(ADD_LOCALDRAFT_DATE);
				db.execSQL(ADD_LOCALPAGEDRAFT_DATE);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		db.close();
		
	}

	
	public boolean addAccount(Context ctx, String url, String blogName, String username, String password, String httpuser, String httppassword, String imagePlacement, boolean centerThumbnail, boolean fullSizeImage, String maxImageWidth, int maxImageWidthId, boolean runService, int blogId, boolean wpcom, String wpVersion) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		ContentValues values = new ContentValues();
		values.put("url", url);
		values.put("blogName", blogName);
		values.put("username", username);
		values.put("password", encryptPassword(password));
		values.put("httpuser", httpuser);
		values.put("httppassword", encryptPassword(httppassword));
		values.put("imagePlacement", imagePlacement);
		values.put("centerThumbnail", centerThumbnail);
		values.put("fullSizeImage", fullSizeImage);
		values.put("maxImageWidth", maxImageWidth);
		values.put("maxImageWidthId", maxImageWidthId);
		values.put("runService", runService);
		values.put("blogId", blogId);
		values.put("dotcomFlag", wpcom);
		values.put("wpVersion", wpVersion);
		boolean returnValue = db.insert(SETTINGS_TABLE, null, values) > 0;
		db.close();
		return (returnValue);
	}	
	public Vector<HashMap<String, Object>> getAccounts(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Cursor c = db.query(SETTINGS_TABLE, new String[] { "id", "blogName", "username", "runService", "blogId", "url"}, null, null, null, null, null);
		String id;
		String blogName, username, url;
		int blogId;
		int runService;
		int numRows = c.getCount();
		c.moveToFirst();
		Vector<HashMap<String, Object>> accounts = new Vector<HashMap<String, Object>>();
		for (int i = 0; i < numRows; i++) {
			
			id = c.getString(0);
			blogName = c.getString(1);
			username = c.getString(2);
			runService = c.getInt(3);
			blogId = c.getInt(4);
			url = c.getString(5);
			if (id != null)
			{	
				HashMap<String, Object> thisHash = new HashMap<String, Object>();
				
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

	public boolean saveSettings(Context ctx, String id, String url, String username, String password, String httpuser, String httppassword, String imagePlacement, boolean centerThumbnail, boolean fullSizeImage, String maxImageWidth, int maxImageWidthId, boolean location, boolean isWPCom, String originalUsername) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		ContentValues values = new ContentValues();
		values.put("url", url);
		values.put("username", username);
		values.put("password", encryptPassword(password));
		values.put("httpuser", httpuser);
		values.put("httppassword", encryptPassword(httppassword));
		values.put("imagePlacement", imagePlacement);
		values.put("centerThumbnail", centerThumbnail);
		values.put("fullSizeImage", fullSizeImage);
		values.put("maxImageWidth", maxImageWidth);
		values.put("maxImageWidthId", maxImageWidthId);
		values.put("location", location);
		boolean returnValue = db.update(SETTINGS_TABLE, values, "id=" + id, null) > 0;
		if (isWPCom){
			//update the login for other wordpress.com accounts
			ContentValues userPass = new ContentValues();
			userPass.put("username", username);
			userPass.put("password", encryptPassword(password));
			returnValue = db.update(SETTINGS_TABLE, userPass, "username=\"" + originalUsername + "\" AND dotcomFlag=1" , null) > 0;
		}
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
		
		// delete QuickPress homescreen shortcuts connected with this account
	    Vector<HashMap<String, Object>> shortcuts = this.getQuickPressShortcuts(ctx, id);
	    for(int i = 0; i < shortcuts.size(); i++) {
	    	HashMap<String, Object> shortcutHash = shortcuts.get(i);
	    	
	    	Intent shortcutIntent = new Intent();
	    	shortcutIntent.setClassName(editPost.class.getPackage().getName(), editPost.class.getName());
	    	shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    	shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    	shortcutIntent.setAction(Intent.ACTION_VIEW); 
	    	Intent broadcastShortcutIntent = new Intent();
	    	broadcastShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
	    	broadcastShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutHash.get("name").toString());
	    	broadcastShortcutIntent.putExtra("duplicate", false);
	    	broadcastShortcutIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
	    	ctx.sendBroadcast(broadcastShortcutIntent);
	    	
	    	deleteQuickPressShortcut(ctx, shortcutHash.get("id").toString());
	    }
	    
	    db.close();
		return (returnValue);
	}

	public Vector<Object> loadSettings(Context ctx, String id) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		Cursor c = db.query(SETTINGS_TABLE, new String[] { "url", "blogName", "username", "password", "httpuser", "httppassword", "imagePlacement", "centerThumbnail", "fullSizeImage", "maxImageWidth", "maxImageWidthId", "runService", "blogId", "location", "dotcomFlag"}, "id=" + id, null, null, null, null);
		
		int numRows = c.getCount();
		c.moveToFirst();

		Vector<Object> returnVector = new Vector<Object>();
		if (numRows > 0){
			if (c.getString(0) != null){
			returnVector.add(c.getString(0));
			returnVector.add(c.getString(1));
			returnVector.add(c.getString(2));
			returnVector.add(decryptPassword(c.getString(3)));
			if (c.getString(4) == null) {
				returnVector.add("");
			}
			else {
				returnVector.add(c.getString(4));	
			}
			if (c.getString(5) == null) {
				returnVector.add("");
			}
			else {
				returnVector.add(decryptPassword(c.getString(5)));	
			}
			returnVector.add(c.getString(6));
			returnVector.add(c.getInt(7));
			returnVector.add(c.getString(8));
			returnVector.add(c.getString(9));
			returnVector.add(c.getInt(10));
			returnVector.add(c.getInt(11));
			returnVector.add(c.getInt(12));
			returnVector.add(c.getInt(13));
			returnVector.add(c.getInt(14));
			}
			else
			{
				returnVector = null;
			}
		}
		c.close();
		db.close();
		
		return returnVector;
	}

	public Vector<String> loadStatsLogin(Context ctx, String id) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		Cursor c = db.query(SETTINGS_TABLE, new String[] { "dotcom_username", "dotcom_password"}, "id=" + id, null, null, null, null);
		
		c.moveToFirst();

		Vector<String> returnVector = new Vector<String>();
		if (c.getString(0) != null){
		returnVector.add(c.getString(0));
		returnVector.add(decryptPassword(c.getString(1)));
		}
		else
		{
			returnVector = null;
		}
		c.close();
		db.close();
		
		return returnVector;
	}
	
	public boolean saveStatsLogin(Context ctx, String id, String statsUsername, String statsPassword) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		ContentValues values = new ContentValues();
		values.put("dotcom_username", statsUsername);
		values.put("dotcom_password", encryptPassword(statsPassword));
		boolean returnValue = db.update(SETTINGS_TABLE, values, "id=" + id, null) > 0;
		db.close();
		
		return (returnValue);
		
	}
	
	public Vector<String> loadAPIData(Context ctx, String id) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		Cursor c = db.query(SETTINGS_TABLE, new String[] { "api_key", "api_blogid"}, "id=" + id, null, null, null, null);

		c.moveToFirst();

		Vector<String> returnVector = new Vector<String>();
		if (c.getString(0) != null){
		returnVector.add(c.getString(0));
		returnVector.add(c.getString(1));
		}
		else
		{
			returnVector = null;
		}
		c.close();
		db.close();
		
		return returnVector;
	}
	
	public boolean saveAPIData(Context ctx, String id, String apiKey, String apiBlogID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		ContentValues values = new ContentValues();
		values.put("api_key", apiKey);
		values.put("api_blogid", apiBlogID);
		boolean returnValue = db.update(SETTINGS_TABLE, values, "id=" + id, null) > 0;
		db.close();
		
		return (returnValue);
		
	}

	public int getLatestCommentID(Context ctx, String id) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		int returnInt = 0;
		Cursor c = db.query(SETTINGS_TABLE, new String[] { "lastCommentId"  }, "id=" + id, null, null, null, null);
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


	public Vector<Integer> getNotificationAccounts(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		Cursor c = null;
		try {
			c = db.query(SETTINGS_TABLE, new String[] { "id" }, "runService=1", null, null, null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		int numRows = c.getCount();
		c.moveToFirst();
		
		Vector<Integer> returnVector = new Vector<Integer>();
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
		String accountName = "";
		Cursor c = db.query(SETTINGS_TABLE, new String[] { "blogName"  }, "id=" + accountID, null, null, null, null);
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
		if(returnValue){
		}
		db.close();
		
	}
	
	public void updateNotificationSettings(Context ctx, String interval, boolean sound, boolean vibrate, boolean light, boolean tagline_flag, String tagline) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		ContentValues values = new ContentValues();
		values.put("interval", interval);
		values.put("sound", sound);
		values.put("vibrate", vibrate);
		values.put("light", light);
		values.put("tagline_flag", tagline_flag);
		values.put("tagline", tagline);

		boolean returnValue = db.update("eula", values, null, null) > 0;
		if (returnValue){};
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
	
	public HashMap<String, Object> getNotificationOptions(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Cursor c = db.query("eula", new String[] { "id", "sound", "vibrate", "light", "tagline_flag", "tagline"}, "id=0", null, null, null, null);
		int sound, vibrate, light;
		String tagline;
		HashMap<String, Object> thisHash = new HashMap<String, Object>();
		int numRows = c.getCount();
		if (numRows >= 1){
		c.moveToFirst();
			
		sound = c.getInt(1);
		vibrate = c.getInt(2);
		light = c.getInt(3);
		tagline = c.getString(5);
		thisHash.put("sound", sound);
		thisHash.put("vibrate", vibrate);
		thisHash.put("light", light);
		thisHash.put("tagline_flag", c.getInt(4));
		if (tagline != null){
			thisHash.put("tagline", tagline);
		}
		else{
			thisHash.put("tagline", "");
		}
		
		
		}

		c.close();
		db.close();
		
		return thisHash;
	}
	
	//localDrafts
	public boolean saveLocalDraft(Context ctx, String blogID, String title, String content, String picturePaths, String tags, String categories, String status, Double latitude, Double longitude, String password, long pubDate) {
		boolean returnValue = false;
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
			ContentValues values = new ContentValues();
			values.put("blogID", blogID);
			values.put("title", title);
			values.put("content", content);
			values.put("picturePaths", picturePaths);
			values.put("tags", tags);
			values.put("categories", categories);
			values.put("status", status);
			values.put("latitude", latitude);
			values.put("longitude", longitude);
			values.put("password", password);
			values.put("date", pubDate);
			returnValue = db.insert(LOCALDRAFTS_TABLE, null, values) > 0;

		db.close();
		return (returnValue);
	}
	
	public boolean updateLocalDraft(Context ctx, String blogID, String postID, String title, String content, String picturePaths, String tags, String categories, String status, Double latitude, Double longitude, String password, long pubDate) {
		boolean returnValue = false;
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
			ContentValues values = new ContentValues();
			values.put("blogID", blogID);
			values.put("title", title);
			values.put("content", content);
			values.put("picturePaths", picturePaths);
			values.put("tags", tags);
			values.put("categories", categories);
			values.put("status", status);
			values.put("latitude", latitude);
			values.put("longitude", longitude);
			values.put("password", password);
			returnValue = db.update(LOCALDRAFTS_TABLE, values, "id=" + postID, null) > 0;

		db.close();
		return (returnValue);
	}

	public Vector<HashMap<String, Object>> loadPosts(Context ctx, String blogID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Vector<HashMap<String, Object>> returnVector = new Vector<HashMap<String, Object>>();
		Cursor c = db.query(LOCALDRAFTS_TABLE, new String[] { "id", "title", "status", "uploaded"}, "blogID=" + blogID, null, null, null, "id desc");
		int numRows = c.getCount();
		c.moveToFirst();
		
		for (int i = 0; i < numRows; ++i) {
		if (c.getString(0) != null){
		HashMap<String, Object> returnHash = new HashMap<String, Object>();
		returnHash.put("id", c.getInt(0));
		returnHash.put("title", c.getString(1));
		returnHash.put("status", c.getString(2));
		returnHash.put("uploaded", c.getInt(3));
		returnVector.add(i, returnHash);
		}
		c.moveToNext();
		}
		c.close();
		db.close();

		if (numRows == 0){
			returnVector = null;
		}
		
		return returnVector;
	}
	
	public Vector<HashMap<String, Object>> loadPost(Context ctx, String postID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Vector<HashMap<String, Object>> returnVector = new Vector<HashMap<String, Object>>();
		Cursor c = db.query(LOCALDRAFTS_TABLE, new String[] { "title", "content", "picturePaths", "tags", "categories", "status", "latitude", "longitude", "password", "date"}, "id=" + postID, null, null, null, null);
		
		int numRows = c.getCount();
		c.moveToFirst();
		
		for (int i = 0; i < numRows; ++i) {
		if (c.getString(0) != null){
		HashMap<String, Object> returnHash = new HashMap<String, Object>();
		returnHash.put("title", c.getString(0));
		returnHash.put("content", c.getString(1));
		returnHash.put("picturePaths", c.getString(2));
		returnHash.put("tags", c.getString(3));
		returnHash.put("categories", c.getString(4));
		returnHash.put("status", c.getString(5));
		returnHash.put("latitude", c.getDouble(6));
		returnHash.put("longitude", c.getDouble(7));
		returnHash.put("password", c.getString(8));
		returnHash.put("pubDate", c.getLong(9));
		returnVector.add(i, returnHash);
		}
		c.moveToNext();
		}
		c.close();
		db.close();
		
		if (numRows == 0){
			returnVector = null;
		}
		
		return returnVector;
	}

	public boolean deletePost(Context ctx, String postID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		boolean returnValue = false;

		int result = 0;
		result = db.delete(LOCALDRAFTS_TABLE, "id=" + postID, null);
		db.close();
		
		if (result == 1){
			returnValue = true;
		}
		
		return returnValue;
	}
	
	public boolean saveLocalPageDraft(Context ctx, String blogID, String title, String content, String picturePaths, String status, String password, long pubDate) {
		boolean returnValue = false;
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
			ContentValues values = new ContentValues();
			values.put("blogID", blogID);
			values.put("title", title);
			values.put("content", content);
			values.put("picturePaths", picturePaths);
			values.put("status", status);
			values.put("password", password);
			values.put("date", pubDate);
			returnValue = db.insert(LOCALPAGEDRAFTS_TABLE, null, values) > 0;

		db.close();
		return (returnValue);
	}
	
	public boolean updateLocalPageDraft(Context ctx, String blogID, String postID, String title, String content, String picturePaths, String status, String password, long pubDate) {
		boolean returnValue = false;
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
			ContentValues values = new ContentValues();
			values.put("blogID", blogID);
			values.put("title", title);
			values.put("content", content);
			values.put("picturePaths", picturePaths);
			values.put("status", status);
			values.put("password", password);
			values.put("date", pubDate);
			returnValue = db.update(LOCALPAGEDRAFTS_TABLE, values, "id=" + postID, null) > 0;

		db.close();
		return (returnValue);
	}
	
	public Vector<HashMap<String, Object>> loadPageDrafts(Context ctx, String blogID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Vector<HashMap<String, Object>> returnVector = new Vector<HashMap<String, Object>>();
		Cursor c = db.query(LOCALPAGEDRAFTS_TABLE, new String[] { "id", "title", "status", "uploaded"}, "blogID=" + blogID, null, null, null, "id desc");
		int numRows = c.getCount();
		c.moveToFirst();
		
		for (int i = 0; i < numRows; ++i) {
		if (c.getString(0) != null){
		HashMap<String, Object> returnHash = new HashMap<String, Object>();
		returnHash.put("id", c.getInt(0));
		returnHash.put("title", c.getString(1));
		returnHash.put("status", c.getString(2));
		returnHash.put("uploaded", c.getInt(3));
		returnVector.add(i, returnHash);
		}
		c.moveToNext();
		}
		c.close();
		db.close();

		if (numRows == 0){
			returnVector = null;
		}
		
		return returnVector;
	}
	
	public Vector<HashMap<String, Object>> loadPageDraft(Context ctx, String postID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Vector<HashMap<String, Object>> returnVector = new Vector<HashMap<String, Object>>();
		Cursor c = db.query(LOCALPAGEDRAFTS_TABLE, new String[] { "title", "content", "picturePaths", "status", "password", "date"}, "id=" + postID, null, null, null, null);
		
		int numRows = c.getCount();
		c.moveToFirst();
		
		for (int i = 0; i < numRows; ++i) {
		if (c.getString(0) != null){
		HashMap<String, Object> returnHash = new HashMap<String, Object>();
		returnHash.put("title", c.getString(0));
		returnHash.put("content", c.getString(1));
		returnHash.put("picturePaths", c.getString(2));
		returnHash.put("status", c.getString(3));
		returnHash.put("password", c.getString(4));
		returnHash.put("date", c.getString(5));
		returnVector.add(i, returnHash);
		}
		c.moveToNext();
		}
		c.close();
		db.close();
		
		if (numRows == 0){
			returnVector = null;
		}
		
		return returnVector;
	}

	public boolean deletePageDraft(Context ctx, String postID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		boolean returnValue = false;

		int result = 0;
		result = db.delete(LOCALPAGEDRAFTS_TABLE, "id=" + postID, null);
		db.close();
		
		if (result == 1){
			returnValue = true;
		}
		
		return returnValue;
	}

	public int getLatestDraftID(Context ctx, String id) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Cursor c = db.query(LOCALDRAFTS_TABLE, new String[] {"id"}, "blogID=" + id, null, null, null, "id desc", "1");
		
		int latestID = -1;
		int numRows = c.getCount();
		if (numRows != 0){
			c.moveToFirst();
			latestID = c.getInt(0);
		}
		c.close();
		db.close();
		
		return latestID;
	}
	
	public int getLatestPageDraftID(Context ctx, String id) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Cursor c = db.query(LOCALPAGEDRAFTS_TABLE, new String[] {"id"}, "blogID=" + id, null, null, null, "id desc", "1");
		
		int latestID = -1;
		int numRows = c.getCount();
		if (numRows != 0){
			c.moveToFirst();
			latestID = c.getInt(0);
		}
		c.close();
		db.close();
		
		return latestID;
	}
	
	//postStore
	public boolean savePosts(Context ctx, Vector<?> postValues) {
		boolean returnValue = false;
		if (postValues.size() != 0)
		{
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		HashMap<?, ?> firstHash = (HashMap<?, ?>) postValues.get(0);
		String blogID = firstHash.get("blogID").toString();
		//delete existing values
		db.delete(POSTSTORE_TABLE, "blogID=" + blogID, null);

		for (int i = 0; i < postValues.size(); i++){
			ContentValues values = new ContentValues();
			HashMap<?, ?> thisHash = (HashMap<?, ?>) postValues.get(i);
			values.put("blogID", thisHash.get("blogID").toString());
			values.put("postID", thisHash.get("postID").toString());
			values.put("title", thisHash.get("title").toString());
			values.put("postDate", thisHash.get("postDate").toString());
			values.put("postDateFormatted", thisHash.get("postDateFormatted").toString());
			returnValue = db.insert(POSTSTORE_TABLE, null, values) > 0;
		}
		
		
		db.close();
		}
		return (returnValue);
	}

	public Vector<HashMap<String, String>> loadSavedPosts(Context ctx, String blogID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Vector<HashMap<String, String>> returnVector = new Vector<HashMap<String, String>>();
		Cursor c = db.query(POSTSTORE_TABLE, new String[] { "blogID", "postID", "title", "postDate", "postDateFormatted"}, "blogID=" + blogID, null, null, null, null);
		
		int numRows = c.getCount();
		c.moveToFirst();
		
		for (int i = 0; i < numRows; ++i) {
		if (c.getString(0) != null){
		HashMap<String, String> returnHash = new HashMap<String, String>();
		returnHash.put("blogID", c.getString(0));
		returnHash.put("postID", c.getString(1));
		returnHash.put("title", c.getString(2));
		returnHash.put("postDate", c.getString(3));
		returnHash.put("postDateFormatted", c.getString(4));
		returnVector.add(i, returnHash);
		}
		c.moveToNext();
		}
		c.close();
		db.close();
		
		if (numRows == 0){
			returnVector = null;
		}
		
		return returnVector;
	}

	public Vector<HashMap<String, String>> loadPages(Context ctx, String blogID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Vector<HashMap<String, String>> returnVector = new Vector<HashMap<String, String>>();
		Cursor c = db.query(PAGES_TABLE, new String[] { "blogID", "pageID", "title", "pageDate", "pageDateFormatted"}, "blogID=" + blogID, null, null, null, null);
		
		int numRows = c.getCount();
		c.moveToFirst();
		
		for (int i = 0; i < numRows; ++i) {
		if (c.getString(0) != null){
		HashMap<String, String> returnHash = new HashMap<String, String>();
		returnHash.put("blogID", c.getString(0));
		returnHash.put("pageID", c.getString(1));
		returnHash.put("title", c.getString(2));
		returnHash.put("pageDate", c.getString(3));
		returnHash.put("pageDateFormatted", c.getString(4));
		returnVector.add(i, returnHash);
		}
		c.moveToNext();
		}
		c.close();
		db.close();
		
		if (numRows == 0){
			returnVector = null;
		}
		
		return returnVector;
	}

	public boolean savePages(Context ctx, Vector<?> pageValues) {
		boolean returnValue = false;
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		HashMap<?, ?> firstHash = (HashMap<?, ?>) pageValues.get(0);
		String blogID = firstHash.get("blogID").toString();
		//delete existing values
		db.delete(PAGES_TABLE, "blogID=" + blogID, null);

		for (int i = 0; i < pageValues.size(); i++){
			ContentValues values = new ContentValues();
			HashMap<?, ?> thisHash = (HashMap<?, ?>) pageValues.get(i);
			values.put("blogID", thisHash.get("blogID").toString());
			values.put("pageID", thisHash.get("pageID").toString());
			values.put("parentID", thisHash.get("parentID").toString());
			values.put("title", thisHash.get("title").toString());
			values.put("pageDate", thisHash.get("pageDate").toString());
			values.put("pageDateFormatted", thisHash.get("pageDateFormatted").toString());
			returnValue = db.insert(PAGES_TABLE, null, values) > 0;
		}
		
		
		db.close();
		return (returnValue);
		
	}
	
	public Vector<HashMap<String, Object>> loadComments(Context ctx, String blogID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Vector<HashMap<String, Object>> returnVector = new Vector<HashMap<String, Object>>();
		Cursor c = db.query(COMMENTS_TABLE, new String[] { "blogID", "postID", "iCommentID", "author", "comment", "commentDate", "commentDateFormatted", "status", "url", "email", "postTitle"}, "blogID=" + blogID, null, null, null, null);
		
		int numRows = c.getCount();
		c.moveToFirst();
		
		HashMap<String, Object> numRecords = new HashMap<String, Object>();
		//add the number of stored records so the offset can be computed
		if (numRows > 0){
			numRecords.put("numRecords", numRows);
			returnVector.add(0, numRecords);
		}
		
		for (int i = 1; i < (numRows + 1); ++i) {
		if (c.getString(0) != null){
		HashMap<String, Object> returnHash = new HashMap<String, Object>();
		returnHash.put("blogID", c.getString(0));
		returnHash.put("postID", c.getInt(1));
		returnHash.put("commentID", c.getInt(2));
		returnHash.put("author", c.getString(3));
		returnHash.put("comment", c.getString(4));
		returnHash.put("commentDate", c.getString(5));
		returnHash.put("commentDateFormatted", c.getString(6));
		returnHash.put("status", c.getString(7));
		returnHash.put("url", c.getString(8));
		returnHash.put("email", c.getString(9));
		returnHash.put("postTitle", c.getString(10));
		returnVector.add(i, returnHash);
		}
		c.moveToNext();
		}
		c.close();
		db.close();
		
		if (numRows == 0){
			returnVector = null;
		}
		
		return returnVector;
	}
	
	public Vector<HashMap<String, Object>> loadMoreComments(Context ctx, String blogID, int limit) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Vector<HashMap<String, Object>> returnVector = new Vector<HashMap<String, Object>>();
		Cursor c = db.query(COMMENTS_TABLE, new String[] { "blogID", "postID", "iCommentID", "author", "comment", "commentDate", "commentDateFormatted", "status", "url", "email", "postTitle"}, "blogID=" + blogID, null, null, null, "iCommentID ASC", String.valueOf(limit));
		int numRows = c.getCount();
		c.moveToFirst();
		
		//HashMap numRecords = new HashMap();
		//add the number of stored records so the offset can be computed
		/*if (numRows > 0){
			numRecords.put("numRecords", numRows);
			returnVector.add(0, numRecords);
		}*/
		for (int i = 0; i < numRows; i++) {
		if (c.getString(0) != null){
		HashMap<String, Object> returnHash = new HashMap<String, Object>();
		returnHash.put("blogID", c.getString(0));
		returnHash.put("postID", c.getInt(1));
		returnHash.put("commentID", c.getInt(2));
		returnHash.put("author", c.getString(3));
		returnHash.put("comment", c.getString(4));
		returnHash.put("commentDate", c.getString(5));
		returnHash.put("commentDateFormatted", c.getString(6));
		returnHash.put("status", c.getString(7));
		returnHash.put("url", c.getString(8));
		returnHash.put("email", c.getString(9));
		returnHash.put("postTitle", c.getString(10));
		returnVector.add(i, returnHash);
		}
		c.moveToNext();
		}
		c.close();
		db.close();
		
		if (numRows == 0){
			returnVector = null;
		}
		
		return returnVector;
	}

	public boolean saveComments(Context ctx, Vector<?> commentValues, boolean loadMore) {
		boolean returnValue = false;
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		HashMap<?, ?> firstHash = (HashMap<?, ?>) commentValues.get(0);
		String blogID = firstHash.get("blogID").toString();
		//delete existing values, if user hit refresh button
		if (!loadMore){
			try {
				db.delete(COMMENTS_TABLE, "blogID=" + blogID, null);
			} catch (Exception e) {
				db.close();
				return false;
			}
		}

		for (int i = 0; i < commentValues.size(); i++){
			ContentValues values = new ContentValues();
			HashMap<?, ?> thisHash = (HashMap<?, ?>) commentValues.get(i);
			values.put("blogID", thisHash.get("blogID").toString());
			values.put("postID", thisHash.get("postID").toString());
			values.put("iCommentID", thisHash.get("commentID").toString());
			values.put("author", thisHash.get("author").toString());
			values.put("comment", thisHash.get("comment").toString());
			values.put("commentDate", thisHash.get("commentDate").toString());
			values.put("commentDateFormatted", thisHash.get("commentDateFormatted").toString());
			values.put("status", thisHash.get("status").toString());
			values.put("url", thisHash.get("url").toString());
			values.put("email", thisHash.get("email").toString());
			values.put("postTitle", thisHash.get("postTitle").toString());
			try {
				returnValue = db.insert(COMMENTS_TABLE, null, values) > 0;
			} catch (Exception e) {
				db.close();
				return false;
			}
		}
		db.close();
		return (returnValue);
		
	}
	
	public void updateCommentStatus(Context ctx, String blogID, String id, String newStatus) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		ContentValues values = new ContentValues();
		values.put("status", newStatus);
		boolean returnValue = db.update(COMMENTS_TABLE, values, "blogID=" + blogID + " AND iCommentID=" + id, null) > 0;
		if (returnValue){};

	db.close();
		
	}

	public void clearPages(Context ctx, String blogID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		//delete existing values
		db.delete(PAGES_TABLE, "blogID=" + blogID, null);
		db.close();
	}

	public void clearPosts(Context ctx, String blogID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		//delete existing values
		db.delete(POSTSTORE_TABLE, "blogID=" + blogID, null);
		db.close();
		
	}
	
	//eula table
	public boolean checkEULA(Context ctx){
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		Cursor c = db.query(EULA_TABLE, new String[] { "read" }, "id=0", null, null, null, null);
		int numRows = c.getCount();
		c.moveToFirst();
		boolean returnValue = false;
		if (numRows == 1){
			returnValue = (c.getInt(0) != 0);
		}
		c.close();
		db.close();
			
		return returnValue;
	}
	
	public void setEULA(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		ContentValues values = new ContentValues();
		values.put("id", 0);
		values.put("read", 1); //set that they've read the EULA
		boolean returnValue = db.insert(EULA_TABLE, null, values) > 0;
		if (returnValue){};
		db.close();

	}
	
	public void setStatsDate(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		ContentValues values = new ContentValues();
		values.put("statsdate", System.currentTimeMillis()); //set to current time

		boolean returnValue = db.update(EULA_TABLE, values, "id=0", null) > 0;
		if (returnValue){};
		
		db.close();
	}
	
	public long getStatsDate(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		Cursor c = db.query(EULA_TABLE, new String[] { "statsdate" }, "id=0", null, null, null, null);
		int numRows = c.getCount();
		c.moveToFirst();
		long returnValue = 0;
		if (numRows == 1){
			returnValue = c.getLong(0);
		}
		c.close();
		db.close();
		return returnValue;
	}
	
	//categories
	public boolean insertCategory(Context ctx, String id, int wp_id, String category_name) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		ContentValues values = new ContentValues();
		values.put("blog_id", id);
		values.put("wp_id", wp_id);
		values.put("category_name", category_name.toString());
		boolean returnValue = db.insert(CATEGORIES_TABLE, null, values) > 0;
		db.close();
		return (returnValue);
	}

	public Vector<String> loadCategories(Context ctx, String id) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		Cursor c = db.query(CATEGORIES_TABLE, new String[] { "id", "wp_id", "category_name" }, "blog_id=" + id, null, null, null, null);
		int numRows = c.getCount();
		c.moveToFirst();
		Vector<String> returnVector = new Vector<String>();
		for (int i = 0; i < numRows; ++i) {
			String category_name = c.getString(2);
			if (category_name != null)
			{	
			returnVector.add(category_name);
			}
			c.moveToNext();
		}
		c.close();
		db.close();
		
		return returnVector;
	}
	
	public int getCategoryId(Context ctx, String id, String category){
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		Cursor c = db.query(CATEGORIES_TABLE, new String[] {"wp_id"}, "category_name=\"" + category + "\" AND blog_id=" + id, null, null, null, null);
		c.moveToFirst();
		int categoryID = 0;
		categoryID = c.getInt(0);
		db.close();
		return categoryID;
	}
	
	public void clearCategories(Context ctx, String id){
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		//clear out the table since we are refreshing the whole enchilada
		db.delete(CATEGORIES_TABLE, "blog_id=" + id, null);
		db.close();
	}
	
	//unique identifier queries
	public void updateUUID(Context ctx, String uuid) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		ContentValues values = new ContentValues();
		values.put("uuid", uuid);
		boolean returnValue = db.update("eula", values, null, null) > 0;
		if (returnValue){};
		db.close();
	}
	
	public String getUUID(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);		
		Cursor c = db.query("eula", new String[] { "uuid" }, "id=0", null, null, null, null);
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
	
	public boolean addQuickPressShortcut(Context ctx, String accountId, String name) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		ContentValues values = new ContentValues();
		values.put("accountId", accountId);
		values.put("name", name);
		boolean returnValue = db.insert(QUICKPRESS_SHORTCUTS_TABLE, null, values) > 0;
		db.close();
		return (returnValue);
	}
	
	public Vector<HashMap<String, Object>> getQuickPressShortcuts(Context ctx, String accountId) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Cursor c = db.query(QUICKPRESS_SHORTCUTS_TABLE, new String[] { "id", "accountId", "name"}, "accountId = "+accountId, null, null, null, null);
		String id, name;
		int numRows = c.getCount();
		c.moveToFirst();
		Vector<HashMap<String, Object>> accounts = new Vector<HashMap<String, Object>>();
		for (int i = 0; i < numRows; i++) {
			
			id = c.getString(0);
			name = c.getString(2);
			if (id != null)
			{	
				HashMap<String, Object> thisHash = new HashMap<String, Object>();
				
				thisHash.put("id", id);
				thisHash.put("name", name);
				accounts.add(thisHash);
			}
			c.moveToNext();
		}
		c.close();
		db.close();
		
		return accounts;
	}
	
	public boolean deleteQuickPressShortcut(Context ctx, String id) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		int rowsAffected = db.delete(QUICKPRESS_SHORTCUTS_TABLE, "id=" + id, null);
		
		boolean returnValue = false;
		if (rowsAffected > 0){
			returnValue = true;
		}
	    
	    db.close();
		
		return (returnValue);
	}
	
	protected String encryptPassword(String clearText) {
		try {
			DESKeySpec keySpec = new DESKeySpec(PASSWORD_SECRET.getBytes("UTF-8"));
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey key = keyFactory.generateSecret(keySpec);
			
			Cipher cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			String encrypedPwd = Base64.encodeBytes(cipher.doFinal(clearText.getBytes("UTF-8")));
			return encrypedPwd;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return clearText;
	}
	
	protected String decryptPassword(String encryptedPwd) {
		try {
			DESKeySpec keySpec = new DESKeySpec(PASSWORD_SECRET.getBytes("UTF-8"));
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey key = keyFactory.generateSecret(keySpec);
			
			byte[] encryptedWithoutB64 = Base64.decode(encryptedPwd);
			Cipher cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] plainTextPwdBytes = cipher.doFinal(encryptedWithoutB64);
			return new String(plainTextPwdBytes);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return encryptedPwd;
	}
	
	private void migratePasswords(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		Cursor c = db.query(SETTINGS_TABLE, new String[] { "id", "password", "httppassword", "dotcom_password"}, null, null, null, null, null);
		int numRows = c.getCount();
		c.moveToFirst();

		for (int i = 0; i < numRows; i++) {
			ContentValues values = new ContentValues();
			
			if(c.getString(1) != null){
				values.put("password", encryptPassword(c.getString(1)));
			}
			if(c.getString(2) != null){
				values.put("httppassword", encryptPassword(c.getString(2)));
			}
			if(c.getString(3) != null){
				values.put("dotcom_password", encryptPassword(c.getString(3)));
			}
			
			db.update(SETTINGS_TABLE, values, "id=" + c.getInt(0), null);
			
			c.moveToNext();
		}
		c.close();
	}

}

