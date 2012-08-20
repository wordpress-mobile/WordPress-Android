package org.wordpress.android.models;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.wordpress.android.Posts;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.EscapeUtils;
import org.wordpress.android.util.ImageHelper;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPImageSpan;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;

public class Post {

	private long id;
	private int blogID;
	private String categories;
	private String custom_fields;
	private long dateCreated;
	private long date_created_gmt;
	private String description;
	private String link;
	private boolean mt_allow_comments;
	private boolean mt_allow_pings;
	private String mt_excerpt;
	private String mt_keywords;
	private String mt_text_more;
	private String permaLink;
	private String post_status;
	private String postid;
	private String title;
	private String userid;
	private String wp_author_display_name;
	private String wp_author_id;
	private String wp_password;
	private String wp_post_format;
	private String wp_slug;
	private boolean localDraft;
	private boolean uploaded;
	private double latitude;
	private double longitude;
	private boolean isPage;
	private boolean isLocalChange;

	private String mediaPaths;
	private String quickPostType;

	private Blog blog;
	static Context context;
	private static NotificationManager nm;
	private static int notificationID;

	public Vector<String> imageUrl = new Vector<String>();
	Vector<String> selectedCategories = new Vector<String>();
	private static Notification n;

	public Post(int blog_id, long post_id, boolean isPage, Context ctx) {
		// load an existing post
		context = ctx;
		Vector<Object> postVals = WordPress.wpDB.loadPost(blog_id, isPage,
				post_id);
		if (postVals != null) {
			try {
				this.blog = new Blog(blog_id, ctx);
			} catch (Exception e) {
			}
			this.id = (Long) postVals.get(0);
			this.blogID = blog_id;
			if (postVals.get(2) != null)
				this.postid = postVals.get(2).toString();
			this.title = postVals.get(3).toString();
			this.dateCreated = (Long) postVals.get(4);
			this.date_created_gmt = (Long) postVals.get(5);
			this.categories = postVals.get(6).toString();
			this.custom_fields = postVals.get(7).toString();
			this.description = postVals.get(8).toString();
			this.link = postVals.get(9).toString();
			this.mt_allow_comments = (Integer) postVals.get(10) > 0;
			this.mt_allow_pings = (Integer) postVals.get(11) > 0;
			this.mt_excerpt = postVals.get(12).toString();
			this.mt_keywords = postVals.get(13).toString();
			if (postVals.get(14) != null)
				this.mt_text_more = postVals.get(14).toString();
			else
				this.mt_text_more = "";
			this.permaLink = postVals.get(15).toString();
			this.post_status = postVals.get(16).toString();
			this.userid = postVals.get(17).toString();
			this.wp_author_display_name = postVals.get(18).toString();
			this.wp_author_id = postVals.get(19).toString();
			this.wp_password = postVals.get(20).toString();
			this.wp_post_format = postVals.get(21).toString();
			this.wp_slug = postVals.get(22).toString();
			this.mediaPaths = postVals.get(23).toString();
			this.latitude = (Double) postVals.get(24);
			this.longitude = (Double) postVals.get(25);
			this.localDraft = (Integer) postVals.get(26) > 0;
			this.uploaded = (Integer) postVals.get(27) > 0;
			this.isPage = (Integer) postVals.get(28) > 0;
			this.isLocalChange = (Integer) postVals.get(29) > 0;
		}
	}

	public Post(int blog_id, String title, String content, String picturePaths,
			long date, String categories, String tags, String status,
			String password, double latitude, double longitude, boolean isPage,
			String postFormat, Context ctx, boolean createBlogReference,
			boolean isLocalChange) {
		// create a new post
		context = ctx;
		if (createBlogReference) {
			try {
				this.blog = new Blog(blog_id, ctx);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		this.blogID = blog_id;
		this.title = title;
		this.description = content;
		this.mediaPaths = picturePaths;
		this.date_created_gmt = date;
		this.categories = categories;
		this.mt_keywords = tags;
		this.post_status = status;
		this.wp_password = password;
		this.isPage = isPage;
		this.wp_post_format = postFormat;
		this.latitude = latitude;
		this.longitude = longitude;
		this.isLocalChange = isLocalChange;
	}

	public long getId() {
		return id;
	}

	public long getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(long dateCreated) {
		this.dateCreated = dateCreated;
	}

	public long getDate_created_gmt() {
		return date_created_gmt;
	}

	public void setDate_created_gmt(long dateCreatedGmt) {
		date_created_gmt = dateCreatedGmt;
	}

	public int getBlogID() {
		return blogID;
	}

	public void setBlogID(int blogID) {
		this.blogID = blogID;
	}

	public boolean isLocalDraft() {
		return localDraft;
	}

	public void setLocalDraft(boolean localDraft) {
		this.localDraft = localDraft;
	}

	public JSONArray getCategories() {
		JSONArray jArray = null;
		try {
			jArray = new JSONArray(categories);
		} catch (JSONException e) {
		}
		return jArray;
	}

	public void setCategories(JSONArray categories) {
		this.categories = categories.toString();
	}

	public JSONArray getCustom_fields() {
		JSONArray jArray = null;
		try {
			jArray = new JSONArray(custom_fields);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jArray;
	}

	public void setCustom_fields(JSONArray customFields) {
		custom_fields = customFields.toString();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public boolean isMt_allow_comments() {
		return mt_allow_comments;
	}

	public void setMt_allow_comments(boolean mtAllowComments) {
		mt_allow_comments = mtAllowComments;
	}

	public boolean isMt_allow_pings() {
		return mt_allow_pings;
	}

	public void setMt_allow_pings(boolean mtAllowPings) {
		mt_allow_pings = mtAllowPings;
	}

	public String getMt_excerpt() {
		return mt_excerpt;
	}

	public void setMt_excerpt(String mtExcerpt) {
		mt_excerpt = mtExcerpt;
	}

	public String getMt_keywords() {
		return mt_keywords;
	}

	public void setMt_keywords(String mtKeywords) {
		mt_keywords = mtKeywords;
	}

	public String getMt_text_more() {
		return mt_text_more;
	}

	public void setMt_text_more(String mtTextMore) {
		mt_text_more = mtTextMore;
	}

	public String getPermaLink() {
		return permaLink;
	}

	public void setPermaLink(String permaLink) {
		this.permaLink = permaLink;
	}

	public String getPost_status() {
		return post_status;
	}

	public void setPost_status(String postStatus) {
		post_status = postStatus;
	}

	public String getPostid() {
		return postid;
	}

	public void setPostid(String postid) {
		this.postid = postid;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getWP_author_display_name() {
		return wp_author_display_name;
	}

	public void setWP_author_display_name(String wpAuthorDisplayName) {
		wp_author_display_name = wpAuthorDisplayName;
	}

	public String getWP_author_id() {
		return wp_author_id;
	}

	public void setWP_author_id(String wpAuthorId) {
		wp_author_id = wpAuthorId;
	}

	public String getWP_password() {
		return wp_password;
	}

	public void setWP_password(String wpPassword) {
		wp_password = wpPassword;
	}

	public String getWP_post_format() {
		return wp_post_format;
	}

	public void setWP_post_form(String wpPostForm) {
		wp_post_format = wpPostForm;
	}

	public String getWP_slug() {
		return wp_slug;
	}

	public void setWP_slug(String wpSlug) {
		wp_slug = wpSlug;
	}

	public String getMediaPaths() {
		return mediaPaths;
	}

	public void setMediaPaths(String mediaPaths) {
		this.mediaPaths = mediaPaths;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public boolean isPage() {
		return isPage;
	}

	public void setPage(boolean isPage) {
		this.isPage = isPage;
	}

	public boolean isUploaded() {
		return uploaded;
	}

	public void setUploaded(boolean uploaded) {
		this.uploaded = uploaded;
	}

	public void upload() {

		new uploadPostTask().execute(this);

	}

	public boolean isLocalChange() {
		return isLocalChange;
	}

	public void setLocalChange(boolean isLocalChange) {
		this.isLocalChange = isLocalChange;
	}

	public boolean save() {
		long newPostID = WordPress.wpDB.savePost(this, this.blogID);

		if (newPostID >= 0 && this.isLocalDraft() && !this.isUploaded()) {
			this.id = newPostID;
			return true;
		}

		return false;
	}

	public boolean update() {
		int success = WordPress.wpDB.updatePost(this, this.blogID);

		return success > 0;
	}

	public void delete() {
		// deletes a post/page draft
		WordPress.wpDB.deletePost(this);
	}

	public static class uploadPostTask extends
			AsyncTask<Post, Boolean, Boolean> {

		private Post post;
		String error = "";
		boolean mediaError = false;

		@Override
		protected void onPostExecute(Boolean result) {

			if (result && !mediaError) {
				WordPress.postUploaded();
				nm.cancel(notificationID);
			} else {
				if (mediaError)
					WordPress.postUploaded();

				String postOrPage = (String) (post.isPage() ? context
						.getResources().getText(R.string.page_id) : context
						.getResources().getText(R.string.post_id));
				Intent notificationIntent = new Intent(context, Posts.class);
				notificationIntent.setData((Uri
						.parse("custom://wordpressNotificationIntent"
								+ post.blogID)));
				notificationIntent.putExtra("fromNotification", true);
				notificationIntent.putExtra("errorMessage", error);
				PendingIntent pendingIntent = PendingIntent.getActivity(
						context, 0, notificationIntent,
						PendingIntent.FLAG_UPDATE_CURRENT);
				n.flags |= Notification.FLAG_AUTO_CANCEL;
				String errorText = context.getResources()
						.getText(R.string.upload_failed).toString();
				if (mediaError)
					errorText = context.getResources().getText(R.string.media)
							+ " "
							+ context.getResources().getText(R.string.error);
				n.setLatestEventInfo(context, (mediaError) ? errorText
						: context.getResources()
								.getText(R.string.upload_failed), postOrPage
						+ " " + errorText + ": " + error, pendingIntent);

				nm.notify(notificationID, n); // needs a unique id
			}
		}

		@Override
		protected Boolean doInBackground(Post... posts) {

			post = posts[0];

			// add the uploader to the notification bar
			nm = (NotificationManager) context.getSystemService("notification");

			String postOrPage = (String) (post.isPage() ? context
					.getResources().getText(R.string.page_id) : context
					.getResources().getText(R.string.post_id));
			String message = context.getResources().getText(R.string.uploading)
					+ " " + postOrPage;
			n = new Notification(R.drawable.notification_icon, message,
					System.currentTimeMillis());

			Intent notificationIntent = new Intent(context, Posts.class);
			notificationIntent
					.setData((Uri.parse("custom://wordpressNotificationIntent"
							+ post.blogID)));
			notificationIntent.putExtra("fromNotification", true);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
					notificationIntent, Intent.FLAG_ACTIVITY_CLEAR_TOP);

			n.setLatestEventInfo(context, message, message, pendingIntent);

			notificationID = 22 + Integer.valueOf(post.blogID);
			nm.notify(notificationID, n); // needs a unique id

			if (post.post_status == null) {
				post.post_status = "publish";
			}
			Boolean publishThis = false;

			Spannable s;
			String descriptionContent = "", moreContent = "";
			int moreCount = 1;
			if (post.getMt_text_more() != null)
				moreCount++;
			String imgTags = "<img[^>]+android-uri\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>";
			Pattern pattern = Pattern.compile(imgTags);

			for (int x = 0; x < moreCount; x++) {
				if (post.isLocalDraft()) {
					if (x == 0)
						s = (Spannable) WPHtml.fromHtml(post.getDescription(),
								context, post);
					else
						s = (Spannable) WPHtml.fromHtml(post.getMt_text_more(),
								context, post);
					WPImageSpan[] click_spans = s.getSpans(0, s.length(),
							WPImageSpan.class);

					if (click_spans.length != 0) {

						for (int i = 0; i < click_spans.length; i++) {
							String prompt = context.getResources().getText(
									R.string.uploading_media_item)
									+ String.valueOf(i + 1);
							n.setLatestEventInfo(context, context
									.getResources().getText(R.string.uploading)
									+ " " + postOrPage, prompt, n.contentIntent);
							nm.notify(notificationID, n);
							WPImageSpan wpIS = click_spans[i];
							int start = s.getSpanStart(wpIS);
							int end = s.getSpanEnd(wpIS);
							MediaFile mf = new MediaFile();
							mf.setPostID(post.getId());
							mf.setTitle(wpIS.getTitle());
							mf.setCaption(wpIS.getCaption());
							mf.setDescription(wpIS.getDescription());
							mf.setFeatured(wpIS.isFeatured());
							mf.setFileName(wpIS.getImageSource().toString());
							mf.setHorizontalAlignment(wpIS
									.getHorizontalAlignment());
							mf.setWidth(wpIS.getWidth());

							String imgHTML = uploadImage(mf);
							if (imgHTML != null) {
								SpannableString ss = new SpannableString(
										imgHTML);
								s.setSpan(ss, start, end,
										Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
								s.removeSpan(wpIS);
							} else {
								s.removeSpan(wpIS);
								mediaError = true;
							}
						}
					}

					if (x == 0)
						descriptionContent = WPHtml.toHtml(s);
					else
						moreContent = WPHtml.toHtml(s);
				} else {
					Matcher matcher;
					if (x == 0) {
						descriptionContent = post.getDescription();
						matcher = pattern.matcher(descriptionContent);
					} else {
						moreContent = post.getMt_text_more();
						matcher = pattern.matcher(moreContent);
					}

					List<String> imageTags = new ArrayList<String>();
					while (matcher.find()) {
						imageTags.add(matcher.group());
					}

					for (String tag : imageTags) {

						Pattern p = Pattern.compile("android-uri=\"([^\"]+)\"");
						Matcher m = p.matcher(tag);
						String imgPath = "";
						if (m.find()) {
							imgPath = m.group(1);
							if (!imgPath.equals("")) {
								MediaFile mf = WordPress.wpDB.getMediaFile(
										imgPath, post);

								if (mf != null) {
									String imgHTML = uploadImage(mf);
									if (imgHTML != null) {
										if (x == 0) {
											descriptionContent = descriptionContent
													.replace(tag, imgHTML);
										} else {
											moreContent = moreContent.replace(
													tag, imgHTML);
										}
									} else {
										if (x == 0)
											descriptionContent = descriptionContent
													.replace(tag, "");
										else
											moreContent = moreContent.replace(
													tag, "");
										mediaError = true;
									}
								}
							}
						}
					}
				}
			}

			JSONArray categories = post.getCategories();
			String[] theCategories = null;
			if (categories != null) {
				theCategories = new String[categories.length()];
				for (int i = 0; i < categories.length(); i++) {
					try {
						theCategories[i] = categories.getString(i);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}

			Map<String, Object> contentStruct = new HashMap<String, Object>();

			if (!post.isPage && post.isLocalDraft()) {
				// add the tagline
				HashMap<?, ?> globalSettings = WordPress.wpDB
						.getNotificationOptions(context);
				boolean taglineValue = false;
				String tagline = "";

				if (globalSettings != null) {
					if (globalSettings.get("tagline_flag") != null) {
						if (globalSettings.get("tagline_flag").toString().equals("1"))
							taglineValue = true;
					}

					if (taglineValue) {
						tagline = globalSettings.get("tagline").toString();
						if (tagline != null) {
							String tag = "\n\n<span class=\"post_sig\">"
									+ tagline + "</span>\n\n";
							if (moreContent == "")
								descriptionContent += tag;
							else
								moreContent += tag;
						}
					}
				}

				// post format
				if (!post.getWP_post_format().equals("")) {
					if (!post.getWP_post_format().equals("standard"))
						contentStruct.put("wp_post_format",
								post.getWP_post_format());
				}
			}

			contentStruct.put("post_type", (post.isPage) ? "page" : "post");
			contentStruct.put("title", post.title);
			long pubDate = post.date_created_gmt;
			if (pubDate != 0) {
				Date date_created_gmt = new Date(pubDate);
				contentStruct.put("date_created_gmt", date_created_gmt);
				Date dateCreated = new Date(pubDate
						+ (date_created_gmt.getTimezoneOffset() * 60000));
				contentStruct.put("dateCreated", dateCreated);
			}

			if (!moreContent.equals("")) {
				descriptionContent = descriptionContent + "\n\n<!--more-->\n\n"
						+ moreContent;
				post.mt_text_more = "";
			}

			// get rid of the p and br tags that the editor adds.
			if (post.isLocalDraft()) {
				descriptionContent = descriptionContent.replace("<p>", "")
						.replace("</p>", "\n").replace("<br>", "");
			}

			// gets rid of the weird character android inserts after images
			descriptionContent = descriptionContent.replaceAll("\uFFFC", "");

			contentStruct.put("description", descriptionContent);
			if (!post.isPage) {
				if (post.mt_keywords != "") {
					contentStruct.put("mt_keywords", post.mt_keywords);
				}
				if (theCategories != null) {
					if (theCategories.length > 0)
						contentStruct.put("categories", theCategories);
				}
			}

			if (post.getMt_excerpt() != null)
				contentStruct.put("mt_excerpt", post.getMt_excerpt());

			contentStruct.put((post.isPage) ? "page_status" : "post_status",
					post.post_status);
			Double latitude = 0.0;
			Double longitude = 0.0;
			if (!post.isPage) {
				latitude = (Double) post.getLatitude();
				longitude = (Double) post.getLongitude();

				if (latitude > 0) {
					HashMap<Object, Object> hLatitude = new HashMap<Object, Object>();
					hLatitude.put("key", "geo_latitude");
					hLatitude.put("value", latitude);

					HashMap<Object, Object> hLongitude = new HashMap<Object, Object>();
					hLongitude.put("key", "geo_longitude");
					hLongitude.put("value", longitude);

					HashMap<Object, Object> hPublic = new HashMap<Object, Object>();
					hPublic.put("key", "geo_public");
					hPublic.put("value", 1);

					Object[] geo = { hLatitude, hLongitude, hPublic };

					contentStruct.put("custom_fields", geo);
				}
			}

			XMLRPCClient client = new XMLRPCClient(post.blog.getUrl(),
					post.blog.getHttpuser(), post.blog.getHttppassword());

			if (post.quickPostType != null)
				client.addQuickPostHeader(post.quickPostType);

			/*
			 * client.setUploadProgressListener(new
			 * XMLRPCClient.UploadProgressListener() { // user selected new blog
			 * in the title bar
			 * 
			 * @Override public void OnUploadProgress(int progress) {
			 * 
			 * n.contentView.setProgressBar(R.id.status_progress, 100, progress,
			 * false); // inform the progress bar of updates in progress
			 * nm.notify(notificationID, n);
			 * 
			 * } });
			 */
			n.setLatestEventInfo(context, message, message, n.contentIntent);
			nm.notify(notificationID, n);
			if (post.wp_password != null) {
				contentStruct.put("wp_password", post.wp_password);
			}
			Object[] params;

			if (post.isLocalDraft() && !post.uploaded)
				params = new Object[] { post.blog.getBlogId(),
						post.blog.getUsername(), post.blog.getPassword(),
						contentStruct, publishThis };
			else
				params = new Object[] { post.getPostid(),
						post.blog.getUsername(), post.blog.getPassword(),
						contentStruct, publishThis };

			try {
				client.call(
						(post.isLocalDraft() && !post.uploaded) ? "metaWeblog.newPost"
								: "metaWeblog.editPost", params);
				post.setUploaded(true);
				post.setLocalChange(false);
				post.update();
				return true;
			} catch (final XMLRPCException e) {
				error = String.format(context.getResources().getText(R.string.error_upload).toString(), post.isPage() ? context.getResources().getText(R.string.page).toString() : context.getResources().getText(R.string.post).toString());
				mediaError = false;
				Log.i("WP", error);
			}

			return false;
		}

		public String uploadImage(MediaFile mf) {
			String content = "";

			// image variables
			String finalThumbnailUrl = null;
			String finalImageUrl = null;

			// check for image, and upload it
			if (mf.getFileName() != null) {
				XMLRPCClient client = new XMLRPCClient(post.blog.getUrl(),
						post.blog.getHttpuser(), post.blog.getHttppassword());

				/*
				 * client.setUploadProgressListener(new
				 * XMLRPCClient.UploadProgressListener() {
				 * 
				 * @Override public void OnUploadProgress(int progress) {
				 * 
				 * n.contentView.setProgressBar(R.id.status_progress, 100,
				 * progress, false);
				 * n.contentView.setTextViewText(R.id.status_text, statusText +
				 * " (" + progress + "%)"); // inform the progress bar of
				 * updates in progress nm.notify(notificationID, n);
				 * 
				 * } });
				 */

				String curImagePath = "";

				curImagePath = mf.getFileName();
				boolean video = false;
				if (curImagePath.contains("video")) {
					video = true;
				}

				if (video) { // upload the video
					
					//create temp file for media upload
					String tempFileName = "wp-" + System.currentTimeMillis();
					try {
						context.openFileOutput(tempFileName, Context.MODE_PRIVATE);
					} catch (FileNotFoundException e) {
						error = "Could not create temp file for media upload.";
						return null;
					}
					
					File tempFile = context.getFileStreamPath(tempFileName);

					Uri videoUri = Uri.parse(curImagePath);
					File fVideo = null;
					String mimeType = "", xRes = "", yRes = "";

					if (videoUri.toString().contains("content:")) {
						String[] projection;
						Uri imgPath;

						projection = new String[] { Video.Media._ID,
								Video.Media.DATA, Video.Media.MIME_TYPE,
								Video.Media.RESOLUTION };
						imgPath = videoUri;

						Cursor cur = context.getContentResolver().query(
								imgPath, projection, null, null, null);
						String thumbData = "";

						if (cur.moveToFirst()) {

							int mimeTypeColumn, resolutionColumn, dataColumn;

							dataColumn = cur.getColumnIndex(Video.Media.DATA);
							mimeTypeColumn = cur
									.getColumnIndex(Video.Media.MIME_TYPE);
							resolutionColumn = cur
									.getColumnIndex(Video.Media.RESOLUTION);

							mf = new MediaFile();

							thumbData = cur.getString(dataColumn);
							mimeType = cur.getString(mimeTypeColumn);
							fVideo = new File(thumbData);
							mf.setFilePath(fVideo.getPath());
							String resolution = cur.getString(resolutionColumn);
							if (resolution != null) {
								String[] resx = resolution.split("x");
								xRes = resx[0];
								yRes = resx[1];
							} else {
								// set the width of the video to the
								// thumbnail
								// width, else 640x480
								if (!post.blog.getMaxImageWidth().equals(
										"Original Size")) {
									xRes = post.blog.getMaxImageWidth();
									yRes = String
											.valueOf(Math.round(Integer.valueOf(post.blog
													.getMaxImageWidth()) * 0.75));
								} else {
									xRes = "640";
									yRes = "480";
								}

							}

						}
					} else { // file is not in media library
						fVideo = new File(videoUri.toString().replace(
								"file://", ""));
					}

					String imageTitle = fVideo.getName();

					// try to upload the video
					HashMap<String, Object> m = new HashMap<String, Object>();

					m.put("name", imageTitle);
					m.put("type", mimeType);
					m.put("bits", mf);
					m.put("overwrite", true);

					Object[] params = { 1, post.blog.getUsername(),
							post.blog.getPassword(), m };

					Object result = null;
					
					try {
						result = (Object) client.callUploadFile("wp.uploadFile", params, tempFile);
					} catch (XMLRPCException e) {
						String mediaErrorMsg = e.getLocalizedMessage();
						if (video) {
							if (mediaErrorMsg.contains("Invalid file type")) {
								mediaErrorMsg = context.getResources()
										.getString(R.string.vp_upgrade);
							}
						}
						error = context.getResources().getText(R.string.error_media_upload).toString();
						return null;
					}

					HashMap<?, ?> contentHash = new HashMap<Object, Object>();

					contentHash = (HashMap<?, ?>) result;

					String resultURL = contentHash.get("url").toString();
					if (contentHash.containsKey("videopress_shortcode")) {
						resultURL = contentHash.get("videopress_shortcode")
								.toString() + "\n";
					} else {
						resultURL = String
								.format("<video width=\"%s\" height=\"%s\" controls=\"controls\"><source src=\"%s\" type=\"%s\" /><a href=\"%s\">Click to view video</a>.</video>",
										xRes, yRes, resultURL, mimeType,
										resultURL);
					}

					content = content + resultURL;

				} // end video
				else {
					for (int i = 0; i < 2; i++) {
						
						//create temp file for media upload
						String tempFileName = "wp-" + System.currentTimeMillis();
						try {
							context.openFileOutput(tempFileName, Context.MODE_PRIVATE);
						} catch (FileNotFoundException e) {
							error = "Could not create temp file for media upload.";
							return null;
						}
						
						File tempFile = context.getFileStreamPath(tempFileName);

						curImagePath = mf.getFileName();

						if (i == 0
								|| (post.blog.isFullSizeImage() || post.blog
										.isScaledImage())) {

							Uri imageUri = Uri.parse(curImagePath);
							File jpeg = null;
							String mimeType = "", orientation = "", path = "";

							if (imageUri.toString().contains("content:")) {
								String[] projection;
								Uri imgPath;

								projection = new String[] { Images.Media._ID,
										Images.Media.DATA,
										Images.Media.MIME_TYPE,
										Images.Media.ORIENTATION };

								imgPath = imageUri;

								Cursor cur = context.getContentResolver()
										.query(imgPath, projection, null, null,
												null);
								String thumbData = "";

								if (cur.moveToFirst()) {

									int dataColumn, mimeTypeColumn, orientationColumn;

									dataColumn = cur
											.getColumnIndex(Images.Media.DATA);
									mimeTypeColumn = cur
											.getColumnIndex(Images.Media.MIME_TYPE);
									orientationColumn = cur
											.getColumnIndex(Images.Media.ORIENTATION);

									orientation = cur
											.getString(orientationColumn);
									thumbData = cur.getString(dataColumn);
									mimeType = cur.getString(mimeTypeColumn);
									jpeg = new File(thumbData);
									path = thumbData;
									mf.setFilePath(jpeg.getPath());

								}
							} else { // file is not in media library
								path = imageUri.toString().replace("file://",
										"");
								jpeg = new File(path);
								mf.setFilePath(path);
							}

							// check if the file is now gone! (removed SD
							// card, etc)
							if (jpeg == null) {
								break;
							}

							ImageHelper ih = new ImageHelper();
							orientation = ih.getExifOrientation(path,
									orientation);

							String imageTitle = jpeg.getName();

							byte[] finalBytes = null;

							if (i == 0 || post.blog.isScaledImage()) {
								byte[] bytes = new byte[(int) jpeg.length()];

								DataInputStream in = null;
								try {
									in = new DataInputStream(
											new FileInputStream(jpeg));
								} catch (FileNotFoundException e) {
									e.printStackTrace();
								}
								try {
									in.readFully(bytes);
								} catch (IOException e) {
									e.printStackTrace();
								}
								try {
									in.close();
								} catch (IOException e) {
									e.printStackTrace();
								}

								String width = String.valueOf(i == 0 ? mf
										.getWidth() : post.blog
										.getScaledImageWidth());
								if (post.blog.getMaxImageWidth().equals(
										"Original Size") && i == 0)
									width = "Original Size";
									
								
								ImageHelper ih2 = new ImageHelper();
								finalBytes = ih2
										.createThumbnail(
												bytes,
												width,
												orientation, false);

								if (finalBytes == null) {
									error = context.getResources()
											.getText(R.string.media_error)
											.toString();
									return null;
								}
							}

							// try to upload the image
							Map<String, Object> m = new HashMap<String, Object>();

							m.put("name", imageTitle);
							m.put("type", mimeType);
							if (i == 0 || post.blog.isScaledImage()) {
								m.put("bits", finalBytes);
							} else {
								m.put("bits", mf);
							}
							m.put("overwrite", true);

							Object[] params = { 1, post.blog.getUsername(),
									post.blog.getPassword(), m };

							Object result = null;

							try {
								result = (Object) client.callUploadFile("wp.uploadFile",
										params, tempFile);
							} catch (XMLRPCException e) {
								error = e.getMessage();
								return null;
							}

							HashMap<?, ?> contentHash = new HashMap<Object, Object>();

							contentHash = (HashMap<?, ?>) result;

							String resultURL = contentHash.get("url")
									.toString();

							if (i == 0) {
								finalThumbnailUrl = resultURL;
							} else {
								if (post.blog.isFullSizeImage()
										|| post.blog.isScaledImage()) {
									finalImageUrl = resultURL;
								} else {
									finalImageUrl = "";
								}
							}

							String alignment = "";
							switch (mf.getHorizontalAlignment()) {
							case 0:
								alignment = "alignnone";
								break;
							case 1:
								alignment = "alignleft";
								break;
							case 2:
								alignment = "aligncenter";
								break;
							case 3:
								alignment = "alignright";
								break;
							}

							String alignmentCSS = "class=\"" + alignment
									+ "\" ";
							if (resultURL != null) {
								if (i != 0
										&& (post.blog.isFullSizeImage() || post.blog
												.isScaledImage())) {
									content = content
											+ "<a href=\""
											+ finalImageUrl
											+ "\"><img title=\""
											+ mf.getTitle() + "\" "
											+ alignmentCSS
											+ "alt=\"image\" src=\""
											+ finalThumbnailUrl + "\" /></a>";
								} else {
									if (i == 0
											&& (post.blog.isFullSizeImage() == false && !post.blog
													.isScaledImage())) {
										content = content + "<img title=\""
												+ mf.getTitle() + "\" "
												+ alignmentCSS
												+ "alt=\"image\" src=\""
												+ finalThumbnailUrl + "\" />";
									}
								}

								if ((i == 0 && !post.blog.isFullSizeImage() && !post.blog
										.isScaledImage()) || i == 1) {
									if (!mf.getCaption().equals("")) {
										content = String
												.format("[caption id=\"\" align=\"%s\" width=\"%d\" caption=\"%s\"]%s[/caption]",
														alignment,
														mf.getWidth(),
														EscapeUtils.escapeHtml(mf
																.getCaption()),
														content);
									}
								}
							}

						} // end if statement
					}// end image check
				}
			}// end image stuff
			return content;
		}
	}

	public void deleteMediaFiles() {
		WordPress.wpDB.deleteMediaFilesForPost(this);
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setQuickPostType(String type) {
		this.quickPostType = type;
	}
}
