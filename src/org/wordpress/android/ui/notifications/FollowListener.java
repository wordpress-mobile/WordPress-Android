package org.wordpress.android.ui.notifications;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.wordpress.android.WordPress;

import org.json.JSONObject;

class FollowListener implements FollowRow.OnFollowListener {
    
    class FollowResponseHandler extends JsonHttpResponseHandler {
        private FollowRow mRow;
        private String mSiteId;
        private boolean mShouldFollow;
        FollowResponseHandler(FollowRow row, String siteId, boolean shouldFollow){
            super();
            mRow = row;
            mSiteId = siteId;
            mShouldFollow = shouldFollow;
        }
        @Override
        public void onStart(){
            if (mRow.isSiteId(mSiteId)) {
                mRow.getFollowButton().setEnabled(false);
            }
        }
        @Override
        public void onFinish(){
            if (mRow.isSiteId(mSiteId)) {
                mRow.getFollowButton().setEnabled(true);
            }
        }
        @Override
        public void onSuccess(int status, JSONObject response){
            if (mRow.isSiteId(mSiteId)) {
                mRow.setFollowing(mShouldFollow);
            }
        }
    }
    @Override
    public void onFollow(final FollowRow row, final String siteId){
        WordPress.restClient.followSite(siteId, new FollowResponseHandler(row, siteId, true));
    }
    @Override
    public void onUnfollow(final FollowRow row, final String siteId){
        WordPress.restClient.unfollowSite(siteId, new FollowResponseHandler(row, siteId, false));
    }
}