package org.wordpress.android.ui.accounts;

import android.util.SparseBooleanArray;
import android.webkit.URLUtil;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.Utils;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public List getBlogList() {
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
            List<Object> userBlogsList = Arrays.asList(userBlogs);
            return userBlogsList;
        } catch (XMLRPCException e) {
            String message = e.getMessage();
            if (message.contains("code 403"))
                mErrorMsgId = R.string.update_credentials;
            else if (message.contains("404"))
                mErrorMsgId = R.string.xmlrpc_error;
            else if (message.contains("425"))
                mErrorMsgId = R.string.account_two_step_auth_enabled;
            return null;
        }
    }

    // Attempts to retrieve the xmlrpc url for a self-hosted site, in this order:
    // 1: Try to retrieve it by finding the ?rsd url in the site's header
    // 2: Take whatever URL the user entered to see if that returns a correct response
    // 3: Finally, just guess as to what the xmlrpc url should be
    private String getSelfHostedXmlrpcUrl(String url) {
        String xmlrpcUrl = null;
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

    // Add selected blog(s) to the database
    public void addBlogs(List fullBlogList, SparseBooleanArray selectedBlogs) {
        for (int i = 0; i < selectedBlogs.size(); i++) {
            if (selectedBlogs.get(selectedBlogs.keyAt(i)) == true) {
                int rowID = selectedBlogs.keyAt(i);
                Map blogMap = (HashMap) fullBlogList.get(rowID);
                String blogName = StringUtils.unescapeHTML(blogMap.get("blogName").toString());
                String xmlrpcUrl = (mIsCustomUrl) ? mXmlrpcUrl : blogMap.get("xmlrpc").toString();

                if (!WordPress.wpDB.checkForExistingBlog(blogName, xmlrpcUrl, mUsername,
                        mPassword)) {
                    // The blog isn't in the app, so let's create it
                    Blog blog = new Blog(xmlrpcUrl, mUsername, mPassword);
                    blog.setHomeURL(blogMap.get("url").toString());
                    blog.setHttpuser(mHttpUsername);
                    blog.setHttppassword(mHttpPassword);
                    blog.setBlogName(blogName);
                    blog.setImagePlacement(""); //deprecated
                    blog.setFullSizeImage(false);
                    blog.setMaxImageWidth(DEFAULT_IMAGE_SIZE);
                    blog.setMaxImageWidthId(5);
                    blog.setRunService(false); //deprecated
                    blog.setBlogId(Integer.parseInt(blogMap.get("blogid").toString()));
                    blog.setDotcomFlag(xmlrpcUrl.contains("wordpress.com"));
                    blog.setWpVersion(""); // assigned later in getOptions call
                    if (blog.save(null) && i == 0)
                        WordPress.setCurrentBlog(blog.getId());
                }
            }
        }
    }
}

