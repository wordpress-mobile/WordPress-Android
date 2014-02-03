package org.xmlrpc.android;

import android.content.Context;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import android.util.Xml;

import com.google.gson.Gson;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.BlogIdentifier;
import org.wordpress.android.models.FeatureSet;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.ui.media.MediaGridFragment.Filter;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.HttpRequest;
import org.wordpress.android.util.HttpRequest.HttpRequestException;
import org.wordpress.android.util.MapUtils;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiHelper {
    public enum ErrorType {NO_ERROR, INVALID_CURRENT_BLOG, NETWORK_XMLRPC, INVALID_CONTEXT,
        INVALID_RESULT, NO_UPLOAD_FILES_CAP, CAST_EXCEPTION}
    /** Called when the activity is first created. */
    private static XMLRPCClient client;

    @SuppressWarnings("unchecked")
    static void refreshComments(final int id, final Context ctx) {
        Blog blog;
        try {
            blog = new Blog(id);
        } catch (Exception e1) {
            return;
        }

        client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(),
                blog.getHttppassword());

        Map<String, Object> hPost = new HashMap<String, Object>();
        hPost.put("status", "");
        hPost.put("post_id", "");
        hPost.put("number", 30);

        Object[] params = { blog.getRemoteBlogId(), blog.getUsername(),
                blog.getPassword(), hPost };
        Object[] result = null;
        try {
            result = (Object[]) client.call("wp.getComments", params);
        } catch (XMLRPCException e) {
        }

        if (result != null) {
            if (result.length > 0) {
                String author, postID, commentID, comment, status, authorEmail, authorURL, postTitle;

                Map<Object, Object> contentHash = new HashMap<Object, Object>();
                List<Map<String, String>> dbVector = new Vector<Map<String, String>>();

                Date d = new Date();
                // loop this!
                for (int ctr = 0; ctr < result.length; ctr++) {
                    Map<String, String> dbValues = new HashMap<String, String>();
                    contentHash = (Map<Object, Object>) result[ctr];
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
                        flags |= DateUtils.FORMAT_SHOW_DATE;
                        flags |= DateUtils.FORMAT_ABBREV_MONTH;
                        flags |= DateUtils.FORMAT_SHOW_YEAR;
                        flags |= DateUtils.FORMAT_SHOW_TIME;
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

    public static abstract class HelperAsyncTask<Params, Progress, Result>
            extends AsyncTask<Params, Progress, Result> {
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
            AppLog.e(T.API, mErrorType.name() + " - " + mErrorMessage);
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
            client = new XMLRPCClient(mBlog.getUrl(), mBlog.getHttpuser(),
                    mBlog.getHttppassword());
            Object result = null;
            Object[] params = { mBlog.getRemoteBlogId(), mBlog.getUsername(),
                    mBlog.getPassword(), "show-supported" };
            try {
                result = client.call("wp.getPostFormats", params);
            } catch (XMLRPCException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
            }
            return result;
        }

        protected void onPostExecute(Object result) {
            if (result != null) {
                Map<?, ?> postFormats = (HashMap<?, ?>) result;
                if (postFormats.size() > 0) {
                    Gson gson = new Gson();
                    String postFormatsJson = gson.toJson(postFormats);
                    if (postFormatsJson != null) {
                        if (mBlog.bsetPostFormats(postFormatsJson)) {
                            mBlog.save();
                        }
                    }
                }
            }
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

        private void updateBlogOptions(Map<?, ?> blogOptions) {
            boolean isModified = false;
            Gson gson = new Gson();
            String blogOptionsJson = gson.toJson(blogOptions);
            if (blogOptionsJson != null) {
                isModified |= mBlog.bsetBlogOptions(blogOptionsJson);
            }
            // Software version
            if (!mBlog.isDotcomFlag()) {
                Map<?, ?> sv = (HashMap<?, ?>) blogOptions.get("software_version");
                String wpVersion = MapUtils.getMapStr(sv, "value");
                if (wpVersion.length() > 0) {
                    isModified |= mBlog.bsetWpVersion(wpVersion);
                }
            }
            // Featured image support
            Map<?, ?> featuredImageHash = (HashMap<?, ?>) blogOptions.get("post_thumbnail");
            if (featuredImageHash != null) {
                boolean featuredImageCapable = MapUtils.getMapBool(featuredImageHash, "value");
                isModified |= mBlog.bsetFeaturedImageCapable(featuredImageCapable);
            } else {
                isModified |= mBlog.bsetFeaturedImageCapable(false);
            }
            if (isModified && WordPress.getCurrentBlog() != null && WordPress.getCurrentBlog().isActive()) {
                WordPress.wpDB.saveBlog(mBlog);
            }
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
                    mBlog.save();
                }
            }
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            if (mBlog == null || mContext == null) {
                setError(ErrorType.INVALID_CURRENT_BLOG, "ApiHelper - invalid blog");
                return false;
            }
            boolean commentsOnly = params[0];
            XMLRPCClient client = new XMLRPCClient(mBlog.getUrl(), mBlog.getHttpuser(),
                    mBlog.getHttppassword());

            if (!commentsOnly) {
                // check the WP number if self-hosted
                Map<String, String> hPost = new HashMap<String, String>();
                hPost.put("software_version", "software_version");
                hPost.put("post_thumbnail", "post_thumbnail");
                hPost.put("jetpack_client_id", "jetpack_client_id");
                hPost.put("blog_public", "blog_public");
                hPost.put("home_url", "home_url");
                hPost.put("admin_url", "admin_url");
                hPost.put("login_url", "login_url");

                Object[] vParams = {mBlog.getRemoteBlogId(), mBlog.getUsername(),
                        mBlog.getPassword(), hPost};
                Object versionResult = null;
                try {
                    versionResult = client.call("wp.getOptions", vParams);
                } catch (XMLRPCException e) {
                    setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
                    return false;
                }

                if (versionResult != null) {
                    Map<?, ?> blogOptions = (HashMap<?, ?>) versionResult;
                    updateBlogOptions(blogOptions);
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
                Map<String, Object> userInfos = (HashMap<String, Object>)
                        client.call("wp.getProfile", userParams);
                updateBlogAdmin(userInfos);
            } catch (XMLRPCException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
                return false;
            }

            // refresh the comments
            Map<String, Object> hPost = new HashMap<String, Object>();
            hPost.put("number", 30);
            Object[] commentParams = {mBlog.getRemoteBlogId(), mBlog.getUsername(),
                    mBlog.getPassword(), hPost};
            try {
                ApiHelper.refreshComments(mContext, commentParams);
            } catch (XMLRPCException e) {
                setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(mCallback != null) {
                if (success) {
                    mCallback.onSuccess();
                } else {
                    mCallback.onFailure(mErrorType, mErrorMessage, mThrowable);
                }
            }
            refreshedBlogs.remove(mBlogIdentifier);
        }
    }

    public static Map<Integer, Map<?, ?>> refreshComments(Context ctx,Object[] commentParams)
            throws XMLRPCException {
        Blog blog = WordPress.getCurrentBlog();
        if (blog == null)
            return null;
        client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(),
                blog.getHttppassword());
        String author, postID, comment, status, authorEmail, authorURL, postTitle;
        int commentID;
        Map<Integer, Map<?, ?>> allComments = new HashMap<Integer, Map<?, ?>>();
        Map<?, ?> contentHash = new HashMap<Object, Object>();
        List<Map<?, ?>> dbVector = new Vector<Map<?, ?>>();

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
            Map<Object, Object> dbValues = new HashMap<Object, Object>();
            contentHash = (Map<?, ?>) result[ctr];
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

            String formattedDate = getFormattedCommentDate(ctx, d);

            dbValues.put("blogID", String.valueOf(blog.getLocalTableBlogId()));
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
     * nbradbury 11/15/13 - this code was originally in refreshComments() above, moved here
     * for re-usability
     * @param context
     * @param date
     * @return
     */
    public static String getFormattedCommentDate(Context context, java.util.Date date) {
        if (date == null)
            return "";
        try {
            int flags = 0;
            flags |= DateUtils.FORMAT_SHOW_DATE;
            flags |= DateUtils.FORMAT_ABBREV_MONTH;
            flags |= DateUtils.FORMAT_SHOW_YEAR;
            flags |= DateUtils.FORMAT_SHOW_TIME;
            return DateUtils.formatDateTime(context, date.getTime(), flags);
        } catch (Exception e) {
            return date.toString();
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
            client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog.getHttppassword());

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
            } catch (XMLRPCException e) {
                AppLog.e(T.API, e);
                // user does not have permission to view media gallery
                if (e.getMessage().contains("401")) {
                    setError(ErrorType.NO_UPLOAD_FILES_CAP, e.getMessage(), e);
                    return 0;
                }
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

            client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog.getHttppassword());

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
            } catch (XMLRPCException e) {
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

            client = new XMLRPCClient(blog.getUrl(),
                    blog.getHttpuser(),
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
            } catch (XMLRPCException e) {
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

            client = new XMLRPCClient(blog.getUrl(),
                    blog.getHttpuser(),
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
            } catch (XMLRPCException e) {
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

            client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog.getHttppassword());
            Object[] apiParams = new Object[]{blog.getRemoteBlogId(), blog.getUsername(),
                    blog.getPassword(), mMediaId};

            try {
                if (client != null) {
                    Boolean result = (Boolean) client.call("wp.deletePost", apiParams);
                    if (!result) {
                        setError(ErrorType.INVALID_RESULT, "wp.deletePost returned false");
                    }
                }
            } catch (XMLRPCException e) {
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

            client = new XMLRPCClient(blog.getUrl(),
                    blog.getHttpuser(),
                    blog.getHttppassword());

            Object[] apiParams = new Object[] {
                    blog.getRemoteBlogId(),
                    blog.getUsername(),
                    blog.getPassword(),
            };

            Map<?, ?> resultMap = null;
            try {
                resultMap = (HashMap<?, ?>) client.call("wpcom.getFeatures", apiParams);
            } catch (XMLRPCException e) {
                AppLog.e(T.API, "XMLRPCException: " + e.getMessage());
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
            try {
                return request.buffer();
            } catch (HttpRequestException e) {
                AppLog.e(T.API, "Cannot setup an InputStream on " + urlString, e);
            }
        }
        return null;
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
            try {
                String body = request.body();
                return body;
            } catch (HttpRequestException e) {
                AppLog.e(T.API, "Cannot load the content of " + urlString, e);
                return null;
            }
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
        if (urlString == null)
            return null;
        try {
            HttpRequest request = HttpRequest.get(urlString);

            // manually follow one additional redirect to support protocol switching
            if (request != null) {
                if (request.code() == HttpURLConnection.HTTP_MOVED_PERM
                        || request.code() == HttpURLConnection.HTTP_MOVED_TEMP) {
                    String location = request.location();
                    if (location != null) {
                        request = HttpRequest.get(location);
                    }
                }
            }

            return request;
        } catch (HttpRequestException e) {
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
    public static String getRSDMetaTagHrefRegEx(String urlString) {
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
    public static String getRSDMetaTagHref(String urlString) {
        // get the html code
        InputStream in = ApiHelper.getResponseStream(urlString);

        // parse the html and get the attribute for xmlrpc endpoint
        if (in != null) {
            XmlPullParser parser = Xml.newPullParser();
            try {
                // auto-detect the encoding from the stream
                parser.setInput(in, null);
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
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null; // never found the rsd tag
    }
}
