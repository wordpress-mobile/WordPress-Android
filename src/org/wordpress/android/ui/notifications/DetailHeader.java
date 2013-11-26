/**
 * Set a line of text and a URL to open in the browser when clicked
 */
package org.wordpress.android.ui.notifications;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    public void setUrl(final String url){
        if (url == null) {
            setClickable(false);
            setOnClickListener(null);
        } else {
            setClickable(true);
            setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view){
                    Context context = getContext();
                    Intent intent = new Intent(context, NotificationsWebViewActivity.class);
                    intent.putExtra(NotificationsWebViewActivity.URL_TO_LOAD, url);
                    context.startActivity(intent);
                }
            });
        }
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