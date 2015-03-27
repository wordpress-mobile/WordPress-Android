package org.wordpress.android.networking;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.StringUtils;

public class OAuthAuthenticator implements Authenticator {
    @Override
    public void authenticate(final AuthenticatorRequest request) {
        String siteId = request.getSiteId();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
        String token = settings.getString(WordPress.ACCESS_TOKEN_PREFERENCE, null);

        if (siteId != null) {
            // Get the token for a Jetpack site if needed
            Blog blog = WordPress.wpDB.getBlogForDotComBlogId(siteId);

            if (blog != null) {
                String jetpackToken = blog.getApi_key();

                // valid OAuth tokens are 64 chars
                if (jetpackToken != null && jetpackToken.length() == 64 && !blog.isDotcomFlag()) {
                    token = jetpackToken;
                }
            }
        }

        request.sendWithAccessToken(StringUtils.notNullStr(token));
    }
}
