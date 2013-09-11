/**
 * Set a line of text and a URL to open in the browser when clicked
 */
package org.wordpress.android.ui.notifications;

import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.provider.Browser;
import android.net.Uri;

import org.wordpress.android.R;
import org.wordpress.passcodelock.AppLockManager;

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
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    Context context = getContext();
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
                    context.startActivity(intent);
                    AppLockManager.getInstance().setExtendedTimeout();
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