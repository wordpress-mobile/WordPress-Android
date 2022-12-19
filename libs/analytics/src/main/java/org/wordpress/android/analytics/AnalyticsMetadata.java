package org.wordpress.android.analytics;

public class AnalyticsMetadata {
    private boolean mIsUserConnected;
    private boolean mIsWordPressComUser;
    private boolean mIsJetpackUser;
    private int mSessionCount;
    private int mNumBlogs;
    private String mUsername = "";
    private String mEmail = "";
    private String mAppScheme = "";
    private boolean mIsGutenbergEnabled;
    private boolean mIsGutenbergEnabledVariableSet;

    public AnalyticsMetadata() {
    }

    public boolean isUserConnected() {
        return mIsUserConnected;
    }

    public void setUserConnected(boolean isUserConnected) {
        mIsUserConnected = isUserConnected;
    }

    public boolean isWordPressComUser() {
        return mIsWordPressComUser;
    }

    public void setWordPressComUser(boolean isWordPressComUser) {
        mIsWordPressComUser = isWordPressComUser;
    }

    public boolean isJetpackUser() {
        return mIsJetpackUser;
    }

    public void setJetpackUser(boolean isJetpackUser) {
        mIsJetpackUser = isJetpackUser;
    }

    public int getSessionCount() {
        return mSessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.mSessionCount = sessionCount;
    }

    public int getNumBlogs() {
        return mNumBlogs;
    }

    public void setNumBlogs(int numBlogs) {
        this.mNumBlogs = numBlogs;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        this.mUsername = username;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        this.mEmail = email;
    }

    public String getAppScheme() {
        return mAppScheme;
    }

    public void setAppScheme(String scheme) {
        this.mAppScheme = scheme;
    }

    public boolean isGutenbergEnabled() {
        return mIsGutenbergEnabled;
    }

    public boolean isGutenbergEnabledVariableSet() {
        return mIsGutenbergEnabledVariableSet;
    }

    public void setGutenbergEnabled(boolean gutenbergEnabled) {
        this.mIsGutenbergEnabled = gutenbergEnabled;
        this.mIsGutenbergEnabledVariableSet = true;
    }
}
