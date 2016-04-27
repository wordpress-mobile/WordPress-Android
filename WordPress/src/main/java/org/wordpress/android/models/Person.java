package org.wordpress.android.models;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

public class Person {
    private long personID;
    private int localTableBlogId;

    private String username;
    private String firstName;
    private String lastName;
    private String displayName;
    private String avatarUrl;
    private Role role;

    public Person(long personID,
                  int localTableBlogId,
                  String username,
                  String firstName,
                  String lastName,
                  String displayName,
                  String avatarUrl,
                  Role role) {
        this.personID = personID;
        this.localTableBlogId = localTableBlogId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.role = role;
    }

    @Nullable
    public static Person fromJSON(JSONObject json, int localTableBlogId) {
        if (json == null) {
            return null;
        }

        // Response parameters can be found in https://developer.wordpress.com/docs/api/1.1/get/sites/%24site/users/%24user_id/
        try {
            long personID = Long.parseLong(json.getString("ID"));
            String username = json.optString("login");
            String firstName = json.optString("first_name");
            String lastName = json.optString("last_name");
            String displayName = json.optString("name");
            String avatarUrl = json.optString("avatar_URL");
            // We don't support multiple roles, so the first role is picked just as it's in Calypso
            Role role = Role.fromKey(json.optJSONArray("roles").optString(0));

            return new Person(personID, localTableBlogId, username, firstName, lastName, displayName, avatarUrl, role);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.PEOPLE, "JSON exception occurred while parsing the user json: " + e);
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
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
