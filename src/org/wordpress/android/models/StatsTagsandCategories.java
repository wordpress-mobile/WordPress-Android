
package org.wordpress.android.models;


public class StatsTagsandCategories {

    private String mBlogId;
    private long mDate;
    private String mTopic;
    private String mType;
    private int mViews;

    public enum Type {
        STAT("stat"), CATEGORY("category");
        
        private String mLabel;

        private Type (String label) {
            mLabel = label;
        }

        public String getLabel() {
            return mLabel;
        }
    }
    
    public StatsTagsandCategories(String blogId, long date, String topic, Type type, int views) {
        this.mBlogId = blogId;
        this.mDate = date;
        this.mTopic = topic;
        this.mType = type.getLabel();
        this.mViews = views;
    }

    public String getBlogId() {
        return mBlogId;
    }

    public void setBlogId(String blogId) {
        this.mBlogId = blogId;
    }

    public long getDate() {
        return mDate;
    }

    public void setDate(long date) {
        this.mDate = date;
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
