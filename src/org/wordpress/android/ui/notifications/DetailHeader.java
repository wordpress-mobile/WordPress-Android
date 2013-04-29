/**
 * Set a line of text and a URL to open in the browser when clicked
 */
package org.wordpress.android.ui.notifications;

import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.content.Context;
import android.util.AttributeSet;

import org.wordpress.android.R;

public class DetailHeader extends LinearLayout {
    public DetailHeader(Context context){
        super(context);
    }
    public DetailHeader(Context context, AttributeSet attributes){
        super(context, attributes);
    }
    public DetailHeader(Context context, AttributeSet attributes, int defStyle){
        super(context, attributes, defStyle);
    }
    public TextView getTextView(){
        return (TextView) findViewById(R.id.label);
    }
    public void setText(CharSequence text){
        getTextView().setText(text);
    }
    public void setClickable(boolean clickable){
        super.setClickable(clickable);
        View indicator = findViewById(R.id.indicator);
        if (clickable == false) {
            indicator.setVisibility(GONE);
        } else {
            indicator.setVisibility(VISIBLE);
        }
    }
}