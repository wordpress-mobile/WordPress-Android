package org.wordpress.android.ui.accounts.login.nav;

import java.util.Stack;

public class LoginNavController implements LoginFsmGetter {

    public interface ContextImplementation {
        void toast(String message);

        void showEmailLoginScreen();
    }

    private ContextImplementation mContextImplementation;

    public LoginNavController(Class<? extends LoginNav> initialLoginNav,
            ContextImplementation contextImplementation) {
        mLoginNavStack.push(newNavHandler(initialLoginNav));

        mContextImplementation = contextImplementation;
    }

    private Object newNavHandler(Class<? extends LoginNav> loginNav) {
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

    private Stack<Object> mLoginNavStack = new Stack<>();

    private boolean isInState(Class<? extends LoginNav> loginNav) {
        return !mLoginNavStack.empty() && loginNav.isAssignableFrom(mLoginNavStack.peek().getClass());
    }

    public void ensureState(Class<? extends LoginNav> loginNav) {
        if (!isInState(loginNav)) {
            throw new RuntimeException("Not in state " + loginNav.getSimpleName());
        }
    }

    private void gotoState(Class<? extends LoginNav> loginNav) {
        if (!isInState(loginNav)) {
            mLoginNavStack.push(newNavHandler(loginNav));
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
        mLoginNavStack.push(newNavHandler(loginNav));
    }

    private class PrologueHandler implements LoginNav.Prologue {
        @Override
        public void doStartLogin() {
            ensureState(LoginNav.Prologue.class);
            gotoState(LoginNav.InputEmail.class);

            mContextImplementation.showEmailLoginScreen();
        }

        @Override
        public void doStartSignup() {
            ensureState(LoginNav.Prologue.class);
            gotoState(LoginNav.Prologue.class);

            mContextImplementation.toast("Signup is not implemented yet");
        }
    }

    private class InputEmailHandler implements LoginNav.InputEmail {
        @Override
        public void gotEmail(String email) {
            ensureState(LoginNav.InputEmail.class);
            gotoState(LoginNav.InputEmail.class);

            mContextImplementation.toast("Input email is not implemented yet. Input email: " + email);
        }

        @Override
        public void loginViaUsernamePassword() {
            ensureState(LoginNav.InputEmail.class);
            gotoState(LoginNav.InputEmail.class);

            mContextImplementation.toast("Fall back to username/password is not implemented yet.");
        }
    }

    private class InputSiteAddressHandler implements LoginNav.InputSiteAddress {
        @Override
        public void gotSiteAddress(String siteAddress) {
            ensureState(LoginNav.InputSiteAddress.class);
            gotoState(LoginNav.InputSiteAddress.class);

            mContextImplementation.toast("Input site address is not implemented yet. Input site address: " + siteAddress);
        }
    }

    @Override
    public LoginNav.Prologue getLoginNavPrologue() {
        try {
            return (PrologueHandler) mLoginNavStack.peek();
        } catch (ClassCastException cce) {
            throw new RuntimeException("Not in state " + LoginNav.Prologue.class.getSimpleName());
        }
    }

    @Override
    public LoginNav.InputEmail getLoginNavInputEmail() {
        try {
            return (InputEmailHandler) mLoginNavStack.peek();
        } catch (ClassCastException cce) {
            throw new RuntimeException("Not in state " + LoginNav.InputEmail.class.getSimpleName());
        }
    }

    @Override
    public LoginNav.InputSiteAddress getLoginNavInputSiteAddress() {
        try {
            return (InputSiteAddressHandler) mLoginNavStack.peek();
        } catch (ClassCastException cce) {
            throw new RuntimeException("Not in state " + LoginNav.InputSiteAddress.class.getSimpleName());
        }
    }
}
