/**
 * One fragment to rule them all (Notes, that is)
 */
package org.wordpress.android.ui.notifications;

import android.app.ListFragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.notifications.blocks.FormattedCommentNoteBlock;
import org.wordpress.android.ui.notifications.blocks.NoteBlock;
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan;
import org.wordpress.android.ui.notifications.blocks.NoteBlockIdType;
import org.wordpress.android.ui.notifications.blocks.UserNoteBlock;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;

public class NotificationsDetailListFragment extends ListFragment implements NotificationFragment {
    private Note mNote;
    private List<NoteBlock> mNoteBlockArray = new ArrayList<NoteBlock>();
    private ViewGroup mFooterView;

    public NotificationsDetailListFragment() {
    }

    public static NotificationsDetailListFragment newInstance(final Note note) {
        NotificationsDetailListFragment fragment = new NotificationsDetailListFragment();
        fragment.setNote(note);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.notifications_fragment_detail_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        ListView listView = getListView();
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setHeaderDividersEnabled(false);

        if (mFooterView != null) {
           listView.addFooterView(mFooterView);
        }

        if (mNote == null) {
            return;
        }

        reloadNoteBlocks();
    }

    @Override
    public Note getNote() {
        return mNote;
    }

    @Override
    public void setNote(Note note) {
        mNote = note;
    }

    public void reloadNoteBlocks() {
        new LoadNoteBlocksTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void setFooterView(ViewGroup footerView) {
        mFooterView = footerView;
    }

    private class NoteBlockAdapter extends ArrayAdapter<NoteBlock> {

        private List<NoteBlock> mNoteBlockList;
        private LayoutInflater mLayoutInflater;

        NoteBlockAdapter(Context context, List<NoteBlock> noteBlocks) {
            super(context, 0, noteBlocks);

            mNoteBlockList = noteBlocks;
            mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            NoteBlock noteBlock = mNoteBlockList.get(position);

            // Check the tag for this recycled view, if it matches we can reuse it
            if (convertView == null || noteBlock.getBlockType() != convertView.getTag(R.id.note_block_tag_id)) {
                convertView = mLayoutInflater.inflate(noteBlock.getLayoutResourceId(), parent, false);
                convertView.setTag(noteBlock.getViewHolder(convertView));
            }

            // Update the block type for this view
            convertView.setTag(R.id.note_block_tag_id, noteBlock.getBlockType());

            return noteBlock.configureView(convertView);
        }
    }

    private boolean hasActivity() {
        return getActivity() != null;
    }

    private NoteBlock.OnNoteBlockTextClickListener mOnNoteBlockTextClickListener = new NoteBlock.OnNoteBlockTextClickListener() {
        @Override
        public void onNoteBlockTextClicked(NoteBlockClickableSpan clickedSpan) {
            if (!hasActivity()) return;

            NotificationUtils.handleNoteBlockSpanClick((NewNotificationsActivity)getActivity(), clickedSpan);
        }
    };

    private UserNoteBlock.OnSiteFollowListener mOnSiteFollowListener = new UserNoteBlock.OnSiteFollowListener() {
        @Override
        public void onSiteFollow(boolean success) {
            if (hasActivity() && !success) {
                ToastUtils.showToast(getActivity(), R.string.reader_toast_err_follow_blog);
            }
        }
    };

    private UserNoteBlock.OnGravatarClickedListener mOnGravatarClickedListener = new UserNoteBlock.OnGravatarClickedListener() {
        @Override
        public void onGravatarClicked(long siteId, long userId) {
            if (!hasActivity()) return;

            NewNotificationsActivity notificationsActivity = (NewNotificationsActivity)getActivity();
            notificationsActivity.showBlogPreviewForSiteId(siteId, null);
        }
    };


    // Loop through the body items in this note, and create blocks for each.
    private class LoadNoteBlocksTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            JSONArray bodyArray = mNote.getBody();
            mNoteBlockArray.clear();
            if (bodyArray != null && bodyArray.length() > 0) {
                for (int i=0; i < bodyArray.length(); i++) {
                    try {
                        JSONObject noteObject = bodyArray.getJSONObject(i);
                        // Determine NoteBlock type and add it to the array
                        NoteBlock noteBlock;
                        String noteBlockTypeString = JSONUtil.queryJSON(noteObject, "type", "");

                        if (NoteBlockIdType.fromString(noteBlockTypeString) == NoteBlockIdType.USER) {
                            noteBlock = new UserNoteBlock(
                                    noteObject,
                                    mOnNoteBlockTextClickListener,
                                    mOnSiteFollowListener,
                                    mOnGravatarClickedListener
                            );
                        } else {
                            noteBlock = new NoteBlock(noteObject, mOnNoteBlockTextClickListener);
                        }

                        mNoteBlockArray.add(noteBlock);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            setListAdapter(new NoteBlockAdapter(getActivity(), mNoteBlockArray));
        }
    }
}
