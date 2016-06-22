/**
 * One fragment to rule them all (Notes, that is)
 */
package org.wordpress.android.ui.notifications;

import android.app.ListFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderCommentTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.notifications.adapters.NoteBlockAdapter;
import org.wordpress.android.ui.notifications.blocks.BlockType;
import org.wordpress.android.ui.notifications.blocks.CommentUserNoteBlock;
import org.wordpress.android.ui.notifications.blocks.FooterNoteBlock;
import org.wordpress.android.ui.notifications.blocks.HeaderNoteBlock;
import org.wordpress.android.ui.notifications.blocks.NoteBlock;
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan;
import org.wordpress.android.ui.notifications.blocks.UserNoteBlock;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.services.ReaderCommentService;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class NotificationsDetailListFragment extends ListFragment implements NotificationFragment, Bucket.Listener<Note> {
    private static final String KEY_NOTE_ID = "noteId";
    private static final String KEY_LIST_POSITION = "listPosition";

    private int mRestoredListPosition;

    public interface OnNoteChangeListener {
        void onNoteChanged(Note note);
    }

    private Note mNote;
    private LinearLayout mRootLayout;
    private ViewGroup mFooterView;

    private String mRestoredNoteId;
    private int mBackgroundColor;
    private int mCommentListPosition = ListView.INVALID_POSITION;
    private boolean mIsUnread;

    private CommentUserNoteBlock.OnCommentStatusChangeListener mOnCommentStatusChangeListener;
    private OnNoteChangeListener mOnNoteChangeListener;
    private NoteBlockAdapter mNoteBlockAdapter;

    public NotificationsDetailListFragment() {
    }

    public static NotificationsDetailListFragment newInstance(final String noteId) {
        NotificationsDetailListFragment fragment = new NotificationsDetailListFragment();
        fragment.setNoteWithNoteId(noteId);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_NOTE_ID)) {
            // The note will be set in onResume() because Simperium will be running there
            // See WordPress.deferredInit()
            mRestoredNoteId = savedInstanceState.getString(KEY_NOTE_ID);
            mRestoredListPosition = savedInstanceState.getInt(KEY_LIST_POSITION, 0);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notifications_fragment_detail_list, container, false);
        mRootLayout = (LinearLayout)view.findViewById(R.id.notifications_list_root);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        mBackgroundColor = getResources().getColor(R.color.white);

        ListView listView = getListView();
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setHeaderDividersEnabled(false);

        if (mFooterView != null) {
           listView.addFooterView(mFooterView);
        }

        reloadNoteBlocks();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Start listening to bucket change events
        if (SimperiumUtils.getNotesBucket() != null) {
            SimperiumUtils.getNotesBucket().addListener(this);
        }

        // Set the note if we retrieved the noteId from savedInstanceState
        if (!TextUtils.isEmpty(mRestoredNoteId)) {
            setNoteWithNoteId(mRestoredNoteId);
            reloadNoteBlocks();
            mRestoredNoteId = null;
        }
    }

    @Override
    public void onPause() {
        // Remove the simperium bucket listener
        if (SimperiumUtils.getNotesBucket() != null) {
            SimperiumUtils.getNotesBucket().removeListener(this);
        }

        // Stop the reader comment service if it is running
        ReaderCommentService.stopService(getActivity());

        super.onPause();
    }

    @Override
    public Note getNote() {
        return mNote;
    }

    @Override
    public void setNote(Note note) {
        mNote = note;
    }

    private void setNoteWithNoteId(String noteId) {
        if (noteId == null) return;

        if (SimperiumUtils.getNotesBucket() != null) {
            try {
                Note note = SimperiumUtils.getNotesBucket().get(noteId);
                mIsUnread = note.isUnread();
                setNote(note);
            } catch (BucketObjectMissingException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mNote != null) {
            outState.putString(KEY_NOTE_ID, mNote.getId());
            outState.putInt(KEY_LIST_POSITION, getListView().getFirstVisiblePosition());
        }

        super.onSaveInstanceState(outState);
    }

    public void setOnNoteChangeListener(OnNoteChangeListener listener) {
        mOnNoteChangeListener = listener;
    }

    private void reloadNoteBlocks() {
        new LoadNoteBlocksTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void setFooterView(ViewGroup footerView) {
        mFooterView = footerView;
    }

    private final NoteBlock.OnNoteBlockTextClickListener mOnNoteBlockTextClickListener = new NoteBlock.OnNoteBlockTextClickListener() {
        @Override
        public void onNoteBlockTextClicked(NoteBlockClickableSpan clickedSpan) {
            if (!isAdded() || !(getActivity() instanceof NotificationsDetailActivity)) return;

            NotificationsUtils.handleNoteBlockSpanClick((NotificationsDetailActivity) getActivity(), clickedSpan);
        }

        @Override
        public void showDetailForNoteIds() {
            if (!isAdded() || mNote == null || !(getActivity() instanceof NotificationsDetailActivity)) {
                return;
            }

            NotificationsDetailActivity detailActivity = (NotificationsDetailActivity)getActivity();
            if (mNote.isCommentReplyType() || (!mNote.isCommentType() && mNote.getCommentId() > 0)) {
                long commentId = mNote.isCommentReplyType() ? mNote.getParentCommentId() : mNote.getCommentId();

                // show comments list if it exists in the reader
                if (ReaderUtils.postAndCommentExists(mNote.getSiteId(), mNote.getPostId(), commentId)) {
                    detailActivity.showReaderCommentsList(mNote.getSiteId(), mNote.getPostId(), commentId);
                } else {
                    detailActivity.showWebViewActivityForUrl(mNote.getUrl());
                }
            } else if (mNote.isFollowType()) {
                detailActivity.showBlogPreviewActivity(mNote.getSiteId());
            } else {
                // otherwise, load the post in the Reader
                detailActivity.showPostActivity(mNote.getSiteId(), mNote.getPostId());
            }
        }

        @Override
        public void showReaderPostComments() {
            if (!isAdded() || mNote == null || mNote.getCommentId() == 0) return;

            ReaderActivityLauncher.showReaderComments(getActivity(), mNote.getSiteId(), mNote.getPostId(), mNote.getCommentId());
        }

        @Override
        public void showSitePreview(long siteId, String siteUrl) {
            if (!isAdded() || mNote == null || !(getActivity() instanceof NotificationsDetailActivity)) {
                return;
            }

            NotificationsDetailActivity detailActivity = (NotificationsDetailActivity)getActivity();
            if (siteId != 0) {
                detailActivity.showBlogPreviewActivity(siteId);
            } else if (!TextUtils.isEmpty(siteUrl)) {
                detailActivity.showWebViewActivityForUrl(siteUrl);
            }
        }
    };

    private final UserNoteBlock.OnGravatarClickedListener mOnGravatarClickedListener = new UserNoteBlock.OnGravatarClickedListener() {
        @Override
        public void onGravatarClicked(long siteId, long userId, String siteUrl) {
            if (!isAdded() || !(getActivity() instanceof NotificationsDetailActivity)) return;

            NotificationsDetailActivity detailActivity = (NotificationsDetailActivity)getActivity();
            if (siteId == 0 && !TextUtils.isEmpty(siteUrl)) {
                detailActivity.showWebViewActivityForUrl(siteUrl);
            } else if (siteId != 0) {
                detailActivity.showBlogPreviewActivity(siteId);
            }
        }
    };

    private boolean hasNoteBlockAdapter() {
        return mNoteBlockAdapter != null;
    }


    // Loop through the 'body' items in this note, and create blocks for each.
    private class LoadNoteBlocksTask extends AsyncTask<Void, Boolean, List<NoteBlock>> {

        private boolean mIsBadgeView;

        @Override
        protected List<NoteBlock> doInBackground(Void... params) {
            if (mNote == null) return null;

            requestReaderContentForNote();

            JSONArray bodyArray = mNote.getBody();
            final List<NoteBlock> noteList = new ArrayList<>();

            // Add the note header if one was provided
            if (mNote.getHeader() != null) {
                ImageType imageType = mNote.isFollowType() ? ImageType.BLAVATAR : ImageType.AVATAR;
                HeaderNoteBlock headerNoteBlock = new HeaderNoteBlock(
                        getActivity(),
                        mNote.getHeader(),
                        imageType,
                        mOnNoteBlockTextClickListener,
                        mOnGravatarClickedListener
                );

                headerNoteBlock.setIsComment(mNote.isCommentType());
                noteList.add(headerNoteBlock);
            }

            if (bodyArray != null && bodyArray.length() > 0) {
                for (int i=0; i < bodyArray.length(); i++) {
                    try {
                        JSONObject noteObject = bodyArray.getJSONObject(i);
                        // Determine NoteBlock type and add it to the array
                        NoteBlock noteBlock;
                        String noteBlockTypeString = JSONUtils.queryJSON(noteObject, "type", "");

                        if (BlockType.fromString(noteBlockTypeString) == BlockType.USER) {
                            if (mNote.isCommentType()) {
                                // Set comment position so we can target it later
                                // See refreshBlocksForCommentStatus()
                                mCommentListPosition = i + noteList.size();

                                // We'll snag the next body array item for comment user blocks
                                if (i + 1 < bodyArray.length()) {
                                    JSONObject commentTextBlock = bodyArray.getJSONObject(i + 1);
                                    noteObject.put("comment_text", commentTextBlock);
                                    i++;
                                }

                                // Add timestamp to block for display
                                noteObject.put("timestamp", mNote.getTimestamp());

                                noteBlock = new CommentUserNoteBlock(
                                        getActivity(),
                                        noteObject,
                                        mOnNoteBlockTextClickListener,
                                        mOnGravatarClickedListener
                                );

                                // Set listener for comment status changes, so we can update bg and text colors
                                CommentUserNoteBlock commentUserNoteBlock = (CommentUserNoteBlock)noteBlock;
                                mOnCommentStatusChangeListener = commentUserNoteBlock.getOnCommentChangeListener();
                                commentUserNoteBlock.setCommentStatus(mNote.getCommentStatus());
                                commentUserNoteBlock.configureResources(getActivity());
                            } else {
                                noteBlock = new UserNoteBlock(
                                        getActivity(),
                                        noteObject,
                                        mOnNoteBlockTextClickListener,
                                        mOnGravatarClickedListener
                                );
                            }
                        } else if (isFooterBlock(noteObject)) {
                            noteBlock = new FooterNoteBlock(noteObject, mOnNoteBlockTextClickListener);
                            ((FooterNoteBlock)noteBlock).setClickableSpan(
                                    JSONUtils.queryJSON(noteObject, "ranges[last]", new JSONObject()),
                                    mNote.getType()
                            );
                        } else {
                            noteBlock = new NoteBlock(noteObject, mOnNoteBlockTextClickListener);
                        }

                        // Badge notifications apply different colors and formatting
                        if (isAdded() && noteBlock.containsBadgeMediaType()) {
                            mIsBadgeView = true;
                            mBackgroundColor = getActivity().getResources().getColor(R.color.transparent);
                        }

                        if (mIsBadgeView) {
                            noteBlock.setIsBadge();
                        }

                        noteList.add(noteBlock);
                    } catch (JSONException e) {
                        AppLog.e(AppLog.T.NOTIFS, "Invalid note data, could not parse.");
                    }
                }
            }

            return noteList;
        }

        @Override
        protected void onPostExecute(List<NoteBlock> noteList) {
            if (!isAdded() || noteList == null) return;

            if (mIsBadgeView) {
                mRootLayout.setGravity(Gravity.CENTER_VERTICAL);
            }

            if (!hasNoteBlockAdapter()) {
                mNoteBlockAdapter = new NoteBlockAdapter(getActivity(), noteList, mBackgroundColor);
                setListAdapter(mNoteBlockAdapter);
            } else {
                mNoteBlockAdapter.setNoteList(noteList);
            }

            if (mRestoredListPosition > 0) {
                getListView().setSelectionFromTop(mRestoredListPosition, 0);
                mRestoredListPosition = 0;
            }
        }
    }

    private boolean isFooterBlock(JSONObject blockObject) {
        if (mNote == null || blockObject == null) return false;

        if (mNote.isCommentType()) {
            // Check if this is a comment notification that has been replied to
            // The block will not have a type, and its id will match the comment reply id in the Note.
            return (JSONUtils.queryJSON(blockObject, "type", null) == null &&
                    mNote.getCommentReplyId() == JSONUtils.queryJSON(blockObject, "ranges[1].id", 0));
        } else if (mNote.isFollowType() || mNote.isLikeType() ||
                mNote.isCommentLikeType() || mNote.isReblogType()) {
            // User list notifications have a footer if they have 10 or more users in the body
            // The last block will not have a type, so we can use that to determine if it is the footer
            return JSONUtils.queryJSON(blockObject, "type", null) == null;
        }

        return false;
    }

    public void refreshBlocksForCommentStatus(CommentStatus newStatus) {
        if (mOnCommentStatusChangeListener != null) {
            mOnCommentStatusChangeListener.onCommentStatusChanged(newStatus);
            ListView listView = getListView();
            if (listView == null || mCommentListPosition == ListView.INVALID_POSITION) {
                return;
            }

            // Redraw the comment row if it is visible so that the background and text colors update
            // See: http://stackoverflow.com/questions/4075975/redraw-a-single-row-in-a-listview/9987616#9987616
            int firstPosition = listView.getFirstVisiblePosition();
            int endPosition = listView.getLastVisiblePosition();
            for (int i = firstPosition; i < endPosition; i++) {
                if (mCommentListPosition == i) {
                    View view = listView.getChildAt(i - firstPosition);
                    listView.getAdapter().getView(i, view, listView);
                    break;
                }
            }
        }
    }

    // Requests Reader content for certain notification types
    private void requestReaderContentForNote() {
        if (mNote == null || !isAdded()) return;

        // Request the reader post so that loading reader activities will work.
        if (mNote.isUserList() && !ReaderPostTable.postExists(mNote.getSiteId(), mNote.getPostId())) {
            ReaderPostActions.requestPost(mNote.getSiteId(), mNote.getPostId(), null);
        }

        // Request reader comments until we retrieve the comment for this note
        if ((mNote.isCommentLikeType() || mNote.isCommentReplyType() || mNote.isCommentWithUserReply()) &&
                !ReaderCommentTable.commentExists(mNote.getSiteId(), mNote.getPostId(), mNote.getCommentId())) {
            ReaderCommentService.startServiceForComment(getActivity(), mNote.getSiteId(), mNote.getPostId(), mNote.getCommentId());
        }
    }

    // Simperium bucket listener
    @Override
    public void onBeforeUpdateObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }

    @Override
    public void onDeleteObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }

    @Override
    public void onNetworkChange(Bucket<Note> noteBucket, Bucket.ChangeType changeType, String noteId) {
        // We're not interested in INDEX events here
        if (changeType == Bucket.ChangeType.INDEX) return;

        // Refresh content if we receive a change for the Note
        if (mNote != null && mNote.getId().equals(noteId)) {
            // If the note was removed, pop the back stack to return to the notes list
            if (changeType == Bucket.ChangeType.REMOVE) {
                getFragmentManager().popBackStack();
                return;
            }

            try {
                mNote = noteBucket.get(noteId);

                // Don't refresh if the note was just marked as read
                if (!mNote.isUnread() && mIsUnread) {
                    mIsUnread = false;
                    return;
                }

                // Mark note as read since we are looking at it already
                if (mNote.isUnread()) {
                    mNote.markAsRead();
                    EventBus.getDefault().post(new NotificationEvents.NotificationsChanged());
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            reloadNoteBlocks();
                            if (mOnNoteChangeListener != null) {
                                mOnNoteChangeListener.onNoteChanged(mNote);
                            }
                        }
                    });
                }
            } catch (BucketObjectMissingException e) {
                AppLog.e(AppLog.T.NOTIFS, "Couldn't load note after receiving change.");
            }
        }
    }

    @Override
    public void onSaveObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }
}
