package org.wordpress.android.ui.stats2.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.StringUtils;

import java.util.Iterator;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;

public class TopPostsModel extends RealmObject {
    private String period;
    private String days;

    @Index
    private String date;

    @Index
    private String blogID;

    @Ignore
    private JSONArray postviewsJSON;// dummy variable

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

    public String getDays() {
        return days;
    }

    public void setDays(String days) {
        this.days = days;
    }

    public JSONArray getPostviewsJSON() {
        JSONArray jArray;
        String decodedString = StringUtils.unescapeHTML(this.getDays() != null ? this.getDays() : "{}");
        try {
            JSONObject jDaysObject = new JSONObject(decodedString);
            Iterator<String> keys = jDaysObject.keys();
            if (keys.hasNext()) {
                String key = keys.next();
                JSONObject jDateObject = jDaysObject.getJSONObject(key);
                jArray = jDateObject.getJSONArray("postviews");
                //jArray = JSONUtil.queryJSON(jDaysObject, key + "/postviews", new JSONArray());
            } else {
                jArray = new JSONArray();
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
            return null;
        }
        return jArray;
    }
}
