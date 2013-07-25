package org.wordpress.android.models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.WordPress;

public class Theme {
    
    private String themeId = null;
    private String screenshotURL = "";
    private int price = 0;
    private String name = "";
    private String description = "";
    private int trendingRank = 0;
    private int popularityRank = 0;
    private String launchDate = "";
    private long launchDateMs = 0;
    private String blogId;
    private String previewURL = "";
    private boolean isCurrentTheme = false;
    
    public Theme() {
        
    }

    public Theme(String themeId, String screenshotURL, int price, String name, String description, int trendingRank, int popularityRank, String launchDate, String blogId, String previewURL) {
        setThemeId(themeId);
        setScreenshotURL(screenshotURL);
        setPrice(price);
        setName(name);
        setDescription(description);
        setTrendingRank(trendingRank);
        setPopularityRank(popularityRank);
        setLaunchDate(launchDate);
        setBlogId(blogId);
        setPreviewURL(previewURL);
    }

    public String getThemeId() {
        return themeId;
    }

    public void setThemeId(String themeId) {
        this.themeId = themeId;
    }

    public String getScreenshotURL() {
        return screenshotURL;
    }

    public void setScreenshotURL(String screenshotURL) {
        this.screenshotURL = screenshotURL;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTrendingRank() {
        return trendingRank;
    }

    public void setTrendingRank(int trendingRank) {
        this.trendingRank = trendingRank;
    }

    public int getPopularityRank() {
        return popularityRank;
    }

    public void setPopularityRank(int popularityRank) {
        this.popularityRank = popularityRank;
    }

    public String getLaunchDate() {
        return launchDate;
    }
    
    public long getLaunchDateMs() {
        return launchDateMs;
    }

    public void setLaunchDate(String launchDate) {
        this.launchDate = launchDate;
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(launchDate);
            this.launchDateMs = date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public String getBlogId() {
        return blogId;
    }
    
    public void setBlogId(String blogId) {
        this.blogId = blogId;
    }
    
    public String getPreviewURL() {
        return previewURL;
    }

    public void setPreviewURL(String previewURL) {
        this.previewURL = previewURL;
    }

    public void save() {
        WordPress.wpDB.saveTheme(this);
    }

    public static Theme fromJSON(JSONObject object) throws JSONException {
        
        String themeId = object.getString("id");
        String screenshotURL = object.getString("screenshot") ;
        int price = object.getInt("price");
        String name = object.getString("name");
        String description = object.getString("description");
        int trendingRank = object.getInt("trending_rank");
        int popularityRank = object.getInt("popularity_rank");
        String launchDate = object.getString("launch_date");
        String previewURL = object.has("preview_url") ? object.getString("preview_url") : ""; // we don't receive preview_url when we fetch current theme

        // if the theme is free, set the blogId to be empty
        // if the theme is not free, set the blogId to the current blog
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        
        return new Theme(themeId, screenshotURL, price, name, description, trendingRank, popularityRank, launchDate, blogId, previewURL);        
    }

    public void setIsCurrentTheme(boolean isCurrentTheme) {
        this.isCurrentTheme = isCurrentTheme;
    }
    
    public boolean getIsCurrentTheme() {
        return isCurrentTheme;
    }

}
