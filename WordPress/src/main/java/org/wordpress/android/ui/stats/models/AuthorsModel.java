package org.wordpress.android.ui.stats.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class AuthorsModel extends BaseStatsModel {
    private String mPeriod;
    private String mDate;
    private String mBlogID;
    private int mOtherViews;
    private List<AuthorModel> mAuthors;

    public AuthorsModel(String blogID, JSONObject response) throws JSONException {
        this.mBlogID = blogID;
        this.mPeriod = response.getString("period");
        this.mDate = response.getString("date");

        JSONObject jDaysObject = response.getJSONObject("days");
        if (jDaysObject.length() == 0) {
            throw new JSONException("Invalid document returned from the REST API");
        }

        JSONArray authorsJSONArray;
        // Read the first day
        Iterator<String> keys = jDaysObject.keys();
        String key = keys.next();
        JSONObject firstDayObject = jDaysObject.getJSONObject(key);
        this.mOtherViews = firstDayObject.optInt("other_views");
        authorsJSONArray = firstDayObject.optJSONArray("authors");

        if (authorsJSONArray != null) {
            mAuthors = new ArrayList<>(authorsJSONArray.length());
            for (int i = 0; i < authorsJSONArray.length(); i++) {
                try {
                    JSONObject currentAuthorJSON = authorsJSONArray.getJSONObject(i);
                    AuthorModel currentAuthor = new AuthorModel(blogID, mDate, currentAuthorJSON);
                    mAuthors.add(currentAuthor);
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.STATS, "Unexpected Author object " +
                            "at position " + i + " Response: " + response.toString(), e);
                }
            }
        }
    }

    public String getBlogID() {
        return mBlogID;
    }

    public void setBlogID(String blogID) {
        this.mBlogID = blogID;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        this.mDate = date;
    }

    public String getPeriod() {
        return mPeriod;
    }

    public void setPeriod(String period) {
        this.mPeriod = period;
    }

    public List<AuthorModel> getAuthors() {
        return this.mAuthors;
    }

    public int getOtherViews() {
        return mOtherViews;
    }
}
