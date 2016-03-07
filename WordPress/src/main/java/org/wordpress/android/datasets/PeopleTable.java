package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Person;
import org.wordpress.android.models.Role;

public class PeopleTable {
    public static final String PEOPLE_TABLE = "people";

    private static SQLiteDatabase getWritableDb() {
        return WordPress.wpDB.getDatabase();
    }

    public static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + PEOPLE_TABLE + " ("
                + "person_id               INTEGER PRIMARY KEY DEFAULT 0,"
                + "user_name               TEXT,"
                + "first_name              TEXT,"
                + "last_name               TEXT,"
                + "display_name            TEXT,"
                + "avatar_url              TEXT)");
    }

    private static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + PEOPLE_TABLE);
    }

    public static void save(Person person) {
        save(person, getWritableDb());
    }

    public static void save(Person person, SQLiteDatabase database) {
        ContentValues values = new ContentValues();
        values.put("person_id", person.getPersonId());
        values.put("user_name", person.getUsername());
        values.put("first_name", person.getFirstName());
        values.put("last_name", person.getLastName());
        values.put("display_name", person.getDisplayName());
        values.put("avatar_url", person.getAvatarUrl());
        database.insertWithOnConflict(PEOPLE_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
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
