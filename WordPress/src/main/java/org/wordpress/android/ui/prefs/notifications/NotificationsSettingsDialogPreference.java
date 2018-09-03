package org.wordpress.android.ui.prefs.notifications;

import android.app.ActionBar;
import android.content.Context;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.models.NotificationsSettings;
import org.wordpress.android.models.NotificationsSettings.Channel;
import org.wordpress.android.models.NotificationsSettings.Type;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtils;

import java.util.Iterator;

// A dialog preference that displays settings for a NotificationSettings Channel and Type
public class NotificationsSettingsDialogPreference extends DialogPreference {
    private static final String SETTING_VALUE_ACHIEVEMENT = "achievement";

    private NotificationsSettings.Channel mChannel;
    private NotificationsSettings.Type mType;
    private NotificationsSettings mSettings;
    private JSONObject mUpdatedJson = new JSONObject();
    private long mBlogId;

    private OnNotificationsSettingsChangedListener mOnNotificationsSettingsChangedListener;

    public interface OnNotificationsSettingsChangedListener {
        void onSettingsChanged(Channel channel, Type type, long siteId, JSONObject newValues);
    }

    public NotificationsSettingsDialogPreference(Context context, AttributeSet attrs, Channel channel,
                                                 Type type, long blogId, NotificationsSettings settings,
                                                 OnNotificationsSettingsChangedListener listener) {
        super(context, attrs);

        mChannel = channel;
        mType = type;
        mBlogId = blogId;
        mSettings = settings;
        mOnNotificationsSettingsChangedListener = listener;
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
    }

    @Override
    protected View onCreateDialogView() {
        ScrollView outerView = new ScrollView(getContext());
        outerView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                                                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout innerView = new LinearLayout(getContext());
        innerView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                                                LinearLayout.LayoutParams.MATCH_PARENT));
        innerView.setOrientation(LinearLayout.VERTICAL);

        View spacerView = new View(getContext());
        int spacerHeight = getContext().getResources().getDimensionPixelSize(R.dimen.margin_medium);
        spacerView.setLayoutParams(new ViewGroup.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, spacerHeight));
        innerView.addView(spacerView);

        outerView.addView(innerView);
        configureLayoutForView(innerView);

        return outerView;
    }

    private View configureLayoutForView(LinearLayout view) {
        JSONObject settingsJson = null;

        String[] settingsArray = new String[0], settingsValues = new String[0], summaryArray = new String[0];
        String typeString = mType.toString();

        switch (mChannel) {
            case BLOGS:
                settingsJson = JSONUtils.queryJSON(mSettings.getBlogSettings().get(mBlogId),
                                                   typeString, new JSONObject());
                settingsArray = getContext().getResources().getStringArray(R.array.notifications_blog_settings);
                settingsValues = getContext().getResources().getStringArray(R.array.notifications_blog_settings_values);
                break;
            case OTHER:
                settingsJson = JSONUtils.queryJSON(mSettings.getOtherSettings(),
                                                   typeString, new JSONObject());
                settingsArray = getContext().getResources().getStringArray(R.array.notifications_other_settings);
                settingsValues =
                        getContext().getResources().getStringArray(R.array.notifications_other_settings_values);
                break;
            case WPCOM:
                settingsJson = mSettings.getWPComSettings();
                settingsArray = getContext().getResources().getStringArray(R.array.notifications_wpcom_settings);
                settingsValues =
                        getContext().getResources().getStringArray(R.array.notifications_wpcom_settings_values);
                summaryArray =
                        getContext().getResources().getStringArray(R.array.notifications_wpcom_settings_summaries);
                break;
        }

        if (settingsJson != null && settingsArray.length == settingsValues.length) {
            for (int i = 0; i < settingsArray.length; i++) {
                String settingName = settingsArray[i];
                String settingValue = settingsValues[i];

                // Skip a few settings for 'Email' section
                if (mType == Type.EMAIL && settingValue.equals(SETTING_VALUE_ACHIEVEMENT)) {
                    continue;
                }

                View commentsSetting = View.inflate(getContext(), R.layout.notifications_settings_switch, null);
                TextView title = commentsSetting.findViewById(R.id.notifications_switch_title);
                title.setText(settingName);

                // Add special summary text for the WPCOM section
                if (mChannel == Channel.WPCOM && i < summaryArray.length) {
                    String summaryText = summaryArray[i];
                    TextView summary = commentsSetting.findViewById(R.id.notifications_switch_summary);
                    summary.setVisibility(View.VISIBLE);
                    summary.setText(summaryText);
                }

                final SwitchCompat toggleSwitch = commentsSetting.findViewById(R.id.notifications_switch);
                toggleSwitch.setChecked(JSONUtils.queryJSON(settingsJson, settingValue, true));
                toggleSwitch.setTag(settingValue);
                toggleSwitch.setOnCheckedChangeListener(mOnCheckedChangedListener);

                View rowContainer = commentsSetting.findViewById(R.id.row_container);
                rowContainer.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        toggleSwitch.setChecked(!toggleSwitch.isChecked());
                    }
                });

                view.addView(commentsSetting);
            }
        }

        return view;
    }

    private CompoundButton.OnCheckedChangeListener mOnCheckedChangedListener =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    try {
                        mUpdatedJson.put(compoundButton.getTag().toString(), isChecked);
                    } catch (JSONException e) {
                        AppLog.e(AppLog.T.NOTIFS, "Could not add notification setting change to JSONObject");
                    }
                }
            };

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && mUpdatedJson.length() > 0 && mOnNotificationsSettingsChangedListener != null) {
            mOnNotificationsSettingsChangedListener.onSettingsChanged(mChannel, mType, mBlogId, mUpdatedJson);

            // Update the settings json
            Iterator<?> keys = mUpdatedJson.keys();
            while (keys.hasNext()) {
                String settingName = (String) keys.next();
                mSettings.updateSettingForChannelAndType(
                        mChannel, mType, settingName,
                        mUpdatedJson.optBoolean(settingName), mBlogId
                );
            }
        }
    }
}
