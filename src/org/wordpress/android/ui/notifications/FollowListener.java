package org.wordpress.android.ui.notifications;

import android.content.Context;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest.ErrorListener;
import com.wordpress.rest.RestRequest.Listener;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

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
            AppLog.d(T.NOTIFS, String.format("Failed to follow the blog: %s ", error));
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
        WordPress.getRestClientUtils().followSite(siteId, handler, handler);
    }
    @Override
    public void onUnfollow(final FollowRow row, final String siteId){
        FollowResponseHandler handler = new FollowResponseHandler(row, siteId, false);
        WordPress.getRestClientUtils().unfollowSite(siteId, handler, handler);
    }
}