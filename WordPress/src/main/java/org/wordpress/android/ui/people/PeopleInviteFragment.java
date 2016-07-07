package org.wordpress.android.ui.people;

import org.wordpress.android.R;
import org.wordpress.android.ui.people.utils.PeopleUtils;
import org.wordpress.android.ui.people.utils.PeopleUtils.ValidateUsernameCallback.ValidationResult;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
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
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

    private ViewGroup mUsernamesContainer;
    private EditText mUsernameEditText;
    private TextView mRoleTextView;
    private EditText mCustomMessageEditText;

    private Map<String, ViewGroup> mUsernameButtons = new LinkedHashMap<>();
    private HashMap<String, String> mUsernameResults = new HashMap<>();
    private Map<String, View> mUsernameErrorViews = new Hashtable<>();
    private String mRole;
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

    /**
     * Sets the enter & pop animation for the fragment. In order to keep the animation even after the configuration
     * changes, this method is used instead of FragmentTransaction for the animation.
     */
    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        if (enter) {
            return AnimatorInflater.loadAnimator(getActivity(), R.animator.fragment_slide_in_from_right);
        } else {
            return AnimatorInflater.loadAnimator(getActivity(), R.animator.fragment_slide_out_to_right);
        }
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

        mUsernamesContainer = (ViewGroup) getView().findViewById(R.id.usernames);

        String role = mRole;
        if (role == null) {
            role = loadDefaultRole();
        }

        mUsernameEditText = (EditText) getView().findViewById(R.id.invite_usernames);
        mUsernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mUsernameEditText.getText().toString().endsWith(" ")) {
                    addUsername(mUsernameEditText, null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
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

        View usernamesContainer = getView().findViewById(R.id.usernames);
        usernamesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditTextUtils.showSoftInput(mUsernameEditText);
            }
        });

        if (mUsernameButtons != null && mUsernameButtons.size() > 0) {
            ArrayList<String> usernames = new ArrayList<>(mUsernameButtons.keySet());
            populateUsernameButtons(usernames);
        }

        View roleContainer = getView().findViewById(R.id.role_container);
        roleContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RoleSelectDialogFragment.show(PeopleInviteFragment.this, 0);
            }
        });
        mRoleTextView = (TextView) getView().findViewById(R.id.role);

        setRole(role);

        final int MAX_CHARS = getResources().getInteger(R.integer.invite_message_char_limit);
        final TextView remainingCharsTextView = (TextView) getView().findViewById(R.id.message_remaining);

        mCustomMessageEditText = (EditText) getView().findViewById(R.id.message);
        mCustomMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCustomMessage = mCustomMessageEditText.getText().toString();
                updateRemainingCharsView(remainingCharsTextView, mCustomMessage, MAX_CHARS);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        updateRemainingCharsView(remainingCharsTextView, mCustomMessage, MAX_CHARS);
    }

    private String loadDefaultRole() {
        final String[] roles = getResources().getStringArray(R.array.roles);
        return roles[roles.length - 1];
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
        String username = editText.getText().toString().trim();
        editText.setText("");

        if (username.isEmpty() || mUsernameButtons.keySet().contains(username)) {
            if (validationEndListener != null) {
                validationEndListener.onValidationEnd();
            }
            return;
        }

        final ViewGroup usernameButton = buttonizeUsername(username);

        mUsernameButtons.put(username, usernameButton);

        validateAndStyleUsername(Arrays.asList(new String[]{ username }), validationEndListener);
    }

    private void removeUsername(String username) {
        final ViewGroup usernamesView = (ViewGroup) getView().findViewById(R.id.usernames);

        ViewGroup removedButton = mUsernameButtons.remove(username);
        mUsernameResults.remove(username);
        usernamesView.removeView(removedButton);

        final ViewGroup usernamesContainer = (ViewGroup) getView().findViewById(R.id.usernames_container);
        View removedErrorView = mUsernameErrorViews.remove(username);
        usernamesContainer.removeView(removedErrorView);
    }

    @Override
    public void onRoleSelected(String newRole) {
        setRole(newRole);
    }

    private void setRole(String newRole) {
        mRole = newRole;
        mRoleTextView.setText(newRole);
    }

    private void validateAndStyleUsername(Collection<String> usernames, final ValidationEndListener validationEndListener) {
        List<String> usernamesToCheck = new ArrayList<>();
        List<String> usernamesChecked = new ArrayList<>();

        for (String username : usernames) {
            if (mUsernameResults.containsKey(username)) {
                usernamesChecked.add(username);
            } else {
                usernamesToCheck.add(username);
            }
        }

        for (String username : usernamesChecked) {
            String resultMessage = mUsernameResults.get(username);
            if (!resultMessage.equals(FLAG_SUCCESS)) {
                styleButton(username, resultMessage);
                appendError(username, resultMessage);
            }
        }

        if (usernamesToCheck.size() > 0) {

            String dotComBlogId = getArguments().getString(ARG_BLOGID);
            PeopleUtils.validateUsernames(usernamesToCheck, dotComBlogId, new PeopleUtils.ValidateUsernameCallback() {
                @Override
                public void onUsernameValidation(String username, ValidationResult validationResult) {
                    if (!isAdded()) {
                        return;
                    }

                    final String usernameResultString = getValidationErrorString(username, validationResult);
                    mUsernameResults.put(username, usernameResultString);

                    if (validationResult != ValidationResult.USER_FOUND) {
                        styleButton(username, usernameResultString);
                        appendError(username, usernameResultString);
                    }
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

    public interface ValidationEndListener {
        void onValidationEnd();
    }

    private void styleButton(String username, String validationResultMessage) {
        if (!isAdded()) {
            return;
        }

        TextView textView = (TextView) mUsernameButtons.get(username).findViewById(R.id.username);

        if (!validationResultMessage.equals(FLAG_SUCCESS)) {
            textView.setTextColor(ContextCompat.getColor(getActivity(), R.color.alert_red));
        } else {
            // properly style the button
        }
    }

    private @Nullable String getValidationErrorString(String username, ValidationResult validationResult) {
        switch (validationResult) {
            case USER_NOT_FOUND:
                return getString(R.string.invite_username_not_found, username);
            case ALREADY_MEMBER:
                return getString(R.string.invite_already_a_member, username);
            case INVALID_EMAIL:
                return getString(R.string.invite_invalid_email, username);
            case USER_FOUND:
                return FLAG_SUCCESS;
        }

        return null;
    }

    private void appendError(String username, String error) {
        if (!isAdded() || error == null) {
            return;
        }

        final ViewGroup usernamesContainer = (ViewGroup) getView().findViewById(R.id.usernames_container);
        final TextView usernameError = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout
                .people_invite_error_view, null);
        usernameError.setText(error);
        usernamesContainer.addView(usernameError);
        mUsernameErrorViews.put(username, usernameError);
    }

    private void clearUsernames(Collection<String> usernames) {
        for (String username : usernames) {
            removeUsername(username);
        }

        if (mUsernameButtons.size() == 0) {
            setRole(loadDefaultRole());

            mCustomMessageEditText.setText("");
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
                    if (!checkAndSend()){
                        //re-enable SEND button if validation failed
                        enableSendButton(true);
                    }
                }
            });
        } else {
            if (!checkAndSend()){
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

                            ToastUtils.showToast(getActivity(), R.string.invite_error_some_failed);
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

    private void enableSendButton(boolean enable){
        mInviteOperationInProgress = !enable;
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }
}
