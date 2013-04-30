/**
 * A row with and avatar, name and follow button
 * 
 * The follow button switches between "Follow" and "Unfollow" depending on the follow status
 * and provides and interface to know when the user has tried to follow or unfollow by tapping
 * the button.
 * 
 * Potentially can integrate with Gravatar using the avatar url to find profile JSON.
 */
package org.wordpress.android.ui.notifications;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONException;

import org.wordpress.android.R;

public class FollowRow extends LinearLayout {
    
    public static interface FollowListener {
        public void onUnfollow( FollowRow row, String blogId);
        public void onFollow( FollowRow row, String blogId);
    }
    
    private static final String TAG="FollowRow";
    private static String PARAMS_FIELD="params";
    private static String TYPE_FIELD="type";
    private static String ACTION_TYPE="follow";
    private static String BLOG_ID_PARAM="blog_id";
    private static String IS_FOLLOWING_PARAM="is_following";
    private static String BLOG_URL_PARAM="blog_url";
    private static String BLOG_DOMAIN_PARAM="blog_domain";
    
    private boolean mFollowing = false;
    private String mBlogId = null;
    private FollowListener mListener = null;
    private JSONObject mParams = null;
    private CharSequence mDefaultText = "";
    
    public FollowRow(Context context){
        super(context);
    }
    public FollowRow(Context context, AttributeSet attributes){
        super(context, attributes);
    }
    public FollowRow(Context context, AttributeSet attributes, int defStyle){
        super(context, attributes, defStyle);
    }
    public void setAction(JSONObject actionJSON){
        Button followButton = getFollowButton();
        try {
            if (actionJSON.getString(TYPE_FIELD).equals(ACTION_TYPE)) {
                // get the parms for following
                mParams = actionJSON.getJSONObject(PARAMS_FIELD);
                // show the button
                followButton.setVisibility(VISIBLE);
                followButton.setOnClickListener(new ClickListener());
                getSiteTextView().setText(getSiteDomain());
                setClickable(true);
            } else {
                mParams = null;
                followButton.setVisibility(GONE);
                followButton.setOnClickListener(null);
                getSiteTextView().setText("");
                setClickable(false);
            }
            updateLabel();
        }catch (JSONException e) {
            Log.e(TAG, String.format("Could not set action from %s", actionJSON), e);
            getFollowButton().setVisibility(GONE);
            getSiteTextView().setText("");
            setClickable(false);
            mParams = null;
        }
    }
    public JSONObject getParams(){
        return mParams;
    }
    public boolean hasParams(){
        return mParams != null;
    }
    public ImageView getImageView(){
        return (ImageView) findViewById(R.id.avatar);
    }
    public Button getFollowButton(){
        return (Button) findViewById(R.id.follow_button);
    }
    public TextView getTextView(){
        return (TextView) findViewById(R.id.name);
    }
    public TextView getSiteTextView(){
        return (TextView) findViewById(R.id.url);
    }
    public void setDefaultText(CharSequence text){
        setText(text);
        mDefaultText = getTextView().getText();
    }
    public void setText(CharSequence text){
        getTextView().setText(text);
    }
    public void setText(int resourceId){
        getTextView().setText(resourceId);
    }
    public boolean isSiteId(String siteId){
        return getSiteId().equals(siteId);
    }
    public void setFollowing(boolean following){
        if (hasParams()) {
            try {
                mParams.putOpt(IS_FOLLOWING_PARAM, following);                
            } catch (JSONException e) {
                Log.e(TAG, String.format("Could not set following %b", following), e);
            }
        };
        updateLabel();
    }
    public boolean isFollowing(){
        if (hasParams()) {
            return mParams.optBoolean(IS_FOLLOWING_PARAM, false);
        } else {
            return false;
        }
    }
    public String getSiteId(){
        if (hasParams()) {
            return mParams.optString(BLOG_ID_PARAM, null);
        } else {
            return null;
        }
    }
    public String getSiteUrl(){
        if (hasParams()) {
            return mParams.optString(BLOG_URL_PARAM, null);
        } else {
            return null;
        }
    }
    public String getSiteDomain(){
        if (hasParams()) {
            return mParams.optString(BLOG_DOMAIN_PARAM, null);
        } else {
            return null;
        }
    }
    public FollowListener getListener(){
        return mListener;
    }
    public void setListener(FollowListener listener){
        mListener = listener;
    }
    public boolean hasListener(){
        return mListener != null;
    }
    protected void updateLabel(){
        Button followButton = getFollowButton();
        followButton.setSelected(isFollowing());
        if(isFollowing()){
            followButton.setText(R.string.unfollow);
        } else {
            followButton.setText(R.string.follow);
        }
    }
    class ClickListener implements View.OnClickListener {
        public void onClick(View v){
            if (!hasListener()) {
                return;
            }
            FollowListener listener = getListener();
            if (isFollowing()) {
                listener.onUnfollow(FollowRow.this, getSiteId());
            } else {
                listener.onFollow(FollowRow.this, getSiteId());
            }
        }
    }
}