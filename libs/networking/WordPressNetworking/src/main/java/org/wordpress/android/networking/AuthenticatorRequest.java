package org.wordpress.android.networking;

import com.android.volley.VolleyError;
import com.wordpress.rest.Oauth;
import com.wordpress.rest.RestClient;
import com.wordpress.rest.RestRequest;
import com.wordpress.rest.RestRequest.ErrorListener;

/**
 * Encapsulates the behaviour for asking the Authenticator for an access token. This
 * allows the request maker to disregard the authentication state when making requests.
 */
public class AuthenticatorRequest {
    private RestRequest mRequest;
    private RestRequest.ErrorListener mListener;
    private RestClient mRestClient;
    private Authenticator mAuthenticator;

    protected AuthenticatorRequest(RestRequest request, ErrorListener listener, RestClient restClient,
                                   Authenticator authenticator) {
        mRequest = request;
        mListener = listener;
        mRestClient = restClient;
        mAuthenticator = authenticator;
    }

    public String getSiteId() {
        return extractSiteIdFromUrl(mRestClient.getEndpointURL(), mRequest.getUrl());
    }

    /**
     * Parse out the site ID from an URL.
     * Note: For batch REST API calls, only the first siteID is returned
     *
     * @return The site ID
     */
    public static String extractSiteIdFromUrl(String restEndpointUrl, String url) {
        if (url == null) {
            return null;
        }

        final String sitePrefix = restEndpointUrl.endsWith("/") ? restEndpointUrl + "sites/" : restEndpointUrl + "/sites/";
        final String batchCallPrefix = restEndpointUrl.endsWith("/") ? restEndpointUrl + "batch/?urls%5B%5D=%2Fsites%2F"
                : restEndpointUrl + "/batch/?urls%5B%5D=%2Fsites%2F";

        if (url.startsWith(sitePrefix) && !sitePrefix.equals(url)) {
            int marker = sitePrefix.length();
            if (url.indexOf("/", marker) < marker) {
                return null;
            }
            return url.substring(marker, url.indexOf("/", marker));
        } else if (url.startsWith(batchCallPrefix) && !batchCallPrefix.equals(url)) {
            int marker = batchCallPrefix.length();
            if (url.indexOf("%2F", marker) < marker) {
                return null;
            }
            return url.substring(marker, url.indexOf("%2F", marker));
        }

        // not a sites/$siteId request or a batch request
        return null;
    }

    /**
     * Attempt to send the request, checks to see if we have an access token and if not
     * asks the Authenticator to authenticate the request.
     *
     * If no Authenticator is provided the request is always sent.
     */
    protected void send(){
        if (mAuthenticator == null) {
            mRestClient.send(mRequest);
        } else {
            mAuthenticator.authenticate(this);
        }
    }

    public void sendWithAccessToken(String token){
        mRequest.setAccessToken(token);
        mRestClient.send(mRequest);
    }

    public void sendWithAccessToken(Oauth.Token token){
        sendWithAccessToken(token.toString());
    }

    /**
     * If an access token cannot be obtained the request can be aborted and the
     * handler's onFailure method is called
     */
    public void abort(VolleyError error){
        if (mListener != null) {
            mListener.onErrorResponse(error);
        }
    }
}
