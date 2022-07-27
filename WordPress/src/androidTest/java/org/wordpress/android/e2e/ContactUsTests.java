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
    public void sendButtonEnabledWhenTextIsEntered() {
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

    @Ignore("As long as CI does not use gradle.properties from MobileSecrets")
    @Test
    public void messageCanBeSent() {
        String userMessageText = "Please ignore, this is an automated test.";
        String automatedReplyText = "Mobile support will respond as soon as possible, "
                                    + "generally within 48-96 hours. "
                                    + "Please reply with your site address (URL) "
                                    + "and any additional details we should know.";

        try {
            new LoginFlow()
                .chooseContinueWithWpCom()
                .tapHelp()
                .openContactUs()
                .setMessageText(userMessageText)
                .tapSendButton()
                .assertUserMessageDelivered(userMessageText)
                .assertSystemMessageReceived(automatedReplyText);
        } finally {
            new ContactSupportScreen().goBackAndDeleteUnsentMessageIfNeeded();
        }
    }

    @Ignore("Ignored temporarily. This sometimes fail on CI while running with whole test suite.")
    @Test
    public void helpCanBeOpenedWhileEnteringEmail() {
        new LoginFlow()
                .chooseContinueWithWpCom()
                .tapHelp()
                .assertHelpAndSupportScreenLoaded();
    }

    @Ignore("Ignored temporarily. This sometimes fail on CI while running with whole test suite.")
    @Test
    public void helpCanBeOpenedWhileEnteringPassword() {
        new LoginFlow()
                .chooseContinueWithWpCom()
                .enterEmailAddress(E2E_WP_COM_USER_EMAIL)
                .tapHelp()
                .assertHelpAndSupportScreenLoaded();
    }
}
