package org.wordpress.android.ui.prefs.notifications;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.models.NotificationsSettings;
import org.wordpress.android.models.NotificationsSettings.Channel;
import org.wordpress.android.models.NotificationsSettings.Type;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;

import javax.annotation.Nonnull;

// A dialog preference that displays settings for a NotificationSettings Channel and Type
public class NotificationsSettingsDialogPreference extends DialogPreference {
    private NotificationsSettings.Channel mChannel;
    private NotificationsSettings.Type mType;
    private JSONObject mSettingsJson;
    private JSONObject mUpdatedJson = new JSONObject();

    private long mBlogId;

    private OnNotificationsSettingsChangedListener mOnNotificationsSettingsChangedListener;

    public interface OnNotificationsSettingsChangedListener {
        void onSettingsChanged(Channel channel, Type type, long siteId, JSONObject newValues);
    }

    public NotificationsSettingsDialogPreference(Context context, AttributeSet attrs, Channel channel,
                                                 Type type, long blogId, JSONObject settings,
                                                 OnNotificationsSettingsChangedListener listener) {
        super(context, attrs);

        mChannel = channel;
        mType = type;
        mBlogId = blogId;
        mSettingsJson = settings;
        mOnNotificationsSettingsChangedListener = listener;
    }

    @Override
    protected void onBindDialogView(@Nonnull View view) {
        super.onBindDialogView(view);
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout view = new LinearLayout(getContext());
        view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        view.setOrientation(LinearLayout.VERTICAL);


        return configureLayoutForView(view);
    }

    private View configureLayoutForView(LinearLayout view) {
        String[] settingsArray = new String[0], settingsValues = new String[0];
        if (mChannel == NotificationsSettings.Channel.BLOGS) {
            settingsArray = getContext().getResources().getStringArray(R.array.notifications_blog_settings);
            settingsValues = getContext().getResources().getStringArray(R.array.notifications_blog_settings_values);
        } else if (mChannel == NotificationsSettings.Channel.OTHER) {
            settingsArray = getContext().getResources().getStringArray(R.array.notifications_other_settings);
            settingsValues = getContext().getResources().getStringArray(R.array.notifications_other_settings_values);
        } else if (mChannel == NotificationsSettings.Channel.DOTCOM) {
            settingsArray = getContext().getResources().getStringArray(R.array.notifications_wpcom_settings);
            settingsValues = getContext().getResources().getStringArray(R.array.notifications_wpcom_settings_values);
        }

        if (settingsArray != null && settingsArray.length == settingsValues.length) {
            for (int i=0; i < settingsArray.length; i++) {
                String settingName = settingsArray[i];
                String settingValue = settingsValues[i];
                View commentsSetting = View.inflate(getContext(), R.layout.notifications_settings_switch, null);
                TextView title = (TextView)commentsSetting.findViewById(R.id.notifications_switch_title);
                title.setText(settingName);

                Switch toggleSwitch = (Switch)commentsSetting.findViewById(R.id.notifications_switch);
                toggleSwitch.setChecked(JSONUtils.queryJSON(mSettingsJson, settingValue, true));
                toggleSwitch.setTag(settingValue);
                toggleSwitch.setOnCheckedChangeListener(mOnCheckedChangedListener);

                view.addView(commentsSetting);
            }
        }

        return view;
    }

    private CompoundButton.OnCheckedChangeListener mOnCheckedChangedListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            try {
                String settingName = StringUtils.notNullStr(compoundButton.getTag().toString());
                if (mUpdatedJson.has(settingName)) {
                    mUpdatedJson.remove(settingName);
                } else {
                    mUpdatedJson.put(compoundButton.getTag().toString(), isChecked);
                }

                mSettingsJson.put(compoundButton.getTag().toString(), isChecked);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.NOTIFS, "Could not add notification setting change to JSONObject");
            }
        }
    };

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && mUpdatedJson.length() > 0 && mOnNotificationsSettingsChangedListener != null) {
            mOnNotificationsSettingsChangedListener.onSettingsChanged(mChannel, mType, mBlogId, mUpdatedJson);
        }
    }
}
