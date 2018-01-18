package org.wordpress.android.ui.accounts.signup;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore.FetchUsernameSuggestionsPayload;
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogContent;
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogController;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class UsernameChangerFullScreenDialogFragment extends Fragment implements FullScreenDialogContent {
    protected FullScreenDialogController mDialogController;
    protected Handler mGetSuggestionsHandler;
    protected RecyclerView mUsernameSuggestions;
    protected Runnable mGetSuggestionsRunnable;
    protected String mDisplayName;
    protected String mUsername;
    protected String mUsernameSelected;
    protected String mUsernameSuggestionInput;
    protected TextInputEditText mUsernameView;
    protected TextView mHeaderView;
    protected boolean mShouldWatchText;  // Flag handling text watcher to avoid network call on device rotation.
    protected int mUsernameSelectedIndex;

    public static final String EXTRA_DISPLAY = "EXTRA_DISPLAY";
    public static final String EXTRA_USERNAME = "EXTRA_USERNAME";
    public static final String KEY_SHOULD_WATCH_TEXT = "KEY_SHOULD_WATCH_TEXT";
    public static final String KEY_USERNAME_SELECTED = "KEY_USERNAME_SELECTED";
    public static final String KEY_USERNAME_SELECTED_INDEX = "KEY_USERNAME_SELECTED_INDEX";
    public static final String KEY_USERNAME_SUGGESTIONS = "KEY_USERNAME_SUGGESTIONS";
    public static final String RESULT_USERNAME = "RESULT_USERNAME";
    public static final int GET_SUGGESTIONS_INTERVAL_MS = 2000;

    @Inject
    protected Dispatcher mDispatcher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.username_changer_dialog_fragment, container, false);

        mDisplayName = getArguments().getString(EXTRA_DISPLAY);
        mUsername = getArguments().getString(EXTRA_USERNAME);

        mUsernameSuggestions = rootView.findViewById(R.id.suggestions);
        mUsernameSuggestions.setLayoutManager(new LinearLayoutManager(getActivity()));
        // Stop list from blinking when data set is updated.
        ((SimpleItemAnimator) mUsernameSuggestions.getItemAnimator()).setSupportsChangeAnimations(false);

        return rootView;
    }

    @Override
    public void onViewCreated(final FullScreenDialogController controller) {
        mDialogController = controller;
    }

    @Override
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mShouldWatchText = savedInstanceState.getBoolean(KEY_SHOULD_WATCH_TEXT);
            mUsernameSelected = savedInstanceState.getString(KEY_USERNAME_SELECTED);
            mUsernameSelectedIndex = savedInstanceState.getInt(KEY_USERNAME_SELECTED_INDEX);
            setUsernameSuggestions(savedInstanceState.getStringArrayList(KEY_USERNAME_SUGGESTIONS));
        } else {
            mShouldWatchText = true;
            mUsernameSelected = mUsername;
            mUsernameSelectedIndex = 0;
            getUsernameSuggestions(getUsernameQueryFromDisplayName());
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
                    mHeaderView.setText(getHeaderText(getUsernameOrSelected(), mDisplayName));
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
        mGetSuggestionsRunnable = new Runnable() {
            @Override
            public void run() {
                mUsernameSuggestionInput = mUsernameView.getText().toString();
                getUsernameSuggestions(mUsernameSuggestionInput);
            }
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
    public boolean onConfirmClicked(FullScreenDialogController controller) {
        Bundle result = new Bundle();
        result.putString(RESULT_USERNAME, mUsername);
        controller.confirm(result);

        return true;
    }

    @Override
    public boolean onDismissClicked(FullScreenDialogController controller) {
        if (hasUsernameChanged()) {
            showDismissDialog();
        } else {
            controller.dismiss();
        }

        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SHOULD_WATCH_TEXT, false);
        outState.putString(KEY_USERNAME_SELECTED, mUsernameSelected);
        outState.putInt(KEY_USERNAME_SELECTED_INDEX, mUsernameSelectedIndex);
    }

    protected Spanned getHeaderText(String username, String display) {
        return Html.fromHtml(
                String.format(
                        getString(R.string.username_changer_header),
                        "<b>",
                        username,
                        "</b>",
                        "<b>",
                        display,
                        "</b>"
                )
        );
    }

    protected String getUsernameOrSelected() {
        return TextUtils.isEmpty(mUsernameSelected) ? mUsername : mUsernameSelected;
    }

    protected void getUsernameSuggestions(String usernameQuery) {
        FetchUsernameSuggestionsPayload payload = new FetchUsernameSuggestionsPayload(usernameQuery);
        mDispatcher.dispatch(AccountActionBuilder.newFetchUsernameSuggestionsAction(payload));
    }

    private String getUsernameQueryFromDisplayName() {
        return mDisplayName.replace(" ", "").toLowerCase();
    }

    protected boolean hasUsernameChanged() {
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
    }

    private void setUsernameSuggestions(List<String> suggestions) {
    }

    protected void showDismissDialog() {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.username_changer_dismiss_message)
                .setPositiveButton(R.string.username_changer_dismiss_button_positive,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mDialogController.dismiss();
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
