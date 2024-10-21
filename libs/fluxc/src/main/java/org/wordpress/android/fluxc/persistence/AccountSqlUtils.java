package org.wordpress.android.fluxc.persistence;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.wellsql.generated.AccountModelTable;
import com.wellsql.generated.SubscriptionModelTable;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;

import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.SubscriptionModel;

import java.util.List;

public class AccountSqlUtils {
    private static final int DEFAULT_ACCOUNT_LOCAL_ID = 1;

    /**
     * Adds or overwrites all columns for a matching row in the Account Table.
     */
    public static int insertOrUpdateDefaultAccount(AccountModel account) {
        return insertOrUpdateAccount(account, DEFAULT_ACCOUNT_LOCAL_ID);
    }

    public static int insertOrUpdateAccount(AccountModel account, int localId) {
        if (account == null) {
            return 0;
        }
        account.setId(localId);
        SQLiteDatabase db = WellSql.giveMeWritableDb();
        db.beginTransaction();
        try {
            List<AccountModel> accountResults = WellSql.select(AccountModel.class)
                                                       .where()
                                                       .equals(AccountModelTable.ID, localId)
                                                       .endWhere().getAsModel();
            if (accountResults.isEmpty()) {
                WellSql.insert(account).execute();
                db.setTransactionSuccessful();
                return 0;
            } else {
                ContentValues cv = new UpdateAllExceptId<>(AccountModel.class).toCv(account);
                int result = updateAccount(accountResults.get(0).getId(), cv);
                db.setTransactionSuccessful();
                return result;
            }
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Updates an existing row in the Account Table that matches the given local ID. Only columns
     * defined in the given {@link ContentValues} keys are modified.
     */
    public static int updateAccount(long localId, final ContentValues cv) {
        AccountModel account = getAccountByLocalId(localId);
        if (account == null || cv == null) return 0;
        return WellSql.update(AccountModel.class).whereId(account.getId())
                .put(account, new InsertMapper<AccountModel>() {
                    @Override
                    public ContentValues toCv(AccountModel item) {
                        return cv;
                    }
                }).execute();
    }

    /**
     * Update the username in the {@link AccountModelTable} that matches the given {@link AccountModel}.
     *
     * @param accountModel  {@link AccountModel} to update with username
     * @param username      username to update in {@link AccountModelTable#USER_NAME}
     *
     * @return zero if update is not performed; non-zero otherwise
     */
    public static int updateUsername(AccountModel accountModel, final String username) {
        if (accountModel == null || username == null) {
            return 0;
        } else {
            return WellSql.update(AccountModel.class).whereId(accountModel.getId())
                    .put(accountModel, new InsertMapper<AccountModel>() {
                        @Override
                        public ContentValues toCv(AccountModel item) {
                            ContentValues cv = new ContentValues();
                            cv.put(AccountModelTable.USER_NAME, username);
                            return cv;
                        }
                    }).execute();
        }
    }

    /**
     * Deletes rows from the Account table that share an ID with the given {@link AccountModel}.
     */
    public static int deleteAccount(AccountModel account) {
        return account == null ? 0 : WellSql.delete(AccountModel.class)
                .where().equals(AccountModelTable.ID, account.getId()).endWhere().execute();
    }

    public static List<AccountModel> getAllAccounts() {
        return WellSql.select(AccountModel.class).getAsModel();
    }

    /**
     * Passthrough to {@link #getAccountByLocalId(long)} using the default Account local ID.
     */
    public static AccountModel getDefaultAccount() {
        return getAccountByLocalId(DEFAULT_ACCOUNT_LOCAL_ID);
    }

    /**
     * Attempts to load an Account with the given local ID from the Account Table.
     *
     * @return the Account row as {@link AccountModel}, null if no row matches the given ID
     */
    public static AccountModel getAccountByLocalId(long localId) {
        List<AccountModel> accountResult = WellSql.select(AccountModel.class)
                .where().equals(AccountModelTable.ID, localId)
                .endWhere().getAsModel();
        return accountResult.isEmpty() ? null : accountResult.get(0);
    }

    /**
     * Get list of {@link SubscriptionModel} matching {@param searchString} by blog name or URL.
     *
     * @param searchString      Text to filter subscriptions by
     *
     * @return {@link List} of {@link SubscriptionModel}
     */
    public static List<SubscriptionModel> getSubscriptionsByNameOrUrlMatching(String searchString) {
        return WellSql.select(SubscriptionModel.class)
                      .where().contains(SubscriptionModelTable.BLOG_NAME, searchString)
                      .or().contains(SubscriptionModelTable.URL, searchString)
                      .endWhere().getAsModel();
    }

    /**
     * Update list of {@link SubscriptionModel} by deleting existing subscriptions and inserting {@param subscriptions}.
     *
     * @param subscriptions     {@link List} of {@link SubscriptionModel} to insert into database
     */
    public static synchronized void updateSubscriptions(@NonNull List<SubscriptionModel> subscriptions) {
        WellSql.delete(SubscriptionModel.class).execute();
        WellSql.insert(subscriptions).execute();
    }
}
