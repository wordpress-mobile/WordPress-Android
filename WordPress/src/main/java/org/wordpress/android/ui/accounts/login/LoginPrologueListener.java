package org.wordpress.android.ui.accounts.login;

import java.util.ArrayList;

public interface LoginPrologueListener {
    // Login Prologue callbacks
    void showEmailLoginScreen();

    void doStartSignup();

    void loginViaSiteAddress();

    void loggedInViaSignup(ArrayList<Integer> oldSitesIds);

    void newUserCreatedButErrored(String email, String password);
}
