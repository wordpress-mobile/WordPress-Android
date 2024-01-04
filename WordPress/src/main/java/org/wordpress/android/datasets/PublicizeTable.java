package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.models.PublicizeConnectionList;
import org.wordpress.android.models.PublicizeService;
import org.wordpress.android.models.PublicizeService.Status;
import org.wordpress.android.models.PublicizeServiceList;
import org.wordpress.android.util.SqlUtils;

public class PublicizeTable {
    private static final String SERVICES_TABLE = "tbl_publicize_services";
    private static final String CONNECTIONS_TABLE = "tbl_publicize_connections";

    public static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + SERVICES_TABLE + " ("
                   + " id TEXT NOT NULL COLLATE NOCASE,"
                   + " label TEXT NOT NULL COLLATE NOCASE,"
                   + " description TEXT NOT NULL,"
                   + " genericon TEXT NOT NULL,"
                   + " icon_url TEXT NOT NULL,"
                   + " connect_url TEXT NOT NULL,"
                   + " is_jetpack_supported INTEGER DEFAULT 0,"
                   + " is_multi_user_id_supported INTEGER DEFAULT 0,"
                   + " is_external_users_only INTEGER DEFAULT 0,"
                   + " status TEXT NOT NULL,"
                   + " PRIMARY KEY (id))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + CONNECTIONS_TABLE + " ("
                   + " id INTEGER DEFAULT 0,"
                   + " site_id INTEGER DEFAULT 0,"
                   + " user_id INTEGER DEFAULT 0,"
                   + " keyring_connection_id INTEGER DEFAULT 0,"
                   + " keyring_connection_user_id INTEGER DEFAULT 0,"
                   + " is_shared INTEGER DEFAULT 0,"
                   + " service TEXT NOT NULL COLLATE NOCASE,"
                   + " label TEXT NOT NULL COLLATE NOCASE,"
                   + " external_id TEXT NOT NULL,"
                   + " external_name TEXT NOT NULL,"
                   + " external_display TEXT NOT NULL,"
                   + " external_profile_picture TEXT NOT NULL,"
                   + " refresh_url TEXT NOT NULL,"
                   + " status TEXT NOT NULL,"
                   + " PRIMARY KEY (id))");
    }

    private static SQLiteDatabase getReadableDb() {
        return WordPress.wpDB.getDatabase();
    }

    private static SQLiteDatabase getWritableDb() {
        return WordPress.wpDB.getDatabase();
    }

    public static void resetServicesTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + SERVICES_TABLE);
    }

    public static PublicizeService getService(String serviceId) {
        if (TextUtils.isEmpty(serviceId)) {
            return null;
        }

        String[] args = {serviceId};
        Cursor c = getReadableDb().rawQuery("SELECT * FROM " + SERVICES_TABLE + " WHERE id=?", args);
        try {
            if (c.moveToFirst()) {
                return getServiceFromCursor(c);
            } else {
                return null;
            }
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static PublicizeServiceList getServiceList() {
        PublicizeServiceList serviceList = new PublicizeServiceList();
        Cursor c = getReadableDb().rawQuery("SELECT * FROM " + SERVICES_TABLE + " ORDER BY label", null);
        try {
            while (c.moveToNext()) {
                serviceList.add(getServiceFromCursor(c));
            }
            return serviceList;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static void setServiceList(final PublicizeServiceList serviceList) {
        SQLiteStatement stmt = null;
        SQLiteDatabase db = getWritableDb();
        db.beginTransaction();
        try {
            db.delete(SERVICES_TABLE, null, null);

            stmt = db.compileStatement(
                    "INSERT INTO " + SERVICES_TABLE
                    + " (id," // 1
                    + " label," // 2
                    + " description," // 3
                    + " genericon," // 4
                    + " icon_url," // 5
                    + " connect_url," // 6
                    + " is_jetpack_supported," // 7
                    + " is_multi_user_id_supported," // 8
                    + " is_external_users_only," // 9
                    + " status)" // 10
                    + " VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10)");
            for (PublicizeService service : serviceList) {
                stmt.bindString(1, service.getId());
                stmt.bindString(2, service.getLabel());
                stmt.bindString(3, service.getDescription());
                stmt.bindString(4, service.getGenericon());
                stmt.bindString(5, service.getIconUrl());
                stmt.bindString(6, service.getConnectUrl());
                stmt.bindLong(7, SqlUtils.boolToSql(service.isJetpackSupported()));
                stmt.bindLong(8, SqlUtils.boolToSql(service.isMultiExternalUserIdSupported()));
                stmt.bindLong(9, SqlUtils.boolToSql(service.isExternalUsersOnly()));
                stmt.bindString(10, service.getStatus().getValue());
                stmt.executeInsert();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }

    private static boolean getBooleanFromCursor(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 && cursor.getInt(columnIndex) != 0;
    }

    private static PublicizeService getServiceFromCursor(Cursor c) {
        PublicizeService service = new PublicizeService();

        service.setId(c.getString(c.getColumnIndexOrThrow("id")));
        service.setLabel(c.getString(c.getColumnIndexOrThrow("label")));
        service.setDescription(c.getString(c.getColumnIndexOrThrow("description")));
        service.setGenericon(c.getString(c.getColumnIndexOrThrow("genericon")));
        service.setIconUrl(c.getString(c.getColumnIndexOrThrow("icon_url")));
        service.setConnectUrl(c.getString(c.getColumnIndexOrThrow("connect_url")));
        service.setIsJetpackSupported(getBooleanFromCursor(c, "is_jetpack_supported"));
        service.setIsMultiExternalUserIdSupported(getBooleanFromCursor(c, "is_multi_user_id_supported"));
        service.setIsExternalUsersOnly(getBooleanFromCursor(c, "is_external_users_only"));
        service.setStatus(Status.fromString(c.getString(c.getColumnIndexOrThrow("status"))));

        return service;
    }

    public static boolean onlyExternalConnections(String serviceId) {
        if (serviceId == null && serviceId.isEmpty()) {
            return false;
        }

        String sql = "SELECT is_external_users_only FROM " + SERVICES_TABLE + " WHERE id=?";
        String[] args = {serviceId};
        return SqlUtils.boolForQuery(getReadableDb(), sql, args);
    }

    public static String getConnectUrlForService(String serviceId) {
        if (TextUtils.isEmpty(serviceId)) {
            return "";
        }
        String sql = "SELECT connect_url FROM " + SERVICES_TABLE + " WHERE id=?";
        String[] args = {serviceId};
        return SqlUtils.stringForQuery(getReadableDb(), sql, args);
    }

    public static String getRefreshUrlForConnection(int connectionId) {
        String sql = "SELECT refresh_url FROM " + CONNECTIONS_TABLE + " WHERE id=?";
        String[] args = {Integer.toString(connectionId)};
        return SqlUtils.stringForQuery(getReadableDb(), sql, args);
    }

    public static boolean deleteConnection(int connectionId) {
        String[] args = {Integer.toString(connectionId)};
        int numDeleted = getReadableDb().delete(CONNECTIONS_TABLE, "id=?", args);
        return numDeleted > 0;
    }

    public static void addOrUpdateConnection(PublicizeConnection connection) {
        if (connection == null) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put("id", connection.connectionId);
        values.put("site_id", connection.siteId);
        values.put("user_id", connection.userId);
        values.put("keyring_connection_id", connection.keyringConnectionId);
        values.put("keyring_connection_user_id", connection.keyringConnectionUserId);
        values.put("is_shared", connection.isShared);
        values.put("service", connection.getService());
        values.put("label", connection.getLabel());
        values.put("external_id", connection.getExternalId());
        values.put("external_name", connection.getExternalName());
        values.put("external_display", connection.getExternalDisplayName());
        values.put("external_profile_picture", connection.getExternalProfilePictureUrl());
        values.put("refresh_url", connection.getRefreshUrl());
        values.put("status", connection.getStatus());

        getReadableDb().insertWithOnConflict(CONNECTIONS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static PublicizeConnectionList getConnectionsForSite(long siteId) {
        PublicizeConnectionList connectionList = new PublicizeConnectionList();
        String[] args = {Long.toString(siteId)};
        Cursor c = getReadableDb().rawQuery("SELECT * FROM " + CONNECTIONS_TABLE + " WHERE site_id=?", args);
        try {
            while (c.moveToNext()) {
                connectionList.add(getConnectionFromCursor(c));
            }
            return connectionList;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static void setConnectionsForSite(long siteId, PublicizeConnectionList connectionList) {
        SQLiteStatement stmt = null;
        SQLiteDatabase db = getWritableDb();
        db.beginTransaction();
        try {
            db.delete(CONNECTIONS_TABLE, "site_id=?", new String[]{Long.toString(siteId)});

            stmt = db.compileStatement(
                    "INSERT INTO " + CONNECTIONS_TABLE
                    + " (id," // 1
                    + " site_id," // 2
                    + " user_id," // 3
                    + " keyring_connection_id," // 4
                    + " keyring_connection_user_id," // 5
                    + " is_shared," // 6
                    + " service," // 7
                    + " label," // 8
                    + " external_id," // 9
                    + " external_name," // 10
                    + " external_display," // 11
                    + " external_profile_picture," // 12
                    + " refresh_url," // 13
                    + " status)" // 14
                    + " VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14)");
            for (PublicizeConnection connection : connectionList) {
                stmt.bindLong(1, connection.connectionId);
                stmt.bindLong(2, connection.siteId);
                stmt.bindLong(3, connection.userId);
                stmt.bindLong(4, connection.keyringConnectionId);
                stmt.bindLong(5, connection.keyringConnectionUserId);

                stmt.bindLong(6, SqlUtils.boolToSql(connection.isShared));

                stmt.bindString(7, connection.getService());
                stmt.bindString(8, connection.getLabel());
                stmt.bindString(9, connection.getExternalId());
                stmt.bindString(10, connection.getExternalName());
                stmt.bindString(11, connection.getExternalDisplayName());
                stmt.bindString(12, connection.getExternalProfilePictureUrl());
                stmt.bindString(13, connection.getRefreshUrl());
                stmt.bindString(14, connection.getStatus());

                stmt.executeInsert();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }

    private static PublicizeConnection getConnectionFromCursor(Cursor c) {
        PublicizeConnection connection = new PublicizeConnection();

        connection.siteId = c.getLong(c.getColumnIndexOrThrow("site_id"));
        connection.connectionId = c.getInt(c.getColumnIndexOrThrow("id"));
        connection.userId = c.getInt(c.getColumnIndexOrThrow("user_id"));
        connection.keyringConnectionId = c.getInt(c.getColumnIndexOrThrow("keyring_connection_id"));
        connection.keyringConnectionUserId = c.getInt(c.getColumnIndexOrThrow("keyring_connection_user_id"));

        connection.isShared = SqlUtils.sqlToBool(c.getInt(c.getColumnIndexOrThrow("is_shared")));

        connection.setService(c.getString(c.getColumnIndexOrThrow("service")));
        connection.setLabel(c.getString(c.getColumnIndexOrThrow("label")));
        connection.setExternalId(c.getString(c.getColumnIndexOrThrow("external_id")));
        connection.setExternalName(c.getString(c.getColumnIndexOrThrow("external_name")));
        connection.setExternalDisplayName(c.getString(c.getColumnIndexOrThrow("external_display")));
        connection.setExternalProfilePictureUrl(c.getString(c.getColumnIndexOrThrow("external_profile_picture")));
        connection.setRefreshUrl(c.getString(c.getColumnIndexOrThrow("refresh_url")));
        connection.setStatus(c.getString(c.getColumnIndexOrThrow("status")));

        return connection;
    }
}
