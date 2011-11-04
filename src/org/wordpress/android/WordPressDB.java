package org.wordpress.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.Base64;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.text.StringCharacterIterator;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class WordPressDB {

	private static final int DATABASE_VERSION = 11;

	private static final String CREATE_TABLE_SETTINGS = "create table if not exists accounts (id integer primary key autoincrement, "
			+ "url text, blogName text, username text, password text, imagePlacement text, centerThumbnail boolean, fullSizeImage boolean, maxImageWidth text, maxImageWidthId integer, lastCommentId integer, runService boolean);";
	private static final String CREATE_TABLE_EULA = "create table if not exists eula (id integer primary key autoincrement, "
			+ "read integer not null, interval text, statsdate integer);";
	private static final String CREATE_TABLE_MEDIA = "create table if not exists media (id integer primary key autoincrement, "
			+ "postID integer not null, filePath text default '', fileName text default '', title text default '', description text default '', caption text default '', horizontalAlignment integer default 0, width integer default 0, height integer default 0, mimeType text default '', featured boolean default false, isVideo boolean default false);";
	private static final String SETTINGS_TABLE = "accounts";
	private static final String DATABASE_NAME = "wordpress";
	private static final String MEDIA_TABLE = "media";

	private static final String CREATE_TABLE_POSTS = "create table if not exists posts (id integer primary key autoincrement, blogID text, "
			+ "postid text, title text default '', dateCreated date, date_created_gmt date, categories text default '', custom_fields text default '', "
			+ "description text default '', link text default '', mt_allow_comments boolean, mt_allow_pings boolean, "
			+ "mt_excerpt text default '', mt_keywords text default '', mt_text_more text default '', permaLink text default '', post_status text default '', userid integer default 0, "
			+ "wp_author_display_name text default '', wp_author_id text default '', wp_password text default '', wp_post_format text default '', wp_slug text default '', mediaPaths text default '', "
			+ "latitude real, longitude real, localDraft boolean default 0, uploaded boolean default 0, isPage boolean default 0, wp_page_parent_id text, wp_page_parent_title text);";

	private static final String CREATE_TABLE_COMMENTS = "create table if not exists comments (blogID text, postID text, iCommentID integer, author text, comment text, commentDate text, commentDateFormatted text, status text, url text, email text, postTitle text);";
	private static final String POSTS_TABLE = "posts";
	private static final String COMMENTS_TABLE = "comments";

	// eula
	private static final String EULA_TABLE = "eula";

	// categories
	private static final String CREATE_TABLE_CATEGORIES = "create table if not exists cats (id integer primary key autoincrement, "
			+ "blog_id text, wp_id integer, category_name text not null);";
	private static final String CATEGORIES_TABLE = "cats";

	// for capturing blogID, trac ticket #
	private static final String ADD_BLOGID = "alter table accounts add blogId integer;";
	private static final String UPDATE_BLOGID = "update accounts set blogId = 1;";

	// add notification options
	private static final String ADD_SOUND_OPTION = "alter table eula add sound boolean default false;";
	private static final String ADD_VIBRATE_OPTION = "alter table eula add vibrate boolean default false;";
	private static final String ADD_LIGHT_OPTION = "alter table eula add light boolean default false;";
	private static final String ADD_TAGLINE = "alter table eula add tagline text;";
	private static final String ADD_TAGLINE_FLAG = "alter table eula add tagline_flag boolean default false;";

	// for capturing blogID, trac ticket #
	private static final String ADD_LOCATION_FLAG = "alter table accounts add location boolean default false;";

	// fix commentID data type
	private static final String ADD_NEW_COMMENT_ID = "ALTER TABLE comments ADD iCommentID INTEGER;";
	private static final String COPY_COMMENT_IDS = "UPDATE comments SET iCommentID = commentID;";

	// add wordpress.com stats login info
	private static final String ADD_DOTCOM_USERNAME = "alter table accounts add dotcom_username text;";
	private static final String ADD_DOTCOM_PASSWORD = "alter table accounts add dotcom_password text;";
	private static final String ADD_API_KEY = "alter table accounts add api_key text;";
	private static final String ADD_API_BLOGID = "alter table accounts add api_blogid text;";

	// add wordpress.com flag and version column
	private static final String ADD_DOTCOM_FLAG = "alter table accounts add dotcomFlag boolean default false;";
	private static final String ADD_WP_VERSION = "alter table accounts add wpVersion text;";

	// add httpuser and httppassword
	private static final String ADD_HTTPUSER = "alter table accounts add httpuser text;";
	private static final String ADD_HTTPPASSWORD = "alter table accounts add httppassword text;";

	// add new unique identifier to no longer use device imei
	private static final String ADD_UNIQUE_ID = "alter table eula add uuid text;";

	// add new table for QuickPress homescreen shortcuts
	private static final String CREATE_TABLE_QUICKPRESS_SHORTCUTS = "create table if not exists quickpress_shortcuts (id integer primary key autoincrement, accountId text, name text);";
	private static final String QUICKPRESS_SHORTCUTS_TABLE = "quickpress_shortcuts";

	// add field to store last used blog
	private static final String ADD_LAST_BLOG_ID = "alter table eula add last_blog_id text;";

	// add field to store last used blog
	private static final String ADD_POST_FORMATS = "alter table accounts add postFormats text default '';";

	private SQLiteDatabase db;

	protected static final String PASSWORD_SECRET = "nottherealpasscode";

	public String defaultBlog = "";

	public WordPressDB(Context ctx) {
	
		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
		
		// db.execSQL("DROP TABLE IF EXISTS "+ SETTINGS_TABLE);
		db.execSQL(CREATE_TABLE_SETTINGS);
		// added eula to this class to fix trac #49
		db.execSQL(CREATE_TABLE_EULA);

		db.execSQL(CREATE_TABLE_POSTS);
		db.execSQL(CREATE_TABLE_COMMENTS);
		db.execSQL(CREATE_TABLE_CATEGORIES);
		db.execSQL(CREATE_TABLE_QUICKPRESS_SHORTCUTS);
		db.execSQL(CREATE_TABLE_MEDIA);

		try {
			if (db.getVersion() < 1) { // user is new install
				db.execSQL(ADD_BLOGID);
				db.execSQL(UPDATE_BLOGID);
				db.execSQL(ADD_SOUND_OPTION);
				db.execSQL(ADD_VIBRATE_OPTION);
				db.execSQL(ADD_LIGHT_OPTION);
				db.execSQL(ADD_LOCATION_FLAG);
				db.execSQL(ADD_TAGLINE);
				db.execSQL(ADD_TAGLINE_FLAG);
				db.execSQL(ADD_DOTCOM_USERNAME);
				db.execSQL(ADD_DOTCOM_PASSWORD);
				db.execSQL(ADD_API_KEY);
				db.execSQL(ADD_API_BLOGID);
				db.execSQL(ADD_DOTCOM_FLAG);
				db.execSQL(ADD_WP_VERSION);
				db.execSQL(ADD_UNIQUE_ID);
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_LAST_BLOG_ID);
				db.execSQL(ADD_POST_FORMATS);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION); // set to latest revision
			} else if (db.getVersion() == 1) { // v1.0 or v1.0.1
				db.execSQL(ADD_BLOGID);
				db.execSQL(UPDATE_BLOGID);
				db.execSQL(ADD_SOUND_OPTION);
				db.execSQL(ADD_VIBRATE_OPTION);
				db.execSQL(ADD_LIGHT_OPTION);
				db.execSQL(ADD_LOCATION_FLAG);
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
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_LAST_BLOG_ID);
				db.execSQL(ADD_POST_FORMATS);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION); // set to latest revision
			} else if (db.getVersion() == 2) {
				db.execSQL(ADD_SOUND_OPTION);
				db.execSQL(ADD_VIBRATE_OPTION);
				db.execSQL(ADD_LIGHT_OPTION);
				db.execSQL(ADD_LOCATION_FLAG);
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
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_LAST_BLOG_ID);
				db.execSQL(ADD_POST_FORMATS);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION);
			} else if (db.getVersion() == 3) {
				db.execSQL(ADD_LOCATION_FLAG);
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
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_LAST_BLOG_ID);
				db.execSQL(ADD_POST_FORMATS);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION);
			} else if (db.getVersion() == 4) {
				db.execSQL(ADD_LOCATION_FLAG);
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
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_LAST_BLOG_ID);
				db.execSQL(ADD_POST_FORMATS);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION);
			} else if (db.getVersion() == 5) {
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
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_LAST_BLOG_ID);
				db.execSQL(ADD_POST_FORMATS);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION);
			} else if (db.getVersion() == 6) {
				db.execSQL(ADD_NEW_COMMENT_ID);
				db.execSQL(COPY_COMMENT_IDS);
				db.execSQL(ADD_DOTCOM_USERNAME);
				db.execSQL(ADD_DOTCOM_PASSWORD);
				db.execSQL(ADD_API_KEY);
				db.execSQL(ADD_API_BLOGID);
				db.execSQL(ADD_DOTCOM_FLAG);
				db.execSQL(ADD_WP_VERSION);
				db.execSQL(ADD_UNIQUE_ID);
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_LAST_BLOG_ID);
				db.execSQL(ADD_POST_FORMATS);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION);
			} else if (db.getVersion() == 7) {
				db.execSQL(ADD_UNIQUE_ID);
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_LAST_BLOG_ID);
				db.execSQL(ADD_POST_FORMATS);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION);
			} else if (db.getVersion() == 8) {
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_LAST_BLOG_ID);
				db.execSQL(ADD_POST_FORMATS);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION);
			} else if (db.getVersion() == 9) {
				db.execSQL(ADD_HTTPUSER);
				db.execSQL(ADD_HTTPPASSWORD);
				db.execSQL(ADD_LAST_BLOG_ID);
				db.execSQL(ADD_POST_FORMATS);
				migratePasswords(ctx);
				db.setVersion(DATABASE_VERSION);
			} else if (db.getVersion() == 10) {

				db.delete(POSTS_TABLE, null, null);
				db.execSQL(CREATE_TABLE_POSTS);

				try {
					// migrate drafts
					// posts
					
					Cursor c = db.query("localdrafts", new String[] { "blogID",
							"title", "content", "picturePaths", "date",
							"categories", "tags", "status", "password",
							"latitude", "longitude" }, null, null, null, null,
							"id desc");
					int numRows = c.getCount();
					c.moveToFirst();

					for (int i = 0; i < numRows; ++i) {
						if (c.getString(0) != null) {
							Post post = new Post(c.getInt(0), c.getString(1),
									c.getString(2), c.getString(3),
									c.getLong(4), c.getString(5),
									c.getString(6), c.getString(7),
									c.getString(8), c.getDouble(9),
									c.getDouble(10), false, "", ctx);
							post.setLocalDraft(true);
							post.save();
						}
						c.moveToNext();
					}
					c.close();

					db.delete("localdrafts", null, null);

					// pages
					
					c = db.query("localpagedrafts", new String[] { "blogID",
							"title", "content", "picturePaths", "date",
							"status", "password" }, null, null, null, null,
							"id desc");
					numRows = c.getCount();
					c.moveToFirst();

					for (int i = 0; i < numRows; ++i) {
						if (c.getString(0) != null) {
							Post post = new Post(c.getInt(0), c.getString(1),
									c.getString(2), c.getString(3),
									c.getLong(4), c.getString(5),
									c.getString(6), null, null, 0, 0, true, "",
									ctx);
							post.setLocalDraft(true);
							post.setPage(true);
							post.save();
						}
						c.moveToNext();
					}
					c.close();
					db.delete("localpagedrafts", null, null);
				} catch (Exception e) {
					e.printStackTrace();
					// didn't work, that's ok.
				}

				db.execSQL(ADD_LAST_BLOG_ID);
				db.execSQL(ADD_POST_FORMATS);
				db.setVersion(DATABASE_VERSION);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public boolean addAccount(String url, String blogName,
			String username, String password, String httpuser,
			String httppassword, String imagePlacement,
			boolean centerThumbnail, boolean fullSizeImage,
			String maxImageWidth, int maxImageWidthId, boolean runService,
			int blogId, boolean wpcom, String wpVersion) {
		
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
		
		return (returnValue);
	}

	public Vector<HashMap<String, Object>> getAccounts(Context ctx) {
		
		Cursor c = db.query(SETTINGS_TABLE, new String[] { "id", "blogName",
				"username", "runService", "blogId", "url" }, null, null, null,
				null, null);
		int id;
		String blogName, username, url;
		int blogId;
		int runService;
		int numRows = c.getCount();
		c.moveToFirst();
		Vector<HashMap<String, Object>> accounts = new Vector<HashMap<String, Object>>();
		for (int i = 0; i < numRows; i++) {

			id = c.getInt(0);
			blogName = c.getString(1);
			username = c.getString(2);
			runService = c.getInt(3);
			blogId = c.getInt(4);
			url = c.getString(5);
			if (id > 0) {
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
		

		return accounts;
	}

	public boolean checkMatch(String blogName, String blogURL,
			String username) {
		
		Cursor c = db.query(SETTINGS_TABLE, new String[] { "blogName", "url" },
				"blogName='" + addSlashes(blogName) + "' AND url='"
						+ addSlashes(blogURL) + "'" + " AND username='"
						+ username + "'", null, null, null, null);
		int numRows = c.getCount();
		boolean result = false;

		if (numRows > 0) {
			// this account is already saved, yo!
			result = true;
		}

		c.close();
		

		return result;
	}

	public static String addSlashes(String text) {
		final StringBuffer sb = new StringBuffer(text.length() * 2);
		final StringCharacterIterator iterator = new StringCharacterIterator(
				text);

		char character = iterator.current();

		while (character != StringCharacterIterator.DONE) {
			if (character == '"')
				sb.append("\\\"");
			else if (character == '\'')
				sb.append("\'\'");
			else if (character == '\\')
				sb.append("\\\\");
			else if (character == '\n')
				sb.append("\\n");
			else if (character == '{')
				sb.append("\\{");
			else if (character == '}')
				sb.append("\\}");
			else
				sb.append(character);

			character = iterator.next();
		}

		return sb.toString();
	}

	public boolean saveSettings(String id, String url,
			String username, String password, String httpuser,
			String httppassword, String imagePlacement,
			boolean centerThumbnail, boolean fullSizeImage,
			String maxImageWidth, int maxImageWidthId, boolean location,
			boolean isWPCom, String originalUsername, String postFormats) {
		
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
		values.put("postFormats", postFormats);
		boolean returnValue = db.update(SETTINGS_TABLE, values, "id=" + id,
				null) > 0;
		if (isWPCom) {
			// update the login for other wordpress.com accounts
			ContentValues userPass = new ContentValues();
			userPass.put("username", username);
			userPass.put("password", encryptPassword(password));
			returnValue = db.update(SETTINGS_TABLE, userPass, "username=\""
					+ originalUsername + "\" AND dotcomFlag=1", null) > 0;
		}
		
		return (returnValue);
	}

	public boolean deleteAccount(Context ctx, int id) {
		

		int rowsAffected = 0;
		try {
			rowsAffected = db.delete(SETTINGS_TABLE, "id=" + id, null);
		} finally {
			
		}

		boolean returnValue = false;
		if (rowsAffected > 0) {
			returnValue = true;
		}

		// delete QuickPress homescreen shortcuts connected with this account
		Vector<HashMap<String, Object>> shortcuts = this
				.getQuickPressShortcuts(ctx, id);
		for (int i = 0; i < shortcuts.size(); i++) {
			HashMap<String, Object> shortcutHash = shortcuts.get(i);

			Intent shortcutIntent = new Intent();
			shortcutIntent.setClassName(EditPost.class.getPackage().getName(),
					EditPost.class.getName());
			shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			shortcutIntent.setAction(Intent.ACTION_VIEW);
			Intent broadcastShortcutIntent = new Intent();
			broadcastShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
					shortcutIntent);
			broadcastShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
					shortcutHash.get("name").toString());
			broadcastShortcutIntent.putExtra("duplicate", false);
			broadcastShortcutIntent
					.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
			ctx.sendBroadcast(broadcastShortcutIntent);

			deleteQuickPressShortcut(ctx, shortcutHash.get("id").toString());
		}

		return (returnValue);
	}

	public Vector<Object> loadSettings(int id) {
		

		Cursor c = db.query(SETTINGS_TABLE, new String[] { "url", "blogName",
				"username", "password", "httpuser", "httppassword",
				"imagePlacement", "centerThumbnail", "fullSizeImage",
				"maxImageWidth", "maxImageWidthId", "runService", "blogId",
				"location", "dotcomFlag", "dotcom_username", "dotcom_password",
				"api_key", "api_blogid", "wpVersion", "postFormats",
				"lastCommentId" }, "id=" + id, null, null, null, null);

		int numRows = c.getCount();
		c.moveToFirst();

		Vector<Object> returnVector = new Vector<Object>();
		if (numRows > 0) {
			if (c.getString(0) != null) {
				returnVector.add(c.getString(0));
				returnVector.add(c.getString(1));
				returnVector.add(c.getString(2));
				returnVector.add(decryptPassword(c.getString(3)));
				if (c.getString(4) == null) {
					returnVector.add("");
				} else {
					returnVector.add(c.getString(4));
				}
				if (c.getString(5) == null) {
					returnVector.add("");
				} else {
					returnVector.add(decryptPassword(c.getString(5)));
				}
				returnVector.add(c.getString(6));
				returnVector.add(c.getInt(7));
				returnVector.add(c.getInt(8));
				returnVector.add(c.getString(9));
				returnVector.add(c.getInt(10));
				returnVector.add(c.getInt(11));
				returnVector.add(c.getInt(12));
				returnVector.add(c.getInt(13));
				returnVector.add(c.getInt(14));
				returnVector.add(c.getString(15));
				returnVector.add(decryptPassword(c.getString(16)));
				returnVector.add(c.getString(17));
				returnVector.add(c.getString(18));
				returnVector.add(c.getString(19));
				returnVector.add(c.getString(20));
				returnVector.add(c.getInt(21));
			} else {
				returnVector = null;
			}
		}
		c.close();
		

		return returnVector;
	}

	public Vector<String> loadStatsLogin(int id) {
		

		Cursor c = db.query(SETTINGS_TABLE, new String[] { "dotcom_username",
				"dotcom_password" }, "id=" + id, null, null, null, null);

		c.moveToFirst();

		Vector<String> returnVector = new Vector<String>();
		if (c.getString(0) != null) {
			returnVector.add(c.getString(0));
			returnVector.add(decryptPassword(c.getString(1)));
		} else {
			returnVector = null;
		}
		c.close();
		

		return returnVector;
	}

	public boolean saveStatsLogin(int id, String statsUsername,
			String statsPassword) {
		
		ContentValues values = new ContentValues();
		values.put("dotcom_username", statsUsername);
		values.put("dotcom_password", encryptPassword(statsPassword));
		boolean returnValue = db.update(SETTINGS_TABLE, values, "id=" + id,
				null) > 0;
		

		return (returnValue);

	}

	public Vector<String> loadAPIData(int id) {
		

		Cursor c = db.query(SETTINGS_TABLE, new String[] { "api_key",
				"api_blogid" }, "id=" + id, null, null, null, null);

		c.moveToFirst();

		Vector<String> returnVector = new Vector<String>();
		if (c.getString(0) != null) {
			returnVector.add(c.getString(0));
			returnVector.add(c.getString(1));
		} else {
			returnVector = null;
		}
		c.close();
		

		return returnVector;
	}

	public boolean saveAPIData(int id, String apiKey,
			String apiBlogID) {
		
		ContentValues values = new ContentValues();
		values.put("api_key", apiKey);
		values.put("api_blogid", apiBlogID);
		boolean returnValue = db.update(SETTINGS_TABLE, values, "id=" + id,
				null) > 0;
		

		return (returnValue);

	}

	public boolean updateLatestCommentID(int id,
			Integer newCommentID) {
		

		boolean returnValue = false;

		synchronized (this) {
			ContentValues values = new ContentValues();
			values.put("lastCommentId", newCommentID);

			returnValue = db.update(SETTINGS_TABLE, values, "id=" + id, null) > 0;
		}
		
		return (returnValue);

	}

	public Vector<Integer> getNotificationAccounts(Context ctx) {
		

		Cursor c = null;
		try {
			c = db.query(SETTINGS_TABLE, new String[] { "id" }, "runService=1",
					null, null, null, null);
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
		
		return returnVector;
	}

	public String getAccountName(String accountID) {
		
		String accountName = "";
		Cursor c = db.query(SETTINGS_TABLE, new String[] { "blogName" }, "id="
				+ accountID, null, null, null, null);
		c.moveToFirst();
		if (c.getString(0) != null) {
			accountName = c.getString(0);
		}
		c.close();
		
		return accountName;
	}

	public void updateNotificationFlag(int id, boolean flag) {
		
		ContentValues values = new ContentValues();
		int iFlag = 0;
		if (flag) {
			iFlag = 1;
		}
		values.put("runService", iFlag);

		boolean returnValue = db.update(SETTINGS_TABLE, values,
				"id=" + String.valueOf(id), null) > 0;
		if (returnValue) {
		}
		

	}

	public void updateNotificationSettings(String interval,
			boolean sound, boolean vibrate, boolean light,
			boolean tagline_flag, String tagline) {
		
		ContentValues values = new ContentValues();
		values.put("interval", interval);
		values.put("sound", sound);
		values.put("vibrate", vibrate);
		values.put("light", light);
		values.put("tagline_flag", tagline_flag);
		values.put("tagline", tagline);

		boolean returnValue = db.update("eula", values, null, null) > 0;
		if (returnValue) {
		}
		;
		

	}

	public String getInterval(Context ctx) {
		

		Cursor c = db.query("eula", new String[] { "interval" }, "id=0", null,
				null, null, null);
		int numRows = c.getCount();
		c.moveToFirst();
		String returnValue = "";
		if (numRows == 1) {
			if (c.getString(0) != null) {
				returnValue = c.getString(0);
			}
		}
		c.close();
		

		return returnValue;

	}

	public HashMap<String, Object> getNotificationOptions(Context ctx) {
		
		Cursor c = db.query("eula", new String[] { "id", "sound", "vibrate",
				"light", "tagline_flag", "tagline" }, "id=0", null, null, null,
				null);
		int sound, vibrate, light;
		String tagline;
		HashMap<String, Object> thisHash = new HashMap<String, Object>();
		int numRows = c.getCount();
		if (numRows >= 1) {
			c.moveToFirst();

			sound = c.getInt(1);
			vibrate = c.getInt(2);
			light = c.getInt(3);
			tagline = c.getString(5);
			thisHash.put("sound", sound);
			thisHash.put("vibrate", vibrate);
			thisHash.put("light", light);
			thisHash.put("tagline_flag", c.getInt(4));
			if (tagline != null) {
				thisHash.put("tagline", tagline);
			} else {
				thisHash.put("tagline", "");
			}

		}

		c.close();
		

		return thisHash;
	}

	public void updateLastBlogID(int blogID) {
		
		ContentValues values = new ContentValues();
		values.put("last_blog_id", blogID);

		db.update("eula", values, null, null);

		

	}

	public int getLastBlogID(Context ctx) {
		
		int returnValue = -1;
		Cursor c = db.query("eula", new String[] { "last_blog_id" }, "id=0",
				null, null, null, null);
		int numRows = c.getCount();
		c.moveToFirst();
		if (numRows == 1) {
			if (c.getString(0) != null) {
				returnValue = c.getInt(0);
			}
		}
		c.close();
		

		return returnValue;
	}

	public Vector<HashMap<String, Object>> loadDrafts(int blogID,
			boolean loadPages) {
		
		Vector<HashMap<String, Object>> returnVector = new Vector<HashMap<String, Object>>();
		Cursor c;
		if (loadPages)
			c = db.query(POSTS_TABLE, new String[] { "id", "title",
					"post_status", "uploaded", "date_created_gmt" }, "blogID="
					+ blogID + " AND localDraft=1 AND uploaded=0 AND isPage=1",
					null, null, null, null);
		else
			c = db.query(POSTS_TABLE, new String[] { "id", "title",
					"post_status", "uploaded", "date_created_gmt" }, "blogID="
					+ blogID + " AND localDraft=1 AND uploaded=0 AND isPage=0",
					null, null, null, null);

		int numRows = c.getCount();
		c.moveToFirst();

		for (int i = 0; i < numRows; ++i) {
			if (c.getString(0) != null) {
				HashMap<String, Object> returnHash = new HashMap<String, Object>();
				returnHash.put("id", c.getString(0));
				returnHash.put("title", c.getString(1));
				returnHash.put("status", c.getString(2));
				returnHash.put("uploaded", c.getInt(3));
				returnHash.put("date_created_gmt", c.getLong(4));
				returnVector.add(i, returnHash);
			}
			c.moveToNext();
		}
		c.close();
		

		if (numRows == 0) {
			returnVector = null;
		}

		return returnVector;
	}

	public boolean deletePost(Post post) {
		

		boolean returnValue = false;

		int result = 0;
		result = db.delete(POSTS_TABLE, "blogID=" + post.getBlogID()
				+ " AND id=" + post.getId(), null);
		

		if (result == 1) {
			returnValue = true;
		}

		return returnValue;
	}

	public boolean savePosts(Vector<?> postValues, int blogID,
			boolean isPage) {
		boolean returnValue = false;
		if (postValues.size() != 0) {
			

			for (int i = 0; i < postValues.size(); i++) {
				ContentValues values = new ContentValues();
				HashMap<?, ?> thisHash = (HashMap<?, ?>) postValues.get(i);
				values.put("blogID", blogID);
				String postID = thisHash.get((isPage) ? "page_id" : "postid")
						.toString();
				values.put("postid", postID);
				values.put("title", thisHash.get("title").toString());
				Date d = (Date) thisHash.get("dateCreated");
				values.put("dateCreated", d.getTime());
				d = (Date) thisHash.get("date_created_gmt");
				values.put("date_created_gmt", d.getTime());
				values.put("description", thisHash.get("description")
						.toString());
				values.put("link", thisHash.get("link").toString());
				values.put("permaLink", thisHash.get("permaLink").toString());

				Object[] cats = (Object[]) thisHash.get("categories");
				JSONArray jsonArray = new JSONArray();
				if (cats != null) {
					for (int x = 0; x < cats.length; x++) {
						jsonArray.put(cats[x].toString());
					}
				}
				values.put("categories", jsonArray.toString());

				Object[] custom_fields = (Object[]) thisHash
						.get("custom_fields");
				jsonArray = new JSONArray();
				if (custom_fields != null) {
					for (int x = 0; x < custom_fields.length; x++) {
						jsonArray.put(custom_fields[x].toString());
					}
				}
				values.put("custom_fields", jsonArray.toString());

				values.put("mt_excerpt",
						thisHash.get((isPage) ? "excerpt" : "mt_excerpt")
								.toString());
				values.put("mt_text_more",
						thisHash.get((isPage) ? "text_more" : "mt_text_more")
								.toString());
				values.put("mt_allow_comments",
						(Integer) thisHash.get("mt_allow_comments"));
				values.put("mt_allow_pings",
						(Integer) thisHash.get("mt_allow_pings"));
				values.put("wp_slug", thisHash.get("wp_slug").toString());
				values.put("wp_password", thisHash.get("wp_password")
						.toString());
				values.put("wp_author_id", thisHash.get("wp_author_id")
						.toString());
				values.put("wp_author_display_name",
						thisHash.get("wp_author_display_name").toString());
				values.put("post_status",
						thisHash.get((isPage) ? "page_status" : "post_status")
								.toString());
				values.put("userid", thisHash.get("userid").toString());

				int isPageInt = 0;
				if (isPage) {
					isPageInt = 1;
					values.put("isPage", true);
					values.put("wp_page_parent_id",
							thisHash.get("wp_page_parent_id").toString());
					values.put("wp_page_parent_title",
							thisHash.get("wp_page_parent_title").toString());
				} else {
					values.put("mt_keywords", thisHash.get("mt_keywords")
							.toString());
					values.put("wp_post_format", thisHash.get("wp_post_format")
							.toString());
				}

				int result = db.update(POSTS_TABLE, values, "postID=" + postID
						+ " AND isPage=" + isPageInt, null);
				if (result == 0)
					returnValue = db.insert(POSTS_TABLE, null, values) > 0;
				else
					returnValue = true;
			}
			
		}
		return (returnValue);
	}

	public long savePost(Post post, int blogID) {
		long returnValue = -1;
		if (post != null) {
			

			ContentValues values = new ContentValues();
			values.put("blogID", blogID);
			values.put("title", post.getTitle());
			values.put("date_created_gmt", post.getDate_created_gmt());
			values.put("description", post.getDescription());
			values.put("mt_text_more", post.getMt_text_more());

			if (post.getCategories() != null) {
				JSONArray jsonArray = null;
				try {
					jsonArray = new JSONArray(post.getCategories().toString());
					values.put("categories", jsonArray.toString());
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			values.put("localDraft", post.isLocalDraft());
			values.put("mediaPaths", post.getMediaPaths());
			values.put("mt_keywords", post.getMt_keywords());
			values.put("wp_password", post.getWP_password());
			values.put("post_status", post.getPost_status());
			values.put("uploaded", post.isUploaded());
			values.put("isPage", post.isPage());
			values.put("wp_post_format", post.getWP_post_format());

			returnValue = db.insert(POSTS_TABLE, null, values);

			
		}
		return (returnValue);
	}

	public int updatePost(Post post, int blogID) {
		int success = 0;
		if (post != null) {
			

			ContentValues values = new ContentValues();
			values.put("blogID", blogID);
			values.put("title", post.getTitle());
			values.put("date_created_gmt", post.getDate_created_gmt());
			values.put("description", post.getDescription());
			values.put("uploaded", post.isUploaded());

			if (post.getCategories() != null) {
				JSONArray jsonArray = null;
				try {
					jsonArray = new JSONArray(post.getCategories().toString());
					values.put("categories", jsonArray.toString());
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			values.put("localDraft", post.isLocalDraft());
			values.put("mediaPaths", post.getMediaPaths());
			values.put("mt_keywords", post.getMt_keywords());
			values.put("wp_password", post.getWP_password());
			values.put("post_status", post.getPost_status());
			values.put("isPage", post.isPage());
			values.put("wp_post_format", post.getWP_post_format());

			int pageInt = 0;
			if (post.isPage())
				pageInt = 1;

			success = db.update(POSTS_TABLE, values,
					"blogID=" + post.getBlogID() + " AND id=" + post.getId()
							+ " AND isPage=" + pageInt, null);

			
		}
		return (success);
	}

	public Vector<HashMap<String, Object>> loadUploadedPosts(Context ctx,
			int blogID, boolean loadPages) {
		
		Vector<HashMap<String, Object>> returnVector = new Vector<HashMap<String, Object>>();
		Cursor c;
		if (loadPages)
			c = db.query(POSTS_TABLE, new String[] { "id", "blogID", "postid",
					"title", "date_created_gmt", "dateCreated" }, "blogID="
					+ blogID + " AND localDraft != 1 AND isPage=1", null, null,
					null, null);
		else
			c = db.query(POSTS_TABLE, new String[] { "id", "blogID", "postid",
					"title", "date_created_gmt", "dateCreated" }, "blogID="
					+ blogID + " AND localDraft != 1 AND isPage=0", null, null,
					null, null);

		int numRows = c.getCount();
		c.moveToFirst();

		for (int i = 0; i < numRows; ++i) {
			if (c.getString(0) != null) {
				HashMap<String, Object> returnHash = new HashMap<String, Object>();
				returnHash.put("id", c.getInt(0));
				returnHash.put("blogID", c.getString(1));
				returnHash.put("postID", c.getString(2));
				returnHash.put("title", c.getString(3));
				returnHash.put("date_created_gmt", c.getLong(4));
				returnHash.put("dateCreated", c.getLong(5));
				returnVector.add(i, returnHash);
			}
			c.moveToNext();
		}
		c.close();
		

		if (numRows == 0) {
			returnVector = null;
		}

		return returnVector;
	}

	public void deleteUploadedPosts(int blogID, boolean isPage) {
		
		if (isPage)
			db.delete(POSTS_TABLE, "blogID=" + blogID
					+ " AND localDraft != 1 AND isPage=1", null);
		else
			db.delete(POSTS_TABLE, "blogID=" + blogID
					+ " AND localDraft != 1 AND isPage=0", null);
		
	}

	public Vector<Object> loadPost(int blogID, boolean isPage,
			long id) {
		Vector<Object> values = null;
		
		int pageInt = 0;
		if (isPage)
			pageInt = 1;
		Cursor c = db.query(POSTS_TABLE, null, "blogID=" + blogID + " AND id="
				+ id + " AND isPage=" + pageInt, null, null, null, null);

		c.moveToFirst();
		
		if (c.getString(0) != null) {
			values = new Vector<Object>();
			values.add(c.getLong(0));
			values.add(c.getString(1));
			values.add(c.getString(2));
			values.add(c.getString(3));
			values.add(c.getLong(4));
			values.add(c.getLong(5));
			values.add(c.getString(6));
			values.add(c.getString(7));
			values.add(c.getString(8));
			values.add(c.getString(9));
			values.add(c.getInt(10));
			values.add(c.getInt(11));
			values.add(c.getString(12));
			values.add(c.getString(13));
			values.add(c.getString(14));
			values.add(c.getString(15));
			values.add(c.getString(16));
			values.add(c.getString(17));
			values.add(c.getString(18));
			values.add(c.getString(19));
			values.add(c.getString(20));
			values.add(c.getString(21));
			values.add(c.getString(22));
			values.add(c.getString(23));
			values.add(c.getDouble(24));
			values.add(c.getDouble(25));
			values.add(c.getInt(26));
			values.add(c.getInt(27));
			values.add(c.getInt(28));
		}
		c.close();
		

		return values;
	}

	public Vector<HashMap<String, Object>> loadComments(int blogID) {
		
		Vector<HashMap<String, Object>> returnVector = new Vector<HashMap<String, Object>>();
		Cursor c = db.query(COMMENTS_TABLE,
				new String[] { "blogID", "postID", "iCommentID", "author",
						"comment", "commentDate", "commentDateFormatted",
						"status", "url", "email", "postTitle" }, "blogID="
						+ blogID, null, null, null, null);

		int numRows = c.getCount();
		c.moveToFirst();

		HashMap<String, Object> numRecords = new HashMap<String, Object>();
		// add the number of stored records so the offset can be computed
		if (numRows > 0) {
			numRecords.put("numRecords", numRows);
			returnVector.add(0, numRecords);
		}

		for (int i = 1; i < (numRows + 1); ++i) {
			if (c.getString(0) != null) {
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
		

		if (numRows == 0) {
			returnVector = null;
		}

		return returnVector;
	}

	public Vector<HashMap<String, Object>> loadMoreComments(Context ctx,
			int blogID, int limit) {
		
		Vector<HashMap<String, Object>> returnVector = new Vector<HashMap<String, Object>>();
		Cursor c = db.query(COMMENTS_TABLE,
				new String[] { "blogID", "postID", "iCommentID", "author",
						"comment", "commentDate", "commentDateFormatted",
						"status", "url", "email", "postTitle" }, "blogID="
						+ blogID, null, null, null, "iCommentID ASC",
				String.valueOf(limit));
		int numRows = c.getCount();
		c.moveToFirst();

		// HashMap numRecords = new HashMap();
		// add the number of stored records so the offset can be computed
		/*
		 * if (numRows > 0){ numRecords.put("numRecords", numRows);
		 * returnVector.add(0, numRecords); }
		 */
		for (int i = 0; i < numRows; i++) {
			if (c.getString(0) != null) {
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
		

		if (numRows == 0) {
			returnVector = null;
		}

		return returnVector;
	}

	public boolean saveComments(Vector<?> commentValues,
			boolean loadMore) {
		boolean returnValue = false;
		
		HashMap<?, ?> firstHash = (HashMap<?, ?>) commentValues.get(0);
		String blogID = firstHash.get("blogID").toString();
		// delete existing values, if user hit refresh button
		if (!loadMore) {
			try {
				db.delete(COMMENTS_TABLE, "blogID=" + blogID, null);
			} catch (Exception e) {
				
				return false;
			}
		}

		for (int i = 0; i < commentValues.size(); i++) {
			ContentValues values = new ContentValues();
			HashMap<?, ?> thisHash = (HashMap<?, ?>) commentValues.get(i);
			values.put("blogID", thisHash.get("blogID").toString());
			values.put("postID", thisHash.get("postID").toString());
			values.put("iCommentID", thisHash.get("commentID").toString());
			values.put("author", thisHash.get("author").toString());
			values.put("comment", thisHash.get("comment").toString());
			values.put("commentDate", thisHash.get("commentDate").toString());
			values.put("commentDateFormatted",
					thisHash.get("commentDateFormatted").toString());
			values.put("status", thisHash.get("status").toString());
			values.put("url", thisHash.get("url").toString());
			values.put("email", thisHash.get("email").toString());
			values.put("postTitle", thisHash.get("postTitle").toString());
			synchronized (this) {
				try {
					returnValue = db.insert(COMMENTS_TABLE, null, values) > 0;
				} catch (Exception e) {
					
					return false;
				}
			}
		}
		
		return (returnValue);

	}

	public void updateCommentStatus(int blogID, String id,
			String newStatus) {
		

		ContentValues values = new ContentValues();
		values.put("status", newStatus);
		synchronized (this) {
			db.update(COMMENTS_TABLE, values, "blogID=" + blogID
					+ " AND iCommentID=" + id, null);
		}

		

	}

	public void clearPosts(String blogID) {
		
		// delete existing values
		db.delete(POSTS_TABLE, "blogID=" + blogID, null);
		

	}

	// eula table
	public boolean checkEULA(Context ctx) {
		

		Cursor c = db.query(EULA_TABLE, new String[] { "read" }, "id=0", null,
				null, null, null);
		int numRows = c.getCount();
		c.moveToFirst();
		boolean returnValue = false;
		if (numRows == 1) {
			returnValue = (c.getInt(0) != 0);
		}
		c.close();
		

		return returnValue;
	}

	public void setEULA(Context ctx) {
		
		ContentValues values = new ContentValues();
		values.put("id", 0);
		values.put("read", 1); // set that they've read the EULA
		synchronized (this) {
			db.insert(EULA_TABLE, null, values);
		}

		

	}

	public void setStatsDate(Context ctx) {
		
		ContentValues values = new ContentValues();
		values.put("statsdate", System.currentTimeMillis()); // set to current
																// time
		synchronized (this) {
			db.update(EULA_TABLE, values, "id=0", null);
		}

		
	}

	public long getStatsDate(Context ctx) {
		

		Cursor c = db.query(EULA_TABLE, new String[] { "statsdate" }, "id=0",
				null, null, null, null);
		int numRows = c.getCount();
		c.moveToFirst();
		long returnValue = 0;
		if (numRows == 1) {
			returnValue = c.getLong(0);
		}
		c.close();
		
		return returnValue;
	}

	// categories
	public boolean insertCategory(int id, int wp_id,
			String category_name) {
		
		ContentValues values = new ContentValues();
		values.put("blog_id", id);
		values.put("wp_id", wp_id);
		values.put("category_name", category_name.toString());
		boolean returnValue = false;
		synchronized (this) {
			returnValue = db.insert(CATEGORIES_TABLE, null, values) > 0;
		}
		
		return (returnValue);
	}

	public Vector<String> loadCategories(int id) {
		

		Cursor c = db.query(CATEGORIES_TABLE, new String[] { "id", "wp_id",
				"category_name" }, "blog_id=" + id, null, null, null, null);
		int numRows = c.getCount();
		c.moveToFirst();
		Vector<String> returnVector = new Vector<String>();
		for (int i = 0; i < numRows; ++i) {
			String category_name = c.getString(2);
			if (category_name != null) {
				returnVector.add(category_name);
			}
			c.moveToNext();
		}
		c.close();
		

		return returnVector;
	}

	public int getCategoryId(int id, String category) {
		

		Cursor c = db.query(CATEGORIES_TABLE, new String[] { "wp_id" },
				"category_name=\"" + category + "\" AND blog_id=" + id, null,
				null, null, null);
		c.moveToFirst();
		int categoryID = 0;
		categoryID = c.getInt(0);
		
		return categoryID;
	}

	public void clearCategories(int id) {
		
		// clear out the table since we are refreshing the whole enchilada
		db.delete(CATEGORIES_TABLE, "blog_id=" + id, null);
		
	}

	// unique identifier queries
	public void updateUUID(String uuid) {
		
		ContentValues values = new ContentValues();
		values.put("uuid", uuid);
		synchronized (this) {
			db.update("eula", values, null, null);
		}

		
	}

	public String getUUID(Context ctx) {
		
		Cursor c = db.query("eula", new String[] { "uuid" }, "id=0", null,
				null, null, null);
		int numRows = c.getCount();
		c.moveToFirst();
		String returnValue = "";
		if (numRows == 1) {
			if (c.getString(0) != null) {
				returnValue = c.getString(0);
			}
		}
		c.close();
		

		return returnValue;

	}

	public boolean addQuickPressShortcut(String accountId,
			String name) {
		
		ContentValues values = new ContentValues();
		values.put("accountId", accountId);
		values.put("name", name);
		boolean returnValue = false;
		synchronized (this) {
			returnValue = db.insert(QUICKPRESS_SHORTCUTS_TABLE, null, values) > 0;
		}
		
		return (returnValue);
	}

	public Vector<HashMap<String, Object>> getQuickPressShortcuts(Context ctx,
			int accountId) {
		
		Cursor c = db.query(QUICKPRESS_SHORTCUTS_TABLE, new String[] { "id",
				"accountId", "name" }, "accountId = " + accountId, null, null,
				null, null);
		String id, name;
		int numRows = c.getCount();
		c.moveToFirst();
		Vector<HashMap<String, Object>> accounts = new Vector<HashMap<String, Object>>();
		for (int i = 0; i < numRows; i++) {

			id = c.getString(0);
			name = c.getString(2);
			if (id != null) {
				HashMap<String, Object> thisHash = new HashMap<String, Object>();

				thisHash.put("id", id);
				thisHash.put("name", name);
				accounts.add(thisHash);
			}
			c.moveToNext();
		}
		c.close();
		

		return accounts;
	}

	public boolean deleteQuickPressShortcut(Context ctx, String id) {
		
		int rowsAffected = db.delete(QUICKPRESS_SHORTCUTS_TABLE, "id=" + id,
				null);

		boolean returnValue = false;
		if (rowsAffected > 0) {
			returnValue = true;
		}

		

		return (returnValue);
	}

	public static String encryptPassword(String clearText) {
		try {
			DESKeySpec keySpec = new DESKeySpec(
					PASSWORD_SECRET.getBytes("UTF-8"));
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey key = keyFactory.generateSecret(keySpec);

			Cipher cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			String encrypedPwd = Base64.encodeBytes(cipher.doFinal(clearText
					.getBytes("UTF-8")));
			return encrypedPwd;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return clearText;
	}

	protected String decryptPassword(String encryptedPwd) {
		try {
			DESKeySpec keySpec = new DESKeySpec(
					PASSWORD_SECRET.getBytes("UTF-8"));
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey key = keyFactory.generateSecret(keySpec);

			byte[] encryptedWithoutB64 = Base64.decode(encryptedPwd);
			Cipher cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] plainTextPwdBytes = cipher.doFinal(encryptedWithoutB64);
			return new String(plainTextPwdBytes);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return encryptedPwd;
	}

	private void migratePasswords(Context ctx) {
		

		Cursor c = db.query(SETTINGS_TABLE, new String[] { "id", "password",
				"httppassword", "dotcom_password" }, null, null, null, null,
				null);
		int numRows = c.getCount();
		c.moveToFirst();

		for (int i = 0; i < numRows; i++) {
			ContentValues values = new ContentValues();

			if (c.getString(1) != null) {
				values.put("password", encryptPassword(c.getString(1)));
			}
			if (c.getString(2) != null) {
				values.put("httppassword", encryptPassword(c.getString(2)));
			}
			if (c.getString(3) != null) {
				values.put("dotcom_password", encryptPassword(c.getString(3)));
			}

			db.update(SETTINGS_TABLE, values, "id=" + c.getInt(0), null);

			c.moveToNext();
		}
		c.close();
	}

	public int getUnmoderatedCommentCount(int blogID) {
		int commentCount = 0;
		
		Cursor c = db
				.rawQuery(
						"select count(*) from comments where blogID=? AND status='hold'",
						new String[] { String.valueOf(blogID) });
		int numRows = c.getCount();
		c.moveToFirst();

		if (numRows > 0) {
			commentCount = c.getInt(0);
		}

		c.close();
		
		return commentCount;
	}

	public boolean saveMediaFile(MediaFile mf) {
		boolean returnValue = false;
		
		ContentValues values = new ContentValues();
		values.put("postID", mf.getPostID());
		values.put("filePath", mf.getFileName());
		values.put("fileName", mf.getFileName());
		values.put("title", mf.getTitle());
		values.put("description", mf.getDescription());
		values.put("caption", mf.getCaption());
		values.put("horizontalAlignment", mf.getHorizontalAlignment());
		values.put("width", mf.getWidth());
		values.put("height", mf.getHeight());
		values.put("mimeType", mf.getMIMEType());
		values.put("featured", mf.isFeatured());
		values.put("isVideo", mf.isVideo());
		synchronized (this) {
			int result = db.update(
					MEDIA_TABLE,
					values,
					"postID=" + mf.getPostID() + " AND filePath='"
							+ mf.getFileName() + "'", null);
			if (result == 0)
				returnValue = db.insert(MEDIA_TABLE, null, values) > 0;
		}

		
		return (returnValue);
	}

	public MediaFile[] getMediaFilesForPost(Post p) {
		
		Cursor c = db.query(MEDIA_TABLE, null, "postID=" + p.getId(), null,
				null, null, null);
		int numRows = c.getCount();
		c.moveToFirst();
		MediaFile[] mediaFiles = new MediaFile[numRows];
		for (int i = 0; i < numRows; i++) {

			MediaFile mf = new MediaFile();
			mf.setPostID(c.getInt(1));
			mf.setFilePath(c.getString(2));
			mf.setFileName(c.getString(3));
			mf.setTitle(c.getString(4));
			mf.setDescription(c.getString(5));
			mf.setCaption(c.getString(6));
			mf.setHorizontalAlignment(c.getInt(7));
			mf.setWidth(c.getInt(8));
			mf.setHeight(c.getInt(9));
			mf.setMIMEType(c.getString(10));
			mf.setFeatured(c.getInt(11) > 0);
			mf.setVideo(c.getInt(12) > 0);
			mediaFiles[i] = mf;
			c.moveToNext();
		}
		c.close();
		

		return mediaFiles;
	}

	public boolean deleteMediaFile(MediaFile mf) {
		

		boolean returnValue = false;

		int result = 0;
		result = db.delete(MEDIA_TABLE, "id=" + mf.getId(), null);
		

		if (result == 1) {
			returnValue = true;
		}

		return returnValue;
	}

	public MediaFile getMediaFile(String src, Post post) {
		
		Cursor c = db.query(MEDIA_TABLE, null, "postID=" + post.getId()
				+ " AND filePath='" + src + "'", null, null, null, null);
		int numRows = c.getCount();
		c.moveToFirst();
		MediaFile mf = new MediaFile();
		if (numRows == 1) {
			mf.setPostID(c.getInt(1));
			mf.setFilePath(c.getString(2));
			mf.setFileName(c.getString(3));
			mf.setTitle(c.getString(4));
			mf.setDescription(c.getString(5));
			mf.setCaption(c.getString(6));
			mf.setHorizontalAlignment(c.getInt(7));
			mf.setWidth(c.getInt(8));
			mf.setHeight(c.getInt(9));
			mf.setMIMEType(c.getString(10));
			mf.setFeatured(c.getInt(11) > 0);
			mf.setVideo(c.getInt(12) > 0);
		}
		c.close();
		

		return mf;
	}

	public void deleteMediaFilesForPost(Post post) {
		
		db.delete(MEDIA_TABLE, "postID=" + post.getId(), null);
		
	}

}
