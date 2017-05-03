package org.wordpress.android.ui.accounts.login.nav;

public class LoginNavController implements LoginFsmGetter {

    public interface ContextImplementation {
        void toast(String message);

        void showEmailLoginScreen();
    }

    private PrologueHandler mPrologueHandler = new PrologueHandler();
    private InputEmailHandler mInputEmailHandler = new InputEmailHandler();
    private InputSiteAddressHandler mInputSiteAddressHandler = new InputSiteAddressHandler();
    private ContextImplementation mContextImplementation;

    public LoginNavController(Class<? extends LoginNav> initialLoginNav, ContextImplementation contextImplementation) {
        mCurrentLoginNav = initialLoginNav;

        mContextImplementation = contextImplementation;
    }

    private Class<? extends LoginNav> mCurrentLoginNav = LoginNav.Prologue.class;

    private boolean isInState(Class<? extends LoginNav> loginNav) {
        return mCurrentLoginNav != null && loginNav.isAssignableFrom(mCurrentLoginNav);
    }

    public void ensureState(Class<? extends LoginNav> loginNav) {
        if (!isInState(loginNav)) {
            throw new RuntimeException("Not in state " + loginNav.getSimpleName());
        }
    }

    private void gotoState(Class<? extends LoginNav> loginNav) {
        if (!isInState(loginNav)) {
            mCurrentLoginNav = loginNav;
        }
    }

    // Implementation of LoginNav.PROLOGUE
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
        return mPrologueHandler;
    }

    @Override
    public LoginNav.InputEmail getLoginNavInputEmail() {
        return mInputEmailHandler;
    }

    @Override
    public LoginNav.InputSiteAddress getLoginNavInputSiteAddress() {
        return mInputSiteAddressHandler;
    }
}
