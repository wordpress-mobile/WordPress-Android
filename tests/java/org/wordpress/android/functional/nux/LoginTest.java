package org.wordpress.android.functional.nux;

import org.wordpress.android.ActivityRobotiumTestCase;
import org.wordpress.android.R;
import org.wordpress.android.mocks.RestClientFactoryTest;
import org.wordpress.android.mocks.XMLRPCFactoryTest;
import org.wordpress.android.ui.accounts.WelcomeActivity;
import org.wordpress.android.ui.posts.PostsActivity;

public class LoginTest extends ActivityRobotiumTestCase<PostsActivity> {
    public LoginTest() {
        super(PostsActivity.class);
    }

    public void testGoodCredentials() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("default");
        XMLRPCFactoryTest.setPrefixAllInstances("default");
        mSolo.enterText(0, "test");
        mSolo.enterText(1, "test");
        mSolo.clickOnText(mSolo.getString(R.string.sign_in));
    }

    public void testBadCredentials() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("login-failure");
        XMLRPCFactoryTest.setPrefixAllInstances("login-failure");
        mSolo.enterText(0, "test");
        mSolo.enterText(1, "test");
        mSolo.clickOnText(mSolo.getString(R.string.sign_in));
        boolean errorMessageFound = mSolo.searchText(mSolo.getString(R.string.username_or_password_incorrect));
        assertTrue("Error message not found", errorMessageFound);
    }

    public void testCreateAccountInvalidEmail() throws Exception {
        mSolo.clickOnText(mSolo.getString(R.string.nux_welcome_create_account));
        mSolo.waitForText(mSolo.getString(R.string.create_account_wpcom));
        mSolo.clearEditText(0);
        mSolo.enterText(0, "test");
        mSolo.enterText(1, "test");
        mSolo.enterText(2, "test");
        mSolo.clickOnText(mSolo.getString(R.string.nux_welcome_create_account));
        boolean errorMessageFound = mSolo.searchText(mSolo.getString(R.string.invalid_email_message));
        assertTrue("Error message not found", errorMessageFound);
    }

    public void testCreateAccountUsernameExists() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("username-exists");
        mSolo.clickOnText(mSolo.getString(R.string.nux_welcome_create_account));
        mSolo.waitForText(mSolo.getString(R.string.create_account_wpcom));
        mSolo.clearEditText(0);
        mSolo.enterText(0, "test@test.com");
        mSolo.enterText(1, "test");
        mSolo.enterText(2, "test");
        mSolo.clickOnText(mSolo.getString(R.string.nux_welcome_create_account));
        boolean errorMessageFound = mSolo.searchText(mSolo.getString(R.string.username_exists));
        assertTrue("Error message not found", errorMessageFound);
    }

    public void testCreateAccountPasswordTooShort() throws Exception {
        mSolo.clickOnText(mSolo.getString(R.string.nux_welcome_create_account));
        mSolo.waitForText(mSolo.getString(R.string.create_account_wpcom));
        mSolo.clearEditText(0);
        mSolo.enterText(0, "test@test.com");
        mSolo.enterText(1, "test");
        mSolo.enterText(2, "tes");
        mSolo.clickOnText(mSolo.getString(R.string.nux_welcome_create_account));
        boolean errorMessageFound = mSolo.searchText(mSolo.getString(R.string.invalid_password_message));
        assertTrue("Error message not found", errorMessageFound);
    }

    public void testCreateAccountPasswordTooWeak() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("password-invalid");
        mSolo.clickOnText(mSolo.getString(R.string.nux_welcome_create_account));
        mSolo.waitForText(mSolo.getString(R.string.create_account_wpcom));
        mSolo.clearEditText(0);
        mSolo.enterText(0, "test@test.com");
        mSolo.enterText(1, "test");
        mSolo.enterText(2, "test");
        mSolo.clickOnText(mSolo.getString(R.string.nux_welcome_create_account));
        boolean errorMessageFound = mSolo.searchText(mSolo.getString(R.string.password_invalid));
        assertTrue("Error message not found", errorMessageFound);
    }

    public void testCreateAccountOk() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("default");
        XMLRPCFactoryTest.setPrefixAllInstances("default");
        mSolo.clickOnText(mSolo.getString(R.string.nux_welcome_create_account));
        mSolo.waitForText(mSolo.getString(R.string.create_account_wpcom));
        mSolo.clearEditText(0);
        mSolo.enterText(0, "test@test.com");
        mSolo.enterText(1, "test");
        mSolo.enterText(2, "test");
        mSolo.clickOnText(mSolo.getString(R.string.nux_welcome_create_account));
    }

    public void testLoginMalformedGetUsersBlog() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("default");
        XMLRPCFactoryTest.setPrefixAllInstances("malformed-getusersblog");
        mSolo.enterText(0, "test");
        mSolo.enterText(1, "test");
        mSolo.clickOnText(mSolo.getString(R.string.sign_in));
    }

    public void testMalformedSelfHostedURL() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("default");
        XMLRPCFactoryTest.setPrefixAllInstances("default");
        mSolo.clickOnText(mSolo.getString(R.string.nux_add_selfhosted_blog));
        mSolo.enterText(0, "test@test.com");
        mSolo.enterText(1, "test");
        mSolo.enterText(2, "==+--\\||##a");
        mSolo.clickOnText(mSolo.getString(R.string.sign_in));
    }

    // reproduce https://github.com/wordpress-mobile/WordPress-Android/issues/1354
    public void testMalformedXMLRPCUrl() throws Exception {
        RestClientFactoryTest.setPrefixAllInstances("default");
        XMLRPCFactoryTest.setPrefixAllInstances("1354");
        mSolo.enterText(0, "test");
        mSolo.enterText(1, "test");
        mSolo.clickOnText(mSolo.getString(R.string.sign_in));
        mSolo.waitForActivity(WelcomeActivity.class);
    }
}
