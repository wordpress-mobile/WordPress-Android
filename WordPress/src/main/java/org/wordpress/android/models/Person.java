package org.wordpress.android.models;

public class Person {
    private long personID;
    private long blogID;

    private String username;
    private String firstName;
    private String lastName;
    private String displayName;
    private String avatarUrl;
    private Role role;

    public Person(long personID,
                  long blogID,
                  String username,
                  String firstName,
                  String lastName,
                  String displayName,
                  String avatarUrl,
                  Role role) {
        this.personID = personID;
        this.blogID = blogID;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.role = role;
    }

    public long getPersonId() {
        return personID;
    }

    public long getBlogId() {
        return blogID;
    }

    public String getUsername() {
        return "@" + username;
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
