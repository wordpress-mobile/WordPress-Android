package org.wordpress.android.stores.account;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.stores.model.AccountModel;

@RunWith(RobolectricTestRunner.class)
public class AccountModelTest {
    @Before
    public void setUp() {
    }

    @Test
    public void testEquals() {
        AccountModel testAccount = getTestAccount();
        AccountModel testAccount2 = getTestAccount();
        Assert.assertFalse(testAccount.equals(new Object()));
        Assert.assertFalse(testAccount.equals(null));
        testAccount2.setUserId(testAccount.getUserId() + 1);
        Assert.assertFalse(testAccount.equals(testAccount2));
        testAccount2.setUserId(testAccount.getUserId());
        Assert.assertTrue(testAccount.equals(testAccount2));
    }

    @Test
    public void testCopyOnlyAccountAttributes() {
        AccountModel testAccount = getTestAccount();
        AccountModel copyAccount = getTestAccount();
        copyAccount.setUserName("copyUsername");
        copyAccount.setUserId(testAccount.getUserId() + 1);
        copyAccount.setDisplayName("copyDisplayName");
        copyAccount.setProfileUrl("copyProfileUrl");
        copyAccount.setAvatarUrl("copyAvatarUrl");
        copyAccount.setPrimaryBlogId(testAccount.getPrimaryBlogId() + 1);
        copyAccount.setSiteCount(testAccount.getSiteCount() + 1);
        copyAccount.setVisibleSiteCount(testAccount.getVisibleSiteCount() + 1);
        copyAccount.setEmail("copyEmail");
        copyAccount.setPendingEmailChange(!testAccount.getPendingEmailChange());
        Assert.assertFalse(copyAccount.equals(testAccount));
        testAccount.copyAccountAttributes(copyAccount);
        Assert.assertFalse(copyAccount.equals(testAccount));
        copyAccount.setPendingEmailChange(testAccount.getPendingEmailChange());
        Assert.assertTrue(copyAccount.equals(testAccount));
    }

    @Test
    public void testCopyOnlyAccountSettingsAttributes() {
        AccountModel testAccount = getTestAccount();
        AccountModel copyAccount = getTestAccount();
        copyAccount.setUserName("copyUsername");
        copyAccount.setPrimaryBlogId(testAccount.getPrimaryBlogId() + 1);
        copyAccount.setFirstName("copyFirstName");
        copyAccount.setLastName("copyLastName");
        copyAccount.setAboutMe("copyAboutMe");
        copyAccount.setDate("copyDate");
        copyAccount.setNewEmail("copyNewEmail");
        copyAccount.setPendingEmailChange(!testAccount.getPendingEmailChange());
        copyAccount.setWebAddress("copyWebAddress");
        copyAccount.setUserId(testAccount.getUserId() + 1);
        Assert.assertFalse(copyAccount.equals(testAccount));
        testAccount.copyAccountSettingsAttributes(copyAccount);
        Assert.assertFalse(copyAccount.equals(testAccount));
        copyAccount.setUserId(testAccount.getUserId());
        Assert.assertTrue(copyAccount.equals(testAccount));
    }

    private AccountModel getTestAccount() {
        AccountModel testModel = new AccountModel();
        testModel.setId(1);
        testModel.setUserId(2);
        return testModel;
    }
}
