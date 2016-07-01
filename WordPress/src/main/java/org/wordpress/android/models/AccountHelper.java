package org.wordpress.android.models;

import org.wordpress.android.datasets.AccountTable;

/**
 * The app supports only one WordPress.com account at the moment, so we might use getDefaultAccount() everywhere we
 * need the account data.
 */
// TODO: STORES: kill this - this is just a mock object used to pass access token to legacy code (rest client mostly)
public class AccountHelper {
    private static AccountLegacy sAccount;
    private final static Object mLock = new Object();

    public static AccountLegacy getDefaultAccount() {
        if (sAccount == null) {
            // Singleton pattern in concurrent env.
            synchronized (mLock) {
                sAccount = AccountTable.getDefaultAccount();
                if (sAccount == null) {
                    sAccount = new AccountLegacy();
                }
            }
        }
        return sAccount;
    }
}
