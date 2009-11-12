//by Dan Roundhill, danroundhill.com/wptogo
package com.roundhill.androidWP;

import java.util.HashMap;
import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class localDraftsDB {

	private static final String CREATE_TABLE_LOCALDRAFTS = "create table if not exists localdrafts (id integer primary key autoincrement, blogID text, uploaded boolean, title text,content text, picturePaths text, tags text, categories text, publish boolean);";


	private static final String LOCALDRAFTS_TABLE = "localdrafts";
	private static final String DATABASE_NAME = "wpToGo";
	private static final int DATABASE_VERSION = 2;

	private SQLiteDatabase db;

	public localDraftsDB(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);

		db.execSQL(CREATE_TABLE_LOCALDRAFTS);
		
		db.close();
	}

	public boolean saveLocalDraft(Context ctx, String blogID, String title, String content, String picturePaths, String tags, String categories, boolean publish) {
		boolean returnValue = false;
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
			ContentValues values = new ContentValues();
			values.put("blogID", blogID);
			values.put("title", title);
			values.put("content", content);
			values.put("picturePaths", picturePaths);
			values.put("tags", tags);
			values.put("categories", categories);
			values.put("publish", publish);
			returnValue = db.insert(LOCALDRAFTS_TABLE, null, values) > 0;

		db.close();
		return (returnValue);
	}
	
	public boolean updateLocalDraft(Context ctx, String blogID, String postID, String title, String content, String picturePaths, String tags, String categories, boolean publish) {
		boolean returnValue = false;
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
			ContentValues values = new ContentValues();
			values.put("blogID", blogID);
			values.put("title", title);
			values.put("content", content);
			values.put("picturePaths", picturePaths);
			values.put("tags", tags);
			values.put("categories", categories);
			values.put("publish", publish);
			returnValue = db.update(LOCALDRAFTS_TABLE, values, "id=" + postID, null) > 0;

		db.close();
		return (returnValue);
	}

	public Vector loadPosts(Context ctx, String blogID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Vector returnVector = new Vector();
		Cursor c = db.query(LOCALDRAFTS_TABLE, new String[] { "id", "title", "publish", "uploaded"}, "blogID=" + blogID, null, null, null, "id desc");
		int numRows = c.getCount();
		c.moveToFirst();
		
		for (int i = 0; i < numRows; ++i) {
		if (c.getString(0) != null){
		HashMap returnHash = new HashMap();
		returnHash.put("id", c.getInt(0));
		returnHash.put("title", c.getString(1));
		returnHash.put("publish", c.getInt(2));
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
	
	public Vector loadPost(Context ctx, String postID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Vector returnVector = new Vector();
		Cursor c = db.query(LOCALDRAFTS_TABLE, new String[] { "title", "content", "picturePaths", "tags", "categories", "publish"}, "id=" + postID, null, null, null, null);
		
		int numRows = c.getCount();
		c.moveToFirst();
		
		for (int i = 0; i < numRows; ++i) {
		if (c.getString(0) != null){
		HashMap returnHash = new HashMap();
		returnHash.put("title", c.getString(0));
		returnHash.put("content", c.getString(1));
		returnHash.put("picturePaths", c.getString(2));
		returnHash.put("tags", c.getString(3));
		returnHash.put("categories", c.getString(4));
		returnHash.put("publish", c.getInt(5));
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
		Vector returnVector = new Vector();
		
		boolean returnValue = false;

		int result = 0;
		result = db.delete(LOCALDRAFTS_TABLE, "id=" + postID, null);
		db.close();
		
		if (result == 1){
			returnValue = true;
		}
		
		return returnValue;
	}

}

