package org.wordpress.android.ui.people;

import org.wordpress.android.R;
import org.wordpress.android.ui.people.utils.PeopleUtils;
import org.wordpress.android.ui.people.utils.PeopleUtils.ValidateUsernameCallback.ValidationResult;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.StringRes;
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
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PeopleInviteFragment extends Fragment implements
        UsernameRemoveDialogFragment.UsernameRemover,
        RoleSelectDialogFragment.OnRoleSelectListener,
        PeopleManagementActivity.InvitationSender {
    private static final String KEY_USERNAMES = "KEY_USERNAMES";
    private static final String KEY_ROLE = "KEY_ROLE";
    private static final String KEY_CUSTOM_MESSAGE = "KEY_CUSTOM_MESSAGE";

    private static final String ARG_BLOGID = "ARG_BLOGID";

    private ViewGroup mUsernamesContainer;
    private EditText mUsernameEditText;
    private TextView mRoleTextView;
    private EditText mCustomMessageEditText;

    private Map<String, ViewGroup> mUsernameButtons = new LinkedHashMap<>();
    private Map<String, ValidationResult> mUsernameResults = new Hashtable<>();
    private String mRole;
    private String mCustomMessage = "";

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
    public void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList(KEY_USERNAMES, new ArrayList<>(mUsernameButtons.keySet()));
        outState.putString(KEY_ROLE, mRole);
        outState.putString(KEY_CUSTOM_MESSAGE, mCustomMessage);

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.people_invite_fragment, container, false);

        mUsernamesContainer = (ViewGroup) rootView.findViewById(R.id.usernames);

        String role = null;
        if (savedInstanceState != null) {
            ArrayList<String> usernames = savedInstanceState.getStringArrayList(KEY_USERNAMES);
            populateUsernameButtons(usernames, inflater, mUsernamesContainer);

            role = savedInstanceState.getString(KEY_ROLE);
        }

        if (role == null) {
            role = loadDefaultRole();
        }

        mUsernameEditText = (EditText) rootView.findViewById(R.id.invite_usernames);
        mUsernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mUsernameEditText.getText().toString().endsWith(" ")) {
                    addUsername(mUsernameEditText, inflater, mUsernamesContainer, null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        mUsernameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event.getAction() == KeyEvent.ACTION_UP && event
                        .getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    addUsername(mUsernameEditText, inflater, mUsernamesContainer, null);
                }
                return true;
            }
        });
        mUsernameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus && mUsernameEditText.getText().toString().length() > 0) {
                    addUsername(mUsernameEditText, inflater, mUsernamesContainer, null);
                }
            }
        });

        View usernamesContainer = rootView.findViewById(R.id.usernames);
        usernamesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUsernameEditText.requestFocus();
            }
        });

        View roleContainer = rootView.findViewById(R.id.role_container);
        roleContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RoleSelectDialogFragment.show(PeopleInviteFragment.this, 0);
            }
        });
        mRoleTextView = (TextView) rootView.findViewById(R.id.role);

        setRole(role);

        final int MAX_CHARS = getResources().getInteger(R.integer.invite_message_char_limit);
        final TextView remainingCharsTextView = (TextView) rootView.findViewById(R.id.message_remaining);

        mCustomMessageEditText = (EditText) rootView.findViewById(R.id.message);
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

        return rootView;
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

    private void populateUsernameButtons(List<String> usernames, LayoutInflater inflater, ViewGroup usernamesView) {
        if (usernames != null && usernames.size() > 0) {

            for (String username : usernames) {
                mUsernameButtons.put(username, buttonizeUsername(username, inflater, usernamesView));
            }

            validateAndStyleUsername(usernames, null);
        }
    }

    private ViewGroup buttonizeUsername(final String username, LayoutInflater inflater, final ViewGroup usernames) {
        final ViewGroup usernameButton = (ViewGroup) inflater.inflate(R.layout.invite_username_button, null);
        final TextView usernameTextView = (TextView) usernameButton.findViewById(R.id.username);
        usernameTextView.setText(username);
        usernames.addView(usernameButton, usernames.getChildCount() - 1);

        final ImageButton delete = (ImageButton) usernameButton.findViewById(R.id.username_delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeUsername(username);
            }
        });

        return usernameButton;
    }

    private void addUsername(EditText editText, LayoutInflater inflater, final ViewGroup usernamesView,
                             ValidationEndListener validationEndListener) {
        String username = editText.getText().toString().trim();
        editText.setText("");

        if (mUsernameButtons.keySet().contains(username)) {
            if (validationEndListener != null) {
                validationEndListener.onValidationEnd();
            }
            return;
        }

        final ViewGroup usernameButton = buttonizeUsername(username, inflater, usernamesView);

        mUsernameButtons.put(username, usernameButton);

        validateAndStyleUsername(Arrays.asList(new String[]{ username }), validationEndListener);
    }

    @Override
    public void removeUsername(String username) {
        ViewGroup removedButton = mUsernameButtons.remove(username);
        mUsernameResults.remove(username);

        final ViewGroup usernamesView = (ViewGroup) getView().findViewById(R.id.usernames);
        usernamesView.removeView(removedButton);
    }

    @Override
    public void onRoleSelected(String newRole) {
        setRole(newRole);
    }

    private void setRole(String newRole) {
        mRole = newRole;
        mRoleTextView.setText(newRole);
    }

    private void validateAndStyleUsername(List<String> usernames, final ValidationEndListener validationEndListener) {
        String dotComBlogId = getArguments().getString(ARG_BLOGID);
        PeopleUtils.validateUsernames(usernames, dotComBlogId, new PeopleUtils.ValidateUsernameCallback() {
            @Override
            public void onUsernameValidation(String username, ValidationResult validationResult) {
                if (!isAdded()) {
                    return;
                }

                mUsernameResults.put(username, validationResult);

                styleButton(username, mUsernameButtons.get(username), validationResult);
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
    }

    public interface ValidationEndListener {
        void onValidationEnd();
    }

    private void styleButton(String username, ViewGroup button, ValidationResult validationResult) {
        if (!isAdded()) {
            return;
        }

        TextView textView = (TextView) button.findViewById(R.id.username);

        switch (validationResult) {
            case USER_NOT_FOUND:
                setupConfirmationDialog(textView, username, R.string.invite_username_not_found_title, R.string
                        .invite_username_not_found);
                textView.setTextColor(ContextCompat.getColor(getActivity(), R.color.alert_red));
                break;
            case ALREADY_MEMBER:
                setupConfirmationDialog(textView, username, R.string.invite_already_a_member_title, R.string
                        .invite_already_a_member);
                textView.setTextColor(ContextCompat.getColor(getActivity(), R.color.alert_red));
                break;
            case INVALID_EMAIL:
                setupConfirmationDialog(textView, username, R.string.invite_invalid_email_title, R.string
                        .invite_invalid_email);
                textView.setTextColor(ContextCompat.getColor(getActivity(), R.color.alert_red));
                break;
            case USER_FOUND:
                // properly style the button
                textView.setBackgroundDrawable(null);
                break;
        }
    }

    private void setupConfirmationDialog(TextView button, final String username, final @StringRes int titleId, final
            @StringRes int messageId) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UsernameRemoveDialogFragment.show(username, titleId, messageId, PeopleInviteFragment.this, 0);
            }
        });
    }

    private void clearUsernames(List<String> usernames) {
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

        if (mUsernameEditText.getText().toString().length() > 0) {
            addUsername(mUsernameEditText, LayoutInflater.from(getActivity()), mUsernamesContainer, new ValidationEndListener() {
                @Override
                public void onValidationEnd() {
                    checkAndSend();
                }
            });
        } else {
            checkAndSend();
        }
    }

    private void checkAndSend() {
        if (!isAdded()) {
            return;
        }

        if (mUsernameButtons.size() == 0) {
            ToastUtils.showToast(getActivity(), R.string.invite_error_no_usenames);
            return;
        }

        if (mUsernameEditText.getText().toString().length() > 0) {

        }
        int invalidCount = 0;
        for (ValidationResult validationResult : mUsernameResults.values()) {
            if (validationResult != ValidationResult.USER_FOUND) {
                invalidCount++;
            }
        }

        if (invalidCount > 0) {
            ToastUtils.showToast(getActivity(), StringUtils.getQuantityString(getActivity(), 0,
                    R.string.invite_error_invalid_usenames_one,
                    R.string.invite_error_invalid_usenames_multiple, invalidCount));
            return;
        }

        String dotComBlogId = getArguments().getString(ARG_BLOGID);
        PeopleUtils.sendInvitations(new ArrayList<>(mUsernameButtons.keySet()), mRole, mCustomMessage, dotComBlogId,
                new PeopleUtils.InvitationsSendCallback() {
                    @Override
                    public void onSent(List<String> succeededUsernames, List<String> failedUsernames) {
                        if (!isAdded()) {
                            return;
                        }

                        clearUsernames(succeededUsernames);

                        if (failedUsernames.size() != 0) {
                            ToastUtils.showToast(getActivity(), R.string.invite_error_some_failed);
                        } else {
                            ToastUtils.showToast(getActivity(), R.string.invite_sent, ToastUtils.Duration.LONG);
                        }
                    }

                    @Override
                    public void onError() {
                        if (!isAdded()) {
                            return;
                        }

                        ToastUtils.showToast(getActivity(), R.string.invite_error_sending);
                    }
                });
    }
}
