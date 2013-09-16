package org.xmlrpc.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.gson.Gson;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.FeatureSet;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.media.MediaGridFragment.Filter;
import org.wordpress.android.util.HttpRequest;
import org.wordpress.android.util.HttpRequest.HttpRequestException;

public class ApiHelper {
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
            AsyncTask<List<?>, Void, Object> {

        Context ctx;
        Blog blog;
        boolean isPage, loadMore;

        protected void onPostExecute(Object result) {
            try {
                Map<?, ?> postFormats = (HashMap<?, ?>) result;
                if (postFormats.size() > 0) {
                    Gson gson = new Gson();
                    String postFormatsJson = gson.toJson(postFormats);
                    if (postFormatsJson != null) {
                        blog.setPostFormats(postFormatsJson);
                        blog.save(null);
                    }
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

        @Override
        protected Object doInBackground(List<?>... args) {

            List<?> arguments = args[0];
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

    /**
     * Task to refresh blog level information (WP version number) and stuff
     * related to the active theme (available post types, recent comments, etc).
     */
    public static class RefreshBlogContentTask extends AsyncTask<Boolean, Void, Boolean> {
        /** Blog being refresh. */
        private Blog mBlog;

        /** Application context. */
        private Context mContext;

        /** Callback */
        public interface Callback {
            public void onSuccess();
            public void onFailure();
        }

        private Callback mCallback;

        public RefreshBlogContentTask(Context context, Blog blog, Callback callback) {
            mBlog = blog;
            mContext = context;
            mCallback = callback;
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
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
                
                Object[] vParams = {
                        mBlog.getBlogId(), mBlog.getUsername(), mBlog.getPassword(), hPost
                };
                Object versionResult = new Object();
                try {
                    versionResult = (Object) client.call("wp.getOptions", vParams);
                } catch (XMLRPCException e) {
                    return false;
                }

                if (versionResult != null) {
                    try {
                        Map<?, ?> blogOptions = (HashMap<?, ?>) versionResult;
                        Gson gson = new Gson();
                        String blogOptionsJson = gson.toJson(blogOptions);
                        if (blogOptionsJson != null)
                            mBlog.setBlogOptions(blogOptionsJson);

                        // Software version
                        if (!mBlog.isDotcomFlag()) {
                            Map<?, ?> sv = (HashMap<?, ?>) blogOptions.get("software_version");
                            String wpVersion = sv.get("value").toString();
                            if (wpVersion.length() > 0) {
                                mBlog.setWpVersion(wpVersion);
                            }
                        }
                        // Featured image support
                        Map<?, ?> featuredImageHash = (HashMap<?, ?>) blogOptions
                                .get("post_thumbnail");
                        if (featuredImageHash != null) {
                            boolean featuredImageCapable = Boolean.parseBoolean(featuredImageHash
                                    .get("value").toString());
                            mBlog.setFeaturedImageCapable(featuredImageCapable);
                        } else {
                            mBlog.setFeaturedImageCapable(false);
                        }
                        if (WordPress.getCurrentBlog() != null && WordPress.getCurrentBlog().isActive())
                            mBlog.save("");
                    } catch (Exception e) {
                    }
                }

                // get theme post formats
                List<Object> args = new Vector<Object>();
                args.add(mBlog);
                args.add(mContext);
                new ApiHelper.getPostFormatsTask().execute(args);
            }

            // Check if user is an admin
            Object[] userParams = {
                    mBlog.getBlogId(), mBlog.getUsername(), mBlog.getPassword()
            };
            try {
                Map<String, Object> userInfo = (HashMap<String, Object>) client.call("wp.getProfile", userParams);
                if (userInfo.containsKey("roles")) {
                    Object[] userRoles = (Object[])userInfo.get("roles");
                    mBlog.setAdmin(false);
                    for (int i = 0; i < userRoles.length; i++) {
                        if (userRoles[i].toString().equals("administrator")) {
                            mBlog.setAdmin(true);
                            break;
                        }
                    }
                    mBlog.save("");
                }
            } catch (XMLRPCException e) {
                return false;
            }

            // refresh the comments
            Map<String, Object> hPost = new HashMap<String, Object>();
            hPost.put("number", 30);
            Object[] commentParams = {
                    mBlog.getBlogId(), mBlog.getUsername(), mBlog.getPassword(), hPost
            };

            try {
                ApiHelper.refreshComments(mContext, commentParams);
            } catch (XMLRPCException e) {
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(mCallback != null) {
                if(!success)
                    mCallback.onFailure();
                else
                    mCallback.onSuccess();
            }
        }
    }

    public static Map<Integer, Map<?, ?>> refreshComments(Context ctx,
            Object[] commentParams) throws XMLRPCException {
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
    
    public static class SyncMediaLibraryTask extends AsyncTask<List<?>, Void, Integer> {

        public static final int NO_UPLOAD_FILES_CAP = -401; // error code is 401
        public static final int UNKNOWN_ERROR = -1;

        public interface Callback {
            public void onSuccess(int count);
            public void onFailure(int error_code);
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
            
            if(blog == null) {
                Log.e("WordPress", "ApiHelper - current blog is null");
                return -1;
            }

            String blogId = String.valueOf(blog.getBlogId());
            
            client = new XMLRPCClient(blog.getUrl(),
                    blog.getHttpuser(),
                    blog.getHttppassword());
            

            Map<String, Object> filter = new HashMap<String, Object>();
            filter.put("number", 50);
            filter.put("offset", mOffset);
            
            if (mFilter == Filter.IMAGES)
                filter.put("mime_type","image/*");
            else if(mFilter == Filter.UNATTACHED)
                filter.put("parent_id", 0);
                
            
            Object[] apiParams = { 
                    blog.getBlogId(),
                    blog.getUsername(),
                    blog.getPassword(),
                    filter
            };
            
            Object[] results = null;
            try {
                results = (Object[]) client.call("wp.getMediaLibrary", apiParams);
            } catch (XMLRPCException e) {
                Log.e("WordPress", e.getMessage());
                if (e.getMessage().contains("401")) { // user does not have permission to view media gallery
                    return NO_UPLOAD_FILES_CAP;
                }
            }
            
            if(results != null && blogId != null) {
                
                Map<?, ?> resultMap;
                
                // results returned, so mark everything existing to deleted
                // since offset is 0, we are doing a full refresh
                if (mOffset == 0) {
                    WordPress.wpDB.setMediaFilesMarkedForDeleted(blogId);
                }
               
                for(Object result : results) {
                    resultMap = (Map<?, ?>) result;
                    MediaFile mediaFile = new MediaFile(blogId, resultMap);
                    WordPress.wpDB.saveMediaFile(mediaFile);
                }
                
                WordPress.wpDB.deleteFilesMarkedForDeleted(blogId);
                
                return results.length;
            }
            
            return UNKNOWN_ERROR;
        }
        
        @Override
        protected void onPostExecute(Integer result) {
            if(mCallback != null) {
                if(result == null || result < 0)
                    mCallback.onFailure(result);
                else
                    mCallback.onSuccess(result);
            }
        }
        
    }
    
    public static class EditMediaItemTask extends AsyncTask<List<?>, Void, Boolean> {
        
        public interface Callback {
            public void onSuccess();
            public void onFailure();
        }
        
        private Callback mCallback;
        private String mMediaId;
        private String mTitle;
        private String mDescription;
        private String mCaption;
        
        public EditMediaItemTask(String mediaId, String title, String description, String caption, Callback callback) {
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
                Log.e("WordPress", "ApiHelper - current blog is null");
                return null;
            }
                        
            client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog.getHttppassword());
            
            Map<String, Object> contentStruct = new HashMap<String, Object>();
            contentStruct.put("post_title", mTitle);
            contentStruct.put("post_content", mDescription);
            contentStruct.put("post_excerpt", mCaption);
            
            Object[] apiParams = {
                    blog.getBlogId(),
                    blog.getUsername(),
                    blog.getPassword(),
                    mMediaId,
                    contentStruct
            };
            
            Boolean result = null;
            try {
                result = (Boolean) client.call("wp.editPost", apiParams);
            } catch (XMLRPCException e) {
                Log.e("WordPress", "XMLRPCException: " + e.getMessage());
            }
            
            return result;
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            if (mCallback != null) {
                if (result == null || !result)
                    mCallback.onFailure();
                else
                    mCallback.onSuccess();
            }
        }

    }
    
    public static class GetMediaItemTask extends AsyncTask<List<?>, Void, MediaFile> {

        public interface Callback {
            public void onSuccess(MediaFile mediaFile);
            public void onFailure();
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
            
            if(blog == null) {
                Log.e("WordPress", "ApiHelper - current blog is null");
                return null;
            }
            
            String blogId = String.valueOf(blog.getBlogId());
            
            client = new XMLRPCClient(blog.getUrl(),
                    blog.getHttpuser(),
                    blog.getHttppassword());
            
            Object[] apiParams = { 
                    blog.getBlogId(),
                    blog.getUsername(),
                    blog.getPassword(),
                    mMediaId
            };
            

            
            Map<?, ?> results = null;
            try {
                results = (Map<?, ?>) client.call("wp.getMediaItem", apiParams);
            } catch (XMLRPCException e) {
                Log.e("WordPress", e.getMessage());
            }
            
            if(results != null && blogId != null) {
                MediaFile mediaFile = new MediaFile(blogId, results);
                mediaFile.save();
                return mediaFile;
            } else {
                return null;
            }
            
        }
        
        @Override
        protected void onPostExecute(MediaFile result) {
            if(mCallback != null) {
                if(result != null)
                    mCallback.onSuccess(result);
                else
                    mCallback.onFailure();
            }
        }
        
    }
    
    public static class UploadMediaTask extends AsyncTask<List<?>, Void, String> {
        
        private Callback mCallback;
        private Context mContext;
        private MediaFile mMediaFile;
        
        public interface Callback {
            public void onSuccess(String id);
            public void onFailure();
        }
        
        public UploadMediaTask(Context applicationContext, MediaFile mediaFile, Callback callback) {
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
                Log.e("WordPress", "UploadMediaTask: ApiHelper - current blog is null");
                return null;
            }

            client = new XMLRPCClient(blog.getUrl(),
                    blog.getHttpuser(),
                    blog.getHttppassword());
         
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("name", mMediaFile.getFileName());
            data.put("type", mMediaFile.getMIMEType());
            data.put("bits", mMediaFile);
            data.put("overwrite", true);
            
            Object[] apiParams = { 
                    1, 
                    blog.getUsername(),
                    blog.getPassword(),
                    data
            };
            
            if (mContext == null)
                return null;
            
            Map<?, ?> resultMap = null;
            try {
                resultMap = (HashMap<?, ?>) client.call("wp.uploadFile", apiParams, getTempFile(mContext));
            } catch (XMLRPCException e) {
                Log.e("WordPress", "XMLRPCException: " + e.getMessage());
            }
            
            if (resultMap != null && resultMap.containsKey("id"))
                return (String) resultMap.get("id");

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
                if (result != null)
                    mCallback.onSuccess(result);
                else
                    mCallback.onFailure();
            }
        }

    }

    public static class DeleteMediaTask extends AsyncTask<List<?>, Void, Boolean> {
        private Callback mCallback;
        private String mMediaId;
        
        public interface Callback {
            public void onSuccess();
            public void onFailure();
        }
        
        public DeleteMediaTask(String mediaId, Callback callback) {
            mMediaId = mediaId;
            mCallback = callback;
        }
        
        @Override
        protected Boolean doInBackground(List<?>... params) {
            
            List<?> arguments = params[0];
            Blog blog = (Blog) arguments.get(0);
            if (blog == null)
                return false;
            

            client = new XMLRPCClient(blog.getUrl(),
                    blog.getHttpuser(),
                    blog.getHttppassword());
            
            Object[] apiParams = new Object[] {
                    blog.getBlogId(),
                    blog.getUsername(),
                    blog.getPassword(),
                    mMediaId
            };
            
            Boolean result = null;
            try {
                if (client != null)
                    result = (Boolean) client.call("wp.deletePost", apiParams);
            } catch (XMLRPCException e) {
                Log.e("WordPress", "XMLRPCException: " + e.getMessage());
            }
            
            return result;
            
        }
        
        @Override
        protected void onPostExecute(Boolean b) {
            if (mCallback != null) {
                if (b == null || !b)
                    mCallback.onFailure();
                else
                    mCallback.onSuccess();
            }
        }
    }
    
    public static class GetFeatures extends AsyncTask<List<?>, Void, FeatureSet> {

        public interface Callback {
            void onResult(FeatureSet featureSet);
        }

        private Callback mCallback;
        
        public GetFeatures(Callback callback) {
            mCallback = callback;
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
                    blog.getBlogId(),
                    blog.getUsername(),
                    blog.getPassword(),
            };
            
            Map<?, ?> resultMap = null;
            try {
                resultMap = (HashMap<?, ?>) client.call("wpcom.getFeatures", apiParams);
            } catch (XMLRPCException e) {
                Log.e("WordPress", "XMLRPCException: " + e.getMessage());
            }
            
            if (resultMap != null) {
                return new FeatureSet(blog.getBlogId(), resultMap);
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
            try {
                String body = request.body();
                return body;
            } catch (HttpRequestException e) {
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
}
