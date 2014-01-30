package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by nbradbury on 6/23/13.
 */
public class ReaderUserList extends ArrayList<ReaderUser> {
    /*
     * returns all userIds in this list
     */
    public ReaderUserIdList getUserIds() {
        ReaderUserIdList ids = new ReaderUserIdList();
        for (ReaderUser user: this)
            ids.add(user.userId);
        return ids;
    }


    /*
     * passed json is response from getting likes for a post
     */
    public static ReaderUserList fromJsonLikes(JSONObject json) {
        ReaderUserList users = new ReaderUserList();
        if (json==null)
            return users;

        JSONArray jsonLikes = json.optJSONArray("likes");
        if (jsonLikes!=null) {
            for (int i=0; i < jsonLikes.length(); i++)
                users.add(ReaderUser.fromJson(jsonLikes.optJSONObject(i)));
        }

        return users;
    }
}
