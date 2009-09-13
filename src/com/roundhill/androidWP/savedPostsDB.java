//by Dan Roundhill, danroundhill.com/wptogo
package com.roundhill.androidWP;

import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class savedPostsDB {

	private static final String CREATE_TABLE_POSTS = "create table if not exists drafts ("
			+ "savedName text primary key, id text, title text, content text, categories text, publish boolean);";


	private static final String POSTS_TABLE = "drafts";
	private static final String DATABASE_NAME = "wpToGo";
	private static final int DATABASE_VERSION = 2;

	private SQLiteDatabase db;

	public savedPostsDB(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);

		db.execSQL(CREATE_TABLE_POSTS);
		
		db.close();
	}

	public boolean savePost(Context ctx, String savedName, String id, String title, String content, String categories, boolean publish) {
		boolean returnValue = false;
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		//same name check
		Cursor c = db.query(POSTS_TABLE, new String[] { "title", "id", "content", "categories", "publish"}, "savedName='" + savedName + "' AND id=" + id, null, null, null, null);
		int numRows = c.getCount();
		if (numRows == 1){
			//it's a match!
			returnValue = false;
		}
		else
		{
		ContentValues values = new ContentValues();
		values.put("savedName", savedName);
		values.put("id", id);
		values.put("title", title);
		values.put("content", content);
		values.put("categories", categories);
		values.put("publish", publish);
		returnValue = db.insert(POSTS_TABLE, null, values) > 0;
		}
		db.close();
		return (returnValue);
	}

	public Vector loadPost(Context ctx, String postSavedName, String id) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		Cursor c = db.query(POSTS_TABLE, new String[] { "title", "content", "categories", "publish"}, "savedName='" + postSavedName + "' AND id=" + id, null, null, null, null);
		
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
		returnVector.add(c.getInt(3));
		}
		else
		{
			returnVector = null;
		}
		c.close();
		db.close();
		
		return returnVector;
	}

	public Vector getPosts(Context ctx, String id) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		Cursor c = db.query(POSTS_TABLE, new String[] {"savedName"}, "id=" + id, null, null, null, null);
		
		int numRows = c.getCount();
		c.moveToFirst();
		Vector returnVector = new Vector();
		for (int i = 0; i < numRows; ++i) {
			String saveName = c.getString(0);
			if (saveName != null)
			{	
			returnVector.add(saveName);
			}
			c.moveToNext();
		}


		c.close();
		db.close();
		
		return returnVector;
	}
	
	public void clearPosts(Context ctx, String id) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		//clear out the table since we are refreshing the whole enchilada
		db.delete(POSTS_TABLE, "id=" + id, null);
		
		db.close();
	}


}

