package org.wordpress.android;

import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class categoriesDB {

	private static final String CREATE_TABLE_CATEGORIES = "create table if not exists cats (id integer primary key autoincrement, "
			+ "blog_id text, wp_id integer, category_name text not null);";


	private static final String CATEGORIES_TABLE = "cats";
	private static final String DATABASE_NAME = "wordpress";

	private SQLiteDatabase db;

	public categoriesDB(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		//db.execSQL("DROP TABLE IF EXISTS "+ CATEGORIES_TABLE);
		db.execSQL(CREATE_TABLE_CATEGORIES);
		
		db.close();
	}

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

}

