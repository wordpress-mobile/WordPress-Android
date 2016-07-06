package org.wordpress.android.models;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.StringUtils;

public class Person {
    public enum PersonType { USER, FOLLOWER, EMAIL_FOLLOWER }

    private long personID;
    private int localTableBlogId;

    private String username;
    private String displayName;
    private String avatarUrl;
    private String role;
    private String subscribed;
    private PersonType personType;

    public Person(long personID, int localTableBlogId) {
        this.personID = personID;
        this.localTableBlogId = localTableBlogId;
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
            person.personType = PersonType.USER;
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
            person.personType = isEmailFollower ? PersonType.EMAIL_FOLLOWER : PersonType.FOLLOWER;

            return person;
        } catch (NumberFormatException e) {
            AppLog.e(AppLog.T.PEOPLE, "The ID parsed from the JSON couldn't be converted to long: " + e);
        }

        return null;
    }

    public long getPersonID() {
        return personID;
    }

    public int getLocalTableBlogId() {
        return localTableBlogId;
    }

    public String getUsername() {
        return StringUtils.notNullStr(username);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return StringUtils.notNullStr(displayName);
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return StringUtils.notNullStr(role);
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAvatarUrl() {
        return StringUtils.notNullStr(avatarUrl);
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getSubscribed() {
        return StringUtils.notNullStr(subscribed);
    }

    public void setSubscribed(String subscribed) {
        this.subscribed = StringUtils.notNullStr(subscribed);
    }

    /*
     * converts iso8601 subscribed date to an actual java date
     */
    private transient java.util.Date dtSubscribed;
    public java.util.Date getDateSubscribed() {
        if (dtSubscribed == null)
            dtSubscribed = DateTimeUtils.iso8601ToJavaDate(subscribed);
        return dtSubscribed;
    }

    public PersonType getPersonType() {
        return personType;
    }

    public void setPersonType(PersonType personType) {
        this.personType = personType;
    }
}
