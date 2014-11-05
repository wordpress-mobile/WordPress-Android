package org.wordpress.android.ui.stats.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class VideoPlaysModel implements Serializable {
    private String period;
    private String date;
    private String blogID;
    private int otherPlays;
    private int totalPlays;
    private List<TopPostModel> mPlays;

    public VideoPlaysModel(String blogID, JSONObject response) throws JSONException {
        this.blogID = blogID;
        this.period = response.getString("period");
        this.date = response.getString("date");

        JSONObject jDaysObject = response.getJSONObject("days");
        if (jDaysObject.length() == 0) {
            throw new JSONException("Invalid document returned from the REST API");
        }

        // Read the first day
        Iterator<String> keys = jDaysObject.keys();
        String key = keys.next();
        JSONObject firstDayObject = jDaysObject.getJSONObject(key);
        this.otherPlays = firstDayObject.getInt("other_plays");
        this.totalPlays = firstDayObject.getInt("total_plays");
        JSONArray playsJSONArray = firstDayObject.optJSONArray("plays");

        if (playsJSONArray != null) {
            mPlays = new ArrayList<TopPostModel>(playsJSONArray.length());
            for (int i = 0; i < playsJSONArray.length(); i++) {
                JSONObject currentVideoplaysJSON = playsJSONArray.getJSONObject(i);
                String postId = String.valueOf(currentVideoplaysJSON.getInt("post_id"));
                String title = currentVideoplaysJSON.getString("title");
                int views = currentVideoplaysJSON.getInt("plays");
                String url = currentVideoplaysJSON.getString("url");
                TopPostModel currentPost = new TopPostModel(blogID, postId, title, views, url);
                mPlays.add(currentPost);
            }
        }
    }

    public String getBlogID() {
        return blogID;
    }

    public void setBlogID(String blogID) {
        this.blogID = blogID;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public List<TopPostModel> getPlays() {
        return this.mPlays;
    }
}
