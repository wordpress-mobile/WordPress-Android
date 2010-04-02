package org.wordpress.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class eulaDB {

	private static final String CREATE_TABLE_EULA = "create table if not exists eula (id integer primary key autoincrement, "
			+ "read integer not null, interval text, statsdate integer);";

	private static final String EULA_TABLE = "eula";
	private static final String DATABASE_NAME = "wordpress";

	private SQLiteDatabase db;

	public eulaDB(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		//db.execSQL("DROP TABLE IF EXISTS "+ CATEGORIES_TABLE);
		db.execSQL(CREATE_TABLE_EULA);
		
		db.close();
	}

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
		db.close();

	}
	
	public void setStatsDate(Context ctx) {
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		ContentValues values = new ContentValues();
		values.put("statsdate", System.currentTimeMillis()); //set to current time

		 boolean returnValue = db.update(EULA_TABLE, values, "id=0", null) > 0;
		
		
		String test = "blah";
		test = "yay";
		
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

}

