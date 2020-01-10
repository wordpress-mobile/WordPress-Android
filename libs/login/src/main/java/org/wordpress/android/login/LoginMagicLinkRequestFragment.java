package org.wordpress.android.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
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

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayloadScheme;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayloadSource;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthEmailSent;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.HashMap;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class LoginMagicLinkRequestFragment extends Fragment {
    public static final String TAG = "login_magic_link_request_fragment_tag";

    private static final String KEY_IN_PROGRESS = "KEY_IN_PROGRESS";
    private static final String KEY_GRAVATAR_IN_PROGRESS = "KEY_GRAVATAR_IN_PROGRESS";
    private static final String ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS";
    private static final String ARG_MAGIC_LINK_SCHEME = "ARG_MAGIC_LINK_SCHEME";
    private static final String ARG_IS_JETPACK_CONNECT = "ARG_IS_JETPACK_CONNECT";
    private static final String ARG_JETPACK_CONNECT_SOURCE = "ARG_JETPACK_CONNECT_SOURCE";
    private static final String ARG_VERIFY_MAGIC_LINK_EMAIL = "ARG_VERIFY_MAGIC_LINK_EMAIL";

    private static final String ERROR_KEY = "error";

    private LoginListener mLoginListener;

    private String mEmail;
    private AuthEmailPayloadScheme mMagicLinkScheme;
    private String mJetpackConnectSource;

    private View mAvatarProgressBar;
    private Button mRequestMagicLinkButton;
    private ProgressDialog mProgressDialog;

    private boolean mInProgress;
    private boolean mIsJetpackConnect;
    private boolean mVerifyMagicLinkEmail;

    @Inject protected Dispatcher mDispatcher;

    @Inject protected LoginAnalyticsListener mAnalyticsListener;
    public static LoginMagicLinkRequestFragment newInstance(String email, AuthEmailPayloadScheme scheme,
                                                            boolean isJetpackConnect, String jetpackConnectSource,
                                                            boolean verifyEmail) {
        LoginMagicLinkRequestFragment fragment = new LoginMagicLinkRequestFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL_ADDRESS, email);
        args.putSerializable(ARG_MAGIC_LINK_SCHEME, scheme);
        args.putBoolean(ARG_IS_JETPACK_CONNECT, isJetpackConnect);
        args.putString(ARG_JETPACK_CONNECT_SOURCE, jetpackConnectSource);
        args.putBoolean(ARG_VERIFY_MAGIC_LINK_EMAIL, verifyEmail);
        fragment.setArguments(args);
        return fragment;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mEmail = getArguments().getString(ARG_EMAIL_ADDRESS);
            mMagicLinkScheme = (AuthEmailPayloadScheme) getArguments().getSerializable(ARG_MAGIC_LINK_SCHEME);
            mIsJetpackConnect = getArguments().getBoolean(ARG_IS_JETPACK_CONNECT);
            mJetpackConnectSource = getArguments().getString(ARG_JETPACK_CONNECT_SOURCE);
            mVerifyMagicLinkEmail = getArguments().getBoolean(ARG_VERIFY_MAGIC_LINK_EMAIL);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_magic_link_request_screen, container, false);
        mRequestMagicLinkButton = view.findViewById(R.id.login_request_magic_link);
        mRequestMagicLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginListener != null) {
                    if (NetworkUtils.checkConnection(getActivity())) {
                        showMagicLinkRequestProgressDialog();
                        AuthEmailPayloadSource source = getAuthEmailPayloadSource();
                        AuthEmailPayload authEmailPayload = new AuthEmailPayload(mEmail, false,
                                mIsJetpackConnect ? AccountStore.AuthEmailPayloadFlow.JETPACK : null,
                                source, mMagicLinkScheme);
                        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(authEmailPayload));
                    }
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

        mAvatarProgressBar = view.findViewById(R.id.avatar_progress);
        ImageView avatarView = view.findViewById(R.id.gravatar);

        // Design changes added to the Woo Magic link sign-in
        if (mVerifyMagicLinkEmail) {
            View avatarContainerView = view.findViewById(R.id.avatar_container);

            LayoutParams lp = avatarContainerView.getLayoutParams();
            lp.width = getContext().getResources().getDimensionPixelSize(R.dimen.magic_link_sent_illustration_sz);
            lp.height = getContext().getResources().getDimensionPixelSize(R.dimen.magic_link_sent_illustration_sz);
            avatarContainerView.setLayoutParams(lp);

            mAvatarProgressBar.setVisibility(View.GONE);
            avatarView.setImageResource(R.drawable.login_email_alert);

            TextView labelTextView = view.findViewById(R.id.label);
            labelTextView.setText(Html.fromHtml(String.format(getResources().getString(
                    R.string.login_site_credentials_magic_link_label), mEmail)));

            mRequestMagicLinkButton.setText(getString(R.string.send_verification_email));
        } else {
            Glide.with(this)
                 .load(GravatarUtils.gravatarFromEmail(mEmail,
                         getContext().getResources().getDimensionPixelSize(R.dimen.avatar_sz_login)))
                 .apply(RequestOptions.circleCropTransform())
                 .apply(RequestOptions.placeholderOf(R.drawable.ic_gridicons_user_circle_100dp))
                 .apply(RequestOptions.errorOf(R.drawable.ic_gridicons_user_circle_100dp))
                 .listener(new RequestListener<Drawable>() {
                     @Override
                     public boolean onLoadFailed(@Nullable GlideException e, Object o, Target<Drawable> target,
                                                 boolean b) {
                         mAvatarProgressBar.setVisibility(View.GONE);
                         return false;
                     }

                     @Override
                     public boolean onResourceReady(Drawable drawable, Object o, Target<Drawable> target,
                                                    DataSource dataSource, boolean b) {
                         mAvatarProgressBar.setVisibility(View.GONE);
                         return false;
                     }
                 })
                 .into(avatarView);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            mAnalyticsListener.trackMagicLinkRequestFormViewed();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mInProgress = savedInstanceState.getBoolean(KEY_IN_PROGRESS);
            if (mInProgress) {
                showMagicLinkRequestProgressDialog();
            }

            boolean gravatarInProgress = savedInstanceState.getBoolean(
                    KEY_GRAVATAR_IN_PROGRESS, false);
            mAvatarProgressBar.setVisibility(gravatarInProgress ? View.VISIBLE : View.GONE);
        }
        // important for accessibility - talkback
        getActivity().setTitle(R.string.magic_link_login_title);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_IN_PROGRESS, mInProgress);
        if (mAvatarProgressBar != null) {
            outState.putBoolean(KEY_GRAVATAR_IN_PROGRESS,
                    mAvatarProgressBar.getVisibility() == View.VISIBLE);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_login, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.help) {
            if (mLoginListener != null) {
                mLoginListener.helpMagicLinkRequest(mEmail);
            }
            return true;
        }

        return false;
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

    private AuthEmailPayloadSource getAuthEmailPayloadSource() {
        if (mJetpackConnectSource != null) {
            if (mJetpackConnectSource.equalsIgnoreCase(AuthEmailPayloadSource.NOTIFICATIONS.toString())) {
                return AuthEmailPayloadSource.NOTIFICATIONS;
            } else if (mJetpackConnectSource.equalsIgnoreCase(AuthEmailPayloadSource.STATS.toString())) {
                return AuthEmailPayloadSource.STATS;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private void showMagicLinkRequestProgressDialog() {
        startProgress(getString(R.string.login_magic_link_email_requesting));
    }

    protected void startProgress(String message) {
        mRequestMagicLinkButton.setEnabled(false);
        mProgressDialog = ProgressDialog.show(getActivity(), "", message, true, true,
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (mInProgress) {
                            endProgress();
                        }
                    }
                });
        mInProgress = true;
    }

    protected void endProgress() {
        mInProgress = false;

        if (mProgressDialog != null) {
            mProgressDialog.cancel();
            mProgressDialog = null;
        }

        // nullify the reference to denote there is no operation in progress
        mProgressDialog = null;

        mRequestMagicLinkButton.setEnabled(true);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthEmailSent(OnAuthEmailSent event) {
        if (!mInProgress) {
            // ignore the response if the magic link request is no longer pending
            return;
        }

        endProgress();

        if (event.isError()) {
            HashMap<String, String> errorProperties = new HashMap<>();
            errorProperties.put(ERROR_KEY, event.error.message);
            mAnalyticsListener.trackMagicLinkFailed(errorProperties);

            AppLog.e(AppLog.T.API, "OnAuthEmailSent has error: " + event.error.type + " - " + event.error.message);
            if (isAdded()) {
                ToastUtils.showToast(getActivity(), R.string.magic_link_unavailable_error_message,
                        ToastUtils.Duration.LONG);
            }
            return;
        }

        mAnalyticsListener.trackMagicLinkRequested();

        if (mLoginListener != null) {
            mLoginListener.showMagicLinkSentScreen(mEmail);
        }
    }
}
