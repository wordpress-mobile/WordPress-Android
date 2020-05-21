package org.wordpress.android.ui.people;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.RoleUtils;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.people.utils.PeopleUtils;
import org.wordpress.android.ui.people.utils.PeopleUtils.ValidateUsernameCallback.ValidationResult;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.MultiUsernameEditText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class PeopleInviteFragment extends Fragment implements RoleSelectDialogFragment.OnRoleSelectListener,
        PeopleManagementActivity.InvitationSender {
    private static final String URL_USER_ROLES_DOCUMENTATION = "https://en.support.wordpress.com/user-roles/";
    private static final String FLAG_SUCCESS = "SUCCESS";
    private static final String KEY_USERNAMES = "usernames";
    private static final String KEY_SELECTED_ROLE = "selected-role";
    private static final int MAX_NUMBER_OF_INVITEES = 10;
    private static final String[] USERNAME_DELIMITERS = {" ", ","};
    private final Map<String, ViewGroup> mUsernameButtons = new LinkedHashMap<>();
    private final HashMap<String, String> mUsernameResults = new HashMap<>();
    private final Map<String, View> mUsernameErrorViews = new Hashtable<>();
    private ArrayList<String> mUsernames = new ArrayList<>();
    private ViewGroup mUsernamesContainer;
    private MultiUsernameEditText mUsernameEditText;
    private TextView mRoleTextView;
    private EditText mCustomMessageEditText;

    private List<RoleModel> mInviteRoles;
    private String mCurrentRole;
    private String mCustomMessage = "";
    private boolean mInviteOperationInProgress = false;
    private SiteModel mSite;

    @Inject SiteStore mSiteStore;

    private int mNormalUsernameColor;
    private int mAddedUsernameColor;
    private int mErrorUsernameColor;

    public static PeopleInviteFragment newInstance(SiteModel site) {
        PeopleInviteFragment peopleInviteFragment = new PeopleInviteFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(WordPress.SITE, site);
        peopleInviteFragment.setArguments(bundle);
        return peopleInviteFragment;
    }

    private void updateSiteOrFinishActivity() {
        if (getArguments() != null) {
            mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
        } else {
            mSite = (SiteModel) getActivity().getIntent().getSerializableExtra(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.people_invite, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.getItem(0).setEnabled(!mInviteOperationInProgress); // here pass the index of send menu item
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCurrentRole != null) {
            outState.putString(KEY_SELECTED_ROLE, mCurrentRole);
        }
        outState.putStringArrayList(KEY_USERNAMES, new ArrayList<>(mUsernameButtons.keySet()));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplicationContext()).component().inject(this);
        updateSiteOrFinishActivity();
        mInviteRoles = RoleUtils.getInviteRoles(mSiteStore, mSite, this);

        if (savedInstanceState != null) {
            mCurrentRole = savedInstanceState.getString(KEY_SELECTED_ROLE);
            ArrayList<String> retainedUsernames = savedInstanceState.getStringArrayList(KEY_USERNAMES);
            if (retainedUsernames != null) {
                mUsernames.clear();
                mUsernames.addAll(retainedUsernames);
            }
        }

        // retain this fragment across configuration changes
        // WARNING: use setRetainInstance wisely. In this case we need this to be able to get the
        // results of network connections in the same fragment if going through a configuration change
        // (for example, device rotation occurs). Given the simplicity of this particular use case
        // (the fragment state keeps only a couple of EditText components and the SAVE button, it is
        // OK to use it here.
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.people_invite_fragment, container, false);

        Toolbar toolbar = rootView.findViewById(R.id.toolbar_main);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.people);
        }

        mNormalUsernameColor = ContextExtensionsKt.getColorResIdFromAttribute(getActivity(), R.attr.colorOnSurface);
        mAddedUsernameColor = ContextExtensionsKt.getColorResIdFromAttribute(getActivity(), R.attr.colorPrimary);
        mErrorUsernameColor = ContextExtensionsKt.getColorResIdFromAttribute(getActivity(), R.attr.wpColorError);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mUsernamesContainer = (ViewGroup) view.findViewById(R.id.usernames);
        mUsernamesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditTextUtils.showSoftInput(mUsernameEditText);
            }
        });

        if (TextUtils.isEmpty(mCurrentRole)) {
            mCurrentRole = getDefaultRole();
        }

        mUsernameEditText = (MultiUsernameEditText) view.findViewById(R.id.invite_usernames);

        // handle key preses from hardware keyboard
        mUsernameEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                return keyEvent.getKeyCode() == KeyEvent.KEYCODE_DEL
                       && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                       && removeLastEnteredUsername();
            }
        });

        mUsernameEditText.setOnBackspacePressedListener(new MultiUsernameEditText.OnBackspacePressedListener() {
            @Override
            public boolean onBackspacePressed() {
                return removeLastEnteredUsername();
            }
        });

        mUsernameEditText.addTextChangedListener(new TextWatcher() {
            private boolean mShouldIgnoreChanges = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mShouldIgnoreChanges) { // used to avoid double call after calling setText from this method
                    return;
                }

                mShouldIgnoreChanges = true;
                if (mUsernameButtons.size() >= MAX_NUMBER_OF_INVITEES && !TextUtils.isEmpty(s)) {
                    resetEditTextContent(mUsernameEditText);
                } else if (endsWithDelimiter(mUsernameEditText.getText().toString())) {
                    addUsername(mUsernameEditText, null);
                }
                mShouldIgnoreChanges = false;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mUsernameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent
                        .KEYCODE_ENTER)) {
                    addUsername(mUsernameEditText, null);
                    return true;
                } else {
                    return false;
                }
            }
        });

        mUsernameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus && mUsernameEditText.getText().toString().length() > 0) {
                    addUsername(mUsernameEditText, null);
                }
            }
        });


        // if mUsernameButtons is not empty, this means fragment retained itself
        // if mUsernameButtons is empty, but we have manually retained usernames, this means that fragment was destroyed
        // and we need to recreate manually added views and revalidate usernames
        if (!mUsernameButtons.isEmpty()) {
            mUsernameErrorViews.clear();
            populateUsernameButtons(new ArrayList<>(mUsernameButtons.keySet()));
        } else if (!mUsernames.isEmpty()) {
            populateUsernameButtons(new ArrayList<>(mUsernames));
        }

        mRoleTextView = (TextView) view.findViewById(R.id.role);
        refreshRoleTextView();
        ImageView imgRoleInfo = (ImageView) view.findViewById(R.id.imgRoleInfo);
        imgRoleInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.openUrlExternal(v.getContext(), URL_USER_ROLES_DOCUMENTATION);
            }
        });

        if (mInviteRoles.size() > 1) {
            view.findViewById(R.id.role_container).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RoleSelectDialogFragment.show(PeopleInviteFragment.this, 0, mSite);
                }
            });
        } else {
            // Don't show drop-down arrow or role selector if there's only one role available
            mRoleTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        final int maxChars = getResources().getInteger(R.integer.invite_message_char_limit);
        final TextView remainingCharsTextView = (TextView) view.findViewById(R.id.message_remaining);

        mCustomMessageEditText = (EditText) view.findViewById(R.id.message);
        mCustomMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCustomMessage = mCustomMessageEditText.getText().toString();
                updateRemainingCharsView(remainingCharsTextView, mCustomMessage, maxChars);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        updateRemainingCharsView(remainingCharsTextView, mCustomMessage, maxChars);
        // important for accessibility - talkback
        getActivity().setTitle(R.string.invite_people);
    }

    private boolean endsWithDelimiter(String string) {
        if (TextUtils.isEmpty(string)) {
            return false;
        }

        for (String usernameDelimiter : USERNAME_DELIMITERS) {
            if (string.endsWith(usernameDelimiter)) {
                return true;
            }
        }

        return false;
    }

    private String removeDelimiterFromUsername(String username) {
        if (TextUtils.isEmpty(username)) {
            return username;
        }

        String trimmedUsername = username.trim();

        for (String usernameDelimiter : USERNAME_DELIMITERS) {
            if (trimmedUsername.endsWith(usernameDelimiter)) {
                return trimmedUsername.substring(0, trimmedUsername.length() - usernameDelimiter.length());
            }
        }

        return trimmedUsername;
    }

    private void resetEditTextContent(EditText editText) {
        if (editText != null) {
            editText.setText("");
        }
    }

    private String getDefaultRole() {
        if (mInviteRoles.isEmpty()) {
            return null;
        }
        return mInviteRoles.get(0).getName();
    }

    private void updateRemainingCharsView(TextView remainingCharsTextView, String currentString, int limit) {
        remainingCharsTextView.setText(StringUtils.getQuantityString(getActivity(),
                R.string.invite_message_remaining_zero,
                R.string.invite_message_remaining_one,
                R.string.invite_message_remaining_other,
                limit - (currentString == null ? 0
                        : currentString.length())));
    }

    private void populateUsernameButtons(Collection<String> usernames) {
        if (usernames != null && usernames.size() > 0) {
            for (String username : usernames) {
                mUsernameButtons.put(username, buttonizeUsername(username));
            }

            validateAndStyleUsername(usernames, null);
        }
    }

    private ViewGroup buttonizeUsername(final String username) {
        if (!isAdded()) {
            return null;
        }

        final ViewGroup usernameButton = (ViewGroup) LayoutInflater.from(
                getActivity()).inflate(R.layout.invite_username_button, mUsernamesContainer, false);
        final TextView usernameTextView = (TextView) usernameButton.findViewById(R.id.username);
        usernameTextView.setText(username);

        mUsernamesContainer.addView(usernameButton, mUsernamesContainer.getChildCount() - 1);

        final ImageButton delete = (ImageButton) usernameButton.findViewById(R.id.username_delete);
        delete.setContentDescription(getString(R.string.invite_user_delete_desc, username));
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeUsername(username);
            }
        });

        return usernameButton;
    }

    private void addUsername(EditText editText, ValidationEndListener validationEndListener) {
        String username = removeDelimiterFromUsername(editText.getText().toString());
        resetEditTextContent(editText);

        if (username.isEmpty() || mUsernameButtons.keySet().contains(username)) {
            if (validationEndListener != null) {
                validationEndListener.onValidationEnd();
            }
            return;
        }

        final ViewGroup usernameButton = buttonizeUsername(username);

        mUsernameButtons.put(username, usernameButton);

        validateAndStyleUsername(Collections.singletonList(username), validationEndListener);
    }

    private void removeUsername(String username) {
        final ViewGroup usernamesView = (ViewGroup) getView().findViewById(R.id.usernames);

        ViewGroup removedButton = mUsernameButtons.remove(username);
        mUsernameResults.remove(username);
        usernamesView.removeView(removedButton);

        updateUsernameError(username, null);
    }

    private boolean isUserInInvitees(String username) {
        return mUsernameButtons.get(username) != null;
    }

    /**
     * Deletes the last entered username.
     *
     * @return true if the username was deleted
     */
    private boolean removeLastEnteredUsername() {
        if (!TextUtils.isEmpty(mUsernameEditText.getText())) {
            return false;
        }

        // try and remove the last entered username
        List<String> list = new ArrayList<>(mUsernameButtons.keySet());
        if (!list.isEmpty()) {
            String username = list.get(list.size() - 1);
            removeUsername(username);
            return true;
        }
        return false;
    }

    @Override
    public void onRoleSelected(RoleModel newRole) {
        setRole(newRole.getName());

        if (!mUsernameButtons.keySet().isEmpty()) {
            // clear the username results list and let the 'validate' routine do the updates
            mUsernameResults.clear();

            validateAndStyleUsername(mUsernameButtons.keySet(), null);
        }
    }

    private void setRole(String newRole) {
        mCurrentRole = newRole;
        refreshRoleTextView();
    }

    private void refreshRoleTextView() {
        mRoleTextView.setText(RoleUtils.getDisplayName(mCurrentRole, mInviteRoles));
    }

    private void validateAndStyleUsername(Collection<String> usernames,
                                          final ValidationEndListener validationEndListener) {
        List<String> usernamesToCheck = new ArrayList<>();

        for (String username : usernames) {
            if (mUsernameResults.containsKey(username)) {
                String resultMessage = mUsernameResults.get(username);
                styleButton(username, resultMessage);
                updateUsernameError(username, resultMessage);
            } else {
                styleButton(username, null);
                updateUsernameError(username, null);

                usernamesToCheck.add(username);
            }
        }

        if (usernamesToCheck.size() > 0) {
            long wpcomBlogId = mSite.getSiteId();
            PeopleUtils.validateUsernames(usernamesToCheck, mCurrentRole, wpcomBlogId,
                    new PeopleUtils.ValidateUsernameCallback() {
                        @Override
                        public void onUsernameValidation(String username,
                                                         ValidationResult validationResult) {
                            if (!isAdded()) {
                                return;
                            }

                            if (!isUserInInvitees(username)) {
                                // user is removed from invitees before validation
                                return;
                            }

                            final String usernameResultString =
                                    getValidationErrorString(username, validationResult);
                            mUsernameResults.put(username, usernameResultString);

                            styleButton(username, usernameResultString);
                            updateUsernameError(username, usernameResultString);
                        }

                        @Override
                        public void onValidationFinished() {
                            if (validationEndListener != null) {
                                validationEndListener.onValidationEnd();
                            }
                        }

                        @Override
                        public void onError() {
                            // properly style the button
                        }
                    });
        } else {
            if (validationEndListener != null) {
                validationEndListener.onValidationEnd();
            }
        }
    }

    private void styleButton(String username, @Nullable String validationResultMessage) {
        if (!isAdded()) {
            return;
        }

        TextView textView = mUsernameButtons.get(username).findViewById(R.id.username);
        textView.setTextColor(ContextCompat.getColor(getActivity(),
                validationResultMessage == null ? mNormalUsernameColor
                        : (validationResultMessage.equals(FLAG_SUCCESS) ? mAddedUsernameColor : mErrorUsernameColor)));
    }

    private
    @Nullable
    String getValidationErrorString(String username, ValidationResult validationResult) {
        switch (validationResult) {
            case USER_NOT_FOUND:
                return getString(R.string.invite_username_not_found, username);
            case ALREADY_MEMBER:
                return getString(R.string.invite_already_a_member, username);
            case ALREADY_FOLLOWING:
                return getString(R.string.invite_already_following, username);
            case BLOCKED_INVITES:
                return getString(R.string.invite_user_blocked_invites, username);
            case INVALID_EMAIL:
                return getString(R.string.invite_invalid_email, username);
            case USER_FOUND:
                return FLAG_SUCCESS;
        }

        return null;
    }

    private void updateUsernameError(String username, @Nullable String usernameResult) {
        if (!isAdded()) {
            return;
        }

        TextView usernameErrorTextView;
        if (mUsernameErrorViews.containsKey(username)) {
            usernameErrorTextView = (TextView) mUsernameErrorViews.get(username);

            if (usernameResult == null || usernameResult.equals(FLAG_SUCCESS)) {
                // no error so we need to remove the existing error view
                ((ViewGroup) usernameErrorTextView.getParent()).removeView(usernameErrorTextView);
                mUsernameErrorViews.remove(username);
                return;
            }
        } else {
            if (usernameResult == null || usernameResult.equals(FLAG_SUCCESS)) {
                // no error so no need to create a new error view
                return;
            }

            final ViewGroup usernameErrorsContainer = (ViewGroup) getView()
                    .findViewById(R.id.username_errors_container);

            usernameErrorTextView = (TextView) LayoutInflater.from(getActivity())
                                                             .inflate(R.layout.people_invite_error_view,
                                                                     usernameErrorsContainer, false);

            usernameErrorsContainer.addView(usernameErrorTextView);

            mUsernameErrorViews.put(username, usernameErrorTextView);
        }
        usernameErrorTextView.setText(usernameResult);
    }

    private void clearUsernames(Collection<String> usernames) {
        for (String username : usernames) {
            removeUsername(username);
        }

        if (mUsernameButtons.size() == 0) {
            setRole(getDefaultRole());
            resetEditTextContent(mCustomMessageEditText);
        }
    }

    @Override
    public void send() {
        if (!isAdded()) {
            return;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            enableSendButton(true);
            return;
        }

        enableSendButton(false);

        if (mUsernameEditText.getText().toString().length() > 0) {
            addUsername(mUsernameEditText, new ValidationEndListener() {
                @Override
                public void onValidationEnd() {
                    if (!checkAndSend()) {
                        // re-enable SEND button if validation failed
                        enableSendButton(true);
                    }
                }
            });
        } else {
            if (!checkAndSend()) {
                // re-enable SEND button if validation failed
                enableSendButton(true);
            }
        }
    }

    /*
     * returns true if send is attempted, false if validation failed
     * */
    private boolean checkAndSend() {
        if (!isAdded()) {
            return false;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            return false;
        }

        if (mUsernameButtons.size() == 0) {
            ToastUtils.showToast(getActivity(), R.string.invite_error_no_usernames);
            return false;
        }

        int invalidCount = 0;
        for (String usernameResultString : mUsernameResults.values()) {
            if (!usernameResultString.equals(FLAG_SUCCESS)) {
                invalidCount++;
            }
        }

        if (invalidCount > 0) {
            ToastUtils.showToast(getActivity(),
                    StringUtils.getQuantityString(getActivity(), 0,
                            R.string.invite_error_invalid_usernames_one,
                            R.string.invite_error_invalid_usernames_multiple,
                            invalidCount));
            return false;
        }

        // set the "SEND" option disabled
        enableSendButton(false);

        long wpcomBlogId = mSite.getSiteId();
        PeopleUtils.sendInvitations(
                new ArrayList<>(mUsernameButtons.keySet()), mCurrentRole, mCustomMessage, wpcomBlogId,
                new PeopleUtils.InvitationsSendCallback() {
                    @Override
                    public void onSent(List<String> succeededUsernames, Map<String, String> failedUsernameErrors) {
                        if (!isAdded()) {
                            return;
                        }

                        clearUsernames(succeededUsernames);

                        if (failedUsernameErrors.size() != 0) {
                            clearUsernames(failedUsernameErrors.keySet());

                            for (Map.Entry<String, String> error : failedUsernameErrors.entrySet()) {
                                final String username = error.getKey();
                                final String errorMessage = error.getValue();
                                mUsernameResults.put(username, getString(R.string.invite_error_for_username,
                                        username, errorMessage));
                            }

                            populateUsernameButtons(failedUsernameErrors.keySet());

                            ToastUtils.showToast(getActivity(), succeededUsernames.isEmpty()
                                    ? R.string.invite_error_sending
                                    : R.string.invite_error_some_failed);
                        } else {
                            ToastUtils.showToast(getActivity(), R.string.invite_sent, ToastUtils.Duration.LONG);
                        }

                        // set the "SEND" option enabled again
                        enableSendButton(true);
                    }

                    @Override
                    public void onError() {
                        if (!isAdded()) {
                            return;
                        }
                        ToastUtils.showToast(getActivity(), R.string.invite_error_sending);
                        // set the "SEND" option enabled again
                        enableSendButton(true);
                    }
                });

        return true;
    }

    private void enableSendButton(boolean enable) {
        mInviteOperationInProgress = !enable;
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // we need to remove focus listener when view is destroyed (ex. orientation change) to prevent mUsernameEditText
        // content from being converted to username
        if (mUsernameEditText != null) {
            mUsernameEditText.setOnFocusChangeListener(null);
        }
    }

    public interface ValidationEndListener {
        void onValidationEnd();
    }
}
