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

import org.wordpress.android.R;

public class FollowRow extends LinearLayout {
    public FollowRow(Context context){
        super(context);
    }
    public FollowRow(Context context, AttributeSet attributes){
        super(context, attributes);
    }
    public FollowRow(Context context, AttributeSet attributes, int defStyle){
        super(context, attributes, defStyle);
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
}