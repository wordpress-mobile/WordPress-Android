package org.wordpress.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitesModel;
import org.wordpress.android.fluxc.store.AccountStore;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link #migrateAccessTokenToAccountStore(Context, Dispatcher)} moves an existing access token from a previous version
 * of WPAndroid into {@link AccountStore}. The access token has historically existed in preferences and two DB tables.
 * The existing access token is deleted if found.
 */
public class WPLegacyMigrationUtils {
    //
    // WPStores Access Token migration
    //
    private static final String DEPRECATED_DATABASE_NAME = "wordpress";
    private static final String DEPRECATED_ACCOUNT_TABLE = "tbl_accounts";
    private static final String DEPRECATED_ACCOUNTS_TABLE = "accounts";
    private static final String DEPRECATED_ACCESS_TOKEN_COLUMN = "access_token";
    private static final String DEPRECATED_ACCESS_TOKEN_PREFERENCE = "wp_pref_wpcom_access_token";
    private static final String DEPRECATED_BLOGS_TABLE = "accounts";

    public static String migrateAccessTokenToAccountStore(Context context, Dispatcher dispatcher) {
        String token = getLatestDeprecatedAccessToken(context);

        // updating from previous app version
        if (!TextUtils.isEmpty(token)) {
            AccountStore.UpdateTokenPayload payload = new AccountStore.UpdateTokenPayload(token);
            dispatcher.dispatch(AccountActionBuilder.newUpdateAccessTokenAction(payload));
        }
        return token;
    }

    public static SitesModel migrateSelfHostedSitesFromDeprecatedDB(Context context, Dispatcher dispatcher) {
        SitesModel sitesModel = getSelfHostedSitedFromDeprecatedDB(context);
        if (sitesModel != null) {
            dispatcher.dispatch(SiteActionBuilder.newUpdateSitesAction(sitesModel));
        }
        return sitesModel;
    }

    private static String getDeprecatedPreferencesAccessTokenThenDelete(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String token = prefs.getString(DEPRECATED_ACCESS_TOKEN_PREFERENCE, null);
        if (!TextUtils.isEmpty(token)) {
            prefs.edit().remove(DEPRECATED_ACCESS_TOKEN_PREFERENCE).apply();
        }
        return token;
    }

    private static String getLatestDeprecatedAccessToken(Context context) {
        String latestToken = getAccessTokenFromTableThenDelete(context, DEPRECATED_ACCOUNT_TABLE);
        if (TextUtils.isEmpty(latestToken)) {
            latestToken = getAccessTokenFromTableThenDelete(context, DEPRECATED_ACCOUNTS_TABLE);
        }
        if (TextUtils.isEmpty(latestToken)) {
            latestToken = getDeprecatedPreferencesAccessTokenThenDelete(context);
        }
        return latestToken;
    }

    private static String getAccessTokenFromTableThenDelete(Context context, String tableName) {
        String token = null;
        try {
            SQLiteDatabase db = context.getApplicationContext().openOrCreateDatabase(DEPRECATED_DATABASE_NAME, 0, null);
            Cursor c = db.rawQuery("SELECT " + DEPRECATED_ACCESS_TOKEN_COLUMN
                    + " FROM " + tableName + " WHERE local_id=0", null);
            if (c.moveToFirst() && c.getColumnIndex(DEPRECATED_ACCESS_TOKEN_COLUMN) != -1) {
                token = c.getString(c.getColumnIndex(DEPRECATED_ACCESS_TOKEN_COLUMN));
            }
            c.close();
            if (!TextUtils.isEmpty(token)) {
                db.delete(tableName, "local_id=0", null);
            }
            db.close();
        } catch (SQLException e) {
            // DB doesn't exist
        }
        return token;
    }

    private static SitesModel getSelfHostedSitedFromDeprecatedDB(Context context) {
        List<SiteModel> siteList = new ArrayList<>();
        try {
            SQLiteDatabase db = context.getApplicationContext().openOrCreateDatabase(DEPRECATED_DATABASE_NAME, 0, null);
            String[] fields = new String[]{"username", "password", "url"};
            String byString = "dotcomFlag=0";
            Cursor c = db.query(DEPRECATED_BLOGS_TABLE, fields, byString, null, null, null, null);
            int numRows = c.getCount();
            c.moveToFirst();
            for (int i = 0; i < numRows; i++) {
                SiteModel siteModel = new SiteModel();
                siteModel.setUsername(c.getString(0));
                siteModel.setPassword(c.getString(1));
                siteModel.setUrl(c.getString(2));
                siteList.add(siteModel);
                c.moveToNext();
            }
            c.close();
        } catch (SQLException e) {
            // DB doesn't exist
        }
        return siteList.size() > 0 ? new SitesModel(siteList) : null;
    }
}
