package org.wordpress.android.e2e;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.wordpress.android.e2e.flows.LoginFlow;
import org.wordpress.android.e2e.pages.ContactSupportScreen;
import org.wordpress.android.support.BaseTest;

import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_EMAIL;

import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class ContactUsTests extends BaseTest {
    @Before
    public void setUp() {
        logoutIfNecessary();
    }

    @Ignore("Ignored temporarily. This sometimes fail on CI while running with whole test suite.")
    @Test
    public void e2eSendButtonEnabledWhenTextIsEntered() {
        try {
            new LoginFlow()
                .chooseContinueWithWpCom()
                .tapHelp()
                .assertHelpAndSupportScreenLoaded()
                .openContactUs()
                .assertContactSupportScreenLoaded()
                .assertSendButtonDisabled()
                .setMessageText("Hello")
                .assertSendButtonEnabled()
                .setMessageText("")
                .assertSendButtonDisabled();
        } finally {
            new ContactSupportScreen().goBackAndDeleteUnsentMessageIfNeeded();
        }
    }

    @Test
    public void e2eHelpCanBeOpenedWhileEnteringEmail() {
        new LoginFlow()
                .chooseContinueWithWpCom()
                .tapHelp()
                .assertHelpAndSupportScreenLoaded();
    }

    @Test
    public void e2eHelpCanBeOpenedWhileEnteringPassword() {
        new LoginFlow()
                .chooseContinueWithWpCom()
                .enterEmailAddress(E2E_WP_COM_USER_EMAIL)
                .tapHelp()
                .assertHelpAndSupportScreenLoaded();
    }
}
