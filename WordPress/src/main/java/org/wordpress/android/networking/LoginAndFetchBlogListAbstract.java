package org.wordpress.android.networking;

import org.wordpress.android.ui.accounts.SetupBlog;

import java.util.List;
import java.util.Map;

public abstract class LoginAndFetchBlogListAbstract {
    protected SetupBlog mSetupBlog;
    protected String mUsername;
    protected String mPassword;
    protected Callback mCallback;

    public interface Callback {
        void onSuccess(List<Map<String, Object>> userBlogList);
        void onError(int messageId, boolean httpAuthRequired, boolean erroneousSslCertificate);
    }

    public LoginAndFetchBlogListAbstract(String username, String password) {
        mUsername = username;
        mPassword = password;
    }

    public void execute(Callback callback) {
        init();
        mCallback = callback;
        new Thread() {
            @Override
            public void run() {
                loginAndGetBlogList();
            }
        }.start();
    }

    protected abstract void loginAndGetBlogList();

    protected void init() {
        mSetupBlog = new SetupBlog();
        mSetupBlog.setUsername(mUsername);
        mSetupBlog.setPassword(mPassword);
    }
}