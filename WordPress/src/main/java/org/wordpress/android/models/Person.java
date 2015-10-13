package org.wordpress.android.models;

public class Person {
    private long localTablePersonId;
    public long personID;

    private String username;
    private String firstName;
    private String lastName;
    private String displayName;
    private String imageUrl;
    private Role role;

    public Person(long personID,
                  String username,
                  String firstName,
                  String lastName,
                  String displayName,
                  String imageUrl,
                  Role role) {
        this.personID = personID;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.imageUrl = imageUrl;
        this.role = role;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getLocalTablePersonId() {
        return localTablePersonId;
    }

    public void setLocalTablePersonId(long localTablePersonId) {
        this.localTablePersonId = localTablePersonId;
    }
}
