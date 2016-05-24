package org.wordpress.android.ui.people;

import org.wordpress.android.R;
import org.wordpress.android.ui.people.utils.PeopleUtils;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

public class PeopleInviteFragment extends Fragment {
    private static final String KEY_USERNAMES = "KEY_USERNAMES";

    private static final String ARG_BLOGID = "ARG_BLOGID";

    private ArrayList<String> mUsernames = new ArrayList<>();

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mUsernames = savedInstanceState.getStringArrayList(KEY_USERNAMES);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.people_invite_fragment, container, false);

        final ViewGroup usernamesView = (ViewGroup) rootView.findViewById(R.id.usernames);

        if (mUsernames != null) {
            for (String username : mUsernames) {
                buttonizeUsername(username, inflater, usernamesView);
            }
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

    private void buttonizeUsername(String username, LayoutInflater inflater, final ViewGroup usernames) {
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

        String dotComBlogId = getArguments().getString(ARG_BLOGID);

        PeopleUtils.validateUsername(username, dotComBlogId, new PeopleUtils.ValidateUsernameCallback() {
            @Override
            public void onUsernameNotFound(String username) {
                // properly style the button
            }

            @Override
            public void onUsernameFound(String username) {
                // properly style the button
            }

            @Override
            public void onUsernameAlreadyMember(String username) {
                // properly style the button
            }

            @Override
            public void onInvalidEmail(String username) {
                // properly style the button
            }

            @Override
            public void onError() {
                // properly style the button
            }
        });
    }

    private void addUsername(EditText editText, LayoutInflater inflater, final ViewGroup usernamesView) {
        String username = editText.getText().toString().trim();
        editText.setText("");

        mUsernames.add(username);

        buttonizeUsername(username, inflater, usernamesView);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(KEY_USERNAMES, mUsernames);

        super.onSaveInstanceState(outState);
    }
}
