package org.wordpress.android.util;

import org.wordpress.android.datasets.AccountTable;
import org.wordpress.android.models.Account;

/**
 * The app supports only one WordPress.com account at the moment, so we might use getDefaultAccount() everywhere we
 * need the account data.
 */
public class AccountHelper {
    private static Account sAccount;

    public static Account getDefaultAccount() {
        if (sAccount == null) {
            sAccount = AccountTable.getDefaultAccount();
            if (sAccount == null) {
                sAccount = new Account();
            }
        }
        return sAccount;
    }
}
