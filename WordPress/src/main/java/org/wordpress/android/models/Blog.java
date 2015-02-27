//Manages data for blog settings

package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

public class Blog {
    private int localTableBlogId;
    private String url;
    private String homeURL;
    private String blogName;
    private String username;
    private String password;
    private String imagePlacement;
    private boolean featuredImageCapable;
    private boolean fullSizeImage;
    private boolean scaledImage;
    private int scaledImageWidth;
    private String maxImageWidth;
    private int maxImageWidthId;
    private int remoteBlogId;
    private String dotcom_username;
    private String dotcom_password;
    private String api_key;
    private String api_blogid;
    private boolean dotcomFlag;
    private String wpVersion;
    private String httpuser = "";
    private String httppassword = "";
    private String postFormats;
    private String blogOptions = "{}";
    private boolean isAdmin;
    private boolean isHidden;

    public Blog() {
    }

    public Blog(int localTableBlogId, String url, String homeURL, String blogName, String username, String password, String imagePlacement, boolean featuredImageCapable, boolean fullSizeImage, boolean scaledImage, int scaledImageWidth, String maxImageWidth, int maxImageWidthId, int remoteBlogId, String dotcom_username, String dotcom_password, String api_key, String api_blogid, boolean dotcomFlag, String wpVersion, String httpuser, String httppassword, String postFormats, String blogOptions, boolean isAdmin, boolean isHidden) {
        this.localTableBlogId = localTableBlogId;
        this.url = url;
        this.homeURL = homeURL;
        this.blogName = blogName;
        this.username = username;
        this.password = password;
        this.imagePlacement = imagePlacement;
        this.featuredImageCapable = featuredImageCapable;
        this.fullSizeImage = fullSizeImage;
        this.scaledImage = scaledImage;
        this.scaledImageWidth = scaledImageWidth;
        this.maxImageWidth = maxImageWidth;
        this.maxImageWidthId = maxImageWidthId;
        this.remoteBlogId = remoteBlogId;
        this.dotcom_username = dotcom_username;
        this.dotcom_password = dotcom_password;
        this.api_key = api_key;
        this.api_blogid = api_blogid;
        this.dotcomFlag = dotcomFlag;
        this.wpVersion = wpVersion;
        this.httpuser = httpuser;
        this.httppassword = httppassword;
        this.postFormats = postFormats;
        this.blogOptions = blogOptions;
        this.isAdmin = isAdmin;
        this.isHidden = isHidden;
    }

    public Blog(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.localTableBlogId = -1;
    }

    public int getLocalTableBlogId() {
        return localTableBlogId;
    }

    public void setLocalTableBlogId(int id) {
        this.localTableBlogId = id;
    }

    public String getNameOrHostUrl() {
        return (getBlogName() == null || getBlogName().isEmpty()) ? getUri().getHost() : getBlogName();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public URI getUri() {
        try {
            String url = getUrl();
            if (url == null) {
                AppLog.e(T.UTILS, "Blog url is null");
                return null;
            }
            return new URI(url);
        } catch (URISyntaxException e) {
            AppLog.e(T.UTILS, "Blog url is invalid: " + getUrl());
            return null;
        }
    }

    public String getHomeURL() {
        return homeURL;
    }

    public void setHomeURL(String homeURL) {
        this.homeURL = homeURL;
    }

    public String getBlogName() {
        return blogName;
    }

    public void setBlogName(String blogName) {
        this.blogName = blogName;
    }

    public String getUsername() {
        return StringUtils.notNullStr(username);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return StringUtils.notNullStr(password);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getImagePlacement() {
        return imagePlacement;
    }

    public void setImagePlacement(String imagePlacement) {
        this.imagePlacement = imagePlacement;
    }

    public boolean isFeaturedImageCapable() {
        return featuredImageCapable;
    }

    public void setFeaturedImageCapable(boolean isCapable) {
        this.featuredImageCapable = isCapable;
    }

    public boolean bsetFeaturedImageCapable(boolean isCapable) {
        if (featuredImageCapable == isCapable) {
            return false;
        }
        setFeaturedImageCapable(isCapable);
        return true;
    }

    public boolean isFullSizeImage() {
        return fullSizeImage;
    }

    public void setFullSizeImage(boolean fullSizeImage) {
        this.fullSizeImage = fullSizeImage;
    }

    public String getMaxImageWidth() {
        return StringUtils.notNullStr(maxImageWidth);
    }

    public void setMaxImageWidth(String maxImageWidth) {
        this.maxImageWidth = maxImageWidth;
    }

    public int getMaxImageWidthId() {
        return maxImageWidthId;
    }

    public void setMaxImageWidthId(int maxImageWidthId) {
        this.maxImageWidthId = maxImageWidthId;
    }

    public int getRemoteBlogId() {
        return remoteBlogId;
    }

    public void setRemoteBlogId(int blogId) {
        this.remoteBlogId = blogId;
    }

    public String getDotcom_username() {
        return dotcom_username;
    }

    public void setDotcom_username(String dotcomUsername) {
        dotcom_username = dotcomUsername;
    }

    public String getDotcom_password() {
        return dotcom_password;
    }

    public void setDotcom_password(String dotcomPassword) {
        dotcom_password = dotcomPassword;
    }

    public String getApi_key() {
        return api_key;
    }

    public void setApi_key(String apiKey) {
        api_key = apiKey;
    }

    public String getApi_blogid() {
        if (api_blogid == null) {
            JSONObject jsonOptions = getBlogOptionsJSONObject();
            if (jsonOptions!=null && jsonOptions.has("jetpack_client_id")) {
                try {
                    String jetpackBlogId = jsonOptions.getJSONObject("jetpack_client_id").getString("value");
                    if (!TextUtils.isEmpty(jetpackBlogId)) {
                        this.setApi_blogid(jetpackBlogId);
                        WordPress.wpDB.saveBlog(this);
                    }
                } catch (JSONException e) {
                    AppLog.e(T.UTILS, "Cannot load jetpack_client_id from options: " + jsonOptions, e);
                }
            }
        }
        return api_blogid;
    }

    public void setApi_blogid(String apiBlogid) {
        api_blogid = apiBlogid;
    }

    public boolean isDotcomFlag() {
        return dotcomFlag;
    }

    public void setDotcomFlag(boolean dotcomFlag) {
        this.dotcomFlag = dotcomFlag;
    }

    public String getWpVersion() {
        return wpVersion;
    }

    public void setWpVersion(String wpVersion) {
        this.wpVersion = wpVersion;
    }

    public boolean bsetWpVersion(String wpVersion) {
        if (StringUtils.equals(this.wpVersion, wpVersion)) {
            return false;
        }
        setWpVersion(wpVersion);
        return true;
    }

    public String getHttpuser() {
        return httpuser;
    }

    public void setHttpuser(String httpuser) {
        this.httpuser = httpuser;
    }

    public String getHttppassword() {
        return httppassword;
    }

    public void setHttppassword(String httppassword) {
        this.httppassword = httppassword;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean isHidden) {
        this.isHidden = isHidden;
    }

    public String getPostFormats() {
        return postFormats;
    }

    public void setPostFormats(String postFormats) {
        this.postFormats = postFormats;
    }

    public boolean bsetPostFormats(String postFormats) {
        if (StringUtils.equals(this.postFormats, postFormats)) {
            return false;
        }
        setPostFormats(postFormats);
        return true;
    }

    public boolean isScaledImage() {
        return scaledImage;
    }

    public void setScaledImage(boolean scaledImage) {
        this.scaledImage = scaledImage;
    }

    public int getScaledImageWidth() {
        return scaledImageWidth;
    }

    public void setScaledImageWidth(int scaledImageWidth) {
        this.scaledImageWidth = scaledImageWidth;
    }

    public String getBlogOptions() {
        return blogOptions;
    }

    public JSONObject getBlogOptionsJSONObject() {
        String optionsString = getBlogOptions();
        if (TextUtils.isEmpty(optionsString)) {
            return null;
        }
        try {
            return new JSONObject(optionsString);
        } catch (JSONException e) {
            AppLog.e(T.UTILS, "invalid blogOptions json", e);
        }
        return null;
    }

    public void setBlogOptions(String blogOptions) {
        this.blogOptions = blogOptions;
        JSONObject options = getBlogOptionsJSONObject();
        if (options == null) {
            this.blogOptions = "{}";
            options = getBlogOptionsJSONObject();
        }

        if (options.has("jetpack_client_id")) {
            try {
                String jetpackBlogId = options.getJSONObject("jetpack_client_id").getString("value");
                if (!TextUtils.isEmpty(jetpackBlogId)) {
                    this.setApi_blogid(jetpackBlogId);
                }
            } catch (JSONException e) {
                AppLog.e(T.UTILS, "Cannot load jetpack_client_id from options: " + blogOptions, e);
            }
        }
    }

    // TODO: it's ugly to compare json strings, we have to normalize both strings before
    // comparison or compare JSON objects after parsing
    public boolean bsetBlogOptions(String blogOptions) {
        if (StringUtils.equals(this.blogOptions, blogOptions)) {
            return false;
        }
        setBlogOptions(blogOptions);
        return true;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public boolean bsetAdmin(boolean isAdmin) {
        if (this.isAdmin == isAdmin) {
            return false;
        }
        setAdmin(isAdmin);
        return true;
    }

    public String getAdminUrl() {
        String adminUrl = null;
        JSONObject jsonOptions = getBlogOptionsJSONObject();
        if (jsonOptions != null) {
            try {
                adminUrl = jsonOptions.getJSONObject("admin_url").getString("value");
            } catch (JSONException e) {
                AppLog.e(T.UTILS, "Cannot load admin_url from options: " + jsonOptions, e);
            }
        }

        // Try to guess the URL of the dashboard if blogOptions is null (blog not added to the app), or WP version is < 3.6
        if (TextUtils.isEmpty(adminUrl)) {
            if (this.getUrl().lastIndexOf("/") != -1) {
                adminUrl = this.getUrl().substring(0, this.getUrl().lastIndexOf("/")) + "/wp-admin";
            } else {
                adminUrl = this.getUrl().replace("xmlrpc.php", "wp-admin");
            }
        }
        return adminUrl;
    }

    public boolean isPrivate() {
        JSONObject jsonOptions = getBlogOptionsJSONObject();
        if (jsonOptions != null && jsonOptions.has("blog_public")) {
            try {
                String blogPublicValue = jsonOptions.getJSONObject("blog_public").getString("value");
                if (!TextUtils.isEmpty(blogPublicValue) && "-1".equals(blogPublicValue)) {
                    return true;
                }
            } catch (JSONException e) {
                AppLog.e(T.UTILS, "Cannot load blog_public from options: " + jsonOptions, e);
            }
        }
        return false;
    }

    public boolean isJetpackPowered() {
        JSONObject jsonOptions = getBlogOptionsJSONObject();
        if (jsonOptions != null && jsonOptions.has("jetpack_client_id")) {
            return true;
        }
        return false;
    }

    public boolean isPhotonCapable() {
        return ((isDotcomFlag() && !isPrivate()) || (isJetpackPowered() && !hasValidHTTPAuthCredentials()));
    }

    public boolean hasValidJetpackCredentials() {
        return !TextUtils.isEmpty(getDotcom_username()) && !TextUtils.isEmpty(getDotcom_password());
    }

    public boolean hasValidHTTPAuthCredentials() {
        return !TextUtils.isEmpty(getHttppassword()) && !TextUtils.isEmpty(getHttpuser());
    }

    /**
     * Get the WordPress.com blog ID
     * Stored in blogId for WP.com, api_blogId for Jetpack
     *
     * @return WP.com blogId string, potentially null for Jetpack sites
     */
    public String getDotComBlogId() {
        if (isDotcomFlag())
            return String.valueOf(getRemoteBlogId());
        else
            return getApi_blogid();
    }
}
