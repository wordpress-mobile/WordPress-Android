
package org.wordpress.android.models;

import org.wordpress.android.WordPress;

/**
 * Model for stats that appear in listviews.
 */

public class Stat {

    // The blog this stat is for.
    private String blogId;
    
    // The category of the stat. See StatsCategory
    private String category;
    
    // The entry type of the stat, if applicable. For when there is more than one type of entry in a category.
    private String entryType;
    
    // The entry in the listview
    private String entry;
    
    // The totals of an entry in the listview
    private int total;
    
    // The timeframe of the entry (e.g. 0 for today, 30 for month)
    private int timeframe;
    
    // The url of the entry, if applicable.
    private String url;
    
    // The image url of the entry's icon if applicable
    private String imageUrl;

    public String getBlogId() {
        return blogId;
    }

    public void setBlogId(String blogId) {
        this.blogId = blogId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public String getEntry() {
        return entry;
    }

    public void setEntry(String entry) {
        this.entry = entry;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(int timeframe) {
        this.timeframe = timeframe;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public void save() {
        WordPress.wpDB.saveStat(this);
    }
}
