/**
 * Provides a list view and list adapter to display a note. It will have a header view to show
 * the avatar and other details for the post.
 * 
 * More specialized note adapters will need to be made to provide the correct views for the type
 * of note/note template it has.
 */
package org.wordpress.android.ui.notifications;

import android.os.Bundle;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ListAdapter;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
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