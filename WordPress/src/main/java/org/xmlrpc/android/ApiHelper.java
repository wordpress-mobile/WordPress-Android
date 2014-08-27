package org.xmlrpc.android;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Xml;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;

import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.BlogIdentifier;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.models.FeatureSet;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.ui.media.MediaGridFragment.Filter;
import org.wordpress.android.ui.posts.PostsListFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.MapUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLHandshakeException;

public class ApiHelper {
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

    public static class GetPostFormatsTask extends HelperAsyncTask<java.util.List<?>, Void, Object> {
        private Blog mBlog;

        @Override
        protected Object doInBackground(List<?>... args) {
            List<?> arguments = args[0];
            mBlog = (Blog) arguments.get(0);
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(mBlog.getUri(), mBlog.getHttpuser(),
                    mBlog.getHttppassword());
            Object result = null;
            Object[] params = { mBlog.getRemoteBlogId(), mBlog.getUsername(),
                    mBlog.getPassword(), "show-supported" };
            try {
                result = client.call("wp.getPostFormats", params);
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
        private Context mContext;
        private BlogIdentifier mBlogIdentifier;
        private GenericCallback mCallback;

        public RefreshBlogContentTask(Context context, Blog blog, GenericCallback callback) {
            if (context == null || blog == null) {
                cancel(true);
                return;
            }

            if (!NetworkUtils.isNetworkAvailable(context)) {
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
            mContext = context;
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

            if (!commentsOnly) {
                // check the WP number if self-hosted
                Map<String, String> hPost = ApiHelper.blogOptionsXMLRPCParameters;

                Object[] vParams = {mBlog.getRemoteBlogId(),
                                    mBlog.getUsername(),
                                    mBlog.getPassword(),
                                    hPost};
                Object versionResult = null;
                try {
                    versionResult = client.call("wp.getOptions", vParams);
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

                // get theme post formats
                List<Object> args = new Vector<Object>();
                args.add(mBlog);
                args.add(mContext);
                new GetPostFormatsTask().execute(args);
            }

            // Check if user is an admin
            Object[] userParams = {mBlog.getRemoteBlogId(), mBlog.getUsername(), mBlog.getPassword()};
            try {
                Map<String, Object> userInfos = (HashMap<String, Object>) client.call("wp.getProfile", userParams);
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
                ApiHelper.refreshComments(mContext, mBlog, commentParams);
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
            Object[] result = (Object[]) client.call("wp.getComments", params);
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

    public static CommentList refreshComments(Context context, Blog blog, Object[] commentParams)
            throws XMLRPCException, IOException, XmlPullParserException {
        if (blog == null) {
            return null;
        }
        XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                blog.getHttppassword());
        Object[] result;
        result = (Object[]) client.call("wp.getComments", commentParams);

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

    public static class FetchPostsTask extends HelperAsyncTask<java.util.List<?>, Boolean, Boolean> {
        public interface Callback extends GenericErrorCallback {
            public void onSuccess(int postCount);
        }

        private Callback mCallback;
        private String mErrorMessage;
        private int mPostCount;

        public FetchPostsTask(Callback callback) {
            mCallback = callback;
        }

        @Override
        protected Boolean doInBackground(List<?>... params) {
            List<?> arguments = params[0];

            Blog blog = (Blog) arguments.get(0);
            if (blog == null)
                return false;

            boolean isPage = (Boolean) arguments.get(1);
            int recordCount = (Integer) arguments.get(2);
            boolean loadMore = (Boolean) arguments.get(3);
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                    blog.getHttppassword());

            Object[] result;
            Object[] xmlrpcParams = { blog.getRemoteBlogId(),
                    blog.getUsername(),
                    blog.getPassword(), recordCount };
            try {
                result = (Object[]) client.call((isPage) ? "wp.getPages"
                        : "metaWeblog.getRecentPosts", xmlrpcParams);
                if (result != null && result.length > 0) {
                    mPostCount = result.length;
                    List<Map<?, ?>> postsList = new ArrayList<Map<?, ?>>();

                    if (!loadMore) {
                        WordPress.wpDB.deleteUploadedPosts(
                                blog.getLocalTableBlogId(), isPage);
                    }

                    // If we're loading more posts, only save the posts at the end of the array.
                    // NOTE: Switching to wp.getPosts wouldn't require janky solutions like this
                    // since it allows for an offset parameter.
                    int startPosition = 0;
                    if (loadMore && result.length > PostsListFragment.POSTS_REQUEST_COUNT) {
                        startPosition = result.length - PostsListFragment.POSTS_REQUEST_COUNT;
                    }

                    for (int ctr = startPosition; ctr < result.length; ctr++) {
                        Map<?, ?> postMap = (Map<?, ?>) result[ctr];
                        postsList.add(postMap);
                    }

                    WordPress.wpDB.savePosts(postsList, blog.getLocalTableBlogId(), isPage, !loadMore);
                }
                return true;
            } catch (XMLRPCFault e) {
                mErrorType = ErrorType.NETWORK_XMLRPC;
                if (e.getFaultCode() == 401) {
                    mErrorType = ErrorType.UNAUTHORIZED;
                }
                mErrorMessage = e.getMessage();
            } catch (XMLRPCException e) {
                mErrorType = ErrorType.NETWORK_XMLRPC;
                mErrorMessage = e.getMessage();
            } catch (IOException e) {
                mErrorType = ErrorType.INVALID_RESULT;
                mErrorMessage = e.getMessage();
            } catch (XmlPullParserException e) {
                mErrorType = ErrorType.INVALID_RESULT;
                mErrorMessage = e.getMessage();
            }

            return false;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mCallback.onFailure(ErrorType.TASK_CANCELLED, mErrorMessage, mThrowable);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (mCallback != null) {
                if (success) {
                    mCallback.onSuccess(mPostCount);
                } else {
                    mCallback.onFailure(mErrorType, mErrorMessage, mThrowable);
                }
            }
        }
    }

    /**
     * Fetch a single post or page from the XML-RPC API and save/update it in the DB
     */
    public static class FetchSinglePostTask extends HelperAsyncTask<java.util.List<?>, Boolean, Boolean> {
        public interface Callback extends GenericErrorCallback {
            public void onSuccess();
        }

        private Callback mCallback;
        private String mErrorMessage;

        public FetchSinglePostTask(Callback callback) {
            mCallback = callback;
        }

        @Override
        protected Boolean doInBackground(List<?>... params) {
            List<?> arguments = params[0];

            Blog blog = (Blog) arguments.get(0);
            if (blog == null)
                return false;

            String postId = (String) arguments.get(1);
            boolean isPage = (Boolean) arguments.get(2);
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                    blog.getHttppassword());

            Object[] apiParams;
            if (isPage) {
                apiParams = new Object[]{
                        blog.getRemoteBlogId(),
                        postId,
                        blog.getUsername(),
                        blog.getPassword()
                };
            } else {
                apiParams = new Object[]{
                        postId,
                        blog.getUsername(),
                        blog.getPassword()
                };
            }

            try {
                Object result = client.call((isPage) ? "wp.getPage" : "metaWeblog.getPost", apiParams);
                if (result != null && result instanceof Map) {
                    Map postMap = (HashMap) result;
                    List<Map<?, ?>> postsList = new ArrayList<Map<?, ?>>();
                    postsList.add(postMap);

                    WordPress.wpDB.savePosts(postsList, blog.getLocalTableBlogId(), isPage, true);
                }

                return true;
            } catch (XMLRPCException e) {
                mErrorMessage = e.getMessage();
            } catch (IOException e) {
                mErrorMessage = e.getMessage();
            } catch (XmlPullParserException e) {
                mErrorMessage = e.getMessage();
            }

            return false;
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
                results = (Object[]) client.call("wp.getMediaLibrary", apiParams);
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
                MediaFile mediaFile = new MediaFile(blogId, resultMap);
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
                result = (Boolean) client.call("wp.editPost", apiParams);
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
                results = (Map<?, ?>) client.call("wp.getMediaItem", apiParams);
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
                MediaFile mediaFile = new MediaFile(blogId, results);
                mediaFile.save();
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
                resultMap = (HashMap<?, ?>) client.call("wp.uploadFile", apiParams, getTempFile(mContext));
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
                    Boolean result = (Boolean) client.call("wp.deletePost", apiParams);
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
                resultMap = (HashMap<?, ?>) client.call("wpcom.getFeatures", apiParams);
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
     * Synchronous method to fetch the String content at the specified URL.
     *
     * @param url                     URL to fetch contents for.
     * @return content of the resource, or null if URL was invalid or resource could not be retrieved.
     */
    public static String getResponse(final String url) throws SSLHandshakeException {
        return getResponse(url, 3);
    }

    private static String getResponse(final String url, int maxRedirection)
            throws SSLHandshakeException {
        RequestFuture<String> requestFuture = RequestFuture.newFuture();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, requestFuture, requestFuture);
        WordPress.requestQueue.add(stringRequest);
        try {
            // Wait for the response
            return requestFuture.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            AppLog.e(T.API, e);
            return null;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ServerError) {
                NetworkResponse networkResponse = ((ServerError) e.getCause()).networkResponse;
                if ((networkResponse != null) && (networkResponse.statusCode == HttpURLConnection.HTTP_MOVED_PERM ||
                                                  networkResponse.statusCode == HttpURLConnection.HTTP_MOVED_TEMP)) {
                    String newUrl = networkResponse.headers.get("Location");
                    if (maxRedirection > 0) {
                        return getResponse(newUrl, maxRedirection - 1);
                    }
                }
            }
            if (e.getCause() != null && e.getCause().getCause() instanceof SSLHandshakeException) {
                throw (SSLHandshakeException) e.getCause().getCause();
            }
            AppLog.e(T.API, e);
            return null;
        } catch (TimeoutException e) {
            AppLog.e(T.API, e);
            return null;
        }
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
}
