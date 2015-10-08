package org.wordpress.android.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Map;
import java.util.UUID;

import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.util.AppLog;

public abstract class Tracker {
    abstract void track(Stat stat);
    abstract void track(Stat stat, Map<String, ?> properties);
    abstract void endSession();
    abstract void flush();
    abstract void refreshMetadata(boolean isUserConnected,boolean isWordPressComUser, boolean isJetpackUser,
                         int sessionCount, int numBlogs, int versionCode, String username, String email);
    abstract void registerPushNotificationToken(String regId);
    abstract String getAnonIdPrefKey();

    private String mAnonID = null; // do not access this variable directly. Use methods.
    private String mWpcomUserName = null;
    Context mContext;

    public Tracker(Context context) throws IllegalArgumentException {
        if (null == context) {
            throw new IllegalArgumentException("Tracker requires a not-null context");
        }
        mContext = context;
    }

    void clearAllData() {
        // Reset the anon ID here
        clearAnonID();
        setWordPressComUserName(null);
    }

    void clearAnonID() {
        mAnonID = null;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (preferences.contains(getAnonIdPrefKey())) {
            final SharedPreferences.Editor editor = preferences.edit();
            editor.remove(getAnonIdPrefKey());
            editor.commit();
        }
    }

    String getAnonID() {
        if (mAnonID == null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            mAnonID = preferences.getString(getAnonIdPrefKey(), null);
        }
        return mAnonID;
    }

    String generateNewAnonID() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        AppLog.d(AppLog.T.STATS, "New anonID generated in " + this.getClass().getSimpleName() + ": " + uuid);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(getAnonIdPrefKey(), uuid);
        editor.commit();

        mAnonID = uuid;
        return uuid;
    }

    String getWordPressComUserName() {
        return mWpcomUserName;
    }

    void setWordPressComUserName(String userName) {
        mWpcomUserName = userName;
    }
}
