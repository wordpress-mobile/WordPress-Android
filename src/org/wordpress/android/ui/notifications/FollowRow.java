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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.passcodelock.AppLockManager;

public class FollowRow extends LinearLayout {
    
    public static interface OnFollowListener {
        public void onUnfollow( FollowRow row, String blogId);
        public void onFollow( FollowRow row, String blogId);
    }
    
    private static final String PARAMS_FIELD="params";
    private static final String TYPE_FIELD="type";
    private static final String ACTION_TYPE="follow";
    private static final String BLOG_ID_PARAM="blog_id";
    private static final String IS_FOLLOWING_PARAM="is_following";
    private static final String BLOG_URL_PARAM="blog_url";
    private static final String BLOG_DOMAIN_PARAM="blog_domain";
    
    private OnFollowListener mListener = null;
    private JSONObject mParams = null;
    private String mBlogURL = null;
    
    public FollowRow(Context context){
        super(context);
    }
    public FollowRow(Context context, AttributeSet attributes){
        super(context, attributes);
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public FollowRow(Context context, AttributeSet attributes, int defStyle){
        super(context, attributes, defStyle);
    }
    
    public void setAction(JSONObject actionJSON){
        ImageButton followButton = getFollowButton();
        View followDivider = getFollowDivider();
        getImageView().setDefaultImageResId(R.drawable.placeholder);
        try {
            if (actionJSON.has(TYPE_FIELD) && actionJSON.getString(TYPE_FIELD).equals(ACTION_TYPE)) {
                // get the parms for following
                mParams = actionJSON.getJSONObject(PARAMS_FIELD);
                // show the button
                followButton.setVisibility(VISIBLE);
                followButton.setOnClickListener(new ClickListener());
                followButton.setOnLongClickListener(new LongClickListener());
                followDivider.setVisibility(VISIBLE);
                getSiteTextView().setText(getSiteDomain());
                setClickable(true);
            } else {
                mParams = null;
                followButton.setVisibility(GONE);
                followButton.setOnClickListener(null);
                followDivider.setVisibility(GONE);
                getSiteTextView().setText("");
                setClickable(false);
            }
            
            if (hasParams())
                setSiteUrl(mParams.optString(BLOG_URL_PARAM, null));
            
            updateButton();
        }catch (JSONException e) {
            AppLog.e(T.NOTIFS, String.format("Could not set action from %s", actionJSON), e);
            getFollowButton().setVisibility(GONE);
            getFollowDivider().setVisibility(GONE);
            getSiteTextView().setText("");
            setClickable(false);
            mParams = null;
        }
    }
    
    public JSONObject getParams(){
        return mParams;
    }
    boolean hasParams(){
        return mParams != null;
    }
    public NetworkImageView getImageView(){
        return (NetworkImageView) findViewById(R.id.avatar);
    }
    public ImageButton getFollowButton(){
        return (ImageButton) findViewById(R.id.follow_button);
    }
    TextView getTextView(){
        return (TextView) findViewById(R.id.name);
    }
    TextView getSiteTextView(){
        return (TextView) findViewById(R.id.url);
    }
    private View getFollowDivider() {
        return findViewById(R.id.follow_divider);
    }
    public void setText(CharSequence text){
        getTextView().setText(text);
    }
    public void setText(int resourceId){
        getTextView().setText(resourceId);
    }
    public boolean isSiteId(String siteId){
        String thisSiteId = getSiteId();
        return (thisSiteId != null && thisSiteId.equals(siteId));
    }
    public void setFollowing(boolean following){
        if (hasParams()) {
            try {
                mParams.putOpt(IS_FOLLOWING_PARAM, following);                
            } catch (JSONException e) {
                AppLog.e(T.NOTIFS, String.format("Could not set following %b", following), e);
            }
        };
        updateButton();
    }
    boolean isFollowing(){
        if (hasParams()) {
            return mParams.optBoolean(IS_FOLLOWING_PARAM, false);
        } else {
            return false;
        }
    }
    String getSiteId(){
        if (hasParams()) {
            return mParams.optString(BLOG_ID_PARAM, null);
        } else {
            return null;
        }
    }
    void setSiteUrl(String url){
        mBlogURL = url;
        if (url != null) {
            this.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (mBlogURL != null) {
                        try {
                            Uri uri = Uri.parse(mBlogURL);
                            getContext().startActivity(new Intent(Intent.ACTION_VIEW, uri));
                            AppLockManager.getInstance().setExtendedTimeout();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } else {
            this.setOnClickListener(null);
        }
    }
    public String getSiteUrl(){
        return mBlogURL;
    }
    String getSiteDomain(){
        if (hasParams()) {
            return mParams.optString(BLOG_DOMAIN_PARAM, null);
        } else {
            return null;
        }
    }
    OnFollowListener getListener(){
        return mListener;
    }
    public void setListener(OnFollowListener listener){
        mListener = listener;
    }
    boolean hasListener(){
        return mListener != null;
    }
    void updateButton(){
        ImageButton followButton = getFollowButton();
        followButton.setSelected(isFollowing());
        if(isFollowing()){
            followButton.setImageResource(R.drawable.follow_minus);
        } else {
            followButton.setImageResource(R.drawable.follow_plus);
        }
    }
    private class ClickListener implements View.OnClickListener {
        public void onClick(View v) {
            if (!hasListener()) {
                return;
            }
            ImageButton followButton = getFollowButton();
            OnFollowListener listener = getListener();
            if (isFollowing()) {
                followButton.setImageResource(R.drawable.follow_plus);
                listener.onUnfollow(FollowRow.this, getSiteId());
            } else {
                followButton.setImageResource(R.drawable.follow_minus);
                listener.onFollow(FollowRow.this, getSiteId());
            }
        }
    }
    
    private class LongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            Toast.makeText(getContext(), getResources().getString(R.string.tooltip_follow), Toast.LENGTH_SHORT).show();
            return true;
        } 
    }
}