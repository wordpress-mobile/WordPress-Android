package org.xmlrpc.android;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.webkit.URLUtil;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.RedirectError;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.ui.media.MediaGridFragment.Filter;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLHandshakeException;

public class ApiHelper {

    public static final class Method {
        public static final String GET_MEDIA_LIBRARY  = "wp.getMediaLibrary";
        public static final String GET_POST_FORMATS   = "wp.getPostFormats";
        public static final String GET_CATEGORIES     = "wp.getCategories";
        public static final String GET_MEDIA_ITEM     = "wp.getMediaItem";
        public static final String GET_BLOGS          = "wp.getUsersBlogs";
        public static final String GET_OPTIONS        = "wp.getOptions";
        public static final String GET_PROFILE        = "wp.getProfile";
        public static final String GET_PAGES          = "wp.getPages";
        public static final String GET_TERM           = "wp.getTerm";
        public static final String GET_PAGE           = "wp.getPage";

        public static final String DELETE_PAGE        = "wp.deletePage";
        public static final String DELETE_POST        = "wp.deletePost";

        public static final String NEW_CATEGORY       = "wp.newCategory";

        public static final String EDIT_POST          = "wp.editPost";

        public static final String SET_OPTIONS        = "wp.setOptions";

        public static final String UPLOAD_FILE        = "wp.uploadFile";

        public static final String WPCOM_GET_FEATURES = "wpcom.getFeatures";

        public static final String LIST_METHODS       = "system.listMethods";
    }

    public static final class Param {
        public static final String SHOW_SUPPORTED_POST_FORMATS = "show-supported";
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

        protected void setError(@NonNull ErrorType errorType, String errorMessage) {
            mErrorMessage = errorMessage;
            mErrorType = errorType;
            AppLog.e(T.API, mErrorType.name() + " - " + mErrorMessage);
        }

        protected void setError(@NonNull ErrorType errorType, String errorMessage, Throwable throwable) {
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

    public interface DatabasePersistCallback {
        void onDataReadyToSave(List list);
    }

    public static class SyncMediaLibraryTask extends HelperAsyncTask<Void, Void, Integer> {
        public interface Callback extends GenericErrorCallback {
            public void onSuccess(int results);
        }

        private Callback mCallback;
        private int mOffset;
        private Filter mFilter;
        private SiteModel mSite;

        public SyncMediaLibraryTask(int offset, Filter filter, Callback callback, SiteModel site) {
            mOffset = offset;
            mCallback = callback;
            mFilter = filter;
            mSite = site;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (mSite == null) {
                setError(ErrorType.INVALID_CURRENT_BLOG, "ApiHelper - current blog is null");
                return 0;
            }

            String blogId = String.valueOf(mSite.getId());
            URI xmlrpcUri = URI.create(mSite.getXmlRpcUrl());
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(xmlrpcUri, "", "");
            Map<String, Object> filter = new HashMap<String, Object>();
            filter.put("number", 50);
            filter.put("offset", mOffset);

            if (mFilter == Filter.IMAGES) {
                filter.put("mime_type","image/*");
            } else if(mFilter == Filter.UNATTACHED) {
                filter.put("parent_id", 0);
            }

            Object[] apiParams = {String.valueOf(mSite.getSiteId()), StringUtils.notNullStr(mSite.getUsername()),
                    StringUtils.notNullStr(mSite.getPassword()), filter};

            Object[] results = null;
            try {
                results = (Object[]) client.call(Method.GET_MEDIA_LIBRARY, apiParams);
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
                MediaFile mediaFile = new MediaFile(blogId, resultMap, mSite.isWPCom());
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

    public static class EditMediaItemTask extends HelperAsyncTask<Void, Void, Boolean> {
        private GenericCallback mCallback;
        private String mMediaId;
        private String mTitle;
        private String mDescription;
        private String mCaption;
        private SiteModel mSite;

        public EditMediaItemTask(SiteModel site, String mediaId, String title, String description, String caption,
                                 GenericCallback callback) {
            mSite = site;
            mMediaId = mediaId;
            mCallback = callback;
            mTitle = title;
            mCaption = caption;
            mDescription = description;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: STORES: this will be replaced in MediaStore
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(URI.create(mSite.getXmlRpcUrl()), "", "");
            Map<String, Object> contentStruct = new HashMap<String, Object>();
            contentStruct.put("post_title", mTitle);
            contentStruct.put("post_content", mDescription);
            contentStruct.put("post_excerpt", mCaption);

            Object[] apiParams = {
                    String.valueOf(mSite.getSiteId()),
                    StringUtils.notNullStr(mSite.getUsername()),
                    StringUtils.notNullStr(mSite.getPassword()),
                    mMediaId,
                    contentStruct
            };

            Boolean result = null;
            try {
                result = (Boolean) client.call(Method.EDIT_POST, apiParams);
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

    public static class GetMediaItemTask extends HelperAsyncTask<Void, Void, MediaFile> {
        public interface Callback extends GenericErrorCallback {
            public void onSuccess(MediaFile results);
        }
        private Callback mCallback;
        private int mMediaId;
        private SiteModel mSite;

        public GetMediaItemTask(SiteModel site, int mediaId, Callback callback) {
            mSite = site;
            mMediaId = mediaId;
            mCallback = callback;
        }

        @Override
        protected MediaFile doInBackground(Void... params) {
            String blogId = String.valueOf(mSite.getId());

            XMLRPCClientInterface client = XMLRPCFactory.instantiate(URI.create(mSite.getXmlRpcUrl()), "", "");
            Object[] apiParams = {
                    String.valueOf(mSite.getSiteId()),
                    StringUtils.notNullStr(mSite.getUsername()),
                    StringUtils.notNullStr(mSite.getPassword()),
                    mMediaId
            };
            Map<?, ?> results = null;
            try {
                results = (Map<?, ?>) client.call(Method.GET_MEDIA_ITEM, apiParams);
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
                MediaFile mediaFile = new MediaFile(blogId, results, mSite.isWPCom());
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

    public static class UploadMediaTask extends HelperAsyncTask<Void, Void, Map<?, ?>> {
        public interface Callback extends GenericErrorCallback {
            void onSuccess(String remoteId, String remoteUrl, String secondaryId);
            void onProgressUpdate(float progress);
        }
        private Callback mCallback;
        private Context mContext;
        private MediaFile mMediaFile;
        private SiteModel mSite;

        public UploadMediaTask(Context applicationContext, SiteModel site, MediaFile mediaFile, Callback callback) {
            mContext = applicationContext;
            mSite = site;
            mMediaFile = mediaFile;
            mCallback = callback;
        }

        @Override
        protected Map<?, ?> doInBackground(Void... params) {
            final XMLRPCClientInterface client = XMLRPCFactory.instantiate(URI.create(mSite.getXmlRpcUrl()), "", "");
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("name", mMediaFile.getFileName());
            data.put("type", mMediaFile.getMimeType());
            data.put("bits", mMediaFile);
            data.put("overwrite", true);

            Object[] apiParams = {
                    String.valueOf(mSite.getSiteId()),
                    StringUtils.notNullStr(mSite.getUsername()),
                    StringUtils.notNullStr(mSite.getPassword()),
                    data
            };
            final File tempFile = getTempFile(mContext);

            if (client instanceof XMLRPCClient) {
                ((XMLRPCClient) client).setOnBytesUploadedListener(new XMLRPCClient.OnBytesUploadedListener() {
                    @Override
                    public void onBytesUploaded(long uploadedBytes) {
                        if (isCancelled()) {
                            // Stop the upload if the task has been cancelled
                            ((XMLRPCClient) client).cancel();
                        }

                        if (tempFile == null || tempFile.length() == 0) {
                            return;
                        }

                        float fractionUploaded = uploadedBytes / (float) tempFile.length();
                        mCallback.onProgressUpdate(fractionUploaded);
                    }
                });
            }

            if (mContext == null) {
                return null;
            }

            Map<?, ?> resultMap;
            try {
                resultMap = (HashMap<?, ?>) client.call(Method.UPLOAD_FILE, apiParams, tempFile);
            } catch (ClassCastException cce) {
                setError(ErrorType.INVALID_RESULT, null, cce);
                return null;
            } catch (XMLRPCFault e) {
                if (e.getFaultCode() == 401) {
                    setError(ErrorType.NETWORK_XMLRPC,
                            mContext.getString(R.string.media_error_no_permission_upload), e);
                } else {
                    // getFaultString() returns the error message from the server without the "[Code 403]" part.
                    setError(ErrorType.NETWORK_XMLRPC, e.getFaultString(), e);
                }
                return null;
            } catch (XMLRPCException e) {
                setError(ErrorType.NETWORK_XMLRPC, null, e);
                return null;
            } catch (IOException e) {
                setError(ErrorType.NETWORK_XMLRPC, null, e);
                return null;
            } catch (XmlPullParserException e) {
                setError(ErrorType.NETWORK_XMLRPC, null, e);
                return null;
            }

            if (resultMap != null && resultMap.containsKey("id")) {
                return resultMap;
            } else {
                setError(ErrorType.INVALID_RESULT, null);
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
        protected void onPostExecute(Map<?, ?> result) {
            if (mCallback != null) {
                if (result != null) {
                    String remoteId = (String) result.get("id");
                    String remoteUrl = (String) result.get("url");
                    String videoPressId = (String) result.get("videopress_shortcode");
                    mCallback.onSuccess(remoteId, remoteUrl, videoPressId);
                } else {
                    mCallback.onFailure(mErrorType, mErrorMessage, mThrowable);
                }
            }
        }
    }

    public static class DeleteMediaTask extends HelperAsyncTask<List<?>, Void, Void> {
        private GenericCallback mCallback;
        private String mMediaId;
        private SiteModel mSite;

        public DeleteMediaTask(SiteModel site, String mediaId, GenericCallback callback) {
            mMediaId = mediaId;
            mCallback = callback;
            mSite = site;
        }

        @Override
        protected Void doInBackground(List<?>... params) {
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(URI.create(mSite.getXmlRpcUrl()), "", "");
            Object[] apiParams = {
                    String.valueOf(mSite.getSiteId()),
                    StringUtils.notNullStr(mSite.getUsername()),
                    StringUtils.notNullStr(mSite.getPassword()),
                    mMediaId,
            };
            try {
                if (client != null) {
                    Boolean result = (Boolean) client.call(Method.DELETE_POST, apiParams);
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

    /**
     * Synchronous method to fetch the String content at the specified HTTP URL.
     *
     * @param stringUrl URL to fetch contents for.
     * @return content of the resource, or null if URL was invalid or resource could not be retrieved.
     */
    public static String getResponse(final String stringUrl) throws SSLHandshakeException, TimeoutError, TimeoutException {
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

    public static String getResponse(final String stringUrl, int numberOfRedirects) throws SSLHandshakeException, TimeoutError, TimeoutException {
        RequestFuture<String> future = RequestFuture.newFuture();
        StringRequest request = new StringRequest(stringUrl, future, future);
        request.setRetryPolicy(new DefaultRetryPolicy(XMLRPCClient.DEFAULT_SOCKET_TIMEOUT_MS, 0, 1));
        WordPress.requestQueue.add(request);
        try {
            return future.get(XMLRPCClient.DEFAULT_SOCKET_TIMEOUT_MS, TimeUnit.MILLISECONDS);
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
            } else if (e.getCause() != null && e.getCause() instanceof com.android.volley.TimeoutError) {
                AppLog.e(T.API, e);
                throw (com.android.volley.TimeoutError) e.getCause();
            } else {
                AppLog.e(T.API, e);
            }
        }
        return null;
    }
}
