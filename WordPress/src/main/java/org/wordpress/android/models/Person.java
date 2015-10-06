package org.wordpress.android.models;

public class Person {
    public long personID;

    private String username;
    private String firstName;
    private String lastName;
    private String displayName;
    private Role role;

    public Person(long personID,
                  String username,
                  String firstName,
                  String lastName,
                  String displayName,
                  Role role) {
        this.personID = personID;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.role = role;
    }
}
