package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.util.Log;
import android.webkit.URLUtil;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.Utils;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import java.net.URI;
import java.net.IDN;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public SetupBlog() {
    }

    public int getErrorMsgId() {
        return mErrorMsgId;
    }

    public String getXmlrpcUrl() {
        return mXmlrpcUrl;
    }

    public void setUsername(String mUsername) {
        this.mUsername = mUsername;
    }

    public void setPassword(String mPassword) {
        this.mPassword = mPassword;
    }

    public String getPassword() {
        return mPassword;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setHttpUsername(String mHttpUsername) {
        this.mHttpUsername = mHttpUsername;
    }

    public void setHttpPassword(String mHttpPassword) {
        this.mHttpPassword = mHttpPassword;
    }

    public void setSelfHostedURL(String mSelfHostedURL) {
        this.mSelfHostedURL = mSelfHostedURL;
    }

    public void setHttpAuthRequired(boolean mHttpAuthRequired) {
        this.mHttpAuthRequired = mHttpAuthRequired;
    }

    public boolean isHttpAuthRequired() {
        return mHttpAuthRequired;
    }

    public List<Map<String, Object>> getBlogList() {
        if (mSelfHostedURL != null && mSelfHostedURL.length() != 0) {
            mXmlrpcUrl = getSelfHostedXmlrpcUrl(mSelfHostedURL);
        } else {
            mXmlrpcUrl = Constants.wpcomXMLRPCURL;
        }

        if (mXmlrpcUrl == null) {
            if (!mHttpAuthRequired)
                mErrorMsgId = R.string.no_site_error;
            return null;
        }

        // Validate the URL found before calling the client. Prevent a crash that can occur
        // during the setup of self-hosted sites.
        try {
            URI.create(mXmlrpcUrl);
        } catch (Exception e1) {
            mErrorMsgId = R.string.no_site_error;
            return null;
        }

        XMLRPCClient client = new XMLRPCClient(mXmlrpcUrl, mHttpUsername, mHttpPassword);
        Object[] params = {mUsername, mPassword};
        try {
            Object[] userBlogs = (Object[]) client.call("wp.getUsersBlogs", params);
            Arrays.sort(userBlogs, Utils.BlogNameComparator);
            List<Map<String, Object>> userBlogList = new ArrayList<Map<String, Object>>();
            for (Object blog : userBlogs) {
                try {
                    userBlogList.add((Map<String, Object>) blog);
                } catch (ClassCastException e) {
                    Log.e(WordPress.TAG, "invalid date received from XMLRPC call wp.getUsersBlogs");
                }
            }
            return userBlogList;
        } catch (XMLRPCException e) {
            String message = e.getMessage();
            if (message.contains("code 403")) {
                mErrorMsgId = R.string.username_or_password_incorrect;
            } else if (message.contains("404")) {
                mErrorMsgId = R.string.xmlrpc_error;
            } else if (message.contains("425")) {
                mErrorMsgId = R.string.account_two_step_auth_enabled;
            } else {
                mErrorMsgId = R.string.no_network_message;
            }
            return null;
        }
    }

    // Attempts to retrieve the xmlrpc url for a self-hosted site, in this order:
    // 1: Try to retrieve it by finding the ?rsd url in the site's header
    // 2: Take whatever URL the user entered to see if that returns a correct response
    // 3: Finally, just guess as to what the xmlrpc url should be
    private String getSelfHostedXmlrpcUrl(String url) {
        String xmlrpcUrl = null;

        // Convert IDN names to punycode if necessary
        if (!Charset.forName("US-ASCII").newEncoder().canEncode(url)) {
            if (url.toLowerCase().startsWith("http://")) {
                url = "http://" + IDN.toASCII(url.substring(7));
            } else if (url.toLowerCase().startsWith("https://")) {
                url = "https://" + IDN.toASCII(url.substring(8));
            } else {
                url = IDN.toASCII(url);
            }
        }

        // Add http to the beginning of the URL if needed
        if (!(url.toLowerCase().startsWith("http://")) && !(url.toLowerCase().startsWith("https://"))) {
            url = "http://" + url; // default to http
        }

        if (!URLUtil.isValidUrl(url)) {
            mErrorMsgId = R.string.invalid_url_message;
            return null;
        }

        // Attempt to get the XMLRPC URL via RSD
        String rsdUrl = ApiHelper.getRSDMetaTagHrefRegEx(url);
        if (rsdUrl == null) {
            rsdUrl = ApiHelper.getRSDMetaTagHref(url);
        }

        if (rsdUrl != null) {
            xmlrpcUrl = ApiHelper.getXMLRPCUrl(rsdUrl);
            if (xmlrpcUrl == null)
                xmlrpcUrl = rsdUrl.replace("?rsd", "");
        } else {
            // Try the user entered path
            try {
                XMLRPCClient client = new XMLRPCClient(url, mHttpUsername, mHttpPassword);
                try {
                    client.call("system.listMethods");
                    xmlrpcUrl = url;
                    mIsCustomUrl = true;
                } catch (XMLRPCException e) {

                    if (e.getMessage().contains("401")) {
                        mHttpAuthRequired = true;
                        return null;
                    }

                    // Guess the xmlrpc path
                    String guessURL = url;
                    if (guessURL.substring(guessURL.length() - 1, guessURL.length()).equals("/")) {
                        guessURL = guessURL.substring(0, guessURL.length() - 1);
                    }
                    guessURL += "/xmlrpc.php";
                    client = new XMLRPCClient(guessURL, mHttpUsername, mHttpPassword);
                    try {
                        client.call("system.listMethods");
                        xmlrpcUrl = guessURL;
                    } catch (XMLRPCException ex) {
                    }
                }
            } catch (Exception e) {
            }
        }
        return xmlrpcUrl;
    }

    public Blog addBlog(String blogName, String xmlRpcUrl, String homeUrl, String blogId,
                        String username, String password, boolean isAdmin) {
        Blog blog = null;
        if (!WordPress.wpDB.isBlogInDatabase(Integer.parseInt(blogId), xmlRpcUrl)) {
            // The blog isn't in the app, so let's create it
            blog = new Blog(xmlRpcUrl, username, password);
            blog.setHomeURL(homeUrl);
            blog.setHttpuser(mHttpUsername);
            blog.setHttppassword(mHttpPassword);
            blog.setBlogName(blogName);
            blog.setImagePlacement(""); //deprecated
            blog.setFullSizeImage(false);
            blog.setMaxImageWidth(DEFAULT_IMAGE_SIZE);
            blog.setMaxImageWidthId(0); //deprecated
            blog.setRunService(false); //deprecated
            blog.setRemoteBlogId(Integer.parseInt(blogId));
            blog.setDotcomFlag(xmlRpcUrl.contains("wordpress.com"));
            blog.setWpVersion(""); // assigned later in getOptions call
            blog.setAdmin(isAdmin);
            blog.save();
        } else {
            // Update blog name
            int localTableBlogId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogIdAndXmlRpcUrl(
                    Integer.parseInt(blogId), xmlRpcUrl);
            try {
                blog = new Blog(localTableBlogId);
                if (!blogName.equals(blog.getBlogName())) {
                    blog.setBlogName(blogName);
                    blog.save();
                }
            } catch (Exception e) {
                Log.e(WordPress.TAG, "localTableBlogId: " + localTableBlogId + " not found");
            }
        }
        return blog;
    }

    /**
     * Remove blogs that are not in the list and add others
     * TODO: it's horribly slow due to datastructures used (List of Map), We should replace
     * that by a HashSet of a specialized Blog class (that supports comparison)
     */
    public void syncBlogs(Context context, List<Map<String, Object>> newBlogList) {
        // Add all blogs from blogList
        addBlogs(newBlogList);
        // Delete blogs if not in blogList
        List<Map<String, Object>> allBlogs = WordPress.wpDB.getAccountsBy("dotcomFlag=1", null);
        Set<String> newBlogURLs = new HashSet<String>();
        for (Map<String, Object> blog : newBlogList) {
            newBlogURLs.add(blog.get("xmlrpc").toString() + blog.get("blogid").toString());
        }
        for (Map<String, Object> blog : allBlogs) {
            if (!newBlogURLs.contains(blog.get("url").toString() + blog.get("blogId"))) {
                WordPress.wpDB.deleteAccount(context, Integer.parseInt(blog.get("id").toString()));
            }
        }
    }

    /**
     * Add selected blog(s) to the database
     */
    public void addBlogs(List<Map<String, Object>> blogList) {
        for (int i = 0; i < blogList.size(); i++) {
            Map<String, Object> blogMap = blogList.get(i);
            String blogName = StringUtils.unescapeHTML(blogMap.get("blogName").toString());
            String xmlrpcUrl = (mIsCustomUrl) ? mXmlrpcUrl : blogMap.get("xmlrpc").toString();
            String homeUrl = blogMap.get("url").toString();
            String blogId = blogMap.get("blogid").toString();
            boolean isAdmin = MapUtils.getMapBool(blogMap, "isAdmin");
            addBlog(blogName, xmlrpcUrl, homeUrl, blogId, mUsername, mPassword, isAdmin);
        }
    }
}

