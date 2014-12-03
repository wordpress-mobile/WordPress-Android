package org.wordpress.android.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

public class AutoSaveTextHelper {
    public static final String PREFERENCES_NAME = "AutoSaveTextHelperPrefs";
    private String mUniqueId;

    public void setUniqueId(String uniqueId) {
        mUniqueId = uniqueId;
    }

    public String getUniqueId() {
        return mUniqueId;
    }

    private String getViewPathId(View view) {
        StringBuilder sb = new StringBuilder();
        for (View currentView = view; currentView != null && currentView.getParent() != null
                && currentView.getParent() instanceof View; currentView = (View) currentView.getParent()) {
            sb.append(currentView.getId());
        }
        return sb.toString();
    }

    public void clearSavedText(Context context, View view) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(getViewPathId(view) + mUniqueId);
        editor.apply();
    }

    public String loadString(Context context, View view) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(getViewPathId(view) + mUniqueId, "");
    }

    public void saveString(Context context, View view, String text) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getViewPathId(view) + mUniqueId, text);
        editor.apply();
    }
}
