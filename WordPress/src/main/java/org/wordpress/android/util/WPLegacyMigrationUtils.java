package org.wordpress.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

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
    private static final String DEPRECATED_ACCESS_TOKEN_COLUMN = "access_token";
    private static final String DEPRECATED_ACCESS_TOKEN_PREFERENCE = "wp_pref_wpcom_access_token";
    private static final String DEPRECATED_BLOGS_TABLE = "accounts";

    private static final String DEPRECATED_DB_PASSWORD_SECRET = BuildConfig.DB_SECRET;

    public static String migrateAccessTokenToAccountStore(Context context, Dispatcher dispatcher) {
        String token = getLatestDeprecatedAccessToken(context);

        // updating from previous app version
        if (!TextUtils.isEmpty(token)) {
            AccountStore.UpdateTokenPayload payload = new AccountStore.UpdateTokenPayload(token);
            dispatcher.dispatch(AccountActionBuilder.newUpdateAccessTokenAction(payload));
        }
        return token;
    }

    public static List<SiteModel> migrateSelfHostedSitesFromDeprecatedDB(Context context, Dispatcher dispatcher) {
        List<SiteModel> siteList = getSelfHostedSitesFromDeprecatedDB(context);
        if (siteList != null) {
            AppLog.i(T.DB, "Starting migration of " + siteList.size() + " self-hosted sites to FluxC");
            for (SiteModel siteModel : siteList) {
                AppLog.i(T.DB, "Migrating self-hosted site with url: " + siteModel.getXmlRpcUrl()
                        + " username: " + siteModel.getUsername());
                dispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(siteModel));
            }
        }
        return siteList;
    }

    private static String getDeprecatedPreferencesAccessToken(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String token = prefs.getString(DEPRECATED_ACCESS_TOKEN_PREFERENCE, null);
        return token;
    }

    public static String getLatestDeprecatedAccessToken(Context context) {
        String latestToken = getAccessTokenFromTable(context, DEPRECATED_ACCOUNT_TABLE);
        if (TextUtils.isEmpty(latestToken)) {
            latestToken = getDeprecatedPreferencesAccessToken(context);
        }
        return latestToken;
    }

    private static String getAccessTokenFromTable(Context context, String tableName) {
        String token = null;
        try {
            SQLiteDatabase db = context.getApplicationContext().openOrCreateDatabase(DEPRECATED_DATABASE_NAME, 0, null);
            Cursor c = db.rawQuery("SELECT " + DEPRECATED_ACCESS_TOKEN_COLUMN
                    + " FROM " + tableName + " WHERE local_id=0", null);
            if (c.moveToFirst() && c.getColumnIndex(DEPRECATED_ACCESS_TOKEN_COLUMN) != -1) {
                token = c.getString(c.getColumnIndex(DEPRECATED_ACCESS_TOKEN_COLUMN));
            }
            c.close();
            db.close();
        } catch (SQLException e) {
            // DB doesn't exist
        }
        return token;
    }

    public static boolean hasSelfHostedSiteToMigrate(Context context) {
        try {
            SQLiteDatabase db = context.getApplicationContext().openOrCreateDatabase(DEPRECATED_DATABASE_NAME, 0, null);
            String[] fields = new String[]{"username", "password", "url", "homeURL", "blogId", "api_blogid"};

            // To exclude the jetpack sites we need to check for empty password
            String byString = String.format("dotcomFlag=0 AND NOT(dotcomFlag=0 AND password='%s')",
                    encryptPassword(""));
            Cursor c = db.query(DEPRECATED_BLOGS_TABLE, fields, byString, null, null, null, null);
            int numRows = c.getCount();
            c.moveToFirst();
            for (int i = 0; i < numRows; i++) {
                if (!TextUtils.isEmpty(c.getString(5))) {
                    continue;
                }
                c.close();
                return true;
            }
            c.close();
            return false;
        } catch (SQLException e) {
            return false;
        }
    }

    private static List<SiteModel> getSelfHostedSitesFromDeprecatedDB(Context context) {
        List<SiteModel> siteList = new ArrayList<>();
        try {
            SQLiteDatabase db = context.getApplicationContext().openOrCreateDatabase(DEPRECATED_DATABASE_NAME, 0, null);
            String[] fields = new String[]{"username", "password", "url", "homeURL", "blogId", "api_blogid"};

            // To exclude the jetpack sites we need to check for empty password
            String byString = String.format("dotcomFlag=0 AND NOT(dotcomFlag=0 AND password='%s')",
                    encryptPassword(""));
            Cursor c = db.query(DEPRECATED_BLOGS_TABLE, fields, byString, null, null, null, null);
            int numRows = c.getCount();
            c.moveToFirst();
            for (int i = 0; i < numRows; i++) {
                // If the api_blogid field is set, that's probably a Jetpack site that is not connected to the main
                // account, so we want to skip it.
                if (!TextUtils.isEmpty(c.getString(5))) {
                    continue;
                }
                SiteModel siteModel = new SiteModel();
                siteModel.setUsername(c.getString(0));
                // Decrypt password before migrating since we no longer encrypt passwords in FluxC
                String encryptedPwd = c.getString(1);
                siteModel.setPassword(decryptPassword(encryptedPwd));

                String xmlrpcUrl = c.getString(2);
                siteModel.setXmlRpcUrl(xmlrpcUrl);
                String url = c.getString(3);
                siteModel.setUrl(url);

                siteModel.setSelfHostedSiteId(c.getLong(4));
                siteList.add(siteModel);
                c.moveToNext();
            }
            c.close();
        } catch (SQLException e) {
            // DB doesn't exist
        }
        return siteList;
    }

    private static String encryptPassword(String clearText) {
        try {
            DESKeySpec keySpec = new DESKeySpec(
                    DEPRECATED_DB_PASSWORD_SECRET.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.encodeToString(cipher.doFinal(clearText.getBytes("UTF-8")), Base64.DEFAULT);
        } catch (Exception e) {
        }
        return clearText;
    }

    private static String decryptPassword(String encryptedPwd) {
        try {
            DESKeySpec keySpec = new DESKeySpec(
                    DEPRECATED_DB_PASSWORD_SECRET.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] encryptedWithoutB64 = Base64.decode(encryptedPwd, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] plainTextPwdBytes = cipher.doFinal(encryptedWithoutB64);
            return new String(plainTextPwdBytes);
        } catch (Exception e) {
        }
        return encryptedPwd;
    }
}
