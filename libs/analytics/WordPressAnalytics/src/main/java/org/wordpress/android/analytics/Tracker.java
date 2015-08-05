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
    abstract void refreshMetadata(boolean isUserConnected,boolean isWordPressComUser, boolean isJetpackUser,
                         int sessionCount, int numBlogs, int versionCode, String username, String email);
    abstract void clearAllData();
    abstract void registerPushNotificationToken(String regId);
    abstract String getAnonIdPrefKey();

    private String mAnonID = null; // do not access this variable directly. Use methods.
    Context mContext;

    public Tracker(Context context) {
        if (null == context) {
            return;
        }
        mContext = context;
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
        String uuid = UUID.randomUUID().toString();
        String[] uuidSplitted = uuid.split("-");
        StringBuilder builder = new StringBuilder();
        for (String currentPart : uuidSplitted) {
            builder.append(currentPart);
        }
        uuid = builder.toString();
        AppLog.d(AppLog.T.STATS, "New anon ID generated: " + uuid);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(getAnonIdPrefKey(), uuid);
        editor.commit();

        mAnonID = uuid;
        return uuid;
    }
}
