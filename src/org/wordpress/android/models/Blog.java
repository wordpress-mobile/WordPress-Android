//Manages data for blog settings

package org.wordpress.android.models;

import java.util.List;

import org.wordpress.android.WordPress;

public class Blog {

    private int id;
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
    private int lastCommentId;
    private boolean runService;
    private int blogId;
    private boolean location;
    private String dotcom_username;
    private String dotcom_password;
    private String api_key;
    private String api_blogid;
    private boolean dotcomFlag;
    private String wpVersion;
    private String httpuser;
    private String httppassword;
    private String postFormats;
    private String blogOptions;
    private boolean isAdmin;

    public Blog(int blog_id) throws Exception{
        //instantiate a new blog
        List<Object> blogVals = WordPress.wpDB.loadSettings(blog_id);

        if (blogVals != null) {
            this.id = blog_id;
            this.url = blogVals.get(0).toString();
            this.blogName = blogVals.get(1).toString();
            this.username = blogVals.get(2).toString();
            this.password = blogVals.get(3).toString();
            this.httpuser = blogVals.get(4).toString();
            this.httppassword = blogVals.get(5).toString();
            this.imagePlacement = blogVals.get(6).toString();
            this.featuredImageCapable = (Integer)blogVals.get(7)>0;
            this.fullSizeImage = (Integer)blogVals.get(8)>0;
            this.maxImageWidth = blogVals.get(9).toString();
            this.maxImageWidthId = (Integer) blogVals.get(10);
            this.runService = (Integer)blogVals.get(11)>0;
            this.blogId = (Integer) blogVals.get(12);
            this.location = (Integer)blogVals.get(13)>0;
            this.dotcomFlag = (Integer)blogVals.get(14)>0;
            //these were accidentally set up to contain null values :(
            if (blogVals.get(15) != null)
                this.dotcom_username = blogVals.get(15).toString();
            if (blogVals.get(16) != null)
                this.dotcom_password = blogVals.get(16).toString();
            if (blogVals.get(17) != null)
                this.api_key = blogVals.get(17).toString();
            if (blogVals.get(18) != null)
                this.api_blogid = blogVals.get(18).toString();
            if (blogVals.get(19) != null)
                this.wpVersion = blogVals.get(19).toString();
            this.postFormats = blogVals.get(20).toString();
            this.lastCommentId = (Integer)blogVals.get(21);
            if(blogVals.get(22)!=null)
                this.scaledImage = (Integer)blogVals.get(22)>0;
            if(blogVals.get(23)!=null)
                this.scaledImageWidth = (Integer)blogVals.get(23);
            this.homeURL = blogVals.get(24).toString();
            if(blogVals.get(25) !=null && blogVals.get(25).toString().length() > 0)
                this.blogOptions = blogVals.get(25).toString();
            else
                this.blogOptions = "";
            if (blogVals.get(26) != null && (Integer) blogVals.get(26) > 0)
                this.setAdmin(true);
        } else {
            throw new Exception();
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
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

    public boolean isFullSizeImage() {
        return fullSizeImage;
    }

    public void setFullSizeImage(boolean fullSizeImage) {
        this.fullSizeImage = fullSizeImage;
    }

    public String getMaxImageWidth() {
        return maxImageWidth;
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

    public int getLastCommentId() {
        return lastCommentId;
    }

    public void setLastCommentId(int lastCommentId) {
        this.lastCommentId = lastCommentId;
    }

    public boolean isRunService() {
        return runService;
    }

    public void setRunService(boolean runService) {
        this.runService = runService;
    }

    public int getBlogId() {
        return blogId;
    }

    public void setBlogId(int blogId) {
        this.blogId = blogId;
    }

    public boolean isLocation() {
        return location;
    }

    public void setLocation(boolean location) {
        this.location = location;
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

    public void save(String originalUsername) {
        //save blog to db
        WordPress.wpDB.saveSettings(String.valueOf(this.id), this.url, this.homeURL, this.username, this.password, this.httpuser, this.httppassword, this.imagePlacement, this.featuredImageCapable, this.fullSizeImage, this.maxImageWidth, this.maxImageWidthId, this.location, this.dotcomFlag, originalUsername, this.postFormats, this.dotcom_username, this.dotcom_password, this.api_blogid, this.api_key, this.scaledImage, this.scaledImageWidth, this.blogOptions, this.isAdmin() );
    }

    public String getPostFormats() {
        return postFormats;
    }

    public void setPostFormats(String postFormats) {
        this.postFormats = postFormats;
    }

    public int getUnmoderatedCommentCount() {
        return WordPress.wpDB.getUnmoderatedCommentCount(this.id);
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

    public void setBlogOptions(String blogOptions) {
        this.blogOptions = blogOptions;
    }

    public boolean isActive() {
        return !password.equals("");
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
}
