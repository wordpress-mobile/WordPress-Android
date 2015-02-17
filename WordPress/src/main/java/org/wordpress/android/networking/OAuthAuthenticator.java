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
        String token = null;
        Blog blog;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
        if (siteId == null) {
            // Use the global access token
            token = settings.getString(WordPress.ACCESS_TOKEN_PREFERENCE, null);
        } else {
            blog = WordPress.wpDB.getBlogForDotComBlogId(siteId);

            if (blog != null) {
                // get the access token from api key field. Jetpack blogs linked with a different wpcom
                // account have the token stored here.
                token = blog.getApi_key();

                // valid oauth tokens are 64 chars
                if (token != null && token.length() < 64 && !blog.isDotcomFlag()) {
                    token = null;
                }

                // if there is no access token, we need to check if it is a dotcom blog, or a jetpack
                // blog linked with the main wpcom account.
                if (token == null) {
                    if (blog.isDotcomFlag() && blog.getUsername().equals(settings.getString(
                            WordPress.WPCOM_USERNAME_PREFERENCE, ""))) {
                        token = settings.getString(WordPress.ACCESS_TOKEN_PREFERENCE, null);
                    } else if (blog.isJetpackPowered()) {
                        if (blog.getDotcom_username() == null || blog.getDotcom_username().equals(settings.getString(
                                WordPress.WPCOM_USERNAME_PREFERENCE, ""))) {
                            token = settings.getString(WordPress.ACCESS_TOKEN_PREFERENCE, null);
                        }
                    }
                }
            }
        }

        request.sendWithAccessToken(StringUtils.notNullStr(token));
    }
}
