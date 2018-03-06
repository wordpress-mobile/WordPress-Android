package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * ViewHolder for a RecyclerView header used on screens shown after a user logs in.
 */
public class LoginHeaderViewHolder extends RecyclerView.ViewHolder {
    private final View mLoggedInAsHeading;
    private final View mUserDetailsCard;
    private final WPNetworkImageView mAvatarImageView;
    private final TextView mDisplayNameTextView;
    private final TextView mUsernameTextView;
    private final TextView mMySitesHeadingTextView;

    public LoginHeaderViewHolder(View view) {
        super(view);
        mLoggedInAsHeading = view.findViewById(R.id.logged_in_as_heading);
        mUserDetailsCard = view.findViewById(R.id.user_details_card);
        mAvatarImageView = (WPNetworkImageView) view.findViewById(R.id.avatar);
        mDisplayNameTextView = (TextView) view.findViewById(R.id.display_name);
        mUsernameTextView = (TextView) view.findViewById(R.id.username);
        mMySitesHeadingTextView = (TextView) view.findViewById(R.id.my_sites_heading);
    }

    public void updateLoggedInAsHeading(Context context, boolean isAfterLogin, AccountModel defaultAccount) {
        updateLoggedInAsHeading(context, isAfterLogin, constructGravatarUrl(context, defaultAccount),
                                defaultAccount.getUserName(), defaultAccount.getDisplayName());
    }

    public void updateLoggedInAsHeading(Context context, boolean isAfterLogin, String avatarUrl, String username,
                                        String displayName) {
        if (isAfterLogin) {
            mLoggedInAsHeading.setVisibility(View.VISIBLE);
            mUserDetailsCard.setVisibility(View.VISIBLE);

            mAvatarImageView.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR, null);

            mUsernameTextView.setText(context.getString(R.string.login_username_at, username));

            if (!TextUtils.isEmpty(displayName)) {
                mDisplayNameTextView.setText(displayName);
            } else {
                mDisplayNameTextView.setText(username);
            }
        } else {
            mLoggedInAsHeading.setVisibility(View.GONE);
            mUserDetailsCard.setVisibility(View.GONE);
        }
    }

    public void showSitesHeading(String text) {
        mMySitesHeadingTextView.setVisibility(View.VISIBLE);
        mMySitesHeadingTextView.setText(text);
    }

    public void hideSitesHeading() {
        mMySitesHeadingTextView.setVisibility(View.GONE);
    }

    private String constructGravatarUrl(Context context, AccountModel account) {
        int avatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
        return GravatarUtils.fixGravatarUrl(account.getAvatarUrl(), avatarSz);
    }
}
