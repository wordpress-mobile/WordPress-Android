package org.wordpress.android.ui.accounts.helpers;

public abstract class LoginAbstract {
    protected String mUsername;
    protected String mPassword;
    protected Callback mCallback;

    public interface Callback {
        void onSuccess();
        void onError(int errorMessageId, boolean twoStepCodeRequired, boolean httpAuthRequired, boolean erroneousSslCertificate);
    }

    public LoginAbstract(String username, String password) {
        mUsername = username;
        mPassword = password;
    }

    public void execute(Callback callback) {
        mCallback = callback;
        new Thread() {
            @Override
            public void run() {
                login();
            }
        }.start();
    }

    protected abstract void login();
}