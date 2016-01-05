package org.wordpress.android.ui.stats.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class SearchTermsModel extends BaseStatsModel {
    private String mPeriod;
    private String mDate;
    private String mBlogID;
    private List<SearchTermModel> mSearchTerms;
    private int mEncryptedSearchTerms, mOtherSearchTerms, mTotalSearchTerms;

    public SearchTermsModel(String blogID, JSONObject response) throws JSONException {
        this.mBlogID = blogID;
        this.mPeriod = response.getString("period");
        this.mDate = response.getString("date");

        JSONArray searchTermsArray = null;
        JSONObject jDaysObject = response.getJSONObject("days");
        if (jDaysObject.length() == 0) {
            throw new JSONException("Invalid document returned from the REST API");
        }

        Iterator<String> keys = jDaysObject.keys();
        if (keys.hasNext()) {
            String key = keys.next();
            JSONObject jDateObject = jDaysObject.optJSONObject(key); // This could be an empty array on site with low traffic
            searchTermsArray = null;
            if (jDateObject != null) {
                searchTermsArray = jDateObject.getJSONArray("search_terms");
                this.mEncryptedSearchTerms = jDateObject.optInt("encrypted_search_terms");
                this.mOtherSearchTerms = jDateObject.optInt("other_search_terms");
                this.mTotalSearchTerms = jDateObject.optInt("total_search_terms");
            }
        }

        if (searchTermsArray == null) {
            searchTermsArray = new JSONArray();
        }

        ArrayList<SearchTermModel> list = new ArrayList<>(searchTermsArray.length());
        for (int i=0; i < searchTermsArray.length(); i++) {
            try {
                JSONObject postObject = searchTermsArray.getJSONObject(i);
                String term = postObject.getString("term");
                int total = postObject.getInt("views");
                SearchTermModel currentModel = new SearchTermModel(blogID, mDate, term, total, false);
                list.add(currentModel);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, "Unexpected SearchTerm object in searchterms array" +
                        "at position " + i + " Response: " + response.toString(), e);
            }
        }

        this.mSearchTerms = list;
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

    public List<SearchTermModel> getSearchTerms() {
        return mSearchTerms;
    }

    public boolean hasSearchTerms() {
        return mSearchTerms != null && mSearchTerms.size() > 0;
    }

    public int getEncryptedSearchTerms() {
        return mEncryptedSearchTerms;
    }

    public int getOtherSearchTerms() {
        return mOtherSearchTerms;
    }

    public int getTotalSearchTerms() {
        return mTotalSearchTerms;
    }
}
