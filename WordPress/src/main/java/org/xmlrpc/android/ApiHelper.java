package org.xmlrpc.android;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Xml;
import android.webkit.URLUtil;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.RedirectError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.BlogIdentifier;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.models.FeatureSet;
import org.wordpress.android.networking.WPDelayedHurlStack;
import org.wordpress.android.ui.media.MediaGridFragment.Filter;
import org.wordpress.android.ui.stats.StatsUtils;
import org.wordpress.android.ui.stats.StatsWidgetProvider;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

public class ApiHelper {

    public static final class Methods {
        public static final String GET_MEDIA_LIBRARY  = "wp.getMediaLibrary";
        public static final String GET_POST_FORMATS   = "wp.getPostFormats";
        public static final String GET_CATEGORIES     = "wp.getCategories";
        public static final String GET_MEDIA_ITEM     = "wp.getMediaItem";
        public static final String GET_COMMENTS       = "wp.getComments";
        public static final String GET_BLOGS          = "wp.getUsersBlogs";
        public static final String GET_OPTIONS        = "wp.getOptions";
        public static final String GET_PROFILE        = "wp.getProfile";
        public static final String GET_PAGES          = "wp.getPages";
        public static final String GET_TERM           = "wp.getTerm";
        public static final String GET_PAGE           = "wp.getPage";

        public static final String DELETE_COMMENT     = "wp.deleteComment";
        public static final String DELETE_PAGE        = "wp.deletePage";
        public static final String DELETE_POST        = "wp.deletePost";

        public static final String NEW_CATEGORY       = "wp.newCategory";
        public static final String NEW_COMMENT        = "wp.newComment";

        public static final String EDIT_POST          = "wp.editPost";
        public static final String EDIT_COMMENT       = "wp.editComment";

        public static final String UPLOAD_FILE        = "wp.uploadFile";

        public static final String WPCOM_GET_FEATURES = "wpcom.getFeatures";

        public static final String LIST_METHODS       = "system.listMethods";
    }

    public enum ErrorType {
        NO_ERROR, UNKNOWN_ERROR, INVALID_CURRENT_BLOG, NETWORK_XMLRPC, INVALID_CONTEXT,
        INVALID_RESULT, NO_UPLOAD_FILES_CAP, CAST_EXCEPTION, TASK_CANCELLED, UNAUTHORIZED
    }

    public static final Map<String, String> blogOptionsXMLRPCParameters = new HashMap<String, String>();

    static {
        blogOptionsXMLRPCParameters.put("software_version", "software_version");
        blogOptionsXMLRPCParameters.put("post_thumbnail", "post_thumbnail");
        blogOptionsXMLRPCParameters.put("jetpack_client_id", "jetpack_client_id");
        blogOptionsXMLRPCParameters.put("blog_public", "blog_public");
        blogOptionsXMLRPCParameters.put("home_url", "home_url");
        blogOptionsXMLRPCParameters.put("admin_url", "admin_url");
        blogOptionsXMLRPCParameters.put("login_url", "login_url");
        blogOptionsXMLRPCParameters.put("blog_title", "blog_title");
        blogOptionsXMLRPCParameters.put("time_zone", "time_zone");
    }

    public static abstract class HelperAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
        protected String mErrorMessage;
        protected ErrorType mErrorType = ErrorType.NO_ERROR;
        protected Throwable mThrowable;

        protected void setError(ErrorType errorType, String errorMessage) {
            mErrorMessage = errorMessage;
            mErrorType = errorType;
            AppLog.e(T.API, mErrorType.name() + " - " + mErrorMessage);
        }

        protected void setError(ErrorType errorType, String errorMessage, Throwable throwable) {
            mErrorMessage = errorMessage;
            mErrorType = errorType;
            mThrowable = throwable;
            AppLog.e(T.API, mErrorType.name() + " - " + mErrorMessage, throwable);
        }
    }

    public interface GenericErrorCallback {
        public void onFailure(ErrorType errorType, String errorMessage, Throwable throwable);
    }

    public interface GenericCallback extends GenericErrorCallback {
        public void onSuccess();
    }

    public static class GetPostFormatsTask extends HelperAsyncTask<Blog, Void, Object> {
        private Blog mBlog;

        @Override
        protected Object doInBackground(Blog... blog) {
            mBlog = blog[0];
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(mBlog.getUri(), mBlog.getHttpuser(),
                    mBlog.getHttppassword());
            Object result = null;
            Object[] params = { mBlog.getRemoteBlogId(), mBlog.getUsername(),
                    mBlog.getPassword(), "show-supported" };
            try {
                result = client.call(ApiHelper.Methods.GET_POST_FORMATS, params);
            } catch (ClassCastException cce) {
                setError(ErrorType.INVALID_RESULT, cce.getMessage(), cce);
            } catch (XMLRPCException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            } catch (IOException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            } catch (XmlPullParserException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            }
            return result;
        }

        protected void onPostExecute(Object result) {
            if (result != null && result instanceof HashMap) {
                Map<?, ?> postFormats = (HashMap<?, ?>) result;
                if (postFormats.size() > 0) {
                    Gson gson = new Gson();
                    String postFormatsJson = gson.toJson(postFormats);
                    if (postFormatsJson != null) {
                        if (mBlog.bsetPostFormats(postFormatsJson)) {
                            WordPress.wpDB.saveBlog(mBlog);
                        }
                    }
                }
            }
        }
    }

    public static synchronized void updateBlogOptions(Blog currentBlog, Map<?, ?> blogOptions) {
        boolean isModified = false;
        Gson gson = new Gson();
        String blogOptionsJson = gson.toJson(blogOptions);
        if (blogOptionsJson != null) {
            isModified |= currentBlog.bsetBlogOptions(blogOptionsJson);
        }

        // Software version
        if (!currentBlog.isDotcomFlag()) {
            Map<?, ?> sv = (HashMap<?, ?>) blogOptions.get("software_version");
            String wpVersion = MapUtils.getMapStr(sv, "value");
            if (wpVersion.length() > 0) {
                isModified |= currentBlog.bsetWpVersion(wpVersion);
            }
        }

        // Featured image support
        Map<?, ?> featuredImageHash = (HashMap<?, ?>) blogOptions.get("post_thumbnail");
        if (featuredImageHash != null) {
            boolean featuredImageCapable = MapUtils.getMapBool(featuredImageHash, "value");
            isModified |= currentBlog.bsetFeaturedImageCapable(featuredImageCapable);
        } else {
            isModified |= currentBlog.bsetFeaturedImageCapable(false);
        }

        // Blog name
        Map<?, ?> blogNameHash = (HashMap<?, ?>) blogOptions.get("blog_title");
        if (blogNameHash != null) {
            String blogName = MapUtils.getMapStr(blogNameHash, "value");
            if (blogName != null && !blogName.equals(currentBlog.getBlogName())) {
                currentBlog.setBlogName(blogName);
                isModified = true;
            }
        }

        if (isModified) {
            WordPress.wpDB.saveBlog(currentBlog);
        }
    }

    /**
     * Task to refresh blog level information (WP version number) and stuff
     * related to the active theme (available post types, recent comments, etc).
     */
    public static class RefreshBlogContentTask extends HelperAsyncTask<Boolean, Void, Boolean> {
        private static HashSet<BlogIdentifier> refreshedBlogs = new HashSet<BlogIdentifier>();
        private Blog mBlog;
        private BlogIdentifier mBlogIdentifier;
        private GenericCallback mCallback;

        public RefreshBlogContentTask(Blog blog, GenericCallback callback) {
            if (blog == null) {
                cancel(true);
                return;
            }

            mBlogIdentifier = new BlogIdentifier(blog.getUrl(), blog.getRemoteBlogId());
            if (refreshedBlogs.contains(mBlogIdentifier)) {
                cancel(true);
            } else {
                refreshedBlogs.add(mBlogIdentifier);
            }

            mBlog = blog;
            mCallback = callback;
        }

        private void updateBlogAdmin(Map<String, Object> userInfos) {
            if (userInfos.containsKey("roles") && ( userInfos.get("roles") instanceof Object[])) {
                boolean isAdmin = false;
                Object[] userRoles = (Object[])userInfos.get("roles");
                for (int i = 0; i < userRoles.length; i++) {
                    if (userRoles[i].toString().equals("administrator")) {
                        isAdmin = true;
                        break;
                    }
                }
                if (mBlog.bsetAdmin(isAdmin)) {
                    WordPress.wpDB.saveBlog(mBlog);
                }
            }
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            boolean commentsOnly = params[0];
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(mBlog.getUri(), mBlog.getHttpuser(),
                    mBlog.getHttppassword());

            boolean alreadyTrackedAsJetpackBlog = mBlog.isJetpackPowered();

            if (!commentsOnly) {
                // check the WP number if self-hosted
                Map<String, String> hPost = ApiHelper.blogOptionsXMLRPCParameters;

                Object[] vParams = {mBlog.getRemoteBlogId(),
                                    mBlog.getUsername(),
                                    mBlog.getPassword(),
                                    hPost};
                Object versionResult = null;
                try {
                    versionResult = client.call(Methods.GET_OPTIONS, vParams);
                } catch (ClassCastException cce) {
                    setError(ErrorType.INVALID_RESULT, cce.getMessage(), cce);
                    return false;
                } catch (Exception e) {
                    setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
                    return false;
                }

                if (versionResult != null) {
                    Map<?, ?> blogOptions = (HashMap<?, ?>) versionResult;
                    ApiHelper.updateBlogOptions(mBlog, blogOptions);
                }

                if (mBlog.isJetpackPowered() && !alreadyTrackedAsJetpackBlog) {
                    // blog just added to the app, or the value of jetpack_client_id has just changed
                    AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNED_INTO_JETPACK);
                }

                // get theme post formats
                new GetPostFormatsTask().execute(mBlog);

                //Update Stats widgets if necessary
                String currentBlogID = String.valueOf(mBlog.getRemoteBlogId());
                if (StatsWidgetProvider.shouldUpdateWidgetForBlog(WordPress.getContext(), currentBlogID)) {
                    String currentDate = StatsUtils.getCurrentDateTZ(mBlog.getLocalTableBlogId());
                    StatsWidgetProvider.enqueueStatsRequestForBlog(WordPress.getContext(), currentBlogID, currentDate);
                }
            }

            // Check if user is an admin
            Object[] userParams = {mBlog.getRemoteBlogId(), mBlog.getUsername(), mBlog.getPassword()};
            try {
                Map<String, Object> userInfos = (HashMap<String, Object>) client.call(ApiHelper.Methods.GET_PROFILE, userParams);
                updateBlogAdmin(userInfos);
            } catch (ClassCastException cce) {
                setError(ErrorType.INVALID_RESULT, cce.getMessage(), cce);
                return false;
            } catch (XMLRPCException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            } catch (IOException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            } catch (XmlPullParserException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            }

            // refresh the comments
            Map<String, Object> hPost = new HashMap<String, Object>();
            hPost.put("number", 30);
            Object[] commentParams = {mBlog.getRemoteBlogId(), mBlog.getUsername(),
                    mBlog.getPassword(), hPost};
            try {
                ApiHelper.refreshComments(mBlog, commentParams);
            } catch (Exception e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (mCallback != null) {
                if (success) {
                    mCallback.onSuccess();
                } else {
                    mCallback.onFailure(mErrorType, mErrorMessage, mThrowable);
                }
            }
            refreshedBlogs.remove(mBlogIdentifier);
        }
    }

    /**
     * request deleted comments for passed blog and remove them from local db
     * @param blog  blog to check
     * @return count of comments that were removed from db
     */
    public static int removeDeletedComments(Blog blog) {
        if (blog == null) {
            return 0;
        }

        XMLRPCClientInterface client = XMLRPCFactory.instantiate(
                blog.getUri(),
                blog.getHttpuser(),
                blog.getHttppassword());

        Map<String, Object> hPost = new HashMap<String, Object>();
        hPost.put("status", "trash");

        Object[] params = { blog.getRemoteBlogId(),
                blog.getUsername(),
                blog.getPassword(),
                hPost };

        int numDeleted = 0;
        try {
            Object[] result = (Object[]) client.call(ApiHelper.Methods.GET_COMMENTS, params);
            if (result == null || result.length == 0) {
                return 0;
            }
            Map<?, ?> contentHash;
            for (Object aComment : result) {
                contentHash = (Map<?, ?>) aComment;
                long commentId = Long.parseLong(contentHash.get("comment_id").toString());
                if (CommentTable.deleteComment(blog.getLocalTableBlogId(), commentId))
                    numDeleted++;
            }
            if (numDeleted > 0) {
                AppLog.d(T.COMMENTS, String.format("removed %d deleted comments", numDeleted));
            }
        } catch (XMLRPCException e) {
            AppLog.e(T.COMMENTS, e);
        } catch (IOException e) {
            AppLog.e(T.COMMENTS, e);
        } catch (XmlPullParserException e) {
            AppLog.e(T.COMMENTS, e);
        }

        return numDeleted;
    }

    public static CommentList refreshComments(Blog blog, Object[] commentParams)
            throws XMLRPCException, IOException, XmlPullParserException {
        if (blog == null) {
            return null;
        }
        XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                blog.getHttppassword());
        Object[] result;
        result = (Object[]) client.call(ApiHelper.Methods.GET_COMMENTS, commentParams);

        if (result.length == 0) {
            return null;
        }

        Map<?, ?> contentHash;
        long commentID, postID;
        String authorName, content, status, authorEmail, authorURL, postTitle, pubDate;
        java.util.Date date;
        CommentList comments = new CommentList();

        for (int ctr = 0; ctr < result.length; ctr++) {
            contentHash = (Map<?, ?>) result[ctr];
            content = contentHash.get("content").toString();
            status = contentHash.get("status").toString();
            postID = Long.parseLong(contentHash.get("post_id").toString());
            commentID = Long.parseLong(contentHash.get("comment_id").toString());
            authorName = contentHash.get("author").toString();
            authorURL = contentHash.get("author_url").toString();
            authorEmail = contentHash.get("author_email").toString();
            postTitle = contentHash.get("post_title").toString();
            date = (java.util.Date) contentHash.get("date_created_gmt");
            pubDate = DateTimeUtils.javaDateToIso8601(date);

            Comment comment = new Comment(
                    postID,
                    commentID,
                    authorName,
                    pubDate,
                    content,
                    status,
                    postTitle,
                    authorURL,
                    authorEmail,
                    null);

            comments.add(comment);
        }

        int localBlogId = blog.getLocalTableBlogId();
        CommentTable.saveComments(localBlogId, comments);

        return comments;
    }

    /**
     * Delete a single post or page via XML-RPC API parameters follow those of FetchSinglePostTask
     */
    public static class DeleteSinglePostTask extends HelperAsyncTask<java.util.List<?>, Boolean, Boolean> {

        @Override
        protected Boolean doInBackground(List<?>... params) {
            List<?> arguments = params[0];
            Blog blog = (Blog) arguments.get(0);
            if (blog == null) {
                return false;
            }

            String postId = (String) arguments.get(1);
            boolean isPage = (Boolean) arguments.get(2);
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                    blog.getHttppassword());

            Object[] postParams = {"", postId,
                    blog.getUsername(),
                    blog.getPassword()};
            Object[] pageParams = {blog.getRemoteBlogId(),
                    blog.getUsername(),
                    blog.getPassword(), postId};

            try {
                client.call(isPage ? ApiHelper.Methods.DELETE_PAGE : "blogger.deletePost", (isPage) ? pageParams : postParams);
                return true;
            } catch (XMLRPCException | IOException | XmlPullParserException e) {
                mErrorMessage = e.getMessage();
                return false;
            }
        }
    }

    public static class SyncMediaLibraryTask extends HelperAsyncTask<java.util.List<?>, Void, Integer> {
        public interface Callback extends GenericErrorCallback {
            public void onSuccess(int results);
        }

        private Callback mCallback;
        private int mOffset;
        private Filter mFilter;

        public SyncMediaLibraryTask(int offset, Filter filter, Callback callback) {
            mOffset = offset;
            mCallback = callback;
            mFilter = filter;
        }

        @Override
        protected Integer doInBackground(List<?>... params) {
            List<?> arguments = params[0];
            WordPress.currentBlog = (Blog) arguments.get(0);
            Blog blog = WordPress.currentBlog;
            if (blog == null) {
                setError(ErrorType.INVALID_CURRENT_BLOG, "ApiHelper - current blog is null");
                return 0;
            }

            String blogId = String.valueOf(blog.getLocalTableBlogId());
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                    blog.getHttppassword());
            Map<String, Object> filter = new HashMap<String, Object>();
            filter.put("number", 50);
            filter.put("offset", mOffset);

            if (mFilter == Filter.IMAGES) {
                filter.put("mime_type","image/*");
            } else if(mFilter == Filter.UNATTACHED) {
                filter.put("parent_id", 0);
            }

            Object[] apiParams = {blog.getRemoteBlogId(), blog.getUsername(), blog.getPassword(),
                    filter};

            Object[] results = null;
            try {
                results = (Object[]) client.call(ApiHelper.Methods.GET_MEDIA_LIBRARY, apiParams);
            } catch (ClassCastException cce) {
                setError(ErrorType.INVALID_RESULT, cce.getMessage(), cce);
                return 0;
            } catch (XMLRPCException e) {
                prepareErrorMessage(e);
                return 0;
            } catch (IOException e) {
                prepareErrorMessage(e);
                return 0;
            } catch (XmlPullParserException e) {
                prepareErrorMessage(e);
                return 0;
            }

            if (blogId == null) {
                setError(ErrorType.INVALID_CURRENT_BLOG, "Invalid blogId");
                return 0;
            }

            if (results == null) {
                setError(ErrorType.INVALID_RESULT, "Invalid blogId");
                return 0;
            }

            Map<?, ?> resultMap;
            // results returned, so mark everything existing to deleted
            // since offset is 0, we are doing a full refresh
            if (mOffset == 0) {
                WordPress.wpDB.setMediaFilesMarkedForDeleted(blogId);
            }
            for (Object result : results) {
                resultMap = (Map<?, ?>) result;
                boolean isDotCom = (WordPress.getCurrentBlog() != null && WordPress.getCurrentBlog().isDotcomFlag());
                MediaFile mediaFile = new MediaFile(blogId, resultMap, isDotCom);
                WordPress.wpDB.saveMediaFile(mediaFile);
            }
            WordPress.wpDB.deleteFilesMarkedForDeleted(blogId);
            return results.length;
        }

        private void prepareErrorMessage(Exception e) {
            // user does not have permission to view media gallery
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                setError(ErrorType.NO_UPLOAD_FILES_CAP, e.getMessage(), e);
            } else {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (mCallback != null) {
                if (mErrorType == ErrorType.NO_ERROR) {
                    mCallback.onSuccess(result);
                } else {
                    mCallback.onFailure(mErrorType, mErrorMessage, mThrowable);
                }
            }
        }
    }

    public static class EditMediaItemTask extends HelperAsyncTask<List<?>, Void, Boolean> {
        private GenericCallback mCallback;
        private String mMediaId;
        private String mTitle;
        private String mDescription;
        private String mCaption;

        public EditMediaItemTask(String mediaId, String title, String description, String caption,
                                 GenericCallback callback) {
            mMediaId = mediaId;
            mCallback = callback;
            mTitle = title;
            mCaption = caption;
            mDescription = description;
        }
        @Override
        protected Boolean doInBackground(List<?>... params) {
            List<?> arguments = params[0];
            WordPress.currentBlog = (Blog) arguments.get(0);
            Blog blog = WordPress.currentBlog;

            if (blog == null) {
                setError(ErrorType.INVALID_CURRENT_BLOG, "ApiHelper - current blog is null");
                return null;
            }
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                    blog.getHttppassword());
            Map<String, Object> contentStruct = new HashMap<String, Object>();
            contentStruct.put("post_title", mTitle);
            contentStruct.put("post_content", mDescription);
            contentStruct.put("post_excerpt", mCaption);

            Object[] apiParams = {
                    blog.getRemoteBlogId(),
                    blog.getUsername(),
                    blog.getPassword(),
                    mMediaId,
                    contentStruct
            };

            Boolean result = null;
            try {
                result = (Boolean) client.call(ApiHelper.Methods.EDIT_POST, apiParams);
            } catch (ClassCastException cce) {
                setError(ErrorType.INVALID_RESULT, cce.getMessage(), cce);
            } catch (XMLRPCException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            } catch (IOException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            } catch (XmlPullParserException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mCallback != null) {
                if (mErrorType == ErrorType.NO_ERROR) {
                    mCallback.onSuccess();
                } else {
                    mCallback.onFailure(mErrorType, mErrorMessage, mThrowable);
                }
            }
        }
    }

    public static class GetMediaItemTask extends HelperAsyncTask<List<?>, Void, MediaFile> {
        public interface Callback extends GenericErrorCallback {
            public void onSuccess(MediaFile results);
        }
        private Callback mCallback;
        private int mMediaId;

        public GetMediaItemTask(int mediaId, Callback callback) {
            mMediaId = mediaId;
            mCallback = callback;
        }

        @Override
        protected MediaFile doInBackground(List<?>... params) {
            List<?> arguments = params[0];
            WordPress.currentBlog = (Blog) arguments.get(0);
            Blog blog = WordPress.currentBlog;
            if (blog == null) {
                setError(ErrorType.INVALID_CURRENT_BLOG, "ApiHelper - current blog is null");
                return null;
            }

            String blogId = String.valueOf(blog.getLocalTableBlogId());

            XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                    blog.getHttppassword());
            Object[] apiParams = {
                    blog.getRemoteBlogId(),
                    blog.getUsername(),
                    blog.getPassword(),
                    mMediaId
            };
            Map<?, ?> results = null;
            try {
                results = (Map<?, ?>) client.call(ApiHelper.Methods.GET_MEDIA_ITEM, apiParams);
            } catch (ClassCastException cce) {
                setError(ErrorType.INVALID_RESULT, cce.getMessage(), cce);
            } catch (XMLRPCException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            } catch (IOException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            } catch (XmlPullParserException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            }

            if (results != null && blogId != null) {
                boolean isDotCom = (WordPress.getCurrentBlog() != null && WordPress.getCurrentBlog().isDotcomFlag());
                MediaFile mediaFile = new MediaFile(blogId, results, isDotCom);
                WordPress.wpDB.saveMediaFile(mediaFile);
                return mediaFile;
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(MediaFile result) {
            if (mCallback != null) {
                if (result != null) {
                    mCallback.onSuccess(result);
                } else {
                    mCallback.onFailure(mErrorType, mErrorMessage, mThrowable);
                }
            }
        }
    }

    public static class UploadMediaTask extends HelperAsyncTask<List<?>, Void, String> {
        public interface Callback extends GenericErrorCallback {
            public void onSuccess(String id);
        }
        private Callback mCallback;
        private Context mContext;
        private MediaFile mMediaFile;

        public UploadMediaTask(Context applicationContext, MediaFile mediaFile,
                               Callback callback) {
            mContext = applicationContext;
            mMediaFile = mediaFile;
            mCallback = callback;
        }

        @Override
        protected String doInBackground(List<?>... params) {
            List<?> arguments = params[0];
            WordPress.currentBlog = (Blog) arguments.get(0);
            Blog blog = WordPress.currentBlog;

            if (blog == null) {
                setError(ErrorType.INVALID_CURRENT_BLOG, "current blog is null");
                return null;
            }

            XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                    blog.getHttppassword());

            Map<String, Object> data = new HashMap<String, Object>();
            data.put("name", mMediaFile.getFileName());
            data.put("type", mMediaFile.getMimeType());
            data.put("bits", mMediaFile);
            data.put("overwrite", true);

            Object[] apiParams = {
                    blog.getRemoteBlogId(),
                    blog.getUsername(),
                    blog.getPassword(),
                    data
            };

            if (mContext == null) {
                return null;
            }

            Map<?, ?> resultMap;
            try {
                resultMap = (HashMap<?, ?>) client.call(Methods.UPLOAD_FILE, apiParams, getTempFile(mContext));
            } catch (ClassCastException cce) {
                setError(ErrorType.INVALID_RESULT, cce.getMessage(), cce);
                return null;
            } catch (XMLRPCException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
                return null;
            } catch (IOException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
                return null;
            } catch (XmlPullParserException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
                return null;
            }

            if (resultMap != null && resultMap.containsKey("id")) {
                return (String) resultMap.get("id");
            } else {
                setError(ErrorType.INVALID_RESULT, "Invalid result");
            }

            return null;
        }

        // Create a temp file for media upload
        private File getTempFile(Context context) {
            String tempFileName = "wp-" + System.currentTimeMillis();
            try {
                context.openFileOutput(tempFileName, Context.MODE_PRIVATE);
            } catch (FileNotFoundException e) {
                return null;
            }
            return context.getFileStreamPath(tempFileName);
        }

        @Override
        protected void onPostExecute(String result) {
            if (mCallback != null) {
                if (result != null) {
                    mCallback.onSuccess(result);
                } else {
                    mCallback.onFailure(mErrorType, mErrorMessage, mThrowable);
                }
            }
        }
    }

    public static class DeleteMediaTask extends HelperAsyncTask<List<?>, Void, Void> {
        private GenericCallback mCallback;
        private String mMediaId;

        public DeleteMediaTask(String mediaId, GenericCallback callback) {
            mMediaId = mediaId;
            mCallback = callback;
        }

        @Override
        protected Void doInBackground(List<?>... params) {
            List<?> arguments = params[0];
            Blog blog = (Blog) arguments.get(0);

            if (blog == null) {
                setError(ErrorType.INVALID_CONTEXT, "ApiHelper - invalid blog");
                return null;
            }

            XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                    blog.getHttppassword());
            Object[] apiParams = new Object[]{blog.getRemoteBlogId(), blog.getUsername(),
                    blog.getPassword(), mMediaId};

            try {
                if (client != null) {
                    Boolean result = (Boolean) client.call(ApiHelper.Methods.DELETE_POST, apiParams);
                    if (!result) {
                        setError(ErrorType.INVALID_RESULT, "wp.deletePost returned false");
                    }
                }
            } catch (ClassCastException cce) {
                setError(ErrorType.INVALID_RESULT, cce.getMessage(), cce);
            } catch (XMLRPCException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            } catch (IOException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            } catch (XmlPullParserException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            if (mCallback != null) {
                if (mErrorType == ErrorType.NO_ERROR) {
                    mCallback.onSuccess();
                } else {
                    mCallback.onFailure(mErrorType, mErrorMessage, mThrowable);
                }
            }
        }
    }

    public static class GetFeatures extends AsyncTask<List<?>, Void, FeatureSet> {
        public interface Callback {
            void onResult(FeatureSet featureSet);
        }

        private Callback mCallback;

        public GetFeatures() {
        }

        public GetFeatures(Callback callback) {
            mCallback = callback;
        }

        public FeatureSet doSynchronously(List<?>... params) {
            return doInBackground(params);
        }

        @Override
        protected FeatureSet doInBackground(List<?>... params) {
            List<?> arguments = params[0];
            Blog blog = (Blog) arguments.get(0);

            if (blog == null)
                return null;

            XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                    blog.getHttppassword());

            Object[] apiParams = new Object[] {
                    blog.getRemoteBlogId(),
                    blog.getUsername(),
                    blog.getPassword(),
            };

            Map<?, ?> resultMap = null;
            try {
                resultMap = (HashMap<?, ?>) client.call(ApiHelper.Methods.WPCOM_GET_FEATURES, apiParams);
            } catch (ClassCastException cce) {
                AppLog.e(T.API, "wpcom.getFeatures error", cce);
            } catch (XMLRPCException e) {
                AppLog.e(T.API, "wpcom.getFeatures error", e);
            } catch (IOException e) {
                AppLog.e(T.API, "wpcom.getFeatures error", e);
            } catch (XmlPullParserException e) {
                AppLog.e(T.API, "wpcom.getFeatures error", e);
            }

            if (resultMap != null) {
                return new FeatureSet(blog.getRemoteBlogId(), resultMap);
            }

            return null;
        }

        @Override
        protected void onPostExecute(FeatureSet result) {
            if (mCallback != null)
                mCallback.onResult(result);
        }

    }

    /**
     * Discover the XML-RPC endpoint for the WordPress API associated with the specified blog URL.
     *
     * @param urlString URL of the blog to get the XML-RPC endpoint for.
     * @return XML-RPC endpoint for the specified blog, or null if unable to discover endpoint.
     */
    public static String getXMLRPCUrl(String urlString) throws SSLHandshakeException {
        Pattern xmlrpcLink = Pattern.compile("<api\\s*?name=\"WordPress\".*?apiLink=\"(.*?)\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        String html = getResponse(urlString);
        if (html != null) {
            Matcher matcher = xmlrpcLink.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null; // never found the rsd tag
    }

    /**
     * Synchronous method to fetch the String content at the specified HTTP URL.
     *
     * @param stringUrl URL to fetch contents for.
     * @return content of the resource, or null if URL was invalid or resource could not be retrieved.
     */
    public static String getResponse(final String stringUrl) throws SSLHandshakeException {
        return getResponse(stringUrl, 0);
    }

    private static String getRedirectURL(String oldURL, NetworkResponse networkResponse) {
        if (networkResponse.headers != null && networkResponse.headers.containsKey("Location")) {
            String newURL = networkResponse.headers.get("Location");
            // Relative URL
            if (newURL != null && newURL.startsWith("/")) {
                Uri oldUri = Uri.parse(oldURL);
                if (oldUri.getScheme() == null || oldUri.getAuthority() == null) {
                    return null;
                }
                return oldUri.getScheme() + "://" + oldUri.getAuthority() + newURL;
            }
            // Absolute URL
            return newURL;
        }
        return null;
    }

    public static String getResponse(final String stringUrl, int numberOfRedirects) throws SSLHandshakeException {
        RequestFuture<String> future = RequestFuture.newFuture();
        StringRequest request = new StringRequest(stringUrl, future, future);
        request.setRetryPolicy(new DefaultRetryPolicy(XMLRPCClient.DEFAULT_SOCKET_TIMEOUT, 0, 1));
        WordPress.requestQueue.add(request);
        try {
            return future.get(XMLRPCClient.DEFAULT_SOCKET_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            AppLog.e(T.API, e);
        } catch (ExecutionException e) {
            if (e.getCause() != null && e.getCause() instanceof RedirectError) {
                // Maximum 5 redirects or die
                if (numberOfRedirects > 5) {
                    AppLog.e(T.API, "Maximum of 5 redirects reached, aborting.", e);
                    return null;
                }
                // Follow redirect
                RedirectError re = (RedirectError) e.getCause();
                if (re.networkResponse != null) {
                    String newURL = getRedirectURL(stringUrl, re.networkResponse);
                    if (newURL == null) {
                        AppLog.e(T.API, "Invalid server response", e);
                        return null;
                    }
                    // Abort redirect if old URL was HTTPS and not the new one
                    if (URLUtil.isHttpsUrl(stringUrl) && !URLUtil.isHttpsUrl(newURL)) {
                        AppLog.e(T.API, "Redirect from HTTPS to HTTP not allowed.", e);
                        return null;
                    }
                    // Retry getResponse
                    AppLog.i(T.API, "Follow redirect from " + stringUrl + " to " + newURL);
                    return getResponse(newURL, numberOfRedirects + 1);
                }
            } else {
                AppLog.e(T.API, e);
            }

        } catch (TimeoutException e) {
            AppLog.e(T.API, e);
        }
        return null;
    }

    /**
     * Regex pattern for matching the RSD link found in most WordPress sites.
     */
    private static final Pattern rsdLink = Pattern.compile(
            "<link\\s*?rel=\"EditURI\"\\s*?type=\"application/rsd\\+xml\"\\s*?title=\"RSD\"\\s*?href=\"(.*?)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Returns RSD URL based on regex match
     * @param urlString
     * @return String RSD url
     */
    public static String getRSDMetaTagHrefRegEx(String urlString)
            throws SSLHandshakeException {
        String html = ApiHelper.getResponse(urlString);
        if (html != null) {
            Matcher matcher = rsdLink.matcher(html);
            if (matcher.find()) {
                String href = matcher.group(1);
                return href;
            }
        }
        return null;
    }

    /**
     * Returns RSD URL based on html tag search
     * @param urlString
     * @return String RSD url
     */
    public static String getRSDMetaTagHref(String urlString)
            throws SSLHandshakeException {
        // get the html code
        String data = ApiHelper.getResponse(urlString);

        // parse the html and get the attribute for xmlrpc endpoint
        if (data != null) {
            StringReader stringReader = new StringReader(data);
            XmlPullParser parser = Xml.newPullParser();
            try {
                // auto-detect the encoding from the stream
                parser.setInput(stringReader);
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    String name = null;
                    String rel = "";
                    String type = "";
                    String href = "";
                    switch (eventType) {
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase("link")) {
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                String attrName = parser.getAttributeName(i);
                                String attrValue = parser.getAttributeValue(i);
                                if (attrName.equals("rel")) {
                                    rel = attrValue;
                                } else if (attrName.equals("type"))
                                    type = attrValue;
                                else if (attrName.equals("href"))
                                    href = attrValue;
                            }

                            if (rel.equals("EditURI") && type.equals("application/rsd+xml")) {
                                return href;
                            }
                            // currentMessage.setLink(parser.nextText());
                        }
                        break;
                    }
                    eventType = parser.next();
                }
            } catch (XmlPullParserException e) {
                AppLog.e(T.API, e);
                return null;
            } catch (IOException e) {
                AppLog.e(T.API, e);
                return null;
            }
        }
        return null; // never found the rsd tag
    }

    /*
     * fetches a single post saves it to the db - note that this should NOT be called from main thread
     */
    public static boolean updateSinglePost(int localBlogId, String remotePostId, boolean isPage) {
        Blog blog = WordPress.getBlog(localBlogId);
        if (blog == null || TextUtils.isEmpty(remotePostId)) {
            return false;
        }

        XMLRPCClientInterface client = XMLRPCFactory.instantiate(
                blog.getUri(),
                blog.getHttpuser(),
                blog.getHttppassword());

        Object[] apiParams;
        if (isPage) {
            apiParams = new Object[]{
                    blog.getRemoteBlogId(),
                    remotePostId,
                    blog.getUsername(),
                    blog.getPassword()
            };
        } else {
            apiParams = new Object[]{
                    remotePostId,
                    blog.getUsername(),
                    blog.getPassword()
            };
        }

        try {
            Object result = client.call(isPage ? ApiHelper.Methods.GET_PAGE : "metaWeblog.getPost", apiParams);

            if (result != null && result instanceof Map) {
                Map postMap = (HashMap) result;
                List<Map<?, ?>> postsList = new ArrayList<>();
                postsList.add(postMap);

                WordPress.wpDB.savePosts(postsList, localBlogId, isPage, true);
                return true;
            } else {
                return false;
            }

        } catch (XMLRPCException | IOException | XmlPullParserException e) {
            AppLog.e(AppLog.T.POSTS, e);
            return false;
        }
    }
}
