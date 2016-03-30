package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Person;
import org.wordpress.android.models.Role;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;

import java.util.List;

public class PeopleTable {
    public static final String PEOPLE_TABLE = "people";

    private static SQLiteDatabase getReadableDb() {
        return WordPress.wpDB.getDatabase();
    }
    private static SQLiteDatabase getWritableDb() {
        return WordPress.wpDB.getDatabase();
    }

    public static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + PEOPLE_TABLE + " ("
                + "person_id               INTEGER DEFAULT 0,"
                + "site_id                 INTEGER DEFAULT 0,"
                + "user_name               TEXT,"
                + "first_name              TEXT,"
                + "last_name               TEXT,"
                + "display_name            TEXT,"
                + "avatar_url              TEXT,"
                + "role                    TEXT,"
                + "PRIMARY KEY (person_id, site_id)"
                + ");");
    }

    private static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + PEOPLE_TABLE);
    }

    public static void reset(SQLiteDatabase db) {
        AppLog.i(AppLog.T.COMMENTS, "resetting people table");
        dropTables(db);
        createTables(db);
    }

    public static void save(Person person) {
        save(person, getWritableDb());
    }

    public static void save(Person person, SQLiteDatabase database) {
        ContentValues values = new ContentValues();
        values.put("person_id", person.getPersonId());
        values.put("site_id", person.getSiteID());
        values.put("user_name", person.getUsername());
        values.put("first_name", person.getFirstName());
        values.put("last_name", person.getLastName());
        values.put("display_name", person.getDisplayName());
        values.put("avatar_url", person.getAvatarUrl());
        values.put("role", Role.toKey(person.getRole()));
        database.insertWithOnConflict(PEOPLE_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void savePeople(List<Person> peopleList) {
        for (Person person : peopleList) {
            PeopleTable.save(person);
        }
    }

    /**
     * retrieve a single person
     * @param personId - id of a person in a particular site
     * @param siteID - site the person belongs to
     * @return Person if found, null otherwise
     */
    public static Person getPerson(long personId, String siteID) {
        String[] args = { Long.toString(personId), siteID };
        Cursor c = getReadableDb().rawQuery("SELECT * FROM " + PEOPLE_TABLE + " WHERE person_id=? AND site_id=?", args);

        try {
            if (!c.moveToFirst()) {
                return null;
            }

            String username = c.getString(c.getColumnIndex("user_name"));
            String firstName = c.getString(c.getColumnIndex("first_name"));
            String lastName = c.getString(c.getColumnIndex("last_name"));
            String displayName = c.getString(c.getColumnIndex("display_name"));
            String avatarUrl = c.getString(c.getColumnIndex("avatar_url"));
            Role role = Role.fromKey(c.getString(c.getColumnIndex("role")));

            return new Person(personId, siteID, username, firstName, lastName, displayName, avatarUrl, role);
        } finally {
            SqlUtils.closeCursor(c);
        }
    }
}
