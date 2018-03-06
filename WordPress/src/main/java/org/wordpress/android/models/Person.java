package org.wordpress.android.models;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.StringUtils;

public class Person {
    public enum PersonType {
        USER, FOLLOWER, EMAIL_FOLLOWER, VIEWER
    }

    private long mPersonID;
    private int mLocalTableBlogId;
    private String mDisplayName;
    private String mAvatarUrl;
    private PersonType mPersonType;

    // Only users have a role
    private String mRole;

    // Users, followers & viewers has a username, email followers don't
    private String mUsername;

    // Only followers & email followers have a subscribed date
    private String mSubscribed;

    public Person(long personID, int localTableBlogId) {
        mPersonID = personID;
        mLocalTableBlogId = localTableBlogId;
    }

    @Nullable
    public static Person userFromJSON(JSONObject json, int localTableBlogId) throws JSONException {
        if (json == null) {
            return null;
        }

        // Response parameters are in: https://developer.wordpress.com/docs/api/1.1/get/sites/%24site/users/%24user_id/
        try {
            long personID = Long.parseLong(json.getString("ID"));
            Person person = new Person(personID, localTableBlogId);
            person.setUsername(json.optString("login"));
            person.setDisplayName(json.optString("name"));
            person.setAvatarUrl(json.optString("avatar_URL"));
            person.mPersonType = PersonType.USER;
            // We don't support multiple roles, so the first role is picked just as it's in Calypso
            String role = json.getJSONArray("roles").optString(0);
            person.setRole(role);

            return person;
        } catch (NumberFormatException e) {
            AppLog.e(AppLog.T.PEOPLE, "The ID parsed from the JSON couldn't be converted to long: " + e);
        }

        return null;
    }

    @Nullable
    public static Person followerFromJSON(JSONObject json, int localTableBlogId, boolean isEmailFollower)
            throws JSONException {
        if (json == null) {
            return null;
        }

        // Response parameters are in: https://developer.wordpress.com/docs/api/1.1/get/sites/%24site/stats/followers/
        try {
            long personID = Long.parseLong(json.getString("ID"));
            Person person = new Person(personID, localTableBlogId);
            person.setDisplayName(json.optString("label"));
            person.setUsername(json.optString("login"));
            person.setAvatarUrl(json.optString("avatar"));
            person.setSubscribed(json.optString("date_subscribed"));
            person.mPersonType = isEmailFollower ? PersonType.EMAIL_FOLLOWER : PersonType.FOLLOWER;

            return person;
        } catch (NumberFormatException e) {
            AppLog.e(AppLog.T.PEOPLE, "The ID parsed from the JSON couldn't be converted to long: " + e);
        }

        return null;
    }

    @Nullable
    public static Person viewerFromJSON(JSONObject json, int localTableBlogId) throws JSONException {
        if (json == null) {
            return null;
        }

        // Similar response parameters in:
        // https://developer.wordpress.com/docs/api/1.1/get/sites/%24site/users/%24user_id/
        try {
            long personID = Long.parseLong(json.getString("ID"));
            Person person = new Person(personID, localTableBlogId);
            person.setUsername(json.optString("login"));
            person.setDisplayName(json.optString("name"));
            person.setAvatarUrl(json.optString("avatar_URL"));
            person.setPersonType(PersonType.VIEWER);

            return person;
        } catch (NumberFormatException e) {
            AppLog.e(AppLog.T.PEOPLE, "The ID parsed from the JSON couldn't be converted to long: " + e);
        }

        return null;
    }

    public long getPersonID() {
        return mPersonID;
    }

    public int getLocalTableBlogId() {
        return mLocalTableBlogId;
    }

    public String getUsername() {
        return StringUtils.notNullStr(mUsername);
    }

    public void setUsername(String username) {
        mUsername = username;
    }

    public String getDisplayName() {
        return StringUtils.notNullStr(mDisplayName);
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public String getRole() {
        return mRole;
    }

    public void setRole(String role) {
        mRole = role;
    }

    public String getAvatarUrl() {
        return StringUtils.notNullStr(mAvatarUrl);
    }

    public void setAvatarUrl(String avatarUrl) {
        mAvatarUrl = avatarUrl;
    }

    public String getSubscribed() {
        return StringUtils.notNullStr(mSubscribed);
    }

    public void setSubscribed(String subscribed) {
        mSubscribed = StringUtils.notNullStr(subscribed);
    }

    /*
     * converts iso8601 subscribed date to an actual java date
     */
    private transient java.util.Date mDateSubscribed;

    public java.util.Date getDateSubscribed() {
        if (mDateSubscribed == null) {
            mDateSubscribed = DateTimeUtils.dateFromIso8601(mSubscribed);
        }
        return mDateSubscribed;
    }

    public PersonType getPersonType() {
        return mPersonType;
    }

    public void setPersonType(PersonType personType) {
        mPersonType = personType;
    }
}
