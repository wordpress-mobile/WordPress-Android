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

import org.wordpress.android.R;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        public boolean canRequestMore();
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
        return inflater.inflate(R.layout.empty_listview, container, false);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        mProgressFooterView = View.inflate(getActivity(), R.layout.list_footer_progress, null);
        mProgressFooterView.setVisibility(View.GONE);
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

    public NotesAdapter getNotesAdapter() {
        return mNotesAdapter;
    }

    public void setNoteProvider(NoteProvider provider) {
        mNoteProvider = provider;
    }

    public void setOnNoteClickListener(OnNoteClickListener listener) {
        mNoteClickListener = listener;
    }

    private void requestMoreNotifications() {
        if (mNoteProvider != null && mNoteProvider.canRequestMore()) {
            showProgressFooter();
            mNoteProvider.onRequestMoreNotifications(getListView(), getListAdapter());
        }
    }

    class NotesAdapter extends ArrayAdapter<Note> {
        final int mAvatarSz;

        NotesAdapter() {
            this(getActivity());
        }

        NotesAdapter(Context context) {
            this(context, new ArrayList<Note>());
        }

        NotesAdapter(Context context, List<Note> notes) {
            super(context, R.layout.note_list_item, R.id.note_label, notes);
            mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            final Note note = getItem(position);

            final TextView txtDetail = (TextView) view.findViewById(R.id.note_detail);
            final TextView unreadIndicator = (TextView) view.findViewById(R.id.unread_indicator);
            final TextView txtDate = (TextView) view.findViewById(R.id.text_date);
            final ProgressBar placeholderLoading = (ProgressBar) view.findViewById(R.id.placeholder_loading);
            final WPNetworkImageView imgAvatar = (WPNetworkImageView) view.findViewById(R.id.note_avatar);
            final ImageView imgNoteIcon = (ImageView) view.findViewById(R.id.note_icon);

            if (note.isCommentType()) {
                txtDetail.setText(note.getCommentPreview());
                txtDetail.setVisibility(View.VISIBLE);
            } else {
                txtDetail.setVisibility(View.GONE);
            }

            txtDate.setText(note.getTimeSpan());
            imgNoteIcon.setImageDrawable(getDrawableForType(note.getType()));

            String avatarUrl = PhotonUtils.fixAvatar(note.getIconURL(), mAvatarSz);
            imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);

            unreadIndicator.setVisibility(note.isUnread() ? View.VISIBLE : View.INVISIBLE);
            placeholderLoading.setVisibility(note.isPlaceholder() ? View.VISIBLE : View.GONE);

            return view;
        }

        public void addAll(List<Note> notes) {
            Collections.sort(notes, new Note.TimeStampComparator());
            if (notes.size() == 0) {
                // No more notes available
                mAllNotesLoaded = true;
                hideProgressFooter();
            } else {
                // disable notifyOnChange while adding notes, otherwise notifyDataSetChanged
                // will be triggered for each added note
                setNotifyOnChange(false);
                try {
                    for (Note note: notes)
                        add(note);
                } finally {
                    setNotifyOnChange(true);
                }
            }
        }
        /*
         * replaces an existing note with an updated one, returns the index of the note
         * or -1 if it doesn't exist
         */
        public int updateNote(final Note originalNote, final Note updatedNote) {
            if (originalNote==null || updatedNote==null)
                return -1;

            int position = getPosition(originalNote);
            if (position >= 0) {
                remove(originalNote);
                insert(updatedNote, position);
                notifyDataSetChanged();
            }

            return position;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            hideProgressFooter();
        }

        // HashMap of drawables for note types
        private final HashMap<String, Drawable> mNoteIcons = new HashMap<String, Drawable>();
        private Drawable getDrawableForType(String noteType) {
            if (noteType==null)
                return null;

            // use like icon for comment likes
            if (noteType.equals(Note.NOTE_COMMENT_LIKE_TYPE))
                noteType = Note.NOTE_LIKE_TYPE;

            Drawable icon = mNoteIcons.get(noteType);
            if (icon != null)
                return icon;

            int imageId = getResources().getIdentifier("note_icon_" + noteType, "drawable", getActivity().getPackageName());
            if (imageId==0) {
                AppLog.w(T.NOTIFS, "unknown note type - " + noteType);
                return null;
            }

            icon = getResources().getDrawable(imageId);
            if (icon==null)
                return null;

            mNoteIcons.put(noteType, icon);
            return icon;
        }
    }

    /*
     * show/hide the "Loading" footer
     */
    private void showProgressFooter() {
        if (mProgressFooterView != null)
            mProgressFooterView.setVisibility(View.VISIBLE);
    }
    private void hideProgressFooter() {
        if (mProgressFooterView != null)
            mProgressFooterView.setVisibility(View.GONE);
    }


    private class ListScrollListener implements AbsListView.OnScrollListener {
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (mAllNotesLoaded || visibleItemCount == 0)
                return;

            // if we're within 5 from the last item we should ask for more items
            if (firstVisibleItem + visibleItemCount >= totalItemCount - LOAD_MORE_WITHIN_X_ROWS) {
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