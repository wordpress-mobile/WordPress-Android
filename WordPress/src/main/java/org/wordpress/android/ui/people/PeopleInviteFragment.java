package org.wordpress.android.ui.people;

import org.wordpress.android.R;
import org.wordpress.android.ui.people.utils.PeopleUtils;
import org.wordpress.android.ui.people.utils.PeopleUtils.ValidateUsernameCallback.ValidationResult;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PeopleInviteFragment extends Fragment implements UsernameRemoveDialogFragment.UsernameRemover {
    private static final String KEY_USERNAMES = "KEY_USERNAMES";

    private static final String ARG_BLOGID = "ARG_BLOGID";

    private Map<String, Button> mUsernameButtons = new LinkedHashMap<>();

    public static PeopleInviteFragment newInstance(String dotComBlogId) {
        PeopleInviteFragment peopleInviteFragment = new PeopleInviteFragment();

        Bundle bundle = new Bundle();
        bundle.putString(ARG_BLOGID, dotComBlogId);

        peopleInviteFragment.setArguments(bundle);
        return peopleInviteFragment;
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
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.people_invite_fragment, container, false);

        final ViewGroup usernamesView = (ViewGroup) rootView.findViewById(R.id.usernames);

        if (savedInstanceState != null) {
            ArrayList<String> usernames = savedInstanceState.getStringArrayList(KEY_USERNAMES);
            populateUsernameButtons(usernames, inflater, usernamesView);
        }

        final EditText editText = (EditText) rootView.findViewById(R.id.invite_usernames);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d("INVITE", "beforeTextChanged: " + count + " char(s): " + s.toString());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (editText.getText().toString().endsWith(" ")) {
                    addUsername(editText, inflater, usernamesView);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event.getAction() == KeyEvent.ACTION_UP && event
                        .getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    addUsername(editText, inflater, usernamesView);
                }
                return true;
            }
        });

        return rootView;
    }

    private void populateUsernameButtons(List<String> usernames, LayoutInflater inflater, ViewGroup usernamesView) {
        if (usernames != null && usernames.size() > 0) {

            for (String username : usernames) {
                mUsernameButtons.put(username, buttonizeUsername(username, inflater, usernamesView));
            }

            String dotComBlogId = getArguments().getString(ARG_BLOGID);
            PeopleUtils.validateUsernames(usernames, dotComBlogId, new PeopleUtils.ValidateUsernameCallback() {
                @Override
                public void onUsernameValidation(String username, ValidationResult validationResult) {
                    if (!isAdded()) {
                        return;
                    }

                    styleButton(username, mUsernameButtons.get(username), validationResult);
                }

                @Override
                public void onError() {
                    // properly style the button
                }
            });
        }
    }

    private Button buttonizeUsername(String username, LayoutInflater inflater, final ViewGroup usernames) {
        final AppCompatButton usernameButton = (AppCompatButton) inflater.inflate(R.layout.invite_username_button,
                null);
        usernameButton.setText(username);
        usernames.addView(usernameButton, usernames.getChildCount() - 1);
        usernameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usernames.removeView(usernameButton);
            }
        });

        return usernameButton;
    }

    private void addUsername(EditText editText, LayoutInflater inflater, final ViewGroup usernamesView) {
        String username = editText.getText().toString().trim();
        editText.setText("");

        final Button usernameButton = buttonizeUsername(username, inflater, usernamesView);

        mUsernameButtons.put(username, usernameButton);

        String dotComBlogId = getArguments().getString(ARG_BLOGID);
        PeopleUtils.validateUsernames(Arrays.asList(new String[]{ username }), dotComBlogId,
                new PeopleUtils.ValidateUsernameCallback() {
            @Override
            public void onUsernameValidation(String username, ValidationResult validationResult) {
                if (!isAdded()) {
                    return;
                }

                styleButton(username, usernameButton, validationResult);
            }

            @Override
            public void onError() {
                // properly style the button
            }
        });
    }

    @Override
    public void removeUsername(String username) {
        Button removedButton = mUsernameButtons.remove(username);

        final ViewGroup usernamesView = (ViewGroup) getView().findViewById(R.id.usernames);
        usernamesView.removeView(removedButton);
    }

    private void styleButton(String username, Button button, ValidationResult validationResult) {
        if (!isAdded()) {
            return;
        }

        switch (validationResult) {
            case USER_NOT_FOUND:
                setupConfirmationDialog(button, username, R.string.invite_username_not_found_title, R.string
                        .invite_username_not_found);
                button.setTextColor(ContextCompat.getColor(getActivity(), R.color.alert_red));
                break;
            case ALREADY_MEMBER:
                setupConfirmationDialog(button, username, R.string.invite_already_a_member_title, R.string
                        .invite_already_a_member);
                button.setTextColor(ContextCompat.getColor(getActivity(), R.color.alert_red));
                break;
            case INVALID_EMAIL:
                setupConfirmationDialog(button, username, R.string.invite_invalid_email_title, R.string
                        .invite_invalid_email);
                button.setTextColor(ContextCompat.getColor(getActivity(), R.color.alert_red));
                break;
            case USER_FOUND:
                // properly style the button
                break;
        }
    }

    private void setupConfirmationDialog(Button button, final String username, final @StringRes int titleId, final
            @StringRes int messageId) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UsernameRemoveDialogFragment.show(username, titleId, messageId, PeopleInviteFragment.this, 0);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList(KEY_USERNAMES, new ArrayList<>(mUsernameButtons.keySet()));

        super.onSaveInstanceState(outState);
    }
}
