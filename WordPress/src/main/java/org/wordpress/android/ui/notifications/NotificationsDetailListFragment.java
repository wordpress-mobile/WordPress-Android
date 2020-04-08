/**
 * One fragment to rule them all (Notes, that is)
 */
package org.wordpress.android.ui.notifications;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.datasets.ReaderCommentTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.tools.FormattableContent;
import org.wordpress.android.fluxc.tools.FormattableRange;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.notifications.adapters.NoteBlockAdapter;
import org.wordpress.android.ui.notifications.blocks.BlockType;
import org.wordpress.android.ui.notifications.blocks.CommentUserNoteBlock;
import org.wordpress.android.ui.notifications.blocks.FooterNoteBlock;
import org.wordpress.android.ui.notifications.blocks.GeneratedNoteBlock;
import org.wordpress.android.ui.notifications.blocks.HeaderNoteBlock;
import org.wordpress.android.ui.notifications.blocks.NoteBlock;
import org.wordpress.android.ui.notifications.blocks.NoteBlock.OnNoteBlockTextClickListener;
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan;
import org.wordpress.android.ui.notifications.blocks.UserNoteBlock;
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.services.ReaderCommentService;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.FormattableContentUtilsKt;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class NotificationsDetailListFragment extends ListFragment implements NotificationFragment {
    private static final String KEY_NOTE_ID = "noteId";
    private static final String KEY_LIST_POSITION = "listPosition";

    private int mRestoredListPosition;

    private Note mNote;
    private LinearLayout mRootLayout;
    private ViewGroup mFooterView;

    private String mRestoredNoteId;
    private int mCommentListPosition = ListView.INVALID_POSITION;

    private CommentUserNoteBlock.OnCommentStatusChangeListener mOnCommentStatusChangeListener;
    private NoteBlockAdapter mNoteBlockAdapter;

    @Inject ImageManager mImageManager;
    @Inject NotificationsUtilsWrapper mNotificationsUtilsWrapper;

    public NotificationsDetailListFragment() {
    }

    public static NotificationsDetailListFragment newInstance(final String noteId) {
        NotificationsDetailListFragment fragment = new NotificationsDetailListFragment();
        fragment.setNote(noteId);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_NOTE_ID)) {
            // The note will be set in onResume()
            // See WordPress.deferredInit()
            mRestoredNoteId = savedInstanceState.getString(KEY_NOTE_ID);
            mRestoredListPosition = savedInstanceState.getInt(KEY_LIST_POSITION, 0);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notifications_fragment_detail_list, container, false);
        mRootLayout = (LinearLayout) view.findViewById(R.id.notifications_list_root);

        return view;
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

        reloadNoteBlocks();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Set the note if we retrieved the noteId from savedInstanceState
        if (!TextUtils.isEmpty(mRestoredNoteId)) {
            setNote(mRestoredNoteId);
            reloadNoteBlocks();
            mRestoredNoteId = null;
        }

        if (getNote() == null) {
            showErrorToastAndFinish();
        }
    }

    @Override
    public void onPause() {
        // Stop the reader comment service if it is running
        ReaderCommentService.stopService(getActivity());

        super.onPause();
    }

    @Override
    public Note getNote() {
        return mNote;
    }

    @Override
    public void setNote(String noteId) {
        if (noteId == null) {
            showErrorToastAndFinish();
            return;
        }

        Note note = NotificationsTable.getNoteById(noteId);
        if (note == null) {
            showErrorToastAndFinish();
            return;
        }
        mNote = note;
    }

    private void showErrorToastAndFinish() {
        AppLog.e(AppLog.T.NOTIFS, "Note could not be found.");
        if (getActivity() != null) {
            ToastUtils.showToast(getActivity(), R.string.error_notification_open);
            getActivity().finish();
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

    private void reloadNoteBlocks() {
        new LoadNoteBlocksTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void setFooterView(ViewGroup footerView) {
        mFooterView = footerView;
    }

    private final NoteBlock.OnNoteBlockTextClickListener mOnNoteBlockTextClickListener =
            new NoteBlock.OnNoteBlockTextClickListener() {
                @Override
                public void onNoteBlockTextClicked(NoteBlockClickableSpan clickedSpan) {
                    if (!isAdded() || !(getActivity() instanceof NotificationsDetailActivity)) {
                        return;
                    }

                    handleNoteBlockSpanClick((NotificationsDetailActivity) getActivity(), clickedSpan);
                }

                @Override
                public void showDetailForNoteIds() {
                    if (!isAdded() || mNote == null || !(getActivity() instanceof NotificationsDetailActivity)) {
                        return;
                    }

                    NotificationsDetailActivity detailActivity = (NotificationsDetailActivity) getActivity();
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
                    if (!isAdded() || mNote == null || mNote.getCommentId() == 0) {
                        return;
                    }

                    ReaderActivityLauncher.showReaderComments(getActivity(), mNote.getSiteId(), mNote.getPostId(),
                                                              mNote.getCommentId());
                }

                @Override
                public void showSitePreview(long siteId, String siteUrl) {
                    if (!isAdded() || mNote == null || !(getActivity() instanceof NotificationsDetailActivity)) {
                        return;
                    }

                    NotificationsDetailActivity detailActivity = (NotificationsDetailActivity) getActivity();
                    if (siteId != 0) {
                        detailActivity.showBlogPreviewActivity(siteId);
                    } else if (!TextUtils.isEmpty(siteUrl)) {
                        detailActivity.showWebViewActivityForUrl(siteUrl);
                    }
                }

                public void handleNoteBlockSpanClick(NotificationsDetailActivity activity,
                                                     NoteBlockClickableSpan clickedSpan) {
                    switch (clickedSpan.getRangeType()) {
                        case SITE:
                            // Show blog preview
                            activity.showBlogPreviewActivity(clickedSpan.getId());
                            break;
                        case USER:
                            // Show blog preview
                            activity.showBlogPreviewActivity(clickedSpan.getSiteId());
                            break;
                        case POST:
                            // Show post detail
                            activity.showPostActivity(clickedSpan.getSiteId(), clickedSpan.getId());
                            break;
                        case COMMENT:
                            // Load the comment in the reader list if it exists, otherwise show a webview
                            if (ReaderUtils.postAndCommentExists(clickedSpan.getSiteId(), clickedSpan.getPostId(),
                                                                 clickedSpan.getId())) {
                                activity.showReaderCommentsList(clickedSpan.getSiteId(), clickedSpan.getPostId(),
                                                                clickedSpan.getId());
                            } else {
                                activity.showWebViewActivityForUrl(clickedSpan.getUrl());
                            }
                            break;
                        case STAT:
                        case FOLLOW:
                            // We can open native stats if the site is a wpcom or Jetpack sites
                            activity.showStatsActivityForSite(clickedSpan.getSiteId(), clickedSpan.getRangeType());
                            break;
                        case LIKE:
                            if (ReaderPostTable.postExists(clickedSpan.getSiteId(), clickedSpan.getId())) {
                                activity.showReaderPostLikeUsers(clickedSpan.getSiteId(), clickedSpan.getId());
                            } else {
                                activity.showPostActivity(clickedSpan.getSiteId(), clickedSpan.getId());
                            }
                            break;
                        default:
                            // We don't know what type of id this is, let's see if it has a URL and push a webview
                            if (!TextUtils.isEmpty(clickedSpan.getUrl())) {
                                activity.showWebViewActivityForUrl(clickedSpan.getUrl());
                            }
                    }
                }
            };

    private final UserNoteBlock.OnGravatarClickedListener mOnGravatarClickedListener =
            new UserNoteBlock.OnGravatarClickedListener() {
                @Override
                public void onGravatarClicked(long siteId, long userId, String siteUrl) {
                    if (!isAdded() || !(getActivity() instanceof NotificationsDetailActivity)) {
                        return;
                    }

                    NotificationsDetailActivity detailActivity = (NotificationsDetailActivity) getActivity();
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
            if (mNote == null) {
                return null;
            }

            requestReaderContentForNote();
            JSONArray bodyArray = mNote.getBody();
            final List<NoteBlock> noteList = new ArrayList<>();

            // Add the note header if one was provided
            if (mNote.getHeader() != null) {
                ImageType imageType = mNote.isFollowType() ? ImageType.BLAVATAR : ImageType.AVATAR_WITH_BACKGROUND;
                HeaderNoteBlock headerNoteBlock = new HeaderNoteBlock(
                        getActivity(),
                        transformToFormattableContentList(mNote.getHeader()),
                        imageType,
                        mOnNoteBlockTextClickListener,
                        mOnGravatarClickedListener,
                        mImageManager,
                        mNotificationsUtilsWrapper
                );

                headerNoteBlock.setIsComment(mNote.isCommentType());
                noteList.add(headerNoteBlock);
            }
            String pingbackUrl = null;

            boolean isPingback = isPingback(mNote);
            if (bodyArray != null && bodyArray.length() > 0) {
                for (int i = 0; i < bodyArray.length(); i++) {
                    try {
                        FormattableContent noteObject = mNotificationsUtilsWrapper
                                .mapJsonToFormattableContent(bodyArray.getJSONObject(i));
                        // Determine NoteBlock type and add it to the array
                        NoteBlock noteBlock;
                        if (BlockType.fromString(noteObject.getType()) == BlockType.USER) {
                            if (mNote.isCommentType()) {
                                // Set comment position so we can target it later
                                // See refreshBlocksForCommentStatus()
                                mCommentListPosition = i + noteList.size();

                                FormattableContent commentTextBlock = null;
                                // Next item in the bodyArray is comment text
                                if (i + 1 < bodyArray.length()) {
                                    commentTextBlock = mNotificationsUtilsWrapper
                                            .mapJsonToFormattableContent(bodyArray.getJSONObject(i + 1));
                                    i++;
                                }

                                noteBlock = new CommentUserNoteBlock(
                                        getActivity(),
                                        noteObject,
                                        commentTextBlock,
                                        mNote.getTimestamp(),
                                        mOnNoteBlockTextClickListener,
                                        mOnGravatarClickedListener,
                                        mImageManager,
                                        mNotificationsUtilsWrapper
                                );
                                pingbackUrl = noteBlock.getMetaSiteUrl();

                                // Set listener for comment status changes, so we can update bg and text colors
                                CommentUserNoteBlock commentUserNoteBlock = (CommentUserNoteBlock) noteBlock;
                                mOnCommentStatusChangeListener = commentUserNoteBlock.getOnCommentChangeListener();
                                commentUserNoteBlock.setCommentStatus(mNote.getCommentStatus());
                                commentUserNoteBlock.configureResources(getActivity());
                            } else {
                                noteBlock = new UserNoteBlock(
                                        getActivity(),
                                        noteObject,
                                        mOnNoteBlockTextClickListener,
                                        mOnGravatarClickedListener,
                                        mImageManager,
                                        mNotificationsUtilsWrapper
                                );
                            }
                        } else if (isFooterBlock(noteObject)) {
                            noteBlock = new FooterNoteBlock(noteObject, mImageManager, mNotificationsUtilsWrapper,
                                    mOnNoteBlockTextClickListener);
                            if (noteObject.getRanges() != null && noteObject.getRanges().size() > 0) {
                                FormattableRange range =
                                        noteObject.getRanges().get(noteObject.getRanges().size() - 1);
                                ((FooterNoteBlock) noteBlock).setClickableSpan(range, mNote.getType());
                            }
                        } else {
                            noteBlock = new NoteBlock(noteObject, mImageManager, mNotificationsUtilsWrapper,
                                    mOnNoteBlockTextClickListener);
                        }

                        // Badge notifications apply different colors and formatting
                        if (isAdded() && noteBlock.containsBadgeMediaType()) {
                            mIsBadgeView = true;
                        }

                        if (mIsBadgeView) {
                            noteBlock.setIsBadge();
                        }

                        if (isPingback) {
                            noteBlock.setIsPingback();
                        }

                        noteList.add(noteBlock);
                    } catch (JSONException e) {
                        AppLog.e(AppLog.T.NOTIFS, "Invalid note data, could not parse.");
                    }
                }
            }

            if (isPingback) {
                // Remove this when we start receiving "Read the source post block" from the backend
                NoteBlock generatedBlock =
                        buildGeneratedLinkBlock(mOnNoteBlockTextClickListener, pingbackUrl,
                                getActivity().getString(R.string.comment_read_source_post));
                generatedBlock.setIsPingback();
                noteList.add(generatedBlock);
            }

            return noteList;
        }

        @NonNull private List<FormattableContent> transformToFormattableContentList(JSONArray headerArray) {
            List<FormattableContent> headersList = new ArrayList<>();
            if (headerArray != null) {
                for (int i = 0; i < headerArray.length(); i++) {
                    try {
                        headersList.add(mNotificationsUtilsWrapper.mapJsonToFormattableContent(
                                headerArray.getJSONObject(i)));
                    } catch (JSONException e) {
                        AppLog.e(T.NOTIFS, "Header array has invalid format.");
                    }
                }
            }
            return headersList;
        }

        private boolean isPingback(Note note) {
            boolean hasRangeOfTypeSite = false;
            boolean hasRangeOfTypePost = false;

            JSONArray rangesArray = note.getSubject().optJSONArray("ranges");
            if (rangesArray != null) {
                for (int i = 0; i < rangesArray.length(); i++) {
                    JSONObject rangeObject = rangesArray.optJSONObject(i);
                    if (rangeObject == null) {
                        continue;
                    }
                    if ("site".equals(rangeObject.optString("type"))) {
                        hasRangeOfTypeSite = true;
                    } else if ("post".equals(rangeObject.optString("type"))) {
                        hasRangeOfTypePost = true;
                    }
                }
            }
            return hasRangeOfTypePost && hasRangeOfTypeSite;
        }

        private NoteBlock buildGeneratedLinkBlock(OnNoteBlockTextClickListener onNoteBlockTextClickListener,
                                                  String pingbackUrl,
                                                  String message) {
            return new GeneratedNoteBlock(
                    message,
                    mImageManager,
                    mNotificationsUtilsWrapper,
                    onNoteBlockTextClickListener,
                    pingbackUrl);
        }

        @Override
        protected void onPostExecute(List<NoteBlock> noteList) {
            if (!isAdded() || noteList == null) {
                return;
            }

            if (mIsBadgeView) {
                mRootLayout.setGravity(Gravity.CENTER_VERTICAL);
            }

            if (!hasNoteBlockAdapter()) {
                mNoteBlockAdapter = new NoteBlockAdapter(getActivity(), noteList);
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

    private boolean isFooterBlock(FormattableContent blockObject) {
        if (mNote == null || blockObject == null) {
            return false;
        }

        if (mNote.isCommentType()) {
            Long commentReplyId = FormattableContentUtilsKt.getRangeIdOrZero(blockObject, 1);
            // Check if this is a comment notification that has been replied to
            // The block will not have a type, and its id will match the comment reply id in the Note.
            return (blockObject.getType() == null
                    && mNote.getCommentReplyId() == commentReplyId);
        } else if (mNote.isFollowType() || mNote.isLikeType() || mNote.isReblogType()) {
            // User list notifications have a footer if they have 10 or more users in the body
            // The last block will not have a type, so we can use that to determine if it is the footer
            return blockObject.getType() == null;
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
        if (mNote == null || !isAdded()) {
            return;
        }

        // Request the reader post so that loading reader activities will work.
        if (mNote.isUserList() && !ReaderPostTable.postExists(mNote.getSiteId(), mNote.getPostId())) {
            ReaderPostActions.requestBlogPost(mNote.getSiteId(), mNote.getPostId(), null);
        }

        // Request reader comments until we retrieve the comment for this note
        if ((mNote.isCommentLikeType() || mNote.isCommentReplyType() || mNote.isCommentWithUserReply())
            && !ReaderCommentTable.commentExists(mNote.getSiteId(), mNote.getPostId(), mNote.getCommentId())) {
            ReaderCommentService
                    .startServiceForComment(getActivity(), mNote.getSiteId(), mNote.getPostId(), mNote.getCommentId());
        }
    }
}
