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
    private static final String TAG="FollowRow";
    private static String PARAMS_FIELD="params";
    private static String TYPE_FIELD="type";
    private static String ACTION_TYPE="follow";
    private static String BLOG_ID_PARAM="blog_id";
    private static String IS_FOLLOWING_PARAM="is_following";
    
    private boolean mFollowing = false;
    private String mBlogId = null;
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
        try {
            if (actionJSON.getString(TYPE_FIELD).equals(ACTION_TYPE)) {
                // get the parms for following
                JSONObject params = actionJSON.getJSONObject(PARAMS_FIELD);
                setBlogId(params.getString(BLOG_ID_PARAM));
                setFollowing(params.getBoolean(IS_FOLLOWING_PARAM));
                // show the button
                getFollowButton().setVisibility(VISIBLE);
            } else {
                getFollowButton().setVisibility(GONE);
            }
        }catch (JSONException e) {
            Log.e(TAG, String.format("Could not set action from %s", actionJSON), e);
            getFollowButton().setVisibility(GONE);
        }
    }
    public ImageView getImageView(){
        return (ImageView) findViewById(R.id.note_icon);
    }
    public Button getFollowButton(){
        return (Button) findViewById(R.id.follow_button);
    }
    public TextView getTextView(){
        return (TextView) findViewById(R.id.note_name);
    }
    public void setText(CharSequence text){
        getTextView().setText(text);
    }
    public void setFollowing(boolean following){
        mFollowing = following;
    }
    public boolean isFollowing(){
        return mFollowing;
    }
    public void setBlogId(String blogId){
        mBlogId = blogId;
    }
    public String getBlogId(){
        return mBlogId;
    }
}