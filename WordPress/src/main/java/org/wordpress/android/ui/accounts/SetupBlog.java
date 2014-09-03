package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.webkit.URLUtil;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;
import org.xmlrpc.android.XMLRPCFault;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

public class SetupBlog {
    private static final String DEFAULT_IMAGE_SIZE = "2000";
    private String mUsername;
    private String mPassword;
    private String mHttpUsername = "";
    private String mHttpPassword = "";
    private String mXmlrpcUrl;

    private int mErrorMsgId;
    private boolean mIsCustomUrl;
    private String mSelfHostedURL;

    private boolean mHttpAuthRequired;
    private boolean mErroneousSslCertificate;

    public SetupBlog() {
    }

    public int getErrorMsgId() {
        return mErrorMsgId;
    }

    public String getXmlrpcUrl() {
        return mXmlrpcUrl;
    }

    public void setUsername(String username) {
        mUsername = username;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public String getPassword() {
        return mPassword;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setHttpUsername(String httpUsername) {
        mHttpUsername = httpUsername;
    }

    public void setHttpPassword(String httpPassword) {
        mHttpPassword = httpPassword;
    }

    public void setSelfHostedURL(String selfHostedURL) {
        mSelfHostedURL = selfHostedURL;
    }

    public void setHttpAuthRequired(boolean httpAuthRequired) {
        mHttpAuthRequired = httpAuthRequired;
    }

    public boolean isHttpAuthRequired() {
        return mHttpAuthRequired;
    }

    public boolean isErroneousSslCertificates() {
        return mErroneousSslCertificate;
    }

    public boolean isDotComBlog() {
        return mUsername != null && mPassword != null && mSelfHostedURL == null;
    }

    private void handleXmlRpcFault(XMLRPCFault xmlRpcFault) {
        AppLog.e(T.NUX, "XMLRPCFault received from XMLRPC call wp.getUsersBlogs", xmlRpcFault);
        switch (xmlRpcFault.getFaultCode()) {
            case 403:
                mErrorMsgId = R.string.username_or_password_incorrect;
                break;
            case 404:
                mErrorMsgId = R.string.xmlrpc_error;
                break;
            case 425:
                mErrorMsgId = R.string.account_two_step_auth_enabled;
                break;
            default:
                mErrorMsgId = R.string.no_site_error;
                break;
        }
    }

    private List<Map<String, Object>> getUsersBlogsRequest(URI uri) {
        XMLRPCClientInterface client = XMLRPCFactory.instantiate(uri, mHttpUsername, mHttpPassword);
        Object[] params = {mUsername, mPassword};
        try {
            Object[] userBlogs = (Object[]) client.call("wp.getUsersBlogs", params);
            if (userBlogs == null) {
                // Could happen if the returned server response is truncated
                mErrorMsgId = R.string.xmlrpc_error;
                return null;
            }
            Arrays.sort(userBlogs, BlogUtils.BlogNameComparator);
            List<Map<String, Object>> userBlogList = new ArrayList<Map<String, Object>>();
            for (Object blog : userBlogs) {
                try {
                    userBlogList.add((Map<String, Object>) blog);
                } catch (ClassCastException e) {
                    AppLog.e(T.NUX, "invalid data received from XMLRPC call wp.getUsersBlogs");
                }
            }
            return userBlogList;
        } catch (XmlPullParserException parserException) {
            mErrorMsgId = R.string.xmlrpc_error;
            AppLog.e(T.NUX, "invalid data received from XMLRPC call wp.getUsersBlogs", parserException);
        } catch (XMLRPCFault xmlRpcFault) {
            handleXmlRpcFault(xmlRpcFault);
        } catch (XMLRPCException xmlRpcException) {
            AppLog.e(T.NUX, "XMLRPCException received from XMLRPC call wp.getUsersBlogs", xmlRpcException);
            mErrorMsgId = R.string.no_site_error;
        } catch (SSLHandshakeException e) {
            if (!UrlUtils.getDomainFromUrl(mXmlrpcUrl).endsWith("wordpress.com")) {
                mErroneousSslCertificate = true;
            }
            AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
        } catch (IOException e) {
            AppLog.e(T.NUX, "Exception received from XMLRPC call wp.getUsersBlogs", e);
            mErrorMsgId = R.string.no_site_error;
        }
        return null;
    }

    public List<Map<String, Object>> getBlogList() {
        if (mSelfHostedURL != null && mSelfHostedURL.length() != 0) {
            mXmlrpcUrl = getSelfHostedXmlrpcUrl(mSelfHostedURL);
        } else {
            mXmlrpcUrl = Constants.wpcomXMLRPCURL;
        }

        if (mXmlrpcUrl == null) {
            if (!mHttpAuthRequired && mErrorMsgId == 0) {
                mErrorMsgId = R.string.no_site_error;
            }
            return null;
        }

        // Validate the URL found before calling the client. Prevent a crash that can occur
        // during the setup of self-hosted sites.
        URI uri;
        try {
            uri = URI.create(mXmlrpcUrl);
            return getUsersBlogsRequest(uri);
        } catch (Exception e) {
            mErrorMsgId = R.string.no_site_error;
            return null;
        }
    }

    private String getRsdUrl(String baseUrl) throws SSLHandshakeException {
        String rsdUrl;
        rsdUrl = ApiHelper.getRSDMetaTagHrefRegEx(baseUrl);
        if (rsdUrl == null) {
            rsdUrl = ApiHelper.getRSDMetaTagHref(baseUrl);
        }
        return rsdUrl;
    }

    private boolean isHTTPAuthErrorMessage(Exception e) {
        if (e != null && e.getMessage() != null && e.getMessage().contains("401")) {
            mHttpAuthRequired = true;
            return mHttpAuthRequired;
        }
        return false;
    }

    private String getmXmlrpcByUserEnteredPath(String baseUrl) {
        String xmlRpcUrl = null;
        if (!UrlUtils.isValidUrlAndHostNotNull(baseUrl)) {
            AppLog.e(T.NUX, "invalid URL: " + baseUrl);
            mErrorMsgId = R.string.invalid_url_message;
            return null;
        }
        URI uri = URI.create(baseUrl);
        XMLRPCClientInterface client = XMLRPCFactory.instantiate(uri, mHttpUsername, mHttpPassword);
        try {
            client.call("system.listMethods");
            xmlRpcUrl = baseUrl;
            mIsCustomUrl = true;
            return xmlRpcUrl;
        } catch (XMLRPCException e) {
            AppLog.i(T.NUX, "system.listMethods failed on: " + baseUrl);
            if (isHTTPAuthErrorMessage(e)) {
                return null;
            }
        } catch (SSLHandshakeException e) {
            if (!UrlUtils.getDomainFromUrl(baseUrl).endsWith("wordpress.com")) {
                mErroneousSslCertificate = true;
            }
            AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
            return null;
        } catch (SSLPeerUnverifiedException e) {
            if (!UrlUtils.getDomainFromUrl(baseUrl).endsWith("wordpress.com")) {
                mErroneousSslCertificate = true;
            }
            AppLog.w(T.NUX, "SSLPeerUnverifiedException failed. Erroneous SSL certificate detected.");
            return null;
        } catch (IOException e) {
            AppLog.i(T.NUX, "system.listMethods failed on: " + baseUrl);
            if (isHTTPAuthErrorMessage(e)) {
                return null;
            }
        } catch (XmlPullParserException e) {
            AppLog.i(T.NUX, "system.listMethods failed on: " + baseUrl);
            if (isHTTPAuthErrorMessage(e)) {
                return null;
            }
        }

        // Guess the xmlrpc path
        String guessURL = baseUrl;
        if (guessURL.substring(guessURL.length() - 1, guessURL.length()).equals("/")) {
            guessURL = guessURL.substring(0, guessURL.length() - 1);
        }
        guessURL += "/xmlrpc.php";
        uri = URI.create(guessURL);
        client = XMLRPCFactory.instantiate(uri, mHttpUsername, mHttpPassword);
        try {
            client.call("system.listMethods");
            xmlRpcUrl = guessURL;
            return xmlRpcUrl;
        } catch (XMLRPCException e) {
            AppLog.e(T.NUX, "system.listMethods failed on: " + guessURL, e);
        } catch (SSLHandshakeException e) {
            if (!UrlUtils.getDomainFromUrl(baseUrl).endsWith("wordpress.com")) {
                mErroneousSslCertificate = true;
            }
            AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
            return null;
        } catch (SSLPeerUnverifiedException e) {
            if (!UrlUtils.getDomainFromUrl(baseUrl).endsWith("wordpress.com")) {
                mErroneousSslCertificate = true;
            }
            AppLog.w(T.NUX, "SSLPeerUnverifiedException failed. Erroneous SSL certificate detected.");
            return null;
        } catch (IOException e) {
            AppLog.e(T.NUX, "system.listMethods failed on: " + guessURL, e);
        } catch (XmlPullParserException e) {
            AppLog.e(T.NUX, "system.listMethods failed on: " + guessURL, e);
        }

        return null;
    }

    // Attempts to retrieve the xmlrpc url for a self-hosted site, in this order:
    // 1: Try to retrieve it by finding the ?rsd url in the site's header
    // 2: Take whatever URL the user entered to see if that returns a correct response
    // 3: Finally, just guess as to what the xmlrpc url should be
    private String getSelfHostedXmlrpcUrl(String url) {
        String xmlrpcUrl;

        // Convert IDN names to punycode if necessary
        url = UrlUtils.convertUrlToPunycodeIfNeeded(url);

        // Add http to the beginning of the URL if needed
        url = UrlUtils.addUrlSchemeIfNeeded(url, false);

        if (!URLUtil.isValidUrl(url)) {
            mErrorMsgId = R.string.invalid_url_message;
            return null;
        }

        // Attempt to get the XMLRPC URL via RSD
        String rsdUrl;
        try {
            rsdUrl = getRsdUrl(url);
        } catch (SSLHandshakeException e) {
            if (!UrlUtils.getDomainFromUrl(url).endsWith("wordpress.com")) {
                mErroneousSslCertificate = true;
            }
            AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
            return null;
        }

        try {
            if (rsdUrl != null) {
                xmlrpcUrl = ApiHelper.getXMLRPCUrl(rsdUrl);
                if (xmlrpcUrl == null) {
                    xmlrpcUrl = rsdUrl.replace("?rsd", "");
                }
            } else {
                xmlrpcUrl = getmXmlrpcByUserEnteredPath(url);
            }
        } catch (SSLHandshakeException e) {
            if (!UrlUtils.getDomainFromUrl(url).endsWith("wordpress.com")) {
                mErroneousSslCertificate = true;
            }
            AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
            return null;
        }

        return xmlrpcUrl;
    }

    /**
     * Add a new blog or update a blog name in local DB.
     *
     * @return true if a new blog has been added or an old blog has been updated.
     * Return false if no change has been made.
     */
    public boolean addOrUpdateBlog(String blogName, String xmlRpcUrl, String homeUrl, String blogId, String username,
                                   String password, boolean isAdmin) {
        Blog blog;
        if (!WordPress.wpDB.isBlogInDatabase(Integer.parseInt(blogId), xmlRpcUrl)) {
            // The blog isn't in the app, so let's create it
            blog = new Blog(xmlRpcUrl, username, password);
            blog.setHomeURL(homeUrl);
            blog.setHttpuser(mHttpUsername);
            blog.setHttppassword(mHttpPassword);
            blog.setBlogName(blogName);
            // deprecated
            blog.setImagePlacement("");
            blog.setFullSizeImage(false);
            blog.setMaxImageWidth(DEFAULT_IMAGE_SIZE);
            // deprecated
            blog.setMaxImageWidthId(0);
            blog.setRemoteBlogId(Integer.parseInt(blogId));
            blog.setDotcomFlag(xmlRpcUrl.contains("wordpress.com"));
            // assigned later in getOptions call
            blog.setWpVersion("");
            blog.setAdmin(isAdmin);
            WordPress.wpDB.saveBlog(blog);
            return true;
        } else {
            // Update blog name
            int localTableBlogId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogIdAndXmlRpcUrl(
                    Integer.parseInt(blogId), xmlRpcUrl);
            try {
                blog = WordPress.wpDB.instantiateBlogByLocalId(localTableBlogId);
                if (!blogName.equals(blog.getBlogName())) {
                    blog.setBlogName(blogName);
                    WordPress.wpDB.saveBlog(blog);
                    return true;
                }
            } catch (Exception e) {
                AppLog.e(T.NUX, "localTableBlogId: " + localTableBlogId + " not found");
            }
            return false;
        }
    }

    /**
     * Remove blogs that are not in the list and add others
     * TODO: it's horribly slow due to datastructures used (List of Map), We should replace
     * that by a HashSet of a specialized Blog class (that supports comparison)
     *
     * @return true if a change has been made (new blog added, old blog updated, blog deleted).
     */
    public boolean syncBlogs(Context context, List<Map<String, Object>> newBlogList) {
        boolean retValue;

        // Add all blogs from blogList
        retValue = addBlogs(newBlogList);

        // Delete blogs if not in blogList
        List<Map<String, Object>> allBlogs = WordPress.wpDB.getAccountsBy("dotcomFlag=1", null);
        Set<String> newBlogURLs = new HashSet<String>();
        for (Map<String, Object> blog : newBlogList) {
            newBlogURLs.add(blog.get("xmlrpc").toString() + blog.get("blogid").toString());
        }
        for (Map<String, Object> blog : allBlogs) {
            if (!newBlogURLs.contains(blog.get("url").toString() + blog.get("blogId"))) {
                WordPress.wpDB.deleteAccount(context, Integer.parseInt(blog.get("id").toString()));
                retValue = true;
            }
        }
        return retValue;
    }

    /**
     * Add selected blog(s) to the database.
     *
     * @return true if a change has been made (new blog added or old blog updated).
     */
    public boolean addBlogs(List<Map<String, Object>> blogList) {
        boolean retValue = false;
        for (int i = 0; i < blogList.size(); i++) {
            Map<String, Object> blogMap = blogList.get(i);
            String blogName = StringUtils.unescapeHTML(blogMap.get("blogName").toString());
            String xmlrpcUrl = (mIsCustomUrl) ? mXmlrpcUrl : blogMap.get("xmlrpc").toString();
            if (!UrlUtils.isValidUrlAndHostNotNull(xmlrpcUrl)) {
                // xmlrpcUrl is invalid, set the error message
                mErrorMsgId = R.string.invalid_xmlrpc_url;
                AppLog.e(T.NUX, "Invalid XMLRPC url: " + xmlrpcUrl);
                return retValue;
            }
            String homeUrl = blogMap.get("url").toString();
            String blogId = blogMap.get("blogid").toString();
            boolean isAdmin = MapUtils.getMapBool(blogMap, "isAdmin");
            retValue |= addOrUpdateBlog(blogName, xmlrpcUrl, homeUrl, blogId, mUsername, mPassword, isAdmin);
        }
        return retValue;
    }
}
