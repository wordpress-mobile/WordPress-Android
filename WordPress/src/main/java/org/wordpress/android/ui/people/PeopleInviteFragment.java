package org.wordpress.android.ui.people;

import org.wordpress.android.R;

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

public class PeopleInviteFragment extends Fragment {

    public static PeopleInviteFragment newInstance() {
        PeopleInviteFragment personDetailFragment = new PeopleInviteFragment();
        Bundle bundle = new Bundle();
        personDetailFragment.setArguments(bundle);
        return personDetailFragment;
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

        final ViewGroup usernames = (ViewGroup) rootView.findViewById(R.id.usernames);

        final EditText editText = (EditText) rootView.findViewById(R.id.invite_usernames);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d("INVITE", "beforeTextChanged: " + count + " char(s): " + s.toString());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (editText.getText().toString().endsWith(" ")) {
                    addUsername(editText, inflater, usernames);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addUsername(editText, inflater, usernames);
                }
                return true;
            }
        });

        return rootView;
    }

    private void addUsername(EditText editText, LayoutInflater inflater, final ViewGroup usernames) {
        String uname = editText.getText().toString().trim();
        final AppCompatButton usernameButton = (AppCompatButton) inflater.inflate(R.layout.invite_username_button,
                null);
        usernameButton.setText(uname);
        usernames.addView(usernameButton, usernames.getChildCount() - 1);
        editText.setText("");

        usernameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usernames.removeView(usernameButton);
            }
        });
    }
}
