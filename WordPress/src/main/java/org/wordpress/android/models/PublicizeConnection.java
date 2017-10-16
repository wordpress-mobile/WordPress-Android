package org.wordpress.android.models;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;

public class PublicizeConnection {

    public enum ConnectStatus {
        OK {
            public String toString() {
                return "ok";
            }
        },
        BROKEN{
            public String toString() {
                return "broken";
            }
        }
    }

    public int connectionId;
    public long siteId;
    public int keyringConnectionId;
    public int keyringConnectionUserId;

    // `user_id` is the ID of the user that the connection belongs to, it will be `0` when `shared`
    // is `true`. only connections belonging to the current user and shared connections should be
    // available to a user
    public int userId;
    public boolean isShared;

    private String mService;
    private String mLabel;
    private String mExternalId;
    private String mExternalName;
    private String mExternalDisplayName;
    private String mExternalProfilePictureUrl;
    private long[] mSites;

    // `status` can be `ok` or `broken` -- `broken` means the connection needs to be re-established via the `refresh_URL`
    private String mStatus;
    private String mRefreshUrl;

    public String getService() {
        return StringUtils.notNullStr(mService);
    }
    public void setService(String service) {
        this.mService = StringUtils.notNullStr(service);
    }

    public String getLabel() {
        return StringUtils.notNullStr(mLabel);
    }
    public void setLabel(String label) {
        this.mLabel = StringUtils.notNullStr(label);
    }

    public String getExternalName() {
        return StringUtils.notNullStr(mExternalName);
    }
    public void setExternalName(String name) {
        this.mExternalName = StringUtils.notNullStr(name);
    }

    public String getExternalDisplayName() {
        return StringUtils.notNullStr(mExternalDisplayName);
    }
    public void setExternalDisplayName(String name) {
        this.mExternalDisplayName = StringUtils.notNullStr(name);
    }
    public boolean hasExternalDisplayName() {
        return !TextUtils.isEmpty(mExternalDisplayName);
    }

    public String getExternalId() {
        return StringUtils.notNullStr(mExternalId);
    }
    public void setExternalId(String id) {
        this.mExternalId = StringUtils.notNullStr(id);
    }

    public String getRefreshUrl() {
        return StringUtils.notNullStr(mRefreshUrl);
    }
    public void setRefreshUrl(String url) {
        this.mRefreshUrl = StringUtils.notNullStr(url);
    }
    public boolean hasRefreshUrl() {
        return !TextUtils.isEmpty(mRefreshUrl);
    }

    public String getExternalProfilePictureUrl() {
        return StringUtils.notNullStr(mExternalProfilePictureUrl);
    }
    public void setExternalProfilePictureUrl(String url) {
        this.mExternalProfilePictureUrl = StringUtils.notNullStr(url);
    }
    public boolean hasExternalProfilePictureUrl() {
        return !TextUtils.isEmpty(mExternalProfilePictureUrl);
    }

    public String getStatus() {
        return StringUtils.notNullStr(mStatus);
    }
    public void setStatus(String status) {
        this.mStatus = StringUtils.notNullStr(status);
    }

    public ConnectStatus getStatusEnum() {
        if (getStatus().equalsIgnoreCase(ConnectStatus.BROKEN.toString())) {
            return ConnectStatus.BROKEN;
        } else {
            return ConnectStatus.OK;
        }
    }
    public void setStatusEnum(@NonNull ConnectStatus status) {
        this.mStatus = status.toString();
    }

    public boolean isSameAs(PublicizeConnection other) {
        return other != null
                && other.connectionId == this.connectionId
                && other.userId == this.userId
                && other.keyringConnectionId == this.keyringConnectionId
                && other.keyringConnectionUserId == this.keyringConnectionUserId
                && other.isShared == this.isShared
                && other.getStatus().equals(this.getStatus())
                && other.getExternalDisplayName().equals(this.getExternalDisplayName())
                && other.getExternalName().equals(this.getExternalName())
                && other.getLabel().equals(this.getLabel())
                && other.getExternalProfilePictureUrl().equals(this.getExternalProfilePictureUrl())
                && other.getRefreshUrl().equals(this.getRefreshUrl())
                && other.getService().equals(this.getService());
    }

    public void setSites(long[] sites) {
        mSites = sites;
    }

    public long[] getSites() {
        return mSites;
    }

    public boolean isInSite(long siteId) {
        for (int i = 0; i < mSites.length; i++) {
            if (siteId == mSites[i]) {
                return true;
            }
        }

        return false;
    }

    /*
     * passed JSON is a single connection from the response to sites/%d/publicize-connections
       {"ID":12783250,
        "site_ID":52451176,
        "user_ID":5399133,
        "keyring_connection_ID":12781808,
        "keyring_connection_user_ID":5399133,
        "shared":false,
        "service":"twitter",
        "label":"Twitter",
        "issued":"2015-11-02 17:46:42",
        "expires":"0000-00-00 00:00:00",
        "external_ID":"4098796763",
        "external_name":"AutomatticNickB",
        "external_display":"@AutomatticNickB",
        "external_profile_picture":"https:\/\/pbs.twimg.com\/profile_images\/661237406360727552\/RycwaFzg.png",
        "external_profile_URL":"http:\/\/twitter.com\/AutomatticNickB",
        "external_follower_count":null,
        "status":"ok",
        "refresh_URL":"https:\/\/public-api.wordpress.com\/connect\/?action=request&kr_nonce=10c147b6fb&nonce=44fce811bb&refresh=1&for=connect&service=twitter&kr_blog_nonce=e3686ea86a&magic=keyring&blog=52451176",
        "meta":{
            "links":{
                "self":"https:\/\/public-api.wordpress.com\/rest\/v1.1\/sites\/52451176\/publicize-connections\/12783250",
                "help":"https:\/\/public-api.wordpress.com\/rest\/v1.1\/sites\/52451176\/publicize-connections\/12783250\/help",
                "site":"https:\/\/public-api.wordpress.com\/rest\/v1.1\/sites\/52451176",
                "service":"https:\/\/public-api.wordpress.com\/rest\/v1.1\/meta\/external-services\/twitter",
                "keyring-connection":"https:\/\/public-api.wordpress.com\/rest\/v1.1\/me\/keyring-connections\/12781808"}
             }}]}
     */
    public static PublicizeConnection fromJson(JSONObject json) {
        PublicizeConnection connection = new PublicizeConnection();

        connection.connectionId = json.optInt("ID");
        connection.siteId = json.optLong("site_ID");
        connection.userId = json.optInt("user_ID");
        connection.keyringConnectionId = json.optInt("keyring_connection_ID");
        connection.keyringConnectionUserId = json.optInt("keyring_connection_user_ID");

        connection.isShared = JSONUtils.getBool(json, "shared");

        connection.mService = json.optString("service");
        connection.mLabel = json.optString("label");
        connection.mExternalId = json.optString("external_ID");
        connection.mExternalName = json.optString("external_name");
        connection.mExternalDisplayName = json.optString("external_display");
        connection.mExternalProfilePictureUrl = json.optString("external_profile_picture");
        connection.mStatus = json.optString("status");
        connection.mRefreshUrl = json.optString("refresh_URL");

        try {
            JSONArray jsonSitesArray = json.getJSONArray("sites");
            connection.mSites = getSitesArrayFromJson(jsonSitesArray);
        } catch (JSONException e) {
            connection.mSites = new long[0];
            e.printStackTrace();
        }

        return connection;
    }

    private static long[] getSitesArrayFromJson(JSONArray jsonArray) throws JSONException {
        long[] sitesArray = new long[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            sitesArray[i] = jsonArray.getLong(i);
        }

        return sitesArray;
    }
}
