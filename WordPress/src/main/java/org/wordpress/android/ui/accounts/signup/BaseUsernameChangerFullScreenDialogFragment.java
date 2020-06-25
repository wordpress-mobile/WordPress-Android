package org.wordpress.android.ui.accounts.signup;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore.FetchUsernameSuggestionsPayload;
import org.wordpress.android.fluxc.store.AccountStore.OnUsernameSuggestionsFetched;
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogContent;
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogController;
import org.wordpress.android.ui.accounts.signup.UsernameChangerRecyclerViewAdapter.OnUsernameSelectedListener;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;

/**
 * Created so that the base suggestions functionality can become shareable as similar functionality is being used in the
 * the Account settings & sign-up flow to change the username.
 */
public abstract class BaseUsernameChangerFullScreenDialogFragment extends DaggerFragment implements
        FullScreenDialogContent, OnUsernameSelectedListener {
    private ProgressBar mProgressBar;

    private FullScreenDialogController mDialogController;
    private Handler mGetSuggestionsHandler;
    private RecyclerView mUsernameSuggestions;
    private Runnable mGetSuggestionsRunnable;
    private String mDisplayName;
    private String mUsername;
    private String mUsernameSelected;
    private String mUsernameSuggestionInput;
    private TextInputEditText mUsernameView;
    private TextView mHeaderView;
    private UsernameChangerRecyclerViewAdapter mUsernamesAdapter;
    private boolean mIsShowingDismissDialog;
    private boolean mShouldWatchText; // Flag handling text watcher to avoid network call on device rotation.
    private int mUsernameSelectedIndex;

    public static final String EXTRA_DISPLAY_NAME = "EXTRA_DISPLAY_NAME";
    public static final String EXTRA_USERNAME = "EXTRA_USERNAME";
    public static final String KEY_IS_SHOWING_DISMISS_DIALOG = "KEY_IS_SHOWING_DISMISS_DIALOG";
    public static final String KEY_SHOULD_WATCH_TEXT = "KEY_SHOULD_WATCH_TEXT";
    public static final String KEY_USERNAME_SELECTED = "KEY_USERNAME_SELECTED";
    public static final String KEY_USERNAME_SELECTED_INDEX = "KEY_USERNAME_SELECTED_INDEX";
    public static final String KEY_USERNAME_SUGGESTIONS = "KEY_USERNAME_SUGGESTIONS";
    public static final String RESULT_USERNAME = "RESULT_USERNAME";
    public static final int GET_SUGGESTIONS_INTERVAL_MS = 1000;

    @Inject protected Dispatcher mDispatcher;

    /**
     * Fragments that extend this class are required to provide the event that should be
     * tracked in case fetching of the username suggestions fail.
     *
     * @return {@link Stat}
     */
    abstract Stat getSuggestionsFailedStat();

    /**
     * Specifies if the header text should be updated when a new username is selected
     * or if the the initial username should remain.
     *
     * @return true or false
     */
    abstract boolean canHeaderTextLiveUpdate();

    /**
     * Creates the text that's displayed in the header.
     *
     * @param username
     * @param display
     * @return formatted header template
     */
    abstract Spanned getHeaderText(String username, String display);

    public static Bundle newBundle(String displayName, String username) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_DISPLAY_NAME, displayName);
        bundle.putString(EXTRA_USERNAME, username);
        return bundle;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.username_changer_dialog_fragment, container, false);

        mDisplayName = getArguments().getString(EXTRA_DISPLAY_NAME);
        mUsername = getArguments().getString(EXTRA_USERNAME);

        mUsernameSuggestions = rootView.findViewById(R.id.suggestions);
        mUsernameSuggestions.setLayoutManager(new LinearLayoutManager(getActivity()));
        // Stop list from blinking when data set is updated.
        ((SimpleItemAnimator) mUsernameSuggestions.getItemAnimator()).setSupportsChangeAnimations(false);

        mProgressBar = rootView.findViewById(R.id.progress);

        return rootView;
    }

    @Override
    public void setController(final FullScreenDialogController controller) {
        mDialogController = controller;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            mIsShowingDismissDialog = savedInstanceState.getBoolean(KEY_IS_SHOWING_DISMISS_DIALOG);
            mShouldWatchText = savedInstanceState.getBoolean(KEY_SHOULD_WATCH_TEXT);
            mUsernameSelected = savedInstanceState.getString(KEY_USERNAME_SELECTED);
            mUsernameSelectedIndex = savedInstanceState.getInt(KEY_USERNAME_SELECTED_INDEX);
            ArrayList<String> suggestions = savedInstanceState.getStringArrayList(KEY_USERNAME_SUGGESTIONS);
            if (suggestions != null) {
                setUsernameSuggestions(suggestions);
            } else {
                mUsernameSuggestionInput = getUsernameQueryFromDisplayName();
                getUsernameSuggestions(mUsernameSuggestionInput);
            }

            if (mIsShowingDismissDialog) {
                showDismissDialog();
            }
        } else {
            mShouldWatchText = true;
            mUsernameSelected = mUsername;
            mUsernameSelectedIndex = 0;
            mUsernameSuggestionInput = getUsernameQueryFromDisplayName();
            getUsernameSuggestions(mUsernameSuggestionInput);
        }

        mHeaderView = getView().findViewById(R.id.header);
        mHeaderView.setText(getHeaderText(getUsernameOrSelected(), mDisplayName));

        mUsernameView = getView().findViewById(R.id.username);
        mUsernameView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    if (canHeaderTextLiveUpdate()) {
                        mHeaderView.setText(getHeaderText(getUsernameOrSelected(), mDisplayName));
                    }
                    mGetSuggestionsHandler.removeCallbacks(mGetSuggestionsRunnable);
                } else if (mShouldWatchText) {
                    mGetSuggestionsHandler.removeCallbacks(mGetSuggestionsRunnable);
                    mGetSuggestionsHandler.postDelayed(mGetSuggestionsRunnable, GET_SUGGESTIONS_INTERVAL_MS);
                } else {
                    mShouldWatchText = true;
                }
            }
        });

        mGetSuggestionsHandler = new Handler();
        mGetSuggestionsRunnable = () -> {
            mUsernameSuggestionInput = mUsernameView.getText().toString();
            getUsernameSuggestions(mUsernameSuggestionInput);
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mDispatcher.unregister(this);
    }

    @Override
    public void onDestroy() {
        mGetSuggestionsHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public boolean onConfirmClicked(FullScreenDialogController controller) {
        ActivityUtils.hideKeyboard(getActivity());

        if (mUsernamesAdapter != null && mUsernamesAdapter.mItems != null) {
            onUsernameConfirmed(controller, mUsernameSelected);
        } else {
            controller.dismiss();
        }

        return true;
    }

    public abstract void onUsernameConfirmed(FullScreenDialogController controller, String usernameSelected);

    @Override
    public boolean onDismissClicked(FullScreenDialogController controller) {
        ActivityUtils.hideKeyboard(getActivity());

        if (hasUsernameChanged()) {
            showDismissDialog();
        } else {
            controller.dismiss();
        }

        return true;
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_SHOWING_DISMISS_DIALOG, mIsShowingDismissDialog);
        outState.putBoolean(KEY_SHOULD_WATCH_TEXT, false);
        outState.putString(KEY_USERNAME_SELECTED, mUsernameSelected);
        outState.putInt(KEY_USERNAME_SELECTED_INDEX, mUsernameSelectedIndex);
        if (mUsernamesAdapter != null) {
            outState.putStringArrayList(KEY_USERNAME_SUGGESTIONS, new ArrayList<>(mUsernamesAdapter.mItems));
        }
    }

    @Override
    public void onUsernameSelected(String username) {
        if (canHeaderTextLiveUpdate()) {
            mHeaderView.setText(getHeaderText(username, mDisplayName));
        }
        mUsernameSelected = username;
        mUsernameSelectedIndex = mUsernamesAdapter.getSelectedItem();
    }

    private String getUsernameOrSelected() {
        return TextUtils.isEmpty(mUsernameSelected) ? mUsername : mUsernameSelected;
    }

    public String getUsernameSelected() {
        return mUsernameSelected;
    }

    private void getUsernameSuggestions(String usernameQuery) {
        showProgress(true);

        FetchUsernameSuggestionsPayload payload = new FetchUsernameSuggestionsPayload(usernameQuery);
        mDispatcher.dispatch(AccountActionBuilder.newFetchUsernameSuggestionsAction(payload));
    }

    private String getUsernameQueryFromDisplayName() {
        return mDisplayName.replace(" ", "").toLowerCase(Locale.ROOT);
    }

    public boolean hasUsernameChanged() {
        return !TextUtils.equals(mUsername, mUsernameSelected);
    }

    private void populateUsernameSuggestions(List<String> suggestions) {
        String usernameOrSelected = getUsernameOrSelected();

        List<String> suggestionList = new ArrayList<>();
        suggestionList.add(usernameOrSelected);

        for (String suggestion : suggestions) {
            if (!TextUtils.equals(suggestion, usernameOrSelected)) {
                suggestionList.add(suggestion);
            }
        }

        mUsernameSelectedIndex = 0;
        setUsernameSuggestions(suggestionList);
    }

    private void setUsernameSuggestions(List<String> suggestions) {
        mUsernamesAdapter = new UsernameChangerRecyclerViewAdapter(getActivity(), suggestions);
        mUsernamesAdapter.setOnUsernameSelectedListener(BaseUsernameChangerFullScreenDialogFragment.this);
        mUsernamesAdapter.setSelectedItem(mUsernameSelectedIndex);
        mUsernameSuggestions.setAdapter(mUsernamesAdapter);
    }

    private void showDismissDialog() {
        mIsShowingDismissDialog = true;

        new MaterialAlertDialogBuilder(getContext())
                .setMessage(R.string.username_changer_dismiss_message)
                .setPositiveButton(R.string.username_changer_dismiss_button_positive,
                        (dialog, which) -> mDialogController.dismiss())
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> mIsShowingDismissDialog = false)
                .show();
    }

    protected void showErrorDialog(Spanned message) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(getContext())
                .setMessage(message)
                .setPositiveButton(R.string.login_error_button, null)
                .create();
        dialog.show();
    }

    protected void showProgress(boolean showProgress) {
        mUsernameSuggestions.setVisibility(showProgress ? View.GONE : View.VISIBLE);
        mProgressBar.setVisibility(showProgress ? View.VISIBLE : View.GONE);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUsernameSuggestionsFetched(OnUsernameSuggestionsFetched event) {
        showProgress(false);

        if (event.isError()) {
            AnalyticsTracker.track(getSuggestionsFailedStat());
            AppLog.e(T.API, "onUsernameSuggestionsFetched: " + event.error.type + " - " + event.error.message);
            showErrorDialog(new SpannedString(getString(R.string.username_changer_error_generic)));
        } else if (event.suggestions.size() == 0) {
            String error = String.format(
                    getString(R.string.username_changer_error_none),
                    "<b>",
                    mUsernameSuggestionInput,
                    "</b>"
            );
            mUsernameView.setError(Html.fromHtml(
                    error
            ));
        } else {
            populateUsernameSuggestions(event.suggestions);
        }
    }
}
