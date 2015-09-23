package org.wordpress.android.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;

public class Theme {
    public static final String ID = "id";
    public static final String AUTHOR = "author";
    public static final String SCREENSHOT = "screenshot";
    public static final String AUTHOR_URI = "author_uri";
    public static final String DEMO_URI = "demo_uri";
    public static final String NAME = "name";
    public static final String STYLESHEET = "stylesheet";
    public static final String PRICE = "price";
    public static final String BLOG_ID = "blogId";
    public static final String IS_CURRENT = "isCurrent";

    public static final String PREVIEW_URL = "preview_url";
    public static final String COST = "cost";
    public static final String DISPLAY = "display";

    private String mId;
    private String mAuthor;
    private String mScreenshot;
    private String mAuthorURI;
    private String mDemoURI;
    private String mName;
    private String mStylesheet;
    private String mPrice;
    private String mBlogId;
    private boolean mIsCurrent;

    public static Theme fromJSONV1_1(JSONObject object) throws JSONException {
        if (object == null) {
            return null;
        } else {
            String id = object.getString(ID);
            String author = "";
            String screenshot = object.getString(SCREENSHOT);
            String authorURI = "";
            String demoURI = object.getString(PREVIEW_URL);
            String name = object.getString(NAME);
            String stylesheet = "";
            String price;
            try {
                JSONObject cost = object.getJSONObject(COST);
                price = cost.getString(DISPLAY);
            } catch (JSONException e) {
                price = "";
            }

            String blogId = String.valueOf(WordPress.getCurrentBlog().getRemoteBlogId());

            return new Theme(id, author, screenshot, authorURI, demoURI, name, stylesheet, price, blogId, false);
        }
    }

    public static Theme fromJSONV1_2(JSONObject object) throws JSONException {
        if (object == null) {
            return null;
        } else {
            String id = object.getString(ID);
            String author = object.getString(AUTHOR);
            String screenshot = object.getString(SCREENSHOT);
            String authorURI = object.getString(AUTHOR_URI);
            String demoURI = object.getString(DEMO_URI);
            String name = object.getString(NAME);
            String stylesheet = object.getString(STYLESHEET);
            String price;
            try {
                price = object.getString(PRICE);
            } catch (JSONException e) {
                price = "";
            }

            String blogId = String.valueOf(WordPress.getCurrentBlog().getRemoteBlogId());

            return new Theme(id, author, screenshot, authorURI, demoURI, name, stylesheet, price, blogId, false);
        }
    }

    public Theme(String id, String author, String screenshot, String authorURI, String demoURI, String name, String stylesheet, String price, String blogId, boolean isCurrent) {
        setId(id);
        setAuthor(author);
        setScreenshot(screenshot);
        setAuthorURI(authorURI);
        setDemoURI(demoURI);
        setName(name);
        setStylesheet(stylesheet);
        setPrice(price);
        setBlogId(blogId);
        setIsCurrent(isCurrent);
    }

    public void setId(String id) {
        mId = id;
    }

    public String getId() {
        return mId;
    }

    public void setAuthor(String author) {
        mAuthor = author;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public String getScreenshot() {
        return mScreenshot;
    }

    public void setScreenshot(String mScreenshot) {
        this.mScreenshot = mScreenshot;
    }

    public String getAuthorURI() {
        return mAuthorURI;
    }

    public void setAuthorURI(String mAuthorURI) {
        this.mAuthorURI = mAuthorURI;
    }

    public String getDemoURI() {
        return mDemoURI;
    }

    public void setDemoURI(String mDemoURI) {
        this.mDemoURI = mDemoURI;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getStylesheet() {
        return mStylesheet;
    }

    public void setStylesheet(String mStylesheet) {
        this.mStylesheet = mStylesheet;
    }

    public String getPrice() {
        return mPrice;
    }

    public void setPrice(String mPrice) {
        this.mPrice = mPrice;
    }

    public String getBlogId() {
        return mBlogId;
    }

    public void setBlogId(String blogId) {
        mBlogId = blogId;
    }

    public boolean getIsCurrent() {
        return mIsCurrent;
    }

    public void setIsCurrent(boolean isCurrent) {
        mIsCurrent = isCurrent;
    }

    public boolean isPremium() {
        return !mPrice.equals("");
    }

    public void save() {
        WordPress.wpDB.saveTheme(this);
    }
}
