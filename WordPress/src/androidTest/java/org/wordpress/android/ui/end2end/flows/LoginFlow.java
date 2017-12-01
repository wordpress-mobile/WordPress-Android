package org.wordpress.android.ui.end2end.flows;

import org.wordpress.android.ui.end2end.pages.login.LoginEmailPage;
import org.wordpress.android.ui.end2end.pages.login.LoginEpiloguePage;
import org.wordpress.android.ui.end2end.pages.login.LoginMagicLinkPage;
import org.wordpress.android.ui.end2end.pages.login.LoginPasswordPage;
import org.wordpress.android.ui.end2end.pages.login.LoginProloguePage;

public class LoginFlow {

    public void wpcomLoginEmailPassword(String email, String password) {
        new LoginProloguePage()
                .login();

        new LoginEmailPage()
                .wpcomEmailLogin(email);

        // Need to wait for the email address to be checked.
        // Added a sleep statement for now.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new LoginMagicLinkPage()
                .selectPasswordOption();

        new LoginPasswordPage()
                .enterPassword(password);

        // Need to wait for the password to be checked.
        // Added a sleep statement for now.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new LoginEpiloguePage()
                .closeEpilogue();
    }
}
