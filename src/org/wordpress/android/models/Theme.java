package org.wordpress.android.models;

import org.json.JSONException;
import org.json.JSONObject;

public class Theme {
    
    private String themeId = null;
    private String screenshotURL = "";
    private int price = 0;
    private String name = "";
    private String description = "";
    
    public Theme() {
        
    }

    public Theme(String themeId, String screenshotURL, int price, String name, String description) {
        setThemeId(themeId);
        setScreenshotURL(screenshotURL);
        setPrice(price);
        setName(name);
        setDescription(description);
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
    
    public static Theme fromJSON(JSONObject object) throws JSONException {
        
        String themeId = object.getString("id");
        String screenshotURL = object.getString("screenshot");
        int price = object.getInt("price");
        String name = object.getString("name");
        String description = object.getString("description");

        return new Theme(themeId, screenshotURL, price, name, description);        
    }
    
    
}
