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

    private String mId;
    private String mAuthor;
    private String mScreenshot;
    private String mAuthorURI;
    private String mDemoURI;
    private String mName;
    private String mStylesheet;
    private String mPrice;

    public static Theme fromJSON(JSONObject object) throws JSONException {
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

            return new Theme(id, author, screenshot, authorURI, demoURI, name, stylesheet, price);
        }
    }

    public Theme(String id, String author, String screenshot, String authorURI, String demoURI, String name, String stylesheet, String price) {
        setId(id);
        setAuthor(author);
        setScreenshot(screenshot);
        setAuthorURI(authorURI);
        setDemoURI(demoURI);
        setName(name);
        setStylesheet(stylesheet);
        setPrice(price);
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

    public void save() {
        WordPress.wpDB.saveTheme(this);
    }
}
