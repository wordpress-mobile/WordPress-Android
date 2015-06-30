package org.wordpress.android.ui.prefs.notifications;

import android.content.Context;
import android.preference.Preference;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPNetworkImageView;

import javax.annotation.Nonnull;

public class NotificationsPreference extends Preference {

    private String mBlavatarUrl;

    public NotificationsPreference(Context context) {
        super(context);
    }

    @Override
    protected void onBindView(@Nonnull View view) {
        super.onBindView(view);

        if (mBlavatarUrl != null) {
            WPNetworkImageView blavatarImageView = (WPNetworkImageView) view.findViewById(R.id.site_blavatar);
            blavatarImageView.setImageUrl(mBlavatarUrl, WPNetworkImageView.ImageType.BLAVATAR);
        }
    }

    public void setBlavatarUrl(String url) {
        mBlavatarUrl = url;
    }
}
