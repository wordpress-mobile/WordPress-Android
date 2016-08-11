package org.wordpress.android.fluxc.account;

import android.content.ContentValues;
import android.content.Context;

import com.wellsql.generated.AccountModelTable;
import com.yarolegovich.wellsql.WellSql;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.persistence.AccountSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AccountSqlUtilsTest {
    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(appContext, AccountModel.class);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testInsertNullAccount() {
        Assert.assertEquals(0, AccountSqlUtils.insertOrUpdateDefaultAccount(null));
        Assert.assertTrue(AccountSqlUtils.getAllAccounts().isEmpty());
    }

    @Test
    public void testInsertAndRetrieveAccount() {
        AccountModel testAccount = getTestAccount();
        Assert.assertEquals(0, AccountSqlUtils.insertOrUpdateDefaultAccount(testAccount));
        AccountModel dbAccount = AccountSqlUtils.getAccountByLocalId(testAccount.getId());
        Assert.assertNotNull(dbAccount);
        Assert.assertEquals(testAccount, dbAccount);
    }

    @Test
    public void testUpdateAccount() {
        AccountModel testAccount = getTestAccount();
        testAccount.setUserName("test0");
        Assert.assertEquals(0, AccountSqlUtils.insertOrUpdateDefaultAccount(testAccount));
        testAccount.setUserName("test1");
        Assert.assertEquals(1, AccountSqlUtils.insertOrUpdateDefaultAccount(testAccount));
        AccountModel dbAccount = AccountSqlUtils.getAccountByLocalId(testAccount.getId());
        Assert.assertEquals(testAccount, dbAccount);
    }

    @Test
    public void testUpdateSpecificFields() {
        AccountModel testAccount = getTestAccount();
        Assert.assertEquals(0, AccountSqlUtils.insertOrUpdateDefaultAccount(testAccount));
        String testAboutMe = "New About Me";
        String testEmail = "newEmail";
        ContentValues testFields = new ContentValues();
        testFields.put(AccountModelTable.ABOUT_ME, testAboutMe);
        testFields.put(AccountModelTable.EMAIL, testEmail);
        Assert.assertEquals(1, AccountSqlUtils.updateAccount(testAccount.getId(), testFields));
        AccountModel dbAccount = AccountSqlUtils.getAccountByLocalId(testAccount.getId());
        Assert.assertNotNull(dbAccount);
        Assert.assertEquals(dbAccount.getAboutMe(), testAboutMe);
        Assert.assertEquals(dbAccount.getEmail(), testEmail);
    }

    @Test
    public void testGetAllAccounts() {
        AccountModel testAccount0 = getTestAccount();
        AccountModel testAccount1 = getTestAccount();
        testAccount1.setId(testAccount0.getId() + 1);
        Assert.assertEquals(0, AccountSqlUtils.insertOrUpdateAccount(testAccount0, testAccount0.getId()));
        Assert.assertEquals(0, AccountSqlUtils.insertOrUpdateAccount(testAccount1, testAccount1.getId()));
        List<AccountModel> allAccounts = AccountSqlUtils.getAllAccounts();
        Assert.assertNotNull(allAccounts);
        Assert.assertEquals(2, allAccounts.size());
    }

    @Test
    public void getDefaultAccount() {
        AccountModel testAccount = getTestAccount();
        Assert.assertEquals(0, AccountSqlUtils.insertOrUpdateDefaultAccount(testAccount));
        AccountModel defaultAccount = AccountSqlUtils.getDefaultAccount();
        Assert.assertNotNull(defaultAccount);
        Assert.assertEquals(testAccount.getUserId(), defaultAccount.getUserId());
    }

    @Test
    public void testDeleteAccount() {
        AccountModel testAccount = getTestAccount();
        Assert.assertEquals(0, AccountSqlUtils.insertOrUpdateDefaultAccount(testAccount));
        Assert.assertEquals(1, AccountSqlUtils.deleteAccount(testAccount));
        Assert.assertNull(AccountSqlUtils.getAccountByLocalId(testAccount.getId()));
    }

    private AccountModel getTestAccount() {
        AccountModel testModel = new AccountModel();
        testModel.setId(1);
        testModel.setUserId(2);
        return testModel;
    }
}
