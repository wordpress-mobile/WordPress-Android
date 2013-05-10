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

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.JSONUtil;

public class BigBadgeFragment extends Fragment implements NotificationFragment {
    private Note mNote;
    private ImageView mBadgeImageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state){
        View view = inflater.inflate(R.layout.notifications_big_badge, parent, false);
        mBadgeImageView = (ImageView) view.findViewById(R.id.badge);
        
        TextView bodyTextView = (TextView) view.findViewById(R.id.body);
        bodyTextView.setMovementMethod(LinkMovementMethod.getInstance());
        String noteHTML = JSONUtil.queryJSON(getNote().toJSONObject(), "body.html", "");
        if (noteHTML.equals(""))
            noteHTML = getNote().getSubject();
        bodyTextView.setText(Html.fromHtml(noteHTML));
        
        // Get the badge
        String iconURL = getNote().getIconURL();
        if (!iconURL.equals("")) {
            AsyncHttpClient client = new AsyncHttpClient();
            String[] allowedContentTypes = new String[] { "image/png" };
            client.get(iconURL, new BinaryHttpResponseHandler(allowedContentTypes) {
                @Override
                public void onSuccess(byte[] fileData) {
                    Bitmap badge = BitmapFactory.decodeByteArray(fileData, 0, fileData.length);
                    if (badge != null) {
                        mBadgeImageView.setImageBitmap(badge);
                        Animation pop = AnimationUtils.loadAnimation(getActivity().getBaseContext(), R.anim.pop);
                        mBadgeImageView.startAnimation(pop);
                    }
                }
            });
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