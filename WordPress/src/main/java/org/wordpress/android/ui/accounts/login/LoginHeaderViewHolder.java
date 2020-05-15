package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

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
    private final ImageView mAvatarImageView;
    private final TextView mDisplayNameTextView;
    private final TextView mUsernameTextView;
    private final TextView mMySitesHeadingTextView;

    public LoginHeaderViewHolder(View view) {
        super(view);
        mAvatarImageView = view.findViewById(R.id.avatar);
        mDisplayNameTextView = view.findViewById(R.id.display_name);
        mUsernameTextView = view.findViewById(R.id.username);
        mMySitesHeadingTextView = view.findViewById(R.id.my_sites_heading);
    }

    public void updateLoggedInAsHeading(Context context, ImageManager imageManager, AccountModel defaultAccount) {
        updateLoggedInAsHeading(context, imageManager, constructGravatarUrl(context, defaultAccount),
                                defaultAccount.getUserName(), defaultAccount.getDisplayName());
    }

    public void updateLoggedInAsHeading(Context context, ImageManager imageManager, String avatarUrl, String username,
                                        String displayName) {
        imageManager.loadIntoCircle(mAvatarImageView, ImageType.AVATAR_WITHOUT_BACKGROUND,
                StringUtils.notNullStr(avatarUrl));
        mUsernameTextView.setText(username);

        if (!TextUtils.isEmpty(displayName)) {
            mDisplayNameTextView.setText(displayName);
        } else {
            mDisplayNameTextView.setText(username);
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
