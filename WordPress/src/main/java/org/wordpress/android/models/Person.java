package org.wordpress.android.models;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

public class Person {
    private long personID;
    private String blogId;
    private int localTableBlogId;

    private String username;
    private String firstName;
    private String lastName;
    private String displayName;
    private String avatarUrl;
    private String role;

    public Person(long personID, String blogId, int localTableBlogId) {
        this.personID = personID;
        this.blogId = blogId;
        this.localTableBlogId = localTableBlogId;
    }

    @Nullable
    public static Person fromJSON(JSONObject json, String blogId, int localTableBlogId) throws JSONException {
        if (json == null) {
            return null;
        }

        // Response parameters are in: https://developer.wordpress.com/docs/api/1.1/get/sites/%24site/users/%24user_id/
        try {
            long personID = Long.parseLong(json.getString("ID"));
            Person person = new Person(personID, blogId, localTableBlogId);
            person.setUsername(json.optString("login"));
            person.setFirstName(json.optString("first_name"));
            person.setLastName(json.optString("last_name"));
            person.setDisplayName(json.optString("name"));
            person.setAvatarUrl(json.optString("avatar_URL"));
            // We don't support multiple roles, so the first role is picked just as it's in Calypso
            String role = json.getJSONArray("roles").optString(0);
            person.setRole(role);

            return person;
        } catch (NumberFormatException e) {
            AppLog.e(AppLog.T.PEOPLE, "The ID parsed from the JSON couldn't be converted to long: " + e);
        }

        return null;
    }

    public long getPersonID() {
        return personID;
    }

    public String getBlogId() {
        return StringUtils.notNullStr(blogId);
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

    public String getFirstName() {
        return StringUtils.notNullStr(firstName);
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return StringUtils.notNullStr(lastName);
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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
}
