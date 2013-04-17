package org.wordpress.android.ui.notifications;

import android.support.v4.app.Fragment;

import org.wordpress.android.models.Note;

public abstract class NotificationFragment extends Fragment {
    protected Note mNote;
    public Note getNote(){
        return mNote;
    }
    public void setNote(Note note){
        mNote = note;
    }
}