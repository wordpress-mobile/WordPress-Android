package org.wordpress.android.ui.notifications;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.WPLinkMovementMethod;

public class NoteMatcherFragment extends Fragment implements NotificationFragment {
    private Note mNote;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state){
        View view = inflater.inflate(R.layout.notifications_matcher, parent, false);

        // No note? No service.
        if (getNote() == null)
            return view;

        JSONObject noteBody = getNote().queryJSON("body", new JSONObject());
        JSONArray noteBodyItems = getNote().queryJSON("body.items", new JSONArray());
        JSONObject noteBodyItemAtPositionZero = JSONUtil.queryJSON(noteBodyItems, String.format("[%d]", 0), new JSONObject());

        DetailHeader noteHeader = (DetailHeader) view.findViewById(R.id.header);
        JSONObject subject = getNote().queryJSON("subject", new JSONObject());
        String headerText = JSONUtil.getStringDecoded(subject, "text");
        noteHeader.setText(headerText);
        noteHeader.setClickable(false);

        String gravURL = JSONUtil.queryJSON(noteBodyItemAtPositionZero, "icon", "");
        if (!gravURL.equals("")) {
            NetworkImageView mBadgeImageView = (NetworkImageView) view.findViewById(R.id.gravatar);
            mBadgeImageView.setImageUrl(gravURL, WordPress.imageLoader);
        }

        TextView bodyTextView = (TextView) view.findViewById(R.id.body);
        bodyTextView.setMovementMethod(WPLinkMovementMethod.getInstance());
        String noteHTML = JSONUtil.getString(noteBodyItemAtPositionZero, "html");
        bodyTextView.setText(Html.fromHtml(noteHTML));

        //setup the footer
        DetailHeader noteFooter = (DetailHeader) view.findViewById(R.id.footer);
        String footerText = JSONUtil.getStringDecoded(noteBody, "header_text");
        noteFooter.setText(footerText);
        JSONObject bodyObject =  getNote().queryJSON("body", new JSONObject());
        String itemURL = JSONUtil.getString(bodyObject, "header_link");
        noteFooter.setNote(getNote(), itemURL);

        if (getActivity() instanceof OnPostClickListener) {
            noteFooter.setOnPostClickListener(((OnPostClickListener)getActivity()));
        }
        if (getActivity() instanceof OnCommentClickListener) {
            noteFooter.setOnCommentClickListener(((OnCommentClickListener)getActivity()));
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