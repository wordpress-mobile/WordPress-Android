package org.wordpress.android.fluxc.persistence;

import android.content.ContentValues;

import com.wellsql.generated.AccountModelTable;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;

import org.wordpress.android.fluxc.model.AccountModel;

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
        if (account == null) return 0;
        account.setId(localId);
        List<AccountModel> accountResults = WellSql.select(AccountModel.class)
                .where()
                .equals(AccountModelTable.ID, localId)
                .endWhere().getAsModel();
        if (accountResults.isEmpty()) {
            WellSql.insert(account).execute();
            return 0;
        } else {
            ContentValues cv = new UpdateAllExceptId<>(AccountModel.class).toCv(account);
            return updateAccount(accountResults.get(0).getId(), cv);
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
}
