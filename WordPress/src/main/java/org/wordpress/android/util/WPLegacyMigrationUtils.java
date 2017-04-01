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
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class WPLegacyMigrationUtils {
    private static final String DEPRECATED_DATABASE_NAME = "wordpress";
    private static final String DEPRECATED_ACCOUNT_TABLE = "tbl_accounts";
    private static final String DEPRECATED_ACCESS_TOKEN_COLUMN = "access_token";
    private static final String DEPRECATED_ACCESS_TOKEN_PREFERENCE = "wp_pref_wpcom_access_token";
    private static final String DEPRECATED_BLOGS_TABLE = "accounts";
    private static final String DEPRECATED_POSTS_TABLE = "posts";

    private static final String DEPRECATED_DB_PASSWORD_SECRET = BuildConfig.DB_SECRET;

    /**
     * Moves an existing access token from a previous version of WPAndroid into FluxC's AccountStore.
     * The access token has historically existed in preferences and two DB tables.
     */
    public static String migrateAccessTokenToAccountStore(Context context, Dispatcher dispatcher) {
        String token = getLatestDeprecatedAccessToken(context.getApplicationContext());

        // updating from previous app version
        if (!TextUtils.isEmpty(token)) {
            AccountStore.UpdateTokenPayload payload = new AccountStore.UpdateTokenPayload(token);
            dispatcher.dispatch(AccountActionBuilder.newUpdateAccessTokenAction(payload));
        }
        return token;
    }

    /**
     * Copies existing self-hosted sites from a previous version of WPAndroid into FluxC's SiteStore.
     * Any Jetpack sites are ignored - those connected to the logged-in WP.com account will be pulled through the
     * REST API after migration. Other Jetpack sites will not be migrated.
     * Existing sites are retained in the deprecated accounts table after migration.
     */
    public static List<SiteModel> migrateSelfHostedSitesFromDeprecatedDB(Context context, Dispatcher dispatcher) {
        List<SiteModel> siteList = getSelfHostedSitesFromDeprecatedDB(context.getApplicationContext());
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

    /**
     * Copies existing drafts and locally changed posts from a previous version of WPAndroid into FluxC's PostStore.
     * Existing posts are retained in the deprecated posts table after migration.
     */
    public static void migrateDraftsFromDeprecatedDB(Context context, Dispatcher dispatcher, SiteStore siteStore) {
        List<PostModel> postList = getDraftsFromDeprecatedDB(context.getApplicationContext(), siteStore);
        if (postList != null) {
            AppLog.i(T.DB, "Starting migration of " + postList.size() + " drafts to FluxC");
            for (PostModel postModel : postList) {
                AppLog.i(T.DB, "Migrating draft with title: " + postModel.getTitle()
                        + " and local site ID: " + postModel.getLocalSiteId());
                dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(postModel));
            }
        }
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

    static String getAccessTokenFromTable(Context context, String tableName) {
        String token = null;
        try {
            SQLiteDatabase db = context.openOrCreateDatabase(DEPRECATED_DATABASE_NAME, 0, null);
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

    public static boolean selfHostedSiteToMigrateEh(Context context) {
        try {
            SQLiteDatabase db = context.openOrCreateDatabase(DEPRECATED_DATABASE_NAME, 0, null);
            String[] fields = new String[]{"username", "password", "url", "homeURL", "blogId", "api_blogid"};

            // To exclude the jetpack sites we need to check for empty password
            String byString = String.format("dotcomFlag=0 AND NOT(dotcomFlag=0 AND password='%s')",
                    encryptPassword(""));
            Cursor c = db.query(DEPRECATED_BLOGS_TABLE, fields, byString, null, null, null, null);
            int numRows = c.getCount();
            c.moveToFirst();
            for (int i = 0; i < numRows; i++) {
                long apiBlogId = StringUtils.stringToLong(c.getString(5));
                if (apiBlogId > 0) {
                    // If the api_blogid field is set, that's probably a Jetpack site that is not connected to the main
                    // account, so we want to skip it.
                    c.moveToNext();
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

    static List<SiteModel> getSelfHostedSitesFromDeprecatedDB(Context context) {
        List<SiteModel> siteList = new ArrayList<>();
        try {
            SQLiteDatabase db = context.openOrCreateDatabase(DEPRECATED_DATABASE_NAME, 0, null);
            String[] fields = new String[]{"username", "password", "url", "homeURL", "blogId", "api_blogid", "isAdmin"};

            // To exclude the jetpack sites we need to check for empty password
            String byString = String.format("dotcomFlag=0 AND NOT(dotcomFlag=0 AND password='%s')",
                    encryptPassword(""));
            Cursor c = db.query(DEPRECATED_BLOGS_TABLE, fields, byString, null, null, null, null);
            int numRows = c.getCount();
            c.moveToFirst();
            for (int i = 0; i < numRows; i++) {
                long apiBlogId = StringUtils.stringToLong(c.getString(5));
                if (apiBlogId > 0) {
                    // If the api_blogid field is set, that's probably a Jetpack site that is not connected to the main
                    // account, so we want to skip it.
                    c.moveToNext();
                    continue;
                }

                String username = c.getString(0);
                String encryptedPwd = c.getString(1);
                String xmlrpcUrl = c.getString(2);

                if (TextUtils.isEmpty(username)) {
                    AppLog.d(T.DB, "Found a self-hosted site with no username - skipping it.");
                    c.moveToNext();
                    continue;
                }

                if (TextUtils.isEmpty(xmlrpcUrl)) {
                    AppLog.d(T.DB, "Found a self-hosted site with no XML-RPC URL - skipping it.");
                    c.moveToNext();
                    continue;
                }

                SiteModel siteModel = new SiteModel();
                siteModel.setUsername(username);
                // Decrypt password before migrating since we no longer encrypt passwords in FluxC
                siteModel.setPassword(decryptPassword(encryptedPwd));
                siteModel.setXmlRpcUrl(xmlrpcUrl);

                String url = c.getString(3);
                if (!TextUtils.isEmpty(url)) {
                    siteModel.setUrl(url);
                }

                siteModel.setSelfHostedSiteId(c.getLong(4));
                siteModel.setIsSelfHostedAdmin(SqlUtils.sqlToBool(c.getInt(6)));

                siteList.add(siteModel);
                c.moveToNext();
            }
            c.close();
        } catch (SQLException e) {
            // DB doesn't exist
        }
        return siteList;
    }

    public static boolean draftsToMigrateEh(Context context) {
        try {
            SQLiteDatabase db = context.openOrCreateDatabase(DEPRECATED_DATABASE_NAME, 0, null);

            String byString = "localDraft=1 OR isLocalChange=1";
            Cursor c = db.query(DEPRECATED_POSTS_TABLE, null, byString, null, null, null, null);
            if (c.getCount() > 0) {
                c.close();
                return true;
            }
            c.close();
            return false;
        } catch (SQLException e) {
            return false;
        }
    }

    static List<PostModel> getDraftsFromDeprecatedDB(Context context, SiteStore siteStore) {
        List<PostModel> postList = new ArrayList<>();
        try {
            SQLiteDatabase db = context.openOrCreateDatabase(DEPRECATED_DATABASE_NAME, 0, null);

            String byString = "localDraft=1 OR isLocalChange=1";
            Cursor c = db.query(DEPRECATED_POSTS_TABLE, null, byString, null, null, null, null);
            int numRows = c.getCount();
            c.moveToFirst();
            for (int i = 0; i < numRows; i++) {
                PostModel postModel = new PostModel();

                Cursor siteCursor = db.query(DEPRECATED_BLOGS_TABLE,
                        new String[]{"dotcomFlag","blogId","url","api_blogid"},
                        String.format("id=%s", c.getInt(c.getColumnIndex("blogID"))), null, null, null, null);

                if (siteCursor.getCount() > 0) {
                    siteCursor.moveToFirst();

                    boolean dotcomFlag = siteCursor.getInt(0) == 1;
                    int blogId = siteCursor.getInt(1);
                    String xmlrpcUrl = siteCursor.getString(2);
                    long apiBlogId = StringUtils.stringToLong(siteCursor.getString(3));

                    int migratedSiteLocalId;
                    if (dotcomFlag) {
                        // WP.com site - identify it by WP.com site ID
                        migratedSiteLocalId = siteStore.getLocalIdForRemoteSiteId(blogId);
                    } else if (apiBlogId > 0) {
                        // Jetpack site - identify it by WP.com site ID
                        migratedSiteLocalId = siteStore.getLocalIdForRemoteSiteId(apiBlogId);
                    } else {
                        // Self-hosted site - identify it by its self-hosted site ID and XML-RPC URL
                        migratedSiteLocalId = siteStore.getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(blogId, xmlrpcUrl);
                    }
                    postModel.setLocalSiteId(migratedSiteLocalId);
                    siteCursor.close();
                } else {
                    AppLog.d(T.DB, "Couldn't find site corresponding to draft in deprecated DB! " +
                            "Site local id " + c.getInt(c.getColumnIndex("blogID")) +
                            " - Post title: " + c.getString(c.getColumnIndex("title")));
                    c.moveToNext();
                    siteCursor.close();
                    continue;
                }

                postModel.setIsLocalDraft(SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("localDraft"))));
                postModel.setIsLocallyChanged(SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("isLocalChange"))));

                String postId = c.getString(c.getColumnIndex("postid"));
                if (!TextUtils.isEmpty(postId)) {
                    postModel.setRemotePostId(StringUtils.stringToLong(postId));
                }

                postModel.setTitle(StringUtils.unescapeHTML(c.getString(c.getColumnIndex("title"))));

                String descriptionText = c.getString(c.getColumnIndex("description"));
                String moreText = c.getString(c.getColumnIndex("mt_text_more"));
                if (TextUtils.isEmpty(moreText)) {
                    postModel.setContent(descriptionText);
                } else {
                    postModel.setContent(descriptionText + "\n<!--more-->\n" + moreText);
                }

                long dateCreated = c.getLong(c.getColumnIndex("date_created_gmt"));
                if (dateCreated > 0) {
                    postModel.setDateCreated(DateTimeUtils.iso8601UTCFromTimestamp(dateCreated / 1000));
                }

                // Safety check as 'dateLastUpdated' was somewhat recently added and a user migrating from an old
                // version of the app might not have it
                int dateLastUpdatedIndex = c.getColumnIndex("dateLastUpdated");
                long dateLocallyChanged = dateLastUpdatedIndex > 0 ? c.getLong(dateLastUpdatedIndex) : 0;
                if (dateLocallyChanged > 0) {
                    postModel.setDateLocallyChanged(DateTimeUtils.iso8601UTCFromTimestamp(dateLocallyChanged / 1000));
                }

                int featuredImageIndex = c.getColumnIndex("wp_post_thumbnail");
                long featuredImageId = featuredImageIndex > 0 ? c.getLong(featuredImageIndex) : 0;
                postModel.setFeaturedImageId(featuredImageId);

                postModel.setExcerpt(c.getString(c.getColumnIndex("mt_excerpt")));
                postModel.setLink(c.getString(c.getColumnIndex("link")));
                postModel.setTagNames(c.getString(c.getColumnIndex("mt_keywords")));
                postModel.setStatus(c.getString(c.getColumnIndex("post_status")));
                postModel.setPassword(c.getString(c.getColumnIndex("wp_password")));
                postModel.setPostFormat(c.getString(c.getColumnIndex("wp_post_format")));
                postModel.setIsPage(SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("pageEh"))));

                int latColumnIndex = c.getColumnIndex("latitude");
                int lngColumnIndex = c.getColumnIndex("longitude");
                if (!c.isNull(latColumnIndex) && !c.isNull(lngColumnIndex)) {
                    postModel.setLocation(c.getDouble(latColumnIndex), c.getDouble(lngColumnIndex));
                }

                postModel.setCustomFields(c.getString(c.getColumnIndex("custom_fields")));

                postList.add(postModel);
                c.moveToNext();
            }
            c.close();
        } catch (SQLException e) {
            // DB doesn't exist
        }
        return postList;
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
