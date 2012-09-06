package org.xmlrpc.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;

import android.content.Context;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import android.util.Log;

public class ApiHelper {
	/** Called when the activity is first created. */
	private static XMLRPCClient client;

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
				if (postFormats.size() > 0) {
					JSONObject jsonPostFormats = new JSONObject(postFormats);
					blog.setPostFormats(jsonPostFormats.toString());
					blog.save(ctx, null);
				}
			} catch (Exception e) {
				//e.printStackTrace();
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
				//e.printStackTrace();
			}

			return result;

		}

	}

	public static HashMap<Integer, HashMap<?, ?>> refreshComments(Context ctx,
			Object[] commentParams) throws XMLRPCException {
		Blog blog = WordPress.currentBlog;
		client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(),
				blog.getHttppassword());
		String author, postID, comment, status, authorEmail, authorURL, postTitle;
		int commentID;
		HashMap<Integer, HashMap<?, ?>> allComments = new HashMap<Integer, HashMap<?, ?>>();
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
			allComments.put(Integer.parseInt(contentHash.get("comment_id").toString()),
					contentHash);
			comment = contentHash.get("content").toString();
			author = contentHash.get("author").toString();
			status = contentHash.get("status").toString();
			postID = contentHash.get("post_id").toString();
			commentID = Integer.parseInt(contentHash.get("comment_id").toString());
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
	
	public static String getXMLRPCUrl(String urlString, boolean getHomePageLink) {
		Pattern xmlrpcLink;
		if (getHomePageLink)
			xmlrpcLink = Pattern.compile("<homePageLink>(.*?)</homePageLink>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		else
			xmlrpcLink = Pattern.compile("<api\\s*?name=\"WordPress\".*?apiLink=\"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		
		InputStream in = getResponse(urlString);
		if (in != null) {
			try {
				String html = convertStreamToString(in);
				Matcher matcher = xmlrpcLink.matcher(html);
				if (matcher.find()) {
					String href = matcher.group(1);
					return href;
				}
			} catch (IOException e) {
				return null;
			}
		}
		return null; // never found the rsd tag
	}

	public static InputStream getResponse(String urlString) {
		InputStream in = null;
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			return null;
		}

		try {
			HttpGet httpRequest = new HttpGet(url.toURI());
			HttpClient httpclient = new DefaultHttpClient();

			HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);
			HttpEntity entity = response.getEntity();

			BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
			in = bufHttpEntity.getContent();
			in.close();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return in;
	}
	
	public static String convertStreamToString(InputStream is) throws IOException {
		/*
		 * To convert the InputStream to String we use the Reader.read(char[]
		 * buffer) method. We iterate until the Reader return -1 which means
		 * there's no more data to read. We use the StringWriter class to
		 * produce the string.
		 */
		int bufSize = 8 * 1024;
		if (is != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[bufSize];
			try {
				InputStreamReader ireader = new InputStreamReader(is, "UTF-8");
				Reader reader = new BufferedReader(ireader, bufSize);
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
				reader.close();
				ireader.close();
				return writer.toString();
			} catch (OutOfMemoryError ex) {
				Log.e("wp_android", "Convert Stream: (out of memory)");
				writer.close();
				writer = null;
				System.gc();
				return "";
			} finally {
				is.close();
			}
		} else {
			return "";
		}
	}

}
