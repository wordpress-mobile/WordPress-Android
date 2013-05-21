package org.wordpress.android.ui.notifications;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;

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
        @Override
        public void onFailure(Throwable e, JSONObject response){
            Log.e("WPNotifications", String.format("Failed to follow the blog: %s", response), e);
            showError(null);
        }
        @Override
        public void onFailure(Throwable e, JSONArray response){
            Log.e("WPNotifications", String.format("Failed to follow the blog: %s", response), e);
            showError(null);
        }
        @Override
        public void onFailure(Throwable e, String response){
            Log.e("WPNotifications", String.format("Failed to follow the blog: %s", response), e);
            showError(null);
        }
        @Override
        public void onFailure(Throwable e){
            Log.e("WPNotifications", "Failed to follow the blog: ", e);
            showError(null);
        }
        private void showError(String errorMessage) {
            if(currentContent == null)
                return;
            if(errorMessage == null)
                errorMessage = currentContent.getString(R.string.error_following_blog);
           Toast.makeText(currentContent, errorMessage, Toast.LENGTH_LONG).show();
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