//by Dan Roundhill, danroundhill.com/wptogo
package com.roundhill.androidWP;

import java.util.HashMap;
import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class postStoreDB {

	private static final String CREATE_TABLE_POSTSTORE = "create table if not exists poststore (blogID text, postID text, title text, postDate text);";
	private static final String CREATE_TABLE_PAGES = "create table if not exists pages (blogID text, pageID text, parentID text, title text, pageDate text);";
	private static final String CREATE_TABLE_COMMENTS = "create table if not exists comments (blogID text, commentID text, author text, comment text, commentDate text, status text, url text, email text, postTitle text);";
	private static final String POSTSTORE_TABLE = "poststore";
	private static final String PAGES_TABLE = "pages";
	private static final String COMMENTS_TABLE = "comments";
	private static final String DATABASE_NAME = "wpToGo";
	private static final int DATABASE_VERSION = 2;

	private SQLiteDatabase db;

	public postStoreDB(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);

		db.execSQL(CREATE_TABLE_POSTSTORE);
		db.execSQL(CREATE_TABLE_PAGES);
		db.execSQL(CREATE_TABLE_COMMENTS);
		db.close();
	}

	public boolean savePosts(Context ctx, Vector postValues) {
		boolean returnValue = false;
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		HashMap firstHash = (HashMap) postValues.get(0);
		String blogID = firstHash.get("blogID").toString();
		//delete existing values
		db.delete(POSTSTORE_TABLE, "blogID=" + blogID, null);

		for (int i = 0; i < postValues.size(); i++){
			ContentValues values = new ContentValues();
			HashMap thisHash = (HashMap) postValues.get(i);
			values.put("blogID", thisHash.get("blogID").toString());
			values.put("postID", thisHash.get("postID").toString());
			values.put("title", thisHash.get("title").toString());
			values.put("postDate", thisHash.get("postDate").toString());
			returnValue = db.insert(POSTSTORE_TABLE, null, values) > 0;
		}
		
		
		db.close();
		return (returnValue);
	}

	public Vector loadPosts(Context ctx, String blogID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Vector returnVector = new Vector();
		Cursor c = db.query(POSTSTORE_TABLE, new String[] { "blogID", "postID", "title", "postDate"}, "blogID=" + blogID, null, null, null, null);
		
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
		
		for (int i = 0; i < numRows; ++i) {
		if (c.getString(0) != null){
		HashMap returnHash = new HashMap();
		returnHash.put("blogID", c.getString(0));
		returnHash.put("postID", c.getString(1));
		returnHash.put("title", c.getString(2));
		returnHash.put("postDate", c.getString(3));
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

	public Vector loadPages(Context ctx, String blogID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Vector returnVector = new Vector();
		Cursor c = db.query(PAGES_TABLE, new String[] { "blogID", "pageID", "title", "pageDate"}, "blogID=" + blogID, null, null, null, null);
		
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
		
		for (int i = 0; i < numRows; ++i) {
		if (c.getString(0) != null){
		HashMap returnHash = new HashMap();
		returnHash.put("blogID", c.getString(0));
		returnHash.put("pageID", c.getString(1));
		returnHash.put("title", c.getString(2));
		returnHash.put("pageDate", c.getString(3));
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

	public boolean savePages(Context ctx, Vector pageValues) {
		boolean returnValue = false;
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		HashMap firstHash = (HashMap) pageValues.get(0);
		String blogID = firstHash.get("blogID").toString();
		//delete existing values
		db.delete(PAGES_TABLE, "blogID=" + blogID, null);

		for (int i = 0; i < pageValues.size(); i++){
			ContentValues values = new ContentValues();
			HashMap thisHash = (HashMap) pageValues.get(i);
			values.put("blogID", thisHash.get("blogID").toString());
			values.put("pageID", thisHash.get("pageID").toString());
			values.put("parentID", thisHash.get("parentID").toString());
			values.put("title", thisHash.get("title").toString());
			values.put("pageDate", thisHash.get("pageDate").toString());
			returnValue = db.insert(PAGES_TABLE, null, values) > 0;
		}
		
		
		db.close();
		return (returnValue);
		
	}
	
	public Vector loadComments(Context ctx, String blogID) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		Vector returnVector = new Vector();
		Cursor c = db.query(COMMENTS_TABLE, new String[] { "blogID", "commentID", "author", "comment", "commentDate", "status", "url", "email", "postTitle"}, "blogID=" + blogID, null, null, null, null);
		
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
		
		for (int i = 0; i < numRows; ++i) {
		if (c.getString(0) != null){
		HashMap returnHash = new HashMap();
		returnHash.put("blogID", c.getString(0));
		returnHash.put("commentID", c.getString(1));
		returnHash.put("author", c.getString(2));
		returnHash.put("comment", c.getString(3));
		returnHash.put("commentDate", c.getString(4));
		returnHash.put("status", c.getString(5));
		returnHash.put("url", c.getString(6));
		returnHash.put("email", c.getString(7));
		returnHash.put("postTitle", c.getString(8));
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

	public boolean saveComments(Context ctx, Vector pageValues) {
		boolean returnValue = false;
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		HashMap firstHash = (HashMap) pageValues.get(0);
		String blogID = firstHash.get("blogID").toString();
		//delete existing values
		db.delete(COMMENTS_TABLE, "blogID=" + blogID, null);

		for (int i = 0; i < pageValues.size(); i++){
			ContentValues values = new ContentValues();
			HashMap thisHash = (HashMap) pageValues.get(i);
			values.put("blogID", thisHash.get("blogID").toString());
			values.put("commentID", thisHash.get("commentID").toString());
			values.put("author", thisHash.get("author").toString());
			values.put("comment", thisHash.get("comment").toString());
			values.put("commentDate", thisHash.get("commentDate").toString());
			values.put("status", thisHash.get("status").toString());
			values.put("url", thisHash.get("url").toString());
			values.put("email", thisHash.get("email").toString());
			values.put("postTitle", thisHash.get("postTitle").toString());
			returnValue = db.insert(COMMENTS_TABLE, null, values) > 0;
		}
		
		
		db.close();
		return (returnValue);
		
	}


}

