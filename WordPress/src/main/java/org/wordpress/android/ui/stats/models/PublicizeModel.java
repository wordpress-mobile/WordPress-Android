package org.wordpress.android.ui.stats.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PublicizeModel extends BaseStatsModel {
    private String mBlogID;
    private List<SingleItemModel> mServices;

    public PublicizeModel(String blogID, JSONObject response) throws JSONException {
        this.mBlogID = blogID;
        JSONArray services = response.getJSONArray("services");
        if (services.length() > 0) {
            mServices = new ArrayList<>(services.length());
            for (int i = 0; i < services.length(); i++) {
                JSONObject current = services.getJSONObject(i);
                String serviceName = current.getString("service");
                int followers = current.getInt("followers");
                SingleItemModel currentItem = new SingleItemModel(blogID, null, null, serviceName, followers, null, null);
                mServices.add(currentItem);
            }
        }
    }

    public List<SingleItemModel> getServices() {
        return mServices;
    }

    public String getBlogId() {
        return mBlogID;
    }

    public void setBlogId(String blogId) {
        this.mBlogID = blogId;
    }
}
