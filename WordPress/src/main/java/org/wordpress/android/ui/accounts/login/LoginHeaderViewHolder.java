package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gravatar.AvatarQueryOptions;
import com.gravatar.AvatarUrl;
import com.gravatar.DefaultAvatarOption;
import com.gravatar.types.Email;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPAvatarUtils;
import org.wordpress.android.util.image.ImageManager;

import static org.wordpress.android.util.image.ImageType.AVATAR_WITHOUT_BACKGROUND;

/**
 * ViewHolder for a RecyclerView header used on screens shown after a user logs in.
 */
class LoginHeaderViewHolder extends RecyclerView.ViewHolder {
    private final ImageView mAvatarImageView;
    private final TextView mDisplayNameTextView;
    private final TextView mUsernameTextView;

    private final boolean mIsOnboardingImprovementsEnabled;
    @Nullable private TextView mMySitesHeadingTextView;
    @Nullable private LinearLayout mMySitesHeadingContainer;

    LoginHeaderViewHolder(View view, boolean isOnboardingImprovementsEnabled) {
        super(view);
        mIsOnboardingImprovementsEnabled = isOnboardingImprovementsEnabled;
        mAvatarImageView = view.findViewById(R.id.login_epilogue_header_avatar);
        mDisplayNameTextView = view.findViewById(R.id.login_epilogue_header_title);
        mUsernameTextView = view.findViewById(R.id.login_epilogue_header_subtitle);
        if (isOnboardingImprovementsEnabled) {
            mMySitesHeadingContainer = view.findViewById(R.id.login_epilogue_header_sites_header);
        } else {
            mMySitesHeadingTextView = view.findViewById(R.id.login_epilogue_header_sites_subheader);
        }
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
        if (!mIsOnboardingImprovementsEnabled) {
            mMySitesHeadingTextView.setVisibility(View.VISIBLE);
            mMySitesHeadingTextView.setText(text);
        }
    }

    void showSitesHeading() {
        if (mIsOnboardingImprovementsEnabled) {
            mMySitesHeadingContainer.setVisibility(View.VISIBLE);
        }
    }

    void hideSitesHeading() {
        if (mIsOnboardingImprovementsEnabled) {
            mMySitesHeadingContainer.setVisibility(View.VISIBLE);
        } else {
            mMySitesHeadingTextView.setVisibility(View.GONE);
        }
    }

    private int getAvatarSize(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
    }

    private @NonNull String constructGravatarUrl(Context context, @Nullable AccountModel account) {
        if (account == null || account.getAvatarUrl() == null) {
            return "";
        }
        return WPAvatarUtils.rewriteAvatarUrl(account.getAvatarUrl(), getAvatarSize(context),
                DefaultAvatarOption.Status404.INSTANCE);
    }

    private @NonNull String constructGravatarUrl(Context context, @Nullable SiteModel site) {
        if (site == null || site.getEmail() == null) {
            return "";
        }
        return new AvatarUrl(
                new Email(site.getEmail()),
                new AvatarQueryOptions.Builder().setPreferredSize(getAvatarSize(context)).setDefaultAvatarOption(DefaultAvatarOption.Status404.INSTANCE).build()
        ).url(null).toString();
    }
}
