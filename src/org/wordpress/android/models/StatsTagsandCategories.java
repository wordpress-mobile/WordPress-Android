package org.wordpress.android.models;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A model to represent a for a tag or category stat
 */
public class StatsTagsandCategories {

    private String mBlogId;
    private String mTopic;
    private String mType;
    private int mViews;

    public enum Type {
        TAG("tag"), CATEGORY("category");
        
        private String mLabel;

        private Type (String label) {
            mLabel = label;
        }

        public String getLabel() {
            return mLabel;
        }
    }
    
    public StatsTagsandCategories(String blogId, String topic, Type type, int views) {
        this.mBlogId = blogId;
        this.mTopic = topic;
        this.mType = type.getLabel();
        this.mViews = views;
    }

    public StatsTagsandCategories(String blogId, JSONObject result) throws JSONException {
        setBlogId(blogId);
        setTopic(result.getString("topic"));
        if (result.get("type").equals(Type.CATEGORY.getLabel()))
            setType(Type.CATEGORY);
        else
            setType(Type.TAG);
        setViews(result.getInt("views"));
    }

    public String getBlogId() {
        return mBlogId;
    }

    public void setBlogId(String blogId) {
        this.mBlogId = blogId;
    }

    public String getTopic() {
        return mTopic;
    }

    public void setTopic(String topic) {
        this.mTopic = topic;
    }

    public String getType() {
        return mType;
    }

    public void setType(Type type) {
        this.mType = type.getLabel();
    }

    public int getViews() {
        return mViews;
    }

    public void setViews(int views) {
        this.mViews = views;
    }
}
