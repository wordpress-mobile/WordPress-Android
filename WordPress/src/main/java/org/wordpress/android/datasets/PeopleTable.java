package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Person;
import org.wordpress.android.ui.people.utils.PeopleUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;

import java.util.ArrayList;
import java.util.List;

public class PeopleTable {
    private static final String TEAM_TABLE = "people_team";
    private static final String FOLLOWERS_TABLE = "people_followers";
    private static final String EMAIL_FOLLOWERS_TABLE = "people_email_followers";

    private static SQLiteDatabase getReadableDb() {
        return WordPress.wpDB.getDatabase();
    }
    private static SQLiteDatabase getWritableDb() {
        return WordPress.wpDB.getDatabase();
    }

    public static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TEAM_TABLE + " ("
                + "person_id               INTEGER DEFAULT 0,"
                + "local_blog_id           INTEGER DEFAULT 0,"
                + "user_name               TEXT,"
                + "display_name            TEXT,"
                + "avatar_url              TEXT,"
                + "role                    TEXT,"
                + "PRIMARY KEY (person_id, local_blog_id)"
                + ");");

        db.execSQL("CREATE TABLE " + FOLLOWERS_TABLE + " ("
                + "person_id               INTEGER DEFAULT 0,"
                + "local_blog_id           INTEGER DEFAULT 0,"
                + "user_name               TEXT,"
                + "display_name            TEXT,"
                + "avatar_url              TEXT,"
                + "subscribed              TEXT,"
                + "PRIMARY KEY (person_id, local_blog_id)"
                + ");");

        db.execSQL("CREATE TABLE " + EMAIL_FOLLOWERS_TABLE + " ("
                + "person_id               INTEGER DEFAULT 0,"
                + "local_blog_id           INTEGER DEFAULT 0,"
                + "display_name            TEXT,"
                + "avatar_url              TEXT,"
                + "subscribed              TEXT,"
                + "PRIMARY KEY (person_id, local_blog_id)"
                + ");");
    }

    private static void dropTables(SQLiteDatabase db) {
        // People table is not used anymore, each filter now has it's own table
        db.execSQL("DROP TABLE IF EXISTS PEOPLE");

        db.execSQL("DROP TABLE IF EXISTS " + TEAM_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + FOLLOWERS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + EMAIL_FOLLOWERS_TABLE);
    }

    public static void reset(SQLiteDatabase db) {
        AppLog.i(AppLog.T.PEOPLE, "resetting people table");
        dropTables(db);
        createTables(db);
    }

    public static void saveUser(Person person) {
        save(TEAM_TABLE, person, getWritableDb());
    }

    private static void save(String table, Person person, SQLiteDatabase database) {
        ContentValues values = new ContentValues();
        values.put("person_id", person.getPersonID());
        values.put("local_blog_id", person.getLocalTableBlogId());
        values.put("display_name", person.getDisplayName());
        values.put("avatar_url", person.getAvatarUrl());

        switch (table) {
            case TEAM_TABLE:
                values.put("user_name", person.getUsername());
                values.put("role", person.getRole());
                break;
            case FOLLOWERS_TABLE:
                values.put("user_name", person.getUsername());
                values.put("subscribed", person.getSubscribed());
                break;
            case EMAIL_FOLLOWERS_TABLE:
                values.put("subscribed", person.getSubscribed());
                break;
        }

        database.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void saveUsers(List<Person> peopleList, int localTableBlogId, boolean isFreshList) {
        savePeople(TEAM_TABLE, peopleList, localTableBlogId, isFreshList);
    }

    public static void saveFollowers(List<Person> peopleList, int localTableBlogId, boolean isFreshList) {
        savePeople(FOLLOWERS_TABLE, peopleList, localTableBlogId, isFreshList);
    }

    public static void saveEmailFollowers(List<Person> peopleList, int localTableBlogId, boolean isFreshList) {
        savePeople(EMAIL_FOLLOWERS_TABLE, peopleList, localTableBlogId, isFreshList);
    }

    private static void savePeople(String table, List<Person> peopleList, int localTableBlogId, boolean isFreshList) {
        getWritableDb().beginTransaction();
        try {
            // We have a fresh list, remove the previous list of email followers in case it was deleted on remote
            if (isFreshList) {
                PeopleTable.deletePeople(table, localTableBlogId);
            }

            for (Person person : peopleList) {
                PeopleTable.save(table, person, getWritableDb());
            }
            getWritableDb().setTransactionSuccessful();
        } finally {
            getWritableDb().endTransaction();
        }
    }

    public static void deletePeopleForLocalBlogId(int localTableBlogId) {
        deletePeople(TEAM_TABLE, localTableBlogId);
        deletePeople(FOLLOWERS_TABLE, localTableBlogId);
        deletePeople(EMAIL_FOLLOWERS_TABLE, localTableBlogId);
    }

    private static void deletePeople(String table, int localTableBlogId) {
        String[] args = new String[]{Integer.toString(localTableBlogId)};
        getWritableDb().delete(table, "local_blog_id=?1", args);
    }

    /**
     * In order to avoid syncing issues, this method will be called when People page is created. We only keep
     * the first page of users, so we don't show an empty screen. When fresh data is received, it'll replace
     * the existing page.
     * @param localTableBlogId - the local blog id people will be deleted from
     */
    public static void deletePeopleExceptForFirstPage(int localTableBlogId) {
        int fetchLimit = PeopleUtils.FETCH_LIMIT;
        String[] tables = {TEAM_TABLE, FOLLOWERS_TABLE, EMAIL_FOLLOWERS_TABLE};
        for (String table : tables) {
            int size = getPeopleCountForLocalBlogId(table, localTableBlogId);
            if (size > fetchLimit) {
                int deleteCount = size - fetchLimit;
                String[] args = new String[] {Integer.toString(localTableBlogId), Integer.toString(deleteCount)};
                getWritableDb().delete(table, "local_blog_id=?1 AND person_id IN (SELECT person_id FROM "
                        + table + " WHERE local_blog_id=?1" + orderByString(table) + " DESC LIMIT ?2)", args);
            }
        }
    }

    public static int getUsersCountForLocalBlogId(int localTableBlogId) {
        return getPeopleCountForLocalBlogId(TEAM_TABLE, localTableBlogId);
    }

    private static int getPeopleCountForLocalBlogId(String table, int localTableBlogId) {
        String[] args = new String[]{Integer.toString(localTableBlogId)};
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE local_blog_id=?";
        return SqlUtils.intForQuery(getReadableDb(), sql, args);
    }

    public static void deleteUser(long personID, int localTableBlogId) {
        deletePerson(TEAM_TABLE, personID, localTableBlogId);
    }

    private static void deletePerson(String table, long personID, int localTableBlogId) {
        String[] args = new String[]{Long.toString(personID), Integer.toString(localTableBlogId)};
        getWritableDb().delete(table, "person_id=? AND local_blog_id=?", args);
    }

    public static List<Person> getUsers(int localTableBlogId) {
        return PeopleTable.getPeople(TEAM_TABLE, localTableBlogId);
    }

    public static List<Person> getFollowers(int localTableBlogId) {
        return PeopleTable.getPeople(FOLLOWERS_TABLE, localTableBlogId);
    }

    public static List<Person> getEmailFollowers(int localTableBlogId) {
        return PeopleTable.getPeople(EMAIL_FOLLOWERS_TABLE, localTableBlogId);
    }

    private static List<Person> getPeople(String table, int localTableBlogId) {
        String[] args = {Integer.toString(localTableBlogId)};
        String orderBy = orderByString(table);
        Cursor c = getReadableDb().rawQuery("SELECT * FROM " + table + " WHERE local_blog_id=?" + orderBy, args);

        List<Person> people = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                Person person = getPersonFromCursor(c, table, localTableBlogId);
                people.add(person);
            }
        } finally {
            SqlUtils.closeCursor(c);
        }
        return people;
    }

    public static Person getPerson(long personId, int localTableBlogId, Person.PersonType personType) {
        String table = null;
        switch (personType) {
            case USER:
                table = TEAM_TABLE;
                break;
            case FOLLOWER:
                table = FOLLOWERS_TABLE;
                break;
            case EMAIL_FOLLOWER:
                table = EMAIL_FOLLOWERS_TABLE;
                break;
        }
        return getPerson(table, personId, localTableBlogId);
    }

    public static Person getUser(long personId, int localTableBlogId) {
        return getPerson(TEAM_TABLE, personId, localTableBlogId);
    }

    /**
     * retrieve a person
     * @param table - sql table the person record is in
     * @param personId - id of a person in a particular blog
     * @param localTableBlogId - the local blog id the user belongs to
     * @return Person if found, null otherwise
     */
    private static Person getPerson(String table, long personId, int localTableBlogId) {
        String[] args = { Long.toString(personId), Integer.toString(localTableBlogId)};
        Cursor c = getReadableDb().rawQuery("SELECT * FROM " + table +
                " WHERE person_id=? AND local_blog_id=?", args);
        try {
            if (!c.moveToFirst()) {
                return null;
            }
            return getPersonFromCursor(c, table, localTableBlogId);
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    private static Person getPersonFromCursor(Cursor c, String table, int localTableBlogId) {
        long personId = c.getInt(c.getColumnIndex("person_id"));

        Person person = new Person(personId, localTableBlogId);
        person.setDisplayName(c.getString(c.getColumnIndex("display_name")));
        person.setAvatarUrl(c.getString(c.getColumnIndex("avatar_url")));
        switch (table) {
            case TEAM_TABLE:
                person.setUsername(c.getString(c.getColumnIndex("user_name")));
                person.setRole(c.getString(c.getColumnIndex("role")));
                person.setPersonType(Person.PersonType.USER);
                break;
            case FOLLOWERS_TABLE:
                person.setUsername(c.getString(c.getColumnIndex("user_name")));
                person.setSubscribed(c.getString(c.getColumnIndex("subscribed")));
                person.setPersonType(Person.PersonType.FOLLOWER);
                break;
            case EMAIL_FOLLOWERS_TABLE:
                person.setSubscribed(c.getString(c.getColumnIndex("subscribed")));
                person.setPersonType(Person.PersonType.EMAIL_FOLLOWER);
                break;
        }

        return person;
    }

    // order is disabled for followers for now since the API is not supporting it
    private static String orderByString(String table) {
        if (table.equals(FOLLOWERS_TABLE) || table.equals(EMAIL_FOLLOWERS_TABLE)) {
            return "";
        }
        return " ORDER BY lower(display_name), lower(user_name)";
    }
}
