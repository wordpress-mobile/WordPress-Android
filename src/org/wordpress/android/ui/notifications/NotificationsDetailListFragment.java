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
import org.wordpress.android.ui.notifications.blocks.NoteBlock;
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan;
import org.wordpress.android.ui.notifications.blocks.NoteBlockIdType;
import org.wordpress.android.ui.notifications.blocks.UserNoteBlock;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.NoticonTextView;
import org.wordpress.android.widgets.WPTextView;

import java.util.ArrayList;
import java.util.List;

public class NotificationsDetailListFragment extends ListFragment implements NotificationFragment {
    private Note mNote;
    private List<NoteBlock> mNoteBlockArray = new ArrayList<NoteBlock>();
    private View mHeaderView;

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

        ListView list = getListView();
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setHeaderDividersEnabled(false);

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
        // Add header if we have a subject
        if (hasActivity() && mNote.getSubject() != null) {
            if (mHeaderView == null) {
                mHeaderView = getActivity().getLayoutInflater().inflate(R.layout.notifications_detail_header, null);
                getListView().addHeaderView(mHeaderView);
            }

            NoticonTextView noticonTextView = (NoticonTextView) mHeaderView.findViewById(R.id.notification_header_icon);
            noticonTextView.setText(mNote.getNoticonCharacter());

            WPTextView subjectTextView = (WPTextView) mHeaderView.findViewById(R.id.notification_header_subject);
            subjectTextView.setText(mNote.getSubject());
        }

        new LoadNoteBlocksTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

            if (clickedSpan.shouldShowBlogPreview()) {
                // Show blog preview
                long siteId = (clickedSpan.getType() == NoteBlockIdType.USER) ? clickedSpan.getId() : clickedSpan.getSiteId();
                NewNotificationsActivity notificationsActivity = (NewNotificationsActivity)getActivity();
                notificationsActivity.showBlogPreviewForSiteId(siteId);
            } else if (clickedSpan.getType() == NoteBlockIdType.POST) {
                // Show post detail
                NewNotificationsActivity notificationsActivity = (NewNotificationsActivity)getActivity();
                long siteId = clickedSpan.getSiteId();
                long postId = clickedSpan.getId();
                notificationsActivity.showPostForSiteAndPostId(siteId, postId);
            }
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
                            noteBlock = new UserNoteBlock(noteObject, mOnNoteBlockTextClickListener, mOnSiteFollowListener);
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
