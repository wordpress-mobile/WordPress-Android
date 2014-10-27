package org.wordpress.android.networking;

public class LoginAndFetchBlogListWPOrg extends LoginAndFetchBlogListAbstract {
    private String mSelfHostedURL;
    protected String mHttpUsername;
    protected String mHttpPassword;

    public LoginAndFetchBlogListWPOrg(String username, String password, String selfHostedURL) {
        super(username, password);
        mSelfHostedURL = selfHostedURL;
    }

    public void setHttpCredentials(String username, String password) {
        mHttpUsername = username;
        mHttpPassword = password;
    }

    protected void init() {
        super.init();
        mSetupBlog.setSelfHostedURL(mSelfHostedURL);
    }

    protected void loginAndGetBlogList() {
        mSetupBlog.getBlogList(mCallback);
    }
}
