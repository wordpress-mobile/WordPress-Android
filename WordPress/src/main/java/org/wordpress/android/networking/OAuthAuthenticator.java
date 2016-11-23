package org.wordpress.android.networking;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.util.StringUtils;

public class OAuthAuthenticator implements Authenticator {
    public static String getAccessToken(final String siteId) {
        String token = AccountHelper.getDefaultAccount().getAccessToken();

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

        return token;
    }

    @Override
    public void authenticate(final AuthenticatorRequest request) {
        String siteId = request.getSiteId();
        String token = getAccessToken(siteId);
        request.sendWithAccessToken(StringUtils.notNullStr(token));
    }
}
