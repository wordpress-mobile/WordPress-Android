package org.wordpress.android.models;

import org.json.JSONObject;

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

    public static Person fromJSON(JSONObject json, int localTableBlogId) {
        if (json == null) {
            return null;
        }

        long personID = Long.parseLong(json.optString("ID"));
        String username = json.optString("login");
        String firstName = json.optString("first_name");
        String lastName = json.optString("last_name");
        String displayName = json.optString("nice_name");
        String avatarUrl = json.optString("avatar_URL");
        Role role = Role.fromKey(json.optJSONArray("roles").optString(0));

        return new Person(personID, localTableBlogId, username, firstName, lastName, displayName, avatarUrl, role);
    }

    public long getPersonId() {
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
