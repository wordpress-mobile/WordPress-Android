package org.wordpress.android.fluxc;

import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.AccountModel;

public class WellSqlTestUtils {
    public static void setupWordPressComAccount() {
        AccountModel account = new AccountModel();
        account.setUserId(20151021);
        account.setUserName("fluxc");
        account.setEmail("flux@capacitorsrus.co");

        WellSql.insert(account).execute();
    }
}
