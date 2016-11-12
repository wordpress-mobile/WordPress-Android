package org.wordpress.android.ui.people;


import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
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

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Role;
import org.wordpress.android.ui.people.utils.PeopleUtils;
import org.wordpress.android.ui.people.utils.PeopleUtils.ValidateUsernameCallback.ValidationResult;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.MultiUsernameEditText;
import org.wordpress.passcodelock.AppLockManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PeopleInviteFragment extends Fragment implements
        RoleSelectDialogFragment.OnRoleSelectListener,
        PeopleManagementActivity.InvitationSender {

    private static final String FLAG_SUCCESS = "SUCCESS";

    private static final String ARG_BLOGID = "ARG_BLOGID";

    private static final int MAX_NUMBER_OF_INVITEES = 10;
    private static final String[] USERNAME_DELIMITERS = {" ", ","};
    private final Map<String, ViewGroup> mUsernameButtons = new LinkedHashMap<>();
    private final HashMap<String, String> mUsernameResults = new HashMap<>();
    private final Map<String, View> mUsernameErrorViews = new Hashtable<>();
    private ViewGroup mUsernamesContainer;
    private MultiUsernameEditText mUsernameEditText;
    private TextView mRoleTextView;
    private EditText mCustomMessageEditText;

    private Role mRole;
    private String mCustomMessage = "";
    private boolean mInviteOperationInProgress = false;

    public static PeopleInviteFragment newInstance(String dotComBlogId) {
        PeopleInviteFragment peopleInviteFragment = new PeopleInviteFragment();

        Bundle bundle = new Bundle();
        bundle.putString(ARG_BLOGID, dotComBlogId);

        peopleInviteFragment.setArguments(bundle);
        return peopleInviteFragment;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        return inflater.inflate(R.layout.people_invite_fragment, container, false);
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

        Role role = mRole;
        if (role == null) {
            role = getDefaultRole();
        }

        mUsernameEditText = (MultiUsernameEditText) view.findViewById(R.id.invite_usernames);

        //handle key preses from hardware keyboard
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
            private boolean shouldIgnoreChanges = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (shouldIgnoreChanges) { //used to avoid double call after calling setText from this method
                    return;
                }

                shouldIgnoreChanges = true;
                if (mUsernameButtons.size() >= MAX_NUMBER_OF_INVITEES && !TextUtils.isEmpty(s)) {
                    resetEditTextContent(mUsernameEditText);
                } else if (endsWithDelimiter(mUsernameEditText.getText().toString())) {
                    addUsername(mUsernameEditText, null);
                }
                shouldIgnoreChanges = false;
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


        if (mUsernameButtons.size() > 0) {
            ArrayList<String> usernames = new ArrayList<>(mUsernameButtons.keySet());
            populateUsernameButtons(usernames);
        }


        view.findViewById(R.id.role_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RoleSelectDialogFragment.show(PeopleInviteFragment.this, 0, isPrivateSite());
            }
        });

        mRoleTextView = (TextView) view.findViewById(R.id.role);
        setRole(role);
        ImageView imgRoleInfo = (ImageView) view.findViewById(R.id.imgRoleInfo);
        imgRoleInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse(getString(R.string.role_info_url));
                AppLockManager.getInstance().setExtendedTimeout();
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        });

        final int MAX_CHARS = getResources().getInteger(R.integer.invite_message_char_limit);
        final TextView remainingCharsTextView = (TextView) view.findViewById(R.id.message_remaining);

        mCustomMessageEditText = (EditText) view.findViewById(R.id.message);
        mCustomMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCustomMessage = mCustomMessageEditText.getText().toString();
                updateRemainingCharsView(remainingCharsTextView, mCustomMessage, MAX_CHARS);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        updateRemainingCharsView(remainingCharsTextView, mCustomMessage, MAX_CHARS);
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

    private Role getDefaultRole() {
        Role[] inviteRoles = Role.inviteRoles(isPrivateSite());
        return inviteRoles[0];
    }

    private void updateRemainingCharsView(TextView remainingCharsTextView, String currentString, int limit) {
        remainingCharsTextView.setText(StringUtils.getQuantityString(getActivity(),
                R.string.invite_message_remaining_zero,
                R.string.invite_message_remaining_one,
                R.string.invite_message_remaining_other, limit - (currentString == null ? 0 : currentString.length())));
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

        final ViewGroup usernameButton = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout
                .invite_username_button, null);
        final TextView usernameTextView = (TextView) usernameButton.findViewById(R.id.username);
        usernameTextView.setText(username);

        mUsernamesContainer.addView(usernameButton, mUsernamesContainer.getChildCount() - 1);

        final ImageButton delete = (ImageButton) usernameButton.findViewById(R.id.username_delete);
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

        //try and remove the last entered username
        List<String> list = new ArrayList<>(mUsernameButtons.keySet());
        if (!list.isEmpty()) {
            String username = list.get(list.size() - 1);
            removeUsername(username);
            return true;
        }
        return false;
    }

    @Override
    public void onRoleSelected(Role newRole) {
        setRole(newRole);

        if (!mUsernameButtons.keySet().isEmpty()) {
            // clear the username results list and let the 'validate' routine do the updates
            mUsernameResults.clear();

            validateAndStyleUsername(mUsernameButtons.keySet(), null);
        }
    }

    private void setRole(Role newRole) {
        mRole = newRole;
        mRoleTextView.setText(newRole.toDisplayString());
    }

    private void validateAndStyleUsername(Collection<String> usernames, final ValidationEndListener validationEndListener) {
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

            String dotComBlogId = getArguments().getString(ARG_BLOGID);
            PeopleUtils.validateUsernames(usernamesToCheck, mRole, dotComBlogId, new PeopleUtils.ValidateUsernameCallback() {
                @Override
                public void onUsernameValidation(String username, ValidationResult validationResult) {
                    if (!isAdded()) {
                        return;
                    }

                    if (!isUserInInvitees(username)) {
                        //user is removed from invitees before validation
                        return;
                    }

                    final String usernameResultString = getValidationErrorString(username, validationResult);
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

        TextView textView = (TextView) mUsernameButtons.get(username).findViewById(R.id.username);
        textView.setTextColor(ContextCompat.getColor(getActivity(),
                validationResultMessage == null ? R.color.grey_dark :
                        (validationResultMessage.equals(FLAG_SUCCESS) ? R.color.blue_wordpress : R.color.alert_red)));
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

            usernameErrorTextView = (TextView) LayoutInflater.from(getActivity())
                    .inflate(R.layout.people_invite_error_view, null);

            final ViewGroup usernameErrorsContainer = (ViewGroup) getView()
                    .findViewById(R.id.username_errors_container);
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
                        //re-enable SEND button if validation failed
                        enableSendButton(true);
                    }
                }
            });
        } else {
            if (!checkAndSend()) {
                //re-enable SEND button if validation failed
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
            ToastUtils.showToast(getActivity(), StringUtils.getQuantityString(getActivity(), 0,
                    R.string.invite_error_invalid_usernames_one,
                    R.string.invite_error_invalid_usernames_multiple, invalidCount));
            return false;
        }

        //set the  "SEND" option disabled
        enableSendButton(false);

        String dotComBlogId = getArguments().getString(ARG_BLOGID);
        PeopleUtils.sendInvitations(new ArrayList<>(mUsernameButtons.keySet()), mRole, mCustomMessage, dotComBlogId,
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
                                    ? R.string.invite_error_sending : R.string.invite_error_some_failed);
                        } else {
                            ToastUtils.showToast(getActivity(), R.string.invite_sent, ToastUtils.Duration.LONG);
                        }

                        //set the  "SEND" option enabled again
                        enableSendButton(true);
                    }

                    @Override
                    public void onError() {
                        if (!isAdded()) {
                            return;
                        }

                        ToastUtils.showToast(getActivity(), R.string.invite_error_sending);

                        //set the  "SEND" option enabled again
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
        //we need to remove focus listener when view is destroyed (ex. orientation change) to prevent mUsernameEditText
        //content from being converted to username
        if (mUsernameEditText != null) {
            mUsernameEditText.setOnFocusChangeListener(null);
        }
    }

    private boolean isPrivateSite() {
        String dotComBlogId = getArguments().getString(ARG_BLOGID);
        Blog blog = WordPress.wpDB.getBlogForDotComBlogId(dotComBlogId);
        return blog != null && blog.isPrivate();
    }

    public interface ValidationEndListener {
        void onValidationEnd();
    }
}
