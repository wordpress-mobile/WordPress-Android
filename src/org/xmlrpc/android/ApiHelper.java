package org.xmlrpc.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Xml;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;

import org.xmlpull.v1.XmlPullParser;

import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.BlogIdentifier;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.models.FeatureSet;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.ui.media.MediaGridFragment.Filter;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.VolleyUtils;

public class ApiHelper {
    public enum ErrorType {NO_ERROR, INVALID_CURRENT_BLOG, NETWORK_XMLRPC, INVALID_CONTEXT,
        INVALID_RESULT, NO_UPLOAD_FILES_CAP, CAST_EXCEPTION}
    /** Called when the activity is first created. */
    private static XMLRPCClient client;

    public static final Map<String, String> blogOptionsXMLRPCParameters = new HashMap<String, String>();;

    static {
        blogOptionsXMLRPCParameters.put("software_version", "software_version");
        blogOptionsXMLRPCParameters.put("post_thumbnail", "post_thumbnail");
        blogOptionsXMLRPCParameters.put("jetpack_client_id", "jetpack_client_id");
        blogOptionsXMLRPCParameters.put("blog_public", "blog_public");
        blogOptionsXMLRPCParameters.put("home_url", "home_url");
        blogOptionsXMLRPCParameters.put("admin_url", "admin_url");
        blogOptionsXMLRPCParameters.put("login_url", "login_url");
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
        if (isModified) {
            WordPress.wpDB.saveBlog(currentBlog);
        }
    }

    public static class VerifyCredentialsCallback implements ApiHelper.GenericCallback {
        private final WeakReference<Activity> activityWeakRef;

        public VerifyCredentialsCallback(Activity refActivity) {
            this.activityWeakRef = new WeakReference<Activity>(refActivity);
        }

        @Override
        public void onSuccess() {
        }

        @Override
        public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
            Activity act = activityWeakRef.get();
            if (act == null || act.isFinishing()) {
                return;
            }
            ToastUtils.showToastOrAuthAlert(act, errorMessage, "An error occurred");
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

                Object[] vParams = {mBlog.getRemoteBlogId(), mBlog.getUsername(),
                        mBlog.getPassword(), hPost};
                Object versionResult = null;
                try {
                    versionResult = client.call("wp.getOptions", vParams);
                } catch (ClassCastException cce) {
                    setError(ErrorType.INVALID_RESULT, cce.getMessage(), cce);
                    return false;
                } catch (XMLRPCException e) {
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

    public static CommentList refreshComments(Context context, Object[] commentParams)
            throws XMLRPCException {
        Blog blog = WordPress.getCurrentBlog();
        if (blog == null) {
            return null;
        }
        XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                blog.getHttppassword());
        Object[] result;
        try {
            result = (Object[]) client.call("wp.getComments", commentParams);
        } catch (XMLRPCException e) {
            throw new XMLRPCException(e);
        }

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
                AppLog.e(T.API, e);
                // user does not have permission to view media gallery
                if (e.getMessage().contains("401")) {
                    setError(ErrorType.NO_UPLOAD_FILES_CAP, e.getMessage(), e);
                    return 0;
                } else {
                    setError(ErrorType.NETWORK_XMLRPC, e.getMessage(), e);
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
                AppLog.e(T.API, cce);
            } catch (XMLRPCException e) {
                AppLog.e(T.API, e);
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
    public static String getXMLRPCUrl(String urlString, boolean trustAllSslCertificates) throws SSLHandshakeException {
        Pattern xmlrpcLink = Pattern.compile("<api\\s*?name=\"WordPress\".*?apiLink=\"(.*?)\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        String html = getResponse(urlString, trustAllSslCertificates);
        if (html != null) {
            Matcher matcher = xmlrpcLink.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null; // never found the rsd tag
    }

    /**
     * Make volley and other libs based on HttpsURLConnection trust all ssl certificates (self signed or non
     * verified hostnames)
     * 
     */
    private static void trustAllSslCertificates(boolean trustAll) {
        try {
            if (trustAll) {
                SSLContext context = SSLContext.getInstance("SSL");
                context.init(null, VolleyUtils.trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String arg0, SSLSession arg1) {
                        return true;
                    }
                });
            } else {
                // use defaults
                HttpsURLConnection.setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory
                        .getDefault());
                HttpsURLConnection.setDefaultHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
            }
        } catch (NoSuchAlgorithmException e) {
            AppLog.e(T.API, e);
        } catch (KeyManagementException e) {
            AppLog.e(T.API, e);
        }
    }

    /**
     * Synchronous method to fetch the String content at the specified URL.
     *
     * @param url                     URL to fetch contents for.
     * @param trustAllSslCertificates if true ignore SSL errors
     * @return content of the resource, or null if URL was invalid or resource could not be retrieved.
     */
    public static String getResponse(final String url, boolean trustAllSslCertificates) throws SSLHandshakeException {
        return getResponse(url, trustAllSslCertificates, 3);
    }

    private static String getResponse(final String url, boolean trustAllSslCertificates, int maxRedirection)
            throws SSLHandshakeException {
        trustAllSslCertificates(trustAllSslCertificates);
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
                        return getResponse(newUrl, trustAllSslCertificates, maxRedirection - 1);
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
        } finally {
            trustAllSslCertificates(false);
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
    public static String getRSDMetaTagHrefRegEx(String urlString, boolean trustAllSslCertificates)
            throws SSLHandshakeException {
        String html = ApiHelper.getResponse(urlString, trustAllSslCertificates);
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
    public static String getRSDMetaTagHref(String urlString, boolean trustAllSslCertificates)
            throws SSLHandshakeException {
        // get the html code
        String data = ApiHelper.getResponse(urlString, trustAllSslCertificates);

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
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null; // never found the rsd tag
    }
}
