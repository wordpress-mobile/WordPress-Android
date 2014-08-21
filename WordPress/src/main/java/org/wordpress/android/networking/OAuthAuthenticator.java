package org.wordpress.android.networking;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.wordpress.rest.Oauth;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;

public class OAuthAuthenticator implements Authenticator {
    @Override
    public void authenticate(AuthenticatorRequest request) {
        String siteId = request.getSiteId();
        String token = null;
        Blog blog = null;

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
        if (token != null) {
            // we have an access token, set the request and send it
            request.sendWithAccessToken(token);
        } else {
            // we don't have an access token, let's request one
            requestAccessToken(request, blog);
        }
    }

    public void requestAccessToken(final AuthenticatorRequest request, final Blog blog) {
        Oauth oauth = new Oauth(BuildConfig.OAUTH_APP_ID, BuildConfig.OAUTH_APP_SECRET, BuildConfig.OAUTH_REDIRECT_URI);
        String username;
        String password;
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
        if (blog == null) {
            // We weren't give a specific blog, so we're going to user the username/password
            // from the "global" dotcom user account
            username = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
            password = WordPressDB.decryptPassword(settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, null));
        } else {
            // use the requested blog's username password, if it's a dotcom blog, use the
            // username and password for the blog. If it's a jetpack blog (not isDotcomFlag)
            // then use the getDotcom_* methods for username/password
            if (blog.isDotcomFlag()) {
                username = blog.getUsername();
                password = blog.getPassword();
            } else {
                username = blog.getDotcom_username();
                password = blog.getDotcom_password();
            }
        }

        Request oauthRequest = oauth.makeRequest(username, password,
                new Oauth.Listener() {
                    @Override
                    public void onResponse(Oauth.Token token) {
                        if (blog == null) {
                            settings.edit().putString(WordPress.ACCESS_TOKEN_PREFERENCE, token.toString()).commit();
                        } else {
                            blog.setApi_key(token.toString());
                            WordPress.wpDB.saveBlog(blog);
                        }

                        // Once we have a token, start up Simperium
                        SimperiumUtils.configureSimperium(WordPress.getContext(), token.toString());

                        request.sendWithAccessToken(token);
                    }
                },

                new Oauth.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        request.abort(error);
                    }
                }
        );
        // add oauth request to the request queue
        WordPress.requestQueue.add(oauthRequest);
    }
}
