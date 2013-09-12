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
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;

import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONObject;
import org.json.JSONException;

import org.wordpress.android.R;
import org.wordpress.passcodelock.AppLockManager;

public class FollowRow extends LinearLayout {
    
    public static interface OnFollowListener {
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
    private OnFollowListener mListener = null;
    private JSONObject mParams = null;
    private CharSequence mDefaultText = "";
    private String mBlogURL = null;
    
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
            Log.e(TAG, String.format("Could not set action from %s", actionJSON), e);
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
    public boolean hasParams(){
        return mParams != null;
    }
    public NetworkImageView getImageView(){
        return (NetworkImageView) findViewById(R.id.avatar);
    }
    public ImageButton getFollowButton(){
        return (ImageButton) findViewById(R.id.follow_button);
    }
    public TextView getTextView(){
        return (TextView) findViewById(R.id.name);
    }
    public TextView getSiteTextView(){
        return (TextView) findViewById(R.id.url);
    }
    private View getFollowDivider() {
        return (View) findViewById(R.id.follow_divider);
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
        updateButton();
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
    public void setSiteUrl(String url){
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
    public String getSiteDomain(){
        if (hasParams()) {
            return mParams.optString(BLOG_DOMAIN_PARAM, null);
        } else {
            return null;
        }
    }
    public OnFollowListener getListener(){
        return mListener;
    }
    public void setListener(OnFollowListener listener){
        mListener = listener;
    }
    public boolean hasListener(){
        return mListener != null;
    }
    protected void updateButton(){
        ImageButton followButton = getFollowButton();
        followButton.setSelected(isFollowing());
        if(isFollowing()){
            followButton.setImageResource(R.drawable.follow_minus);
        } else {
            followButton.setImageResource(R.drawable.follow_plus);
        }
    }
    class ClickListener implements View.OnClickListener {
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
    
    class LongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            Toast.makeText(getContext(), getResources().getString(R.string.tooltip_follow), Toast.LENGTH_SHORT).show();
            return true;
        } 
    }
}