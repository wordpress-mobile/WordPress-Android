package org.wordpress.android.ui.accounts.login.nav;

import java.util.Stack;

public class LoginNavController implements LoginStateGetter {

    private LoginNavHandler mLoginNavHandler;

    public LoginNavController(Class<? extends LoginNav> initialLoginNav) {
        mLoginNavStack.push(initialLoginNav);
    }

    public LoginNavController setLoginNavHandler(LoginNavHandler loginNavHandler) {
        mLoginNavHandler = loginNavHandler;

        return this;
    }

    private LoginNav newNavHandler(Class<? extends LoginNav> loginNav) {
        if (loginNav.isAssignableFrom(LoginNav.Prologue.class)) {
            return new PrologueHandler();
        }

        if (loginNav.isAssignableFrom(LoginNav.InputEmail.class)) {
            return new InputEmailHandler();
        }

        if (loginNav.isAssignableFrom(LoginNav.InputSiteAddress.class)) {
            return new InputSiteAddressHandler();
        }

        throw new RuntimeException("Unsupported login state " + loginNav.getSimpleName());
    }

    private Stack<Class<? extends LoginNav>> mLoginNavStack = new Stack<>();

    private boolean isInState(Class<? extends LoginNav> loginNav) {
        return !mLoginNavStack.empty() && loginNav.equals(mLoginNavStack.peek());
    }

    public void ensureState(Class<? extends LoginNav> loginNav) {
        if (!isInState(loginNav)) {
            throw new RuntimeException("Not in state " + loginNav.getSimpleName());
        }
    }

    private void gotoState(Class<? extends LoginNav> loginNav) {
        if (!isInState(loginNav)) {
            mLoginNavStack.push(loginNav);
        }
    }

    public void goBack() {
        if (mLoginNavStack.isEmpty()) {
            throw new RuntimeException("Navigation stack is empty! Can't go back.");
        }

        mLoginNavStack.pop();
    }

    public boolean isNavStackEmpty() {
        return mLoginNavStack.isEmpty();
    }

    // available for testing purposes. Don't use otherwise
    public void force(Class<? extends LoginNav> loginNav) {
        mLoginNavStack.push(loginNav);
    }

    private class PrologueHandler implements LoginNav.Prologue {
        @Override
        public void doStartLogin() {
            ensureState(LoginNav.Prologue.class);
            gotoState(LoginNav.InputEmail.class);

            mLoginNavHandler.showEmailLoginScreen();
        }

        @Override
        public void doStartSignup() {
            ensureState(LoginNav.Prologue.class);
            gotoState(LoginNav.Prologue.class);

            mLoginNavHandler.toast("Signup is not implemented yet");
        }
    }

    private class InputEmailHandler implements LoginNav.InputEmail {
        @Override
        public void gotEmail(String email) {
            ensureState(LoginNav.InputEmail.class);
            gotoState(LoginNav.InputEmail.class);

            mLoginNavHandler.toast("Input email is not implemented yet. Input email: " + email);
        }

        @Override
        public void loginViaUsernamePassword() {
            ensureState(LoginNav.InputEmail.class);
            gotoState(LoginNav.InputEmail.class);

            mLoginNavHandler.toast("Fall back to username/password is not implemented yet.");
        }

        @Override
        public void help() {
            ensureState(LoginNav.InputEmail.class);
            gotoState(LoginNav.InputEmail.class);

            mLoginNavHandler.toast("Help is not implemented yet.");
        }
    }

    private class InputSiteAddressHandler implements LoginNav.InputSiteAddress {
        @Override
        public void gotSiteAddress(String siteAddress) {
            ensureState(LoginNav.InputSiteAddress.class);
            gotoState(LoginNav.InputSiteAddress.class);

            mLoginNavHandler.toast("Input site address is not implemented yet. Input site address: " + siteAddress);
        }
    }

    @Override
    public LoginNav.Prologue getLoginNavPrologue() {
        try {
            return (LoginNav.Prologue) newNavHandler(mLoginNavStack.peek());
        } catch (ClassCastException cce) {
            throw new RuntimeException("Not in state " + LoginNav.Prologue.class.getSimpleName());
        }
    }

    @Override
    public LoginNav.InputEmail getLoginNavInputEmail() {
        try {
            return (InputEmailHandler) newNavHandler(mLoginNavStack.peek());
        } catch (ClassCastException cce) {
            throw new RuntimeException("Not in state " + LoginNav.InputEmail.class.getSimpleName());
        }
    }

    @Override
    public LoginNav.InputSiteAddress getLoginNavInputSiteAddress() {
        try {
            return (InputSiteAddressHandler) newNavHandler(mLoginNavStack.peek());
        } catch (ClassCastException cce) {
            throw new RuntimeException("Not in state " + LoginNav.InputSiteAddress.class.getSimpleName());
        }
    }
}
