package org.wordpress.android.ui.notifications;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;

public class BigBadgeFragment extends Fragment implements NotificationFragment {
    private Note mNote;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state){
        View view = inflater.inflate(R.layout.notifications_big_badge, parent, false);
        DetailHeader header = (DetailHeader) view.findViewById(R.id.header);
        header.setText(getNote().getSubject());
        return view;
    }

    public void setNote(Note note){
        mNote = note;
    }
    public Note getNote(){
        return mNote;
    }
    
}