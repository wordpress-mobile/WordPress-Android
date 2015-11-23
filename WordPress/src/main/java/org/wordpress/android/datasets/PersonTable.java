package org.wordpress.android.datasets;

import org.wordpress.android.models.Person;
import org.wordpress.android.models.Role;

public class PersonTable {
    /**
     * retrieve a single person
     * @param personId - unique id in person table
     * @return Person if found, null otherwise
     */
    public static Person getPerson(int personId) {
        // This is a stub method for now so it returns a mock object, once implemented it will query the db
        return new Person(4, "oguzkocer", "Oguz", "Kocer", "Oguz", "http://lorempixum.com/76/76", Role.EDITOR);
    }
}
