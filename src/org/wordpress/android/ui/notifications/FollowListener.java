package org.wordpress.android.ui.notifications;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.VolleyError;

import com.wordpress.rest.RestRequest.Listener;
import com.wordpress.rest.RestRequest.ErrorListener;

import org.json.JSONArray;
import org.json.JSONObject;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

class FollowListener implements FollowRow.OnFollowListener {
    
    protected Context currentContent = null;
    
    public FollowListener(Context currentContent) {
        super();
        this.currentContent = currentContent;
    }
    
    class FollowResponseHandler implements Listener, ErrorListener {
        private FollowRow mRow;
        private String mSiteId;
        private boolean mShouldFollow;
        FollowResponseHandler(FollowRow row, String siteId, boolean shouldFollow){
            mRow = row;
            mSiteId = siteId;
            mShouldFollow = shouldFollow;
            disableButton();
        }
        @Override
        public void onResponse(JSONObject response){
            if (mRow.isSiteId(mSiteId)) {
                mRow.setFollowing(mShouldFollow);
            }
            enableButton();
        }
        @Override
        public void onErrorResponse(VolleyError error){
            enableButton();
            Log.d("WPNotifications", String.format("Failed to follow the blog: %s ", error));
        }
        public void disableButton(){
            if (mRow.isSiteId(mSiteId)) {
                mRow.getFollowButton().setEnabled(false);
            }
        }
        public void enableButton(){
            if (mRow.isSiteId(mSiteId)) {
                mRow.getFollowButton().setEnabled(true);
            }
        }
    }
    @Override
    public void onFollow(final FollowRow row, final String siteId){
        FollowResponseHandler handler = new FollowResponseHandler(row, siteId, true);
        WordPress.restClient.followSite(siteId, handler, handler);
    }
    @Override
    public void onUnfollow(final FollowRow row, final String siteId){
        FollowResponseHandler handler = new FollowResponseHandler(row, siteId, false);
        WordPress.restClient.unfollowSite(siteId, handler, handler);
    }
}