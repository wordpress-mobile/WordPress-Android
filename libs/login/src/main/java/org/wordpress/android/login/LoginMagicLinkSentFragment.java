package org.wordpress.android.login;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import org.wordpress.android.util.GravatarUtils;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class LoginMagicLinkSentFragment extends Fragment {
    public static final String TAG = "login_magic_link_sent_fragment_tag";

    private static final String ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS";

    private LoginListener mLoginListener;

    private String mEmail;

    @Inject protected LoginAnalyticsListener mAnalyticsListener;

    public static LoginMagicLinkSentFragment newInstance(String email) {
        LoginMagicLinkSentFragment fragment = new LoginMagicLinkSentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL_ADDRESS, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mEmail = getArguments().getString(ARG_EMAIL_ADDRESS);
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

        view.findViewById(R.id.login_enter_password).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginListener != null) {
                    mLoginListener.usePasswordInstead(mEmail);
                }
            }
        });

        final View avatarProgressBar = view.findViewById(R.id.avatar_progress);
        ImageView avatarView = view.findViewById(R.id.gravatar);

        TextView emailView = view.findViewById(R.id.email);
        emailView.setText(mEmail);

        Glide.with(this)
             .load(GravatarUtils.gravatarFromEmail(mEmail,
                     getContext().getResources().getDimensionPixelSize(R.dimen.avatar_sz_login)))
             .apply(RequestOptions.circleCropTransform())
             .apply(RequestOptions.placeholderOf(R.drawable.ic_gridicons_user_circle_100dp))
             .apply(RequestOptions.errorOf(R.drawable.ic_gridicons_user_circle_100dp))
             .listener(new RequestListener<Drawable>() {
                 @Override
                 public boolean onLoadFailed(@Nullable GlideException e, Object o, Target<Drawable> target, boolean b) {
                     avatarProgressBar.setVisibility(View.GONE);
                     return false;
                 }

                 @Override
                 public boolean onResourceReady(Drawable drawable, Object o, Target<Drawable> target,
                                                DataSource dataSource, boolean b) {
                     avatarProgressBar.setVisibility(View.GONE);
                     return false;
                 }
             })
             .into(avatarView);

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
            mAnalyticsListener.trackMagicLinkOpenEmailClientViewed();
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
            if (mLoginListener != null) {
                mLoginListener.helpMagicLinkSent(mEmail);
            }
            return true;
        }

        return false;
    }
}
