package org.wordpress.android.datasets;

import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.models.Person;
import org.wordpress.android.models.Role;

public class PeopleTable {
    public static final String PEOPLE_TABLE = "people";

    public static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + PEOPLE_TABLE + " ("
                + "person_id               INTEGER PRIMARY KEY DEFAULT 0,"
                + "user_name               TEXT,"
                + "first_name              TEXT,"
                + "last_name               TEXT,"
                + "display_name            TEXT,"
                + "avatar_url              TEXT)");
    }

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
