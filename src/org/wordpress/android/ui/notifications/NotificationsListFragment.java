package org.wordpress.android.ui.notifications;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.DisplayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class NotificationsListFragment extends ListFragment {
    private static final int LOAD_MORE_WITHIN_X_ROWS = 5;
    private NoteProvider mNoteProvider;
    private NotesAdapter mNotesAdapter;
    private OnNoteClickListener mNoteClickListener;
    private View mProgressFooterView;
    private boolean mAllNotesLoaded;

    /**
     * For responding to tapping of notes
     */
    public interface OnNoteClickListener {
        public void onClickNote(Note note);
    }

    /**
     * For providing more notes data when getting to the end of the list
     */
    public interface NoteProvider {
        public void onRequestMoreNotifications(ListView listView, ListAdapter adapter);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        // setup the initial notes adapter
        mNotesAdapter = new NotesAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.empty_listview, container, false);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        mProgressFooterView = View.inflate(getActivity(), R.layout.list_footer_progress, null);
        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnScrollListener(new ListScrollListener());
        listView.setDivider(getResources().getDrawable(R.drawable.list_divider));
        listView.setDividerHeight(1);
        listView.addFooterView(mProgressFooterView, null, false);
        setListAdapter(mNotesAdapter);

        // Set empty text if no notifications
        TextView textview = (TextView) listView.getEmptyView();
        if (textview != null) {
            textview.setText(getText(R.string.notifications_empty_list));
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Note note = mNotesAdapter.getItem(position);
        l.setItemChecked(position, true);
        if (note != null && !note.isPlaceholder() && mNoteClickListener != null) {
            mNoteClickListener.onClickNote(note);
        }
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        super.setListAdapter(adapter);
    }

    public void setNotesAdapter(NotesAdapter adapter) {
        mNotesAdapter = adapter;
        this.setListAdapter(adapter);
    }

    public NotesAdapter getNotesAdapter() {
        return mNotesAdapter;
    }

    public void setNoteProvider(NoteProvider provider) {
        mNoteProvider = provider;
    }

    public void setOnNoteClickListener(OnNoteClickListener listener) {
        mNoteClickListener = listener;
    }

    protected void requestMoreNotifications() {
        if (mNoteProvider != null) {
            mNoteProvider.onRequestMoreNotifications(getListView(), getListAdapter());
        }
    }

    class NotesAdapter extends ArrayAdapter<Note> {
        int mAvatarSz;

        NotesAdapter() {
            this(getActivity());
        }

        NotesAdapter(Context context) {
            this(context, new ArrayList<Note>());
        }

        NotesAdapter(Context context, List<Note> notes) {
            super(context, R.layout.note_list_item, R.id.note_label, notes);
            mAvatarSz = DisplayUtils.dpToPx(context, 48);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            final Note note = getItem(position);

            final TextView detailText = (TextView) view.findViewById(R.id.note_detail);
            final ProgressBar placeholderLoading = (ProgressBar) view.findViewById(R.id.placeholder_loading);
            final NetworkImageView avatarView = (NetworkImageView) view.findViewById(R.id.note_avatar);
            final ImageView iconView = (ImageView) view.findViewById(R.id.note_icon);
            final TextView unreadIndicator = (TextView) view.findViewById(R.id.unread_indicator);

            if (note.isCommentType()) {
                detailText.setText(note.getCommentPreview());
                detailText.setVisibility(View.VISIBLE);
            } else {
                detailText.setVisibility(View.GONE);
            }

            // gravatars default to having s=256 which is considerably larger than we need here, so
            // change the s= param to the actual size used here
            String avatarUrl = note.getIconURL();
            if (avatarUrl!=null && avatarUrl.contains("s=256"))
                avatarUrl = avatarUrl.replace("s=256", "s=" + mAvatarSz);
            avatarView.setImageUrl(avatarUrl, WordPress.imageLoader);
            avatarView.setDefaultImageResId(R.drawable.placeholder);

            iconView.setImageDrawable(getDrawableForType(note.getType()));

            unreadIndicator.setVisibility(note.isUnread() ? View.VISIBLE : View.GONE);
            placeholderLoading.setVisibility(note.isPlaceholder() ? View.VISIBLE : View.GONE);

            return view;
        }

        public void addAll(List<Note> notes) {
            Collections.sort(notes, new Note.TimeStampComparator());
            if (notes.size() == 0) {
                // No more notes available
                mAllNotesLoaded = true;
                if (mProgressFooterView != null)
                    mProgressFooterView.setVisibility(View.GONE);
            } else {
                // disable notifyOnChange while adding notes - otherwise every call to add() will
                // trigger a call to notifyDataSetChanged()
                setNotifyOnChange(false);
                try {
                    Iterator<Note> noteIterator = notes.iterator();
                    while (noteIterator.hasNext())
                        add(noteIterator.next());
                } finally {
                    notifyDataSetChanged(); // this will reset notifyOnChange to True
                }
            }
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            if (mProgressFooterView != null)
                mProgressFooterView.setVisibility(View.GONE);
        }

        // HashMap of drawables for note types
        private HashMap<String, Drawable> mNoteIcons = new HashMap<String, Drawable>();
        private Drawable getDrawableForType(String noteType) {
            if (noteType==null)
                return null;

            Drawable icon = mNoteIcons.get(noteType);
            if (icon != null)
                return icon;

            int imageId = getResources().getIdentifier("note_icon_" + noteType, "drawable", getActivity().getPackageName());
            if (imageId==0)
                return null;

            icon = getResources().getDrawable(imageId);
            if (icon==null)
                return null;

            mNoteIcons.put(noteType, icon);
            return icon;
        }
    }

    private class ListScrollListener implements AbsListView.OnScrollListener {
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (mAllNotesLoaded)
                return;

            // if we're within 5 from the last item we should ask for more items
            if (firstVisibleItem + visibleItemCount >= totalItemCount - LOAD_MORE_WITHIN_X_ROWS) {
                if (totalItemCount <= 1)
                    mProgressFooterView.setVisibility(View.GONE);
                else
                    mProgressFooterView.setVisibility(View.VISIBLE);

                requestMoreNotifications();
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

}