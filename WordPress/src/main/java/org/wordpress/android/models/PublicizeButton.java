package org.wordpress.android.models;

import org.json.JSONException;
import org.json.JSONObject;

public class PublicizeButton {
    private static final String ID_KEY = "ID";
    private static final String NAME_KEY = "name";
    private static final String SHORT_NAME_KEY = "shortname";
    private static final String CUSTOM_KEY = "custom";
    private static final String ENABLED_KEY = "enabled";
    private static final String VISIBILITY_KEY = "visibility";
    private static final String GENERICON_KEY = "genericon";
    public static final String VISIBLE = "visible";
    public static final String HIDDEN = "hidden";

    private String mId;
    private String mName;
    private String mShortName;
    private boolean mIsCustom;
    private boolean mIsEnabled;
    private String mVisibility;
    private String mGenericon;

    public PublicizeButton(JSONObject jsonObject) {
        mId = jsonObject.optString(ID_KEY, "");
        mName = jsonObject.optString(NAME_KEY, "");
        mShortName = jsonObject.optString(SHORT_NAME_KEY, "");
        mIsCustom = jsonObject.optBoolean(CUSTOM_KEY, false);
        mIsEnabled = jsonObject.optBoolean(ENABLED_KEY, false);
        mVisibility = jsonObject.optString(VISIBILITY_KEY, VISIBLE);
        mGenericon = jsonObject.optString(GENERICON_KEY, "");
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put(ID_KEY, mId);
            jsonObject.put(NAME_KEY, mName);
            jsonObject.put(SHORT_NAME_KEY, mShortName);
            jsonObject.put(CUSTOM_KEY, mIsCustom);
            jsonObject.put(ENABLED_KEY, mIsEnabled);
            jsonObject.put(VISIBILITY_KEY, mVisibility);
            jsonObject.put(GENERICON_KEY, mGenericon);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getShortName() {
        return mShortName;
    }

    public void setShortName(String shortName) {
        this.mShortName = shortName;
    }

    public boolean isCustom() {
        return mIsCustom;
    }

    public void setCustom(boolean custom) {
        mIsCustom = custom;
    }

    public boolean isEnabled() {
        return mIsEnabled;
    }

    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    public String getVisibility() {
        return mVisibility;
    }

    public boolean isVisible() {
        return mVisibility.equals(VISIBLE);
    }

    public void setVisibility(boolean isVisible) {
        if (isVisible) {
            mVisibility = VISIBLE;
        } else {
            mVisibility = HIDDEN;
        }
    }

    public void setVisibility(String visibility) {
        mVisibility = visibility;
    }

    public String getGenericon() {
        return mGenericon;
    }

    public void setGenericon(String genericon) {
        this.mGenericon = genericon;
    }


}
