package org.wordpress.android.ui.notifications;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;

import org.wordpress.android.R;

class NotificationsCommentFragment extends NotificationsDetailFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state){
        View view = inflater.inflate(R.layout.notifications_detail, parent, false);
        TextView noteText = (TextView) view.findViewById(R.id.note_text);
        noteText.setText("Comment");
        return view;
    }
}