package org.wordpress.android.login;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import org.wordpress.android.login.util.AvatarHelper;
import org.wordpress.android.login.util.AvatarHelper.AvatarRequestListener;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class LoginMagicLinkSentFragment extends Fragment {
    public static final String TAG = "login_magic_link_sent_fragment_tag";

    private static final String ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS";
    private static final String ARG_ALLOW_PASSWORD = "ARG_ALLOW_PASSWORD";

    private LoginListener mLoginListener;

    private String mEmail;
    private boolean mAllowPassword;

    @Inject protected LoginAnalyticsListener mAnalyticsListener;

    public static LoginMagicLinkSentFragment newInstance(String email) {
        return newInstance(email, true);
    }

    public static LoginMagicLinkSentFragment newInstance(String email, boolean allowPassword) {
        LoginMagicLinkSentFragment fragment = new LoginMagicLinkSentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL_ADDRESS, email);
        args.putBoolean(ARG_ALLOW_PASSWORD, allowPassword);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mEmail = getArguments().getString(ARG_EMAIL_ADDRESS);
            mAllowPassword = getArguments().getBoolean(ARG_ALLOW_PASSWORD);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_magic_link_sent_screen, container, false);

        view.findViewById(R.id.login_open_email_client).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginListener != null) {
                    mLoginListener.openEmailClient(true);
                }
            }
        });

        final Button passwordButton = view.findViewById(R.id.login_enter_password);
        passwordButton.setVisibility(mAllowPassword ? View.VISIBLE : View.GONE);
        passwordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAnalyticsListener.trackLoginWithPasswordClick();
                if (mLoginListener != null) {
                    mLoginListener.usePasswordInstead(mEmail);
                }
            }
        });

        final View avatarProgressBar = view.findViewById(R.id.avatar_progress);
        ImageView avatarView = view.findViewById(R.id.gravatar);

        TextView emailView = view.findViewById(R.id.email);
        emailView.setText(mEmail);

        AvatarHelper.loadAvatarFromEmail(this, mEmail, avatarView, new AvatarRequestListener() {
            @Override public void onRequestFinished() {
                avatarProgressBar.setVisibility(View.GONE);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.log_in);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            mAnalyticsListener.trackLoginMagicLinkOpenEmailClientViewed();
        }
    }

    @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // important for accessibility - talkback
        getActivity().setTitle(R.string.magic_link_sent_login_title);
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
        if (context instanceof LoginListener) {
            mLoginListener = (LoginListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement LoginListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_login, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.help) {
            mAnalyticsListener.trackShowHelpClick();
            if (mLoginListener != null) {
                mLoginListener.helpMagicLinkSent(mEmail);
            }
            return true;
        }

        return false;
    }
}
