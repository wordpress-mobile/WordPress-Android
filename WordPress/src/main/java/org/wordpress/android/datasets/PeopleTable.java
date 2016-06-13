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
    private static final String PEOPLE_TABLE = "people";

    private static SQLiteDatabase getReadableDb() {
        return WordPress.wpDB.getDatabase();
    }
    private static SQLiteDatabase getWritableDb() {
        return WordPress.wpDB.getDatabase();
    }

    public static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + PEOPLE_TABLE + " ("
                + "person_id               INTEGER DEFAULT 0,"
                + "blog_id                 TEXT,"
                + "local_blog_id           INTEGER DEFAULT 0,"
                + "user_name               TEXT,"
                + "first_name              TEXT,"
                + "last_name               TEXT,"
                + "display_name            TEXT,"
                + "avatar_url              TEXT,"
                + "role                    TEXT,"
                + "PRIMARY KEY (person_id, local_blog_id)"
                + ");");
    }

    private static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + PEOPLE_TABLE);
    }

    public static void reset(SQLiteDatabase db) {
        AppLog.i(AppLog.T.PEOPLE, "resetting people table");
        dropTables(db);
        createTables(db);
    }

    public static void migrationAddFollowerFields(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + PEOPLE_TABLE + " ADD is_follower BOOLEAN DEFAULT false;");
        db.execSQL("ALTER TABLE " + PEOPLE_TABLE + " ADD is_email_follower BOOLEAN DEFAULT false;");
    }

    public static void save(Person person) {
        save(person, getWritableDb());
    }

    public static void save(Person person, SQLiteDatabase database) {
        ContentValues values = new ContentValues();
        values.put("person_id", person.getPersonID());
        values.put("blog_id", person.getBlogId());
        values.put("local_blog_id", person.getLocalTableBlogId());
        values.put("user_name", person.getUsername());
        values.put("first_name", person.getFirstName());
        values.put("last_name", person.getLastName());
        values.put("display_name", person.getDisplayName());
        values.put("avatar_url", person.getAvatarUrl());
        values.put("role", person.getRole());
        values.put("is_follower", person.isFollower());
        values.put("is_email_follower", person.isEmailFollower());
        database.insertWithOnConflict(PEOPLE_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void saveUsers(List<Person> peopleList, int localTableBlogId, boolean isFreshList) {
        getWritableDb().beginTransaction();
        try {
            // We have a fresh list, remove the previous list of users in case it was deleted on remote
            if (isFreshList) {
                PeopleTable.deleteUsersForLocalBlogId(localTableBlogId);
            }

            for (Person person : peopleList) {
                PeopleTable.save(person);
            }
            getWritableDb().setTransactionSuccessful();
        } finally {
            getWritableDb().endTransaction();
        }
    }

    public static void saveFollowers(List<Person> peopleList, int localTableBlogId, boolean isFreshList) {
        getWritableDb().beginTransaction();
        try {
            // We have a fresh list, remove the previous list of followers in case it was deleted on remote
            if (isFreshList) {
                PeopleTable.deleteFollowersForLocalBlogId(localTableBlogId);
            }

            for (Person person : peopleList) {
                PeopleTable.save(person);
            }
            getWritableDb().setTransactionSuccessful();
        } finally {
            getWritableDb().endTransaction();
        }
    }

    public static void deleteUsersForLocalBlogId(int localTableBlogId) {
        String[] args = new String[]{Integer.toString(localTableBlogId), Boolean.toString(false)};
        getWritableDb().delete(PEOPLE_TABLE, "local_blog_id=?&is_follower=?", args);
    }

    public static void deleteFollowersForLocalBlogId(int localTableBlogId) {
        String[] args = new String[]{Integer.toString(localTableBlogId), Boolean.toString(true)};
        getWritableDb().delete(PEOPLE_TABLE, "local_blog_id=?&is_follower=?", args);
    }

    public static void deleteUsersForLocalBlogIdExceptForFirstPage(int localTableBlogId) {
        PeopleTable.deletePeopleForLocalBlogIdExceptForFirstPage(localTableBlogId, false);
    }

    public static void deleteFollowersForLocalBlogIdExceptForFirstPage(int localTableBlogId) {
        PeopleTable.deletePeopleForLocalBlogIdExceptForFirstPage(localTableBlogId, false);
    }

    private static void deletePeopleForLocalBlogIdExceptForFirstPage(int localTableBlogId, boolean isFollower) {
        int size = getPeopleCountForLocalBlogId(localTableBlogId, isFollower);
        int fetchLimit = PeopleUtils.FETCH_USERS_LIMIT;
        if (size > fetchLimit) {
            int deleteCount = size - fetchLimit;
            String[] args = new String[] {
                    Integer.toString(localTableBlogId),
                    Boolean.toString(isFollower),
                    Integer.toString(deleteCount)
            };
            getWritableDb().delete(PEOPLE_TABLE, "local_blog_id=?1 AND is_follower=?2 AND person_id " +
                    "IN (SELECT person_id FROM " + PEOPLE_TABLE + " WHERE local_blog_id=?1 AND is_follower=?2 " +
                    "ORDER BY display_name DESC LIMIT ?3)", args);
        }
    }

    public static int getUsersCountForLocalBlogId(int localTableBlogId) {
        return PeopleTable.getPeopleCountForLocalBlogId(localTableBlogId, false);
    }

    private static int getPeopleCountForLocalBlogId(int localTableBlogId, boolean isFollower) {
        String[] args = new String[]{Integer.toString(localTableBlogId), Boolean.toString(isFollower)};
        String sql = "SELECT COUNT(*) FROM " + PEOPLE_TABLE + " WHERE local_blog_id=?&is_follower=?";
        return SqlUtils.intForQuery(getReadableDb(), sql, args);
    }

    public static void deletePerson(long personID, int localTableBlogId) {
        String[] args = new String[]{Long.toString(personID), Integer.toString(localTableBlogId)};
        getWritableDb().delete(PEOPLE_TABLE, "person_id=? AND local_blog_id=?", args);
    }

    public static List<Person> getUsers(int localTableBlogId) {
        return PeopleTable.getPeople(localTableBlogId, false);
    }

    private static List<Person> getPeople(int localTableBlogId, boolean isFollower) {
        List<Person> people = new ArrayList<>();
        String[] args = { Integer.toString(localTableBlogId), Boolean.toString(isFollower) };
        Cursor c = getReadableDb().rawQuery("SELECT * FROM " + PEOPLE_TABLE +
                " WHERE local_blog_id=? AND is_follower=? ORDER BY lower(display_name), lower(user_name)", args);

        try {
            while (c.moveToNext()) {
                Person person = getPersonFromCursor(c, localTableBlogId);
                people.add(person);
            }

            return people;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    /**
     * retrieve a single person
     * @param personId - id of a person in a particular blog
     * @param localTableBlogId - the local blog id the user belongs to
     * @return Person if found, null otherwise
     */
    public static Person getPerson(long personId, int localTableBlogId) {
        String[] args = { Long.toString(personId), Integer.toString(localTableBlogId) };
        Cursor c = getReadableDb().rawQuery("SELECT * FROM " + PEOPLE_TABLE +
                " WHERE person_id=? AND local_blog_id=?", args);
        try {
            if (!c.moveToFirst()) {
                return null;
            }
            return getPersonFromCursor(c, localTableBlogId);
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    private static Person getPersonFromCursor(Cursor c, int localTableBlogId) {
        long personId = c.getInt(c.getColumnIndex("person_id"));
        String blogId = c.getString(c.getColumnIndex("blog_id"));

        Person person = new Person(personId, blogId, localTableBlogId);
        person.setUsername(c.getString(c.getColumnIndex("user_name")));
        person.setFirstName(c.getString(c.getColumnIndex("first_name")));
        person.setLastName(c.getString(c.getColumnIndex("last_name")));
        person.setDisplayName(c.getString(c.getColumnIndex("display_name")));
        person.setAvatarUrl(c.getString(c.getColumnIndex("avatar_url")));
        person.setRole(c.getString(c.getColumnIndex("role")));
        person.setFollower(c.getInt(c.getColumnIndex("is_follower")) > 0);
        person.setEmailFollower(c.getInt(c.getColumnIndex("is_email_follower")) > 0);

        return person;
    }
}
