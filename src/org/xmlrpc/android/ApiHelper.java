package org.xmlrpc.android;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.HttpRequest;
import org.wordpress.android.util.HttpRequest.HttpRequestException;

import android.content.Context;
import android.os.AsyncTask;
import android.text.format.DateUtils;

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
	
	/**
	 * Discover the XML-RPC endpoint for the WordPress API associated with the specified blog URL.
	 *
	 * @param urlString URL of the blog to get the XML-RPC endpoint for.
	 * @return XML-RPC endpoint for the specified blog, or null if unable to discover endpoint.
	 */
	public static String getXMLRPCUrl(String urlString) {
		Pattern xmlrpcLink = Pattern.compile("<api\\s*?name=\"WordPress\".*?apiLink=\"(.*?)\"",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

		String html = getResponse(urlString);
		if (html != null) {
			Matcher matcher = xmlrpcLink.matcher(html);
			if (matcher.find()) {
				String href = matcher.group(1);
				return href;
			}
		}
		return null; // never found the rsd tag
	}

	/**
	 * Discover the RSD homepage URL associated with the specified blog URL.
	 *
	 * @param urlString URL of the blog to get the link for.
	 * @return RSD homepage URL for the specified blog, or null if unable to discover URL.
	 */
	public static String getHomePageLink(String urlString) {
		Pattern xmlrpcLink = Pattern.compile("<homePageLink>(.*?)</homePageLink>",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

		String html = getResponse(urlString);
		if (html != null) {
			Matcher matcher = xmlrpcLink.matcher(html);
			if (matcher.find()) {
				String href = matcher.group(1);
				return href;
			}
		}
		return null; // never found the rsd tag
	}

	/**
	 * Fetch the content stream of the resource at the specified URL.
	 *
	 * @param urlString URL to fetch contents for.
	 * @return content stream, or null if URL was invalid or resource could not be retrieved.
	 */
	public static InputStream getResponseStream(String urlString) {
		HttpRequest request = getHttpRequest(urlString);
		if (request != null) {
			return request.buffer();
		} else {
			return null;
		}
	}

	/**
	 * Fetch the content of the resource at the specified URL.
	 *
	 * @param urlString URL to fetch contents for.
	 * @return content of the resource, or null if URL was invalid or resource could not be retrieved.
	 */
	public static String getResponse(String urlString) {
		HttpRequest request = getHttpRequest(urlString);
		if (request != null) {
			return request.body();
		} else {
			return null;
		}
	}

	/**
	 * Fetch the specified HTTP resource.
	 *
	 * The URL class will automatically follow up to five redirects, with the
	 * exception of redirects between HTTP and HTTPS URLs. This method manually
	 * handles one additional redirect to allow for this protocol switch.
	 *
	 * @param urlString URL to fetch.
	 * @return the request / response object or null if the resource could not be retrieved.
	 */
	public static HttpRequest getHttpRequest(String urlString) {
		try {
			HttpRequest request = HttpRequest.get(urlString);

			// manually follow one additional redirect to support protocol switching
			if (request.code() == HttpURLConnection.HTTP_MOVED_PERM
					|| request.code() == HttpURLConnection.HTTP_MOVED_TEMP) {
				String location = request.location();
				if (location != null) {
					request = HttpRequest.get(location);
				}
			}

			return request;
		} catch (HttpRequestException e) {
			e.printStackTrace();
			return null;
		}
	}
}
