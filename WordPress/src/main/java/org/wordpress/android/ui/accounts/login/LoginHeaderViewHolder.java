package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

/**
 * ViewHolder for a RecyclerView header used on screens shown after a user logs in.
 */
public class LoginHeaderViewHolder extends RecyclerView.ViewHolder {
    private final View mLoggedInAsHeading;
    private final View mUserDetailsCard;
    private final ImageView mAvatarImageView;
    private final TextView mDisplayNameTextView;
    private final TextView mUsernameTextView;
    private final TextView mMySitesHeadingTextView;

    public LoginHeaderViewHolder(View view) {
        super(view);
        mLoggedInAsHeading = view.findViewById(R.id.logged_in_as_heading);
        mUserDetailsCard = view.findViewById(R.id.user_details_card);
        mAvatarImageView = view.findViewById(R.id.avatar);
        mDisplayNameTextView = view.findViewById(R.id.display_name);
        mUsernameTextView = view.findViewById(R.id.username);
        mMySitesHeadingTextView = view.findViewById(R.id.my_sites_heading);
    }

    public void updateLoggedInAsHeading(Context context, ImageManager imageManager, boolean isAfterLogin,
                                        AccountModel defaultAccount) {
        updateLoggedInAsHeading(context, imageManager, isAfterLogin, constructGravatarUrl(context, defaultAccount),
                                defaultAccount.getUserName(), defaultAccount.getDisplayName());
    }

    public void updateLoggedInAsHeading(Context context, ImageManager imageManager, boolean isAfterLogin,
                                        String avatarUrl, String username, String displayName) {
        if (isAfterLogin) {
            mLoggedInAsHeading.setVisibility(View.VISIBLE);
            mUserDetailsCard.setVisibility(View.VISIBLE);
            imageManager.loadIntoCircle(mAvatarImageView, ImageType.AVATAR_WITHOUT_BACKGROUND,
                    StringUtils.notNullStr(avatarUrl));
            mUsernameTextView.setText(context.getString(R.string.login_username_at, username));

            if (!TextUtils.isEmpty(displayName)) {
                mDisplayNameTextView.setText(displayName);
            } else {
                mDisplayNameTextView.setText(username);
            }
        } else {
            imageManager.cancelRequestAndClearImageView(mAvatarImageView);
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
