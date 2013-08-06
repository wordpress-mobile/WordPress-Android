package org.wordpress.android.ui.notifications;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.JSONUtil;

public class BigBadgeFragment extends Fragment implements NotificationFragment {
    private Note mNote;
    private NetworkImageView mBadgeImageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state){
        View view = inflater.inflate(R.layout.notifications_big_badge, parent, false);
        mBadgeImageView = (NetworkImageView) view.findViewById(R.id.badge);
        
        TextView bodyTextView = (TextView) view.findViewById(R.id.body);
        bodyTextView.setMovementMethod(LinkMovementMethod.getInstance());

        if (getNote() != null) {
            String noteHTML = JSONUtil.queryJSON(getNote().toJSONObject(), "body.html", "");
            if (noteHTML.equals(""))
                noteHTML = getNote().getSubject();
            bodyTextView.setText(Html.fromHtml(noteHTML));

            // Get the badge
            String iconURL = getNote().getIconURL();
            if (!iconURL.equals("")) {
                mBadgeImageView.setImageUrl(iconURL, WordPress.imageLoader);
            }
        }
        
        return view;
    }

    public void setNote(Note note){
        mNote = note;
    }
    public Note getNote(){
        return mNote;
    }
    
}