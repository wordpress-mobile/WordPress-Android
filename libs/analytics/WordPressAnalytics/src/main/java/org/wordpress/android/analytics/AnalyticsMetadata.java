package org.wordpress.android.analytics;

public class AnalyticsMetadata {
    private boolean mUserConnectedEh;
    private boolean mWordPressComUserEh;
    private boolean mJetpackUserEh;
    private int mSessionCount;
    private int mNumBlogs;
    private String mUsername = "";
    private String mEmail = "";

    public AnalyticsMetadata() {}

    public boolean userConnectedEh() {
        return mUserConnectedEh;
    }

    public void setUserConnected(boolean userConnectedEh) {
        mUserConnectedEh = userConnectedEh;
    }

    public boolean wordPressComUserEh() {
        return mWordPressComUserEh;
    }

    public void setWordPressComUser(boolean wordPressComUserEh) {
        mWordPressComUserEh = wordPressComUserEh;
    }

    public boolean jetpackUserEh() {
        return mJetpackUserEh;
    }

    public void setJetpackUser(boolean jetpackUserEh) {
        mJetpackUserEh = jetpackUserEh;
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
}
