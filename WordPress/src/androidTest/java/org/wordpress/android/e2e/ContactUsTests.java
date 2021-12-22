package org.wordpress.android.e2e;

import androidx.test.espresso.Espresso;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wordpress.android.R;
import org.wordpress.android.e2e.flows.LoginFlow;
import org.wordpress.android.e2e.pages.ContactSupportScreen;
import org.wordpress.android.support.BaseTest;

import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_EMAIL;
import static org.wordpress.android.support.WPSupportUtils.pressBackUntilElementIsDisplayed;

public class ContactUsTests extends BaseTest {
    @Before
    public void setUp() {
        logoutIfNecessary();
    }

    @After
    public void tearDown() {
        pressBackUntilElementIsDisplayed(R.id.continue_with_wpcom_button);
    }

    @Test
    public void sendButtonEnabledWhenTextIsEntered() {
        try {
            new LoginFlow()
                .chooseContinueWithWpCom()
                .tapHelp()
                .assertHelpAndSupportScreenLoaded()
                .setEmailIfNeeded("WPcomTest@test.com")
                .openContactUs()
                .assertContactSupportScreenLoaded()
                .assertSendButtonDisabled()
                .setMessageText("Hello")
                .assertSendButtonEnabled()
                .setMessageText("")
                .assertSendButtonDisabled();
        } finally {
            Espresso.pressBack();
            new ContactSupportScreen().deleteUnsentMessageIfNeeded();
        }
    }

    @Test
    public void messageCanBeSent() {
        String senderEmailAddress = "WPcomTest@test.com";
        String userMessageText = "Please ignore, this is an automated test.";
        String automatedReplyText = "Mobile support will respond as soon as possible, "
                                    + "generally within 48-96 hours. "
                                    + "Please reply with your site address (URL) "
                                    + "and any additional details we should know.";

        try {
            new LoginFlow()
                .chooseContinueWithWpCom()
                .tapHelp()
                .setEmailIfNeeded(senderEmailAddress)
                .openContactUs()
                .setMessageText(userMessageText)
                .tapSendButton()
                .assertUserMessageDelivered(userMessageText)
                .assertSystemMessageReceived(automatedReplyText);
        } finally {
            Espresso.pressBack();
            new ContactSupportScreen().deleteUnsentMessageIfNeeded();
        }
    }

    @Test
    public void helpCanBeOpenedWhileEnteringEmail() {
        new LoginFlow()
                .chooseContinueWithWpCom()
                .tapHelp()
                .assertHelpAndSupportScreenLoaded();
    }

    @Test
    public void helpCanBeOpenedWhileEnteringPassword() {
        new LoginFlow()
                .chooseContinueWithWpCom()
                .enterEmailAddress(E2E_WP_COM_USER_EMAIL)
                .tapHelp()
                .assertHelpAndSupportScreenLoaded();
    }
}
