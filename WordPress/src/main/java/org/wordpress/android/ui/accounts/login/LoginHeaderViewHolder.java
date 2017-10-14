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

    public final View mLoggedInAsHeading;
    public final View mUserDetailsCard;
    public final WPNetworkImageView mAvatarImageView;
    public final TextView mDisplayNameTextView;
    public final TextView mUsernameTextView;
    public final TextView mMySitesHeadingTextView;

    public LoginHeaderViewHolder(View view) {
        super(view);
        mLoggedInAsHeading = view.findViewById(R.id.logged_in_as_heading);
        mUserDetailsCard = view.findViewById(R.id.user_details_card);
        mAvatarImageView = (WPNetworkImageView) view.findViewById(R.id.avatar);
        mDisplayNameTextView = (TextView) view.findViewById(R.id.display_name);
        mUsernameTextView = (TextView) view.findViewById(R.id.username);
        mMySitesHeadingTextView = (TextView) view.findViewById(R.id.my_sites_heading);
    }

    public void update(Context context, LoginHeaderViewHolder holder, boolean isLoggedInWpcom, boolean isAfterLogin,
            AccountModel defaultAccount) {
        if (isLoggedInWpcom && isAfterLogin) {
            holder.mLoggedInAsHeading.setVisibility(View.VISIBLE);
            holder.mUserDetailsCard.setVisibility(View.VISIBLE);

            final String avatarUrl = constructGravatarUrl(context, defaultAccount);
            holder.mAvatarImageView.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR, null);

            holder.mUsernameTextView.setText(
                    context.getString(R.string.login_username_at, defaultAccount.getUserName()));

            String displayName = defaultAccount.getDisplayName();
            if (!TextUtils.isEmpty(displayName)) {
                holder.mDisplayNameTextView.setText(displayName);
            } else {
                holder.mDisplayNameTextView.setText(defaultAccount.getUserName());
            }
        } else {
            holder.mLoggedInAsHeading.setVisibility(View.GONE);
            holder.mUserDetailsCard.setVisibility(View.GONE);
        }
    }

    private String constructGravatarUrl(Context context, AccountModel account) {
        int avatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
        return GravatarUtils.fixGravatarUrl(account.getAvatarUrl(), avatarSz);
    }
}
