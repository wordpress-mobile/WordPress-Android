package org.wordpress.android.ui.prefs.notifications;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.models.NotificationsSettings;
import org.wordpress.android.models.NotificationsSettings.Channel;
import org.wordpress.android.models.NotificationsSettings.Type;
import org.wordpress.android.ui.prefs.notifications.PrefMasterSwitchToolbarView.MasterSwitchToolbarListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ContextUtilsKt;
import org.wordpress.android.util.JSONUtils;

import java.util.Iterator;

// A dialog preference that displays settings for a NotificationSettings Channel and Type
public class NotificationsSettingsDialogPreference extends DialogPreference
        implements MasterSwitchToolbarListener {
    private static final String SETTING_VALUE_ACHIEVEMENT = "achievement";

    private NotificationsSettings.Channel mChannel;
    private NotificationsSettings.Type mType;
    private NotificationsSettings mSettings;
    private JSONObject mUpdatedJson = new JSONObject();
    private long mBlogId;

    private ViewGroup mTitleViewWithMasterSwitch;

    // view to display when master switch is on
    private View mDisabledView;
    // view to display when master switch is off
    private LinearLayout mOptionsView;

    private PrefMasterSwitchToolbarView mMasterSwitchToolbarView;
    private boolean mShouldDisplayMasterSwitch;

    private String[] mSettingsArray = new String[0], mSettingsValues = new String[0];

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
        mShouldDisplayMasterSwitch = mSettings.shouldDisplayMasterSwitch(mChannel, mType);
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
        if (mShouldDisplayMasterSwitch) {
            setupTitleViewWithMasterSwitch(view);
        }
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);
        if (mShouldDisplayMasterSwitch) {
            if (mTitleViewWithMasterSwitch == null) {
                AppLog.e(T.NOTIFS, "Master switch enabled but layout not set");
                return;
            }
            builder.setCustomTitle(mTitleViewWithMasterSwitch);
        }
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

        if (mShouldDisplayMasterSwitch) {
            View dividerView = new View(getContext());
            int dividerHeight = getContext().getResources().getDimensionPixelSize(
                    R.dimen.notifications_settings_dialog_divider_height
            );
            dividerView
                    .setBackground(ContextUtilsKt.getDrawableFromAttribute(getContext(), android.R.attr.listDivider));
            dividerView.setLayoutParams(new ViewGroup.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, dividerHeight));
            innerView.addView(dividerView);
        } else {
            View spacerView = new View(getContext());
            int spacerHeight = getContext().getResources().getDimensionPixelSize(R.dimen.margin_medium);
            spacerView.setLayoutParams(new ViewGroup.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, spacerHeight));
            innerView.addView(spacerView);
        }

        mDisabledView = View.inflate(getContext(), R.layout.notifications_tab_disabled_text_layout, null);
        mDisabledView.setLayoutParams(
                new ViewGroup.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                )
        );

        mOptionsView = new LinearLayout(getContext());
        mOptionsView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                )
        );
        mOptionsView.setOrientation(LinearLayout.VERTICAL);

        innerView.addView(mDisabledView);
        innerView.addView(mOptionsView);

        outerView.addView(innerView);
        configureLayoutForView(mOptionsView);

        return outerView;
    }

    private View configureLayoutForView(LinearLayout view) {
        JSONObject settingsJson = mSettings.getSettingsJsonForChannelAndType(mChannel, mType, mBlogId);
        String[] summaryArray = new String[0];

        switch (mChannel) {
            case BLOGS:
                mSettingsArray = getContext().getResources().getStringArray(R.array.notifications_blog_settings);
                mSettingsValues = getContext().getResources()
                                              .getStringArray(R.array.notifications_blog_settings_values);
                break;
            case OTHER:
                mSettingsArray = getContext().getResources().getStringArray(R.array.notifications_other_settings);
                mSettingsValues =
                        getContext().getResources().getStringArray(R.array.notifications_other_settings_values);
                break;
            case WPCOM:
                mSettingsArray = getContext().getResources().getStringArray(R.array.notifications_wpcom_settings);
                mSettingsValues =
                        getContext().getResources().getStringArray(R.array.notifications_wpcom_settings_values);
                summaryArray =
                        getContext().getResources().getStringArray(R.array.notifications_wpcom_settings_summaries);
                break;
        }

        if (settingsJson != null && mSettingsArray.length == mSettingsValues.length) {
            for (int i = 0; i < mSettingsArray.length; i++) {
                String settingName = mSettingsArray[i];
                String settingValue = mSettingsValues[i];

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

                if (mShouldDisplayMasterSwitch && i == mSettingsArray.length - 1) {
                    View divider = commentsSetting.findViewById(R.id.notifications_list_divider);
                    if (divider != null) {
                        MarginLayoutParams mlp = (MarginLayoutParams) divider.getLayoutParams();
                        mlp.leftMargin = 0;
                        mlp.rightMargin = 0;
                        divider.setLayoutParams(mlp);
                    }
                }

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

                        // Switch off master switch if all current settings switches are off
                        if (mMasterSwitchToolbarView != null
                            && !isChecked
                            && areAllSettingsSwitchesUnchecked()
                        ) {
                            mMasterSwitchToolbarView.setChecked(false);
                        }
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

    private void setupTitleViewWithMasterSwitch(View view) {
        switch (mChannel) {
            case BLOGS:
                if (mType == Type.TIMELINE) {
                    mTitleViewWithMasterSwitch = (ViewGroup) LayoutInflater
                            .from(getContext())
                            .inflate(R.layout.notifications_tab_for_blog_title_layout, (ViewGroup) view, false);
                }
                break;
            case OTHER:
            case WPCOM:
            default:
                break;
        }

        if (mTitleViewWithMasterSwitch != null) {
            TextView titleView = mTitleViewWithMasterSwitch.findViewById(R.id.title);
            CharSequence dialogTitle = getDialogTitle();
            if (dialogTitle != null) {
                titleView.setText(dialogTitle);
            }

            mMasterSwitchToolbarView = mTitleViewWithMasterSwitch.findViewById(R.id.master_switch);

            mMasterSwitchToolbarView.setMasterSwitchToolbarListener(this);
            mMasterSwitchToolbarView
                    .setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.transparent));

            // Master Switch initial state:
            // On: If at least one of the settings options is on
            // Off: If all settings options are off
            JSONObject settingsJson = mSettings.getSettingsJsonForChannelAndType(mChannel, mType, mBlogId);
            boolean checkMasterSwitch = mSettings.isAtLeastOneSettingsEnabled(
                    settingsJson,
                    mSettingsArray,
                    mSettingsValues
            );
            mMasterSwitchToolbarView.loadInitialState(checkMasterSwitch);

            hideDisabledView(mMasterSwitchToolbarView.isMasterChecked());
        }
    }

    @Override
    public void onMasterSwitchCheckedChanged(
            CompoundButton buttonView,
            boolean isChecked) {
        setSettingsSwitchesChecked(isChecked);
        hideDisabledView(isChecked);
    }

    /**
     * Hide view when Notifications Tab Settings are disabled by toggling the master switch off.
     *
     * @param isMasterChecked TRUE to hide disabled view, FALSE to show disabled view
     */
    private void hideDisabledView(boolean isMasterChecked) {
        mDisabledView.setVisibility(isMasterChecked ? View.GONE : View.VISIBLE);
        mOptionsView.setVisibility(isMasterChecked ? View.VISIBLE : View.GONE);
    }

    /**
     * Updates Notifications current settings switches state based on the master switch state
     *
     * @param isMasterChecked TRUE to switch on the settings switches.
     *                        FALSE to switch off the settings switches.
     */
    private void setSettingsSwitchesChecked(boolean isMasterChecked) {
        for (String settingValue : mSettingsValues) {
            final SwitchCompat toggleSwitch = mOptionsView.findViewWithTag(settingValue);
            if (toggleSwitch != null) {
                toggleSwitch.setChecked(isMasterChecked);
            }
        }
    }

    // returns true if all current settings switches on the dialog are unchecked
    private boolean areAllSettingsSwitchesUnchecked() {
        boolean settingsSwitchesUnchecked = true;

        for (String settingValue : mSettingsValues) {
            final SwitchCompat toggleSwitch = mOptionsView.findViewWithTag(settingValue);
            if (toggleSwitch != null) {
                boolean isChecked = toggleSwitch.isChecked();
                if (isChecked) {
                    settingsSwitchesUnchecked = false;
                    break;
                }
            }
        }

        return settingsSwitchesUnchecked;
    }
}
