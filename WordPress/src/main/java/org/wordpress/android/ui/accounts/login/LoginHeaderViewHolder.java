package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.image.ImageManager;

import static org.wordpress.android.util.image.ImageType.AVATAR_WITHOUT_BACKGROUND;

/**
 * ViewHolder for a RecyclerView header used on screens shown after a user logs in.
 */
class LoginHeaderViewHolder extends RecyclerView.ViewHolder {
    private final ImageView mAvatarImageView;
    private final TextView mDisplayNameTextView;
    private final TextView mUsernameTextView;
    private final TextView mMySitesHeadingTextView;

    LoginHeaderViewHolder(View view) {
        super(view);
        mAvatarImageView = view.findViewById(R.id.login_epilogue_header_avatar);
        mDisplayNameTextView = view.findViewById(R.id.login_epilogue_header_title);
        mUsernameTextView = view.findViewById(R.id.login_epilogue_header_subtitle);
        mMySitesHeadingTextView = view.findViewById(R.id.login_epilogue_header_sites_subheader);
    }

    void updateLoggedInAsHeading(Context context, ImageManager imageManager, AccountModel account) {
        final String avatarUrl = constructGravatarUrl(context, account);
        final String username = account.getUserName();
        final String displayName = account.getDisplayName();
        updateLoggedInAsHeading(imageManager, avatarUrl, username, displayName);
    }

    void updateLoggedInAsHeading(Context context, ImageManager imageManager, SiteModel site) {
        final String avatarUrl = constructGravatarUrl(context, site);
        final String username = site.getUsername();
        final String displayName = site.getDisplayName();
        updateLoggedInAsHeading(imageManager, avatarUrl, username, displayName);
    }

    void updateLoggedInAsHeading(ImageManager imageManager, String avatarUrl, String username, String displayName) {
        imageManager.loadIntoCircle(mAvatarImageView, AVATAR_WITHOUT_BACKGROUND, StringUtils.notNullStr(avatarUrl));

        mUsernameTextView.setText(username);

        if (!TextUtils.isEmpty(displayName)) {
            mDisplayNameTextView.setText(displayName);
        } else {
            mDisplayNameTextView.setText(username);
        }
    }

    void showSitesHeading(String text) {
        mMySitesHeadingTextView.setVisibility(View.VISIBLE);
        mMySitesHeadingTextView.setText(text);
    }

    void hideSitesHeading() {
        mMySitesHeadingTextView.setVisibility(View.GONE);
    }

    private int getAvatarSize(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
    }

    private String constructGravatarUrl(Context context, AccountModel account) {
        return GravatarUtils.fixGravatarUrl(account.getAvatarUrl(), getAvatarSize(context));
    }

    private String constructGravatarUrl(Context context, SiteModel site) {
        return GravatarUtils.gravatarFromEmail(site.getEmail(), getAvatarSize(context));
    }
}
