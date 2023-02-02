package org.wordpress.android.ui.prefs.notifications;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.preference.DialogPreference;
import android.text.TextUtils;
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
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.databinding.NotificationsSettingsSwitchBinding;
import org.wordpress.android.models.NotificationsSettings;
import org.wordpress.android.models.NotificationsSettings.Channel;
import org.wordpress.android.models.NotificationsSettings.Type;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.notifications.PrefMainSwitchToolbarView.MainSwitchToolbarListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.extensions.ContextExtensionsKt;

import java.util.Iterator;

// A dialog preference that displays settings for a NotificationSettings Channel and Type
public class NotificationsSettingsDialogPreference extends DialogPreference
        implements MainSwitchToolbarListener {
    private static final String SETTING_VALUE_ACHIEVEMENT = "achievement";

    private NotificationsSettings.Channel mChannel;
    private NotificationsSettings.Type mType;
    private NotificationsSettings mSettings;
    private JSONObject mUpdatedJson = new JSONObject();
    private long mBlogId;

    private ViewGroup mTitleViewWithMainSwitch;

    // view to display when main switch is on
    private View mDisabledView;
    // view to display when main switch is off
    private LinearLayout mOptionsView;

    private PrefMainSwitchToolbarView mMainSwitchToolbarView;
    private boolean mShouldDisplayMainSwitch;

    private String[] mSettingsArray = new String[0], mSettingsValues = new String[0];

    private OnNotificationsSettingsChangedListener mOnNotificationsSettingsChangedListener;

    private final BloggingRemindersProvider mBloggingRemindersProvider;

    public interface OnNotificationsSettingsChangedListener {
        void onSettingsChanged(Channel channel, Type type, long siteId, JSONObject newValues);
    }

    public interface BloggingRemindersProvider {
        boolean isEnabled();

        String getSummary(long blogId);

        void onClick(long blogId);
    }

    public NotificationsSettingsDialogPreference(Context context, AttributeSet attrs, Channel channel,
                                                 Type type, long blogId, NotificationsSettings settings,
                                                 OnNotificationsSettingsChangedListener listener) {
        this(context, attrs, channel, type, blogId, settings, listener, null);
    }

    public NotificationsSettingsDialogPreference(Context context, AttributeSet attrs, Channel channel,
                                                 Type type, long blogId, NotificationsSettings settings,
                                                 OnNotificationsSettingsChangedListener listener,
                                                 BloggingRemindersProvider bloggingRemindersProvider) {
        super(context, attrs);

        mChannel = channel;
        mType = type;
        mBlogId = blogId;
        mSettings = settings;
        mOnNotificationsSettingsChangedListener = listener;
        mBloggingRemindersProvider = bloggingRemindersProvider;
        mShouldDisplayMainSwitch = mSettings.shouldDisplayMainSwitch(mChannel, mType);
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
        if (mShouldDisplayMainSwitch) {
            setupTitleViewWithMainSwitch(view);
        }
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);
        if (mShouldDisplayMainSwitch) {
            if (mTitleViewWithMainSwitch == null) {
                AppLog.e(T.NOTIFS, "Main switch enabled but layout not set");
                return;
            }
            builder.setCustomTitle(mTitleViewWithMainSwitch);
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

        if (mShouldDisplayMainSwitch) {
            View dividerView = new View(getContext());
            int dividerHeight = getContext().getResources().getDimensionPixelSize(
                    R.dimen.notifications_settings_dialog_divider_height
            );
            dividerView.setBackground(
                    ContextExtensionsKt.getDrawableFromAttribute(getContext(), android.R.attr.listDivider)
            );
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

        boolean shouldShowLocalNotifications = mChannel == Channel.BLOGS && mType == Type.DEVICE;

        if (settingsJson != null && mSettingsArray.length == mSettingsValues.length) {
            for (int i = 0; i < mSettingsArray.length; i++) {
                String settingName = mSettingsArray[i];
                String settingValue = mSettingsValues[i];

                // Skip a few settings for 'Email' section
                if (mType == Type.EMAIL && settingValue.equals(SETTING_VALUE_ACHIEVEMENT)) {
                    continue;
                }

                // Add special summary text for the WPCOM section
                String settingSummary = null;
                if (mChannel == Channel.WPCOM && i < summaryArray.length) {
                    settingSummary = summaryArray[i];
                }

                boolean isSettingChecked = JSONUtils.queryJSON(settingsJson, settingValue, true);

                boolean isSettingLast = !shouldShowLocalNotifications && i == mSettingsArray.length - 1;

                view.addView(setupSwitchSettingView(settingName, settingValue, settingSummary, isSettingChecked,
                        isSettingLast, mOnCheckedChangedListener));
            }
        }

        if (shouldShowLocalNotifications) {
            boolean isBloggingRemindersEnabled =
                    mBloggingRemindersProvider != null && mBloggingRemindersProvider.isEnabled();
            addWeeklyRoundupSetting(view, !isBloggingRemindersEnabled);
            if (isBloggingRemindersEnabled) {
                addBloggingReminderSetting(view);
            }
        }

        return view;
    }

    private void addWeeklyRoundupSetting(LinearLayout view, boolean isLast) {
        view.addView(setupSwitchSettingView(
                getContext().getString(R.string.weekly_roundup),
                null,
                null,
                AppPrefs.shouldShowWeeklyRoundupNotification(mBlogId),
                isLast,
                (compoundButton, isChecked) -> AppPrefs.setShouldShowWeeklyRoundupNotification(mBlogId, isChecked)
        ));
    }


    private void addBloggingReminderSetting(LinearLayout view) {
        view.addView(setupClickSettingView(
                getContext().getString(R.string.site_settings_blogging_reminders_notification_title),
                mBloggingRemindersProvider != null ? mBloggingRemindersProvider.getSummary(mBlogId) : null,
                true,
                (v -> {
                    if (mBloggingRemindersProvider != null) {
                        mBloggingRemindersProvider.onClick(mBlogId);
                    }
                    getDialog().dismiss();
                })
        ));
    }

    private View setupSwitchSettingView(String settingName, @Nullable String settingValue,
                                        @Nullable String settingSummary, boolean isSettingChecked,
                                        boolean isSettingLast,
                                        CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
        return setupSettingView(settingName, settingValue, settingSummary, isSettingChecked, isSettingLast,
                onCheckedChangeListener, null);
    }

    private View setupClickSettingView(String settingName, String settingSummary, boolean isSettingLast,
                                       View.OnClickListener onClickListener) {
        return setupSettingView(settingName, null, settingSummary, false, isSettingLast, null, onClickListener);
    }

    private View setupSettingView(String settingName, @Nullable String settingValue, @Nullable String settingSummary,
                                  boolean isSettingChecked, boolean isSettingLast,
                                  @Nullable CompoundButton.OnCheckedChangeListener onCheckedChangeListener,
                                  @Nullable View.OnClickListener onClickListener) {
        NotificationsSettingsSwitchBinding binding =
                NotificationsSettingsSwitchBinding.inflate(LayoutInflater.from(getContext()));

        binding.notificationsSwitchTitle.setText(settingName);

        if (!TextUtils.isEmpty(settingSummary)) {
            binding.notificationsSwitchSummary.setVisibility(View.VISIBLE);
            binding.notificationsSwitchSummary.setText(settingSummary);
        }

        if (onCheckedChangeListener != null) {
            binding.notificationsSwitch.setChecked(isSettingChecked);
            binding.notificationsSwitch.setTag(settingValue);
            binding.notificationsSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
            binding.rowContainer.setOnClickListener(v -> binding.notificationsSwitch.toggle());
        } else {
            binding.notificationsSwitch.setVisibility(View.GONE);
        }

        if (onClickListener != null) {
            binding.rowContainer.setOnClickListener(onClickListener);
        }

        if (mShouldDisplayMainSwitch && isSettingLast) {
            View divider = binding.notificationsListDivider;
            MarginLayoutParams mlp = (MarginLayoutParams) divider.getLayoutParams();
            mlp.leftMargin = 0;
            mlp.rightMargin = 0;
            divider.setLayoutParams(mlp);
        }

        return binding.getRoot();
    }

    private final CompoundButton.OnCheckedChangeListener mOnCheckedChangedListener =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    try {
                        mUpdatedJson.put(compoundButton.getTag().toString(), isChecked);

                        // Switch off main switch if all current settings switches are off
                        if (mMainSwitchToolbarView != null
                            && !isChecked
                            && areAllSettingsSwitchesUnchecked()
                        ) {
                            mMainSwitchToolbarView.setChecked(false);
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

    private void setupTitleViewWithMainSwitch(View view) {
        switch (mChannel) {
            case BLOGS:
                if (mType == Type.TIMELINE) {
                    mTitleViewWithMainSwitch = (ViewGroup) LayoutInflater
                            .from(getContext())
                            .inflate(R.layout.notifications_tab_for_blog_title_layout, (ViewGroup) view, false);
                }
                break;
            case OTHER:
            case WPCOM:
            default:
                break;
        }

        if (mTitleViewWithMainSwitch != null) {
            TextView titleView = mTitleViewWithMainSwitch.findViewById(R.id.title);
            CharSequence dialogTitle = getDialogTitle();
            if (dialogTitle != null) {
                titleView.setText(dialogTitle);
            }

            mMainSwitchToolbarView = mTitleViewWithMainSwitch.findViewById(R.id.main_switch);

            mMainSwitchToolbarView.setMainSwitchToolbarListener(this);
            mMainSwitchToolbarView
                    .setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.transparent));

            // Main Switch initial state:
            // On: If at least one of the settings options is on
            // Off: If all settings options are off
            JSONObject settingsJson = mSettings.getSettingsJsonForChannelAndType(mChannel, mType, mBlogId);
            boolean checkMainSwitch = mSettings.isAtLeastOneSettingsEnabled(
                    settingsJson,
                    mSettingsArray,
                    mSettingsValues
            );
            mMainSwitchToolbarView.loadInitialState(checkMainSwitch);

            hideDisabledView(mMainSwitchToolbarView.isMainChecked());
        }
    }

    @Override
    public void onMainSwitchCheckedChanged(
            CompoundButton buttonView,
            boolean isChecked) {
        setSettingsSwitchesChecked(isChecked);
        hideDisabledView(isChecked);
    }

    /**
     * Hide view when Notifications Tab Settings are disabled by toggling the main switch off.
     *
     * @param isMainChecked TRUE to hide disabled view, FALSE to show disabled view
     */
    private void hideDisabledView(boolean isMainChecked) {
        mDisabledView.setVisibility(isMainChecked ? View.GONE : View.VISIBLE);
        mOptionsView.setVisibility(isMainChecked ? View.VISIBLE : View.GONE);
    }

    /**
     * Updates Notifications current settings switches state based on the main switch state
     *
     * @param isMainChecked TRUE to switch on the settings switches.
     *                      FALSE to switch off the settings switches.
     */
    private void setSettingsSwitchesChecked(boolean isMainChecked) {
        for (String settingValue : mSettingsValues) {
            final SwitchCompat toggleSwitch = mOptionsView.findViewWithTag(settingValue);
            if (toggleSwitch != null) {
                toggleSwitch.setChecked(isMainChecked);
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
