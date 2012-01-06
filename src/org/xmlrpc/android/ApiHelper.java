package org.xmlrpc.android;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

public class ApiHelper extends Activity {
	/** Called when the activity is first created. */
	private static XMLRPCClient client;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
	}

	@SuppressWarnings("unchecked")
	static void refreshComments(final int id, final Context ctx) {

		Blog blog;
		try {
			blog = new Blog(id, ctx);
		} catch (Exception e1) {
			return;
		}

		client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(),
				blog.getHttppassword());

		HashMap<String, Object> hPost = new HashMap<String, Object>();
		hPost.put("status", "");
		hPost.put("post_id", "");
		hPost.put("number", 30);

		Object[] params = { blog.getBlogId(), blog.getUsername(),
				blog.getPassword(), hPost };
		Object[] result = null;
		try {
			result = (Object[]) client.call("wp.getComments", params);
		} catch (XMLRPCException e) {
		}

		if (result != null) {
			if (result.length > 0) {
				String author, postID, commentID, comment, status, authorEmail, authorURL, postTitle;

				HashMap<Object, Object> contentHash = new HashMap<Object, Object>();
				Vector<HashMap<String, String>> dbVector = new Vector<HashMap<String, String>>();

				Date d = new Date();
				// loop this!
				for (int ctr = 0; ctr < result.length; ctr++) {
					HashMap<String, String> dbValues = new HashMap<String, String>();
					contentHash = (HashMap<Object, Object>) result[ctr];
					comment = contentHash.get("content").toString();
					author = contentHash.get("author").toString();
					status = contentHash.get("status").toString();
					postID = contentHash.get("post_id").toString();
					commentID = contentHash.get("comment_id").toString();
					d = (Date) contentHash.get("date_created_gmt");
					authorURL = contentHash.get("author_url").toString();
					authorEmail = contentHash.get("author_email").toString();
					postTitle = contentHash.get("post_title").toString();

					String formattedDate = d.toString();
					try {
						int flags = 0;
						flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
						flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
						flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
						flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
						formattedDate = DateUtils.formatDateTime(ctx,
								d.getTime(), flags);
					} catch (Exception e) {
					}

					dbValues.put("blogID", String.valueOf(id));
					dbValues.put("postID", postID);
					dbValues.put("commentID", commentID);
					dbValues.put("author", author);
					dbValues.put("comment", comment);
					dbValues.put("commentDate", formattedDate);
					dbValues.put("commentDateFormatted", formattedDate);
					dbValues.put("status", status);
					dbValues.put("url", authorURL);
					dbValues.put("email", authorEmail);
					dbValues.put("postTitle", postTitle);
					dbVector.add(ctr, dbValues);
				}

				WordPress.wpDB.saveComments(dbVector);
			}
		}
	}

	public static class getPostFormatsTask extends
			AsyncTask<Vector<?>, Void, Object> {

		Context ctx;
		Blog blog;
		boolean isPage, loadMore;

		protected void onPostExecute(Object result) {
			try {
				HashMap<?, ?> postFormats = (HashMap<?, ?>) result;
				JSONObject jsonPostFormats = new JSONObject(postFormats);
				blog.setPostFormats(jsonPostFormats.toString());
				blog.save(ctx, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		protected Object doInBackground(Vector<?>... args) {

			Vector<?> arguments = args[0];
			blog = (Blog) arguments.get(0);
			ctx = (Context) arguments.get(1);
			client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(),
					blog.getHttppassword());

			Object result = null;
			Object[] params = { blog.getBlogId(), blog.getUsername(),
					blog.getPassword(), "show-supported" };
			try {
				result = (Object) client.call("wp.getPostFormats", params);
			} catch (XMLRPCException e) {
				e.printStackTrace();
			}

			return result;

		}

	}

	public static HashMap<String, HashMap<?, ?>> refreshComments(Context ctx,
			Object[] commentParams) throws XMLRPCException {
		Blog blog = WordPress.currentBlog;
		client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(),
				blog.getHttppassword());
		String author, postID, commentID, comment, status, authorEmail, authorURL, postTitle;
		HashMap<String, HashMap<?, ?>> allComments = new HashMap<String, HashMap<?, ?>>();
		HashMap<?, ?> contentHash = new HashMap<Object, Object>();
		Vector<HashMap<?, ?>> dbVector = new Vector<HashMap<?, ?>>();

		Date d = new Date();
		Object[] result;
		try {
			result = (Object[]) client.call("wp.getComments", commentParams);
		} catch (XMLRPCException e) {
			throw new XMLRPCException(e);
		}
		
		if (result.length == 0)
			return null;
		// loop this!
		for (int ctr = 0; ctr < result.length; ctr++) {
			HashMap<Object, Object> dbValues = new HashMap<Object, Object>();
			contentHash = (HashMap<?, ?>) result[ctr];
			allComments.put(contentHash.get("comment_id").toString(),
					contentHash);
			comment = contentHash.get("content").toString();
			author = contentHash.get("author").toString();
			status = contentHash.get("status").toString();
			postID = contentHash.get("post_id").toString();
			commentID = contentHash.get("comment_id").toString();
			d = (Date) contentHash.get("date_created_gmt");
			authorURL = contentHash.get("author_url").toString();
			authorEmail = contentHash.get("author_email").toString();
			postTitle = contentHash.get("post_title").toString();

			String formattedDate = d.toString();
			try {
				int flags = 0;
				flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
				flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
				flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
				flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
				formattedDate = DateUtils.formatDateTime(ctx,
						d.getTime(), flags);
			} catch (Exception e) {
			}

			dbValues.put("blogID", String.valueOf(blog.getId()));
			dbValues.put("postID", postID);
			dbValues.put("commentID", commentID);
			dbValues.put("author", author);
			dbValues.put("comment", comment);
			dbValues.put("commentDate", formattedDate);
			dbValues.put("commentDateFormatted", formattedDate);
			dbValues.put("status", status);
			dbValues.put("url", authorURL);
			dbValues.put("email", authorEmail);
			dbValues.put("postTitle", postTitle);
			dbVector.add(ctr, dbValues);
		}

		WordPress.wpDB.saveComments(dbVector);

		return allComments;

	}

}
