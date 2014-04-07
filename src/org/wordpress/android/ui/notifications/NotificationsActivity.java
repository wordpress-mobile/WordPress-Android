package org.wordpress.android.ui.notifications;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.GCMIntentService;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.comments.CommentDialogs;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.ui.reader.actions.ReaderAuthActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;

import static org.wordpress.android.WordPress.getRestClientUtils;

public class NotificationsActivity extends WPActionBarActivity
                                   implements NotificationFragment.OnPostClickListener,
                                              NotificationFragment.OnCommentClickListener {

    public static final String NOTIFICATION_ACTION      = "org.wordpress.android.NOTIFICATION";
    public static final String NOTE_ID_EXTRA            = "noteId";
    public static final String FROM_NOTIFICATION_EXTRA  = "fromNotification";
    public static final String NOTE_INSTANT_REPLY_EXTRA = "instantReply";

    private static final String KEY_INITIAL_UPDATE = "initial_update";

    private NotificationsListFragment mNotesList;
    private boolean mLoadingMore = false;
    private boolean mFirstLoadComplete = false;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        createMenuDrawer(R.layout.notifications);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        setTitle(getResources().getString(R.string.notifications));

        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
        mNotesList = (NotificationsListFragment) fm.findFragmentById(R.id.fragment_notes_list);
        mNotesList.setOnNoteClickListener(new NoteClickListener());

        GCMIntentService.activeNotificationsMap.clear();

        if (savedInstanceState != null) {
            mHasPerformedInitialUpdate = savedInstanceState.getBoolean(KEY_INITIAL_UPDATE);
        }

        if (savedInstanceState != null) {
            popNoteDetail();
        }

        // remove window background since background color is set in fragment (reduces overdraw)
        getWindow().setBackgroundDrawable(null);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        GCMIntentService.activeNotificationsMap.clear();

        launchWithNoteId();
    }

    private final FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0)
                mMenuDrawer.setDrawerIndicatorEnabled(true);
        }
    };
    private boolean mHasPerformedInitialUpdate;

    /**
     * Detect if Intent has a noteId extra and display that specific note detail fragment
     */
    private void launchWithNoteId(){
        /*final Intent intent = getIntent();
        if (intent.hasExtra(NOTE_ID_EXTRA)) {
            int noteID = Integer.valueOf(intent.getStringExtra(NOTE_ID_EXTRA));
            Note note = WordPress.wpDB.getNoteById(noteID);
            if (note != null) {
                openNote(note);
            } else {
                // find it/load it etc
                Map<String, String> params = new HashMap<String, String>();
                params.put("ids", intent.getStringExtra(NOTE_ID_EXTRA));
                NotesResponseHandler handler = new NotesResponseHandler(){
                    @Override
                    public void onNotes(List<Note> notes){
                        // there should only be one note!
                        if (!notes.isEmpty()) {
                            openNote(notes.get(0));
                        }
                    }
                };
                getRestClientUtils().getNotifications(params, handler, handler);
            }
        } else {
            // on a tablet: open first note if none selected
            String fragmentTag = mNotesList.getTag();
            if (fragmentTag != null && fragmentTag.equals("tablet-view")) {
                if (mNotesList.hasAdapter() && !mNotesList.getNotesAdapter().isEmpty()) {
                    openNote(mNotesList.getNotesAdapter().getItem(0));
                }
            }
            mNotesList.animateRefresh(true);
            refreshNotes();
        }*/
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isLargeOrXLarge()) {
                    // let WPActionBarActivity handle it (toggles menu drawer)
                    return super.onOptionsItemSelected(item);
                } else {
                    FragmentManager fm = getSupportFragmentManager();
                    if (fm.getBackStackEntryCount() > 0) {
                        popNoteDetail();
                        return true;
                    } else {
                        return super.onOptionsItemSelected(item);
                    }
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.notifications, menu);
        return true;
    }

    void popNoteDetail(){
        FragmentManager fm = getSupportFragmentManager();
        Fragment f = fm.findFragmentById(R.id.fragment_comment_detail);
        if (f == null) {
            fm.popBackStack();
        }
    }

    /**
     * Tries to pick the correct fragment detail type for a given note
     */
    private Fragment getDetailFragmentForNote(Note note){
        if (note == null)
            return null;

        if (note.isCommentType()) {
            // show comment detail for comment notifications
            return CommentDetailFragment.newInstance(note);
        } else if (note.isCommentLikeType()) {
            return new NoteCommentLikeFragment();
        } else if (note.isAutomattcherType()) {
            // show reader post detail for automattchers about posts - note that comment
            // automattchers are handled by note.isCommentType() above
            boolean isPost = (note.getBlogId() !=0 && note.getPostId() != 0 && note.getCommentId() == 0);
            if (isPost) {
                return ReaderPostDetailFragment.newInstance(note.getBlogId(), note.getPostId());
            } else {
                // right now we'll never get here
                return new NoteMatcherFragment();
            }
        } else if (note.isSingleLineListTemplate()) {
            return new NoteSingleLineListFragment();
        } else if (note.isBigBadgeTemplate()) {
            return new BigBadgeFragment();
        }

        return null;
    }

    /**
     *  Open a note fragment based on the type of note
     */
    private void openNote(final Note note) {
        if (note == null || isFinishing())
            return;

        // mark the note as read if it's unread
        if (note.isUnread()) {
            // mark as read which syncs with simperium
            note.markAsRead();
        }

        FragmentManager fm = getSupportFragmentManager();

        // remove the note detail if it's already on there
        if (fm.getBackStackEntryCount() > 0){
            fm.popBackStack();
        }

        // create detail fragment for this note type
        Fragment detailFragment = getDetailFragmentForNote(note);
        if (detailFragment == null) {
            AppLog.d(T.NOTIFS, String.format("No fragment found for %s", note.toJSONObject()));
            return;
        }

        // set arguments from activity if called from a notification
        /*Intent intent = getIntent();
        if (intent.hasExtra(NOTE_ID_EXTRA) && intent.getStringExtra(NOTE_ID_EXTRA).equals(note.getId())) {
            if (intent.hasExtra(NOTE_REPLY_EXTRA) || intent.hasExtra(NOTE_INSTANT_REPLY_EXTRA)) {
                detailFragment.setArguments(intent.getExtras());
            }
        }*/

        // set the note if this is a NotificationFragment (ReaderPostDetailFragment is the only
        // fragment used here that is not a NotificationFragment)
        if (detailFragment instanceof NotificationFragment) {
            ((NotificationFragment) detailFragment).setNote(note);
        }

        // swap the fragment
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.layout_fragment_container, detailFragment)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

        // only add to backstack if we're removing the list view from the fragment container
        View container = findViewById(R.id.layout_fragment_container);
        if (container.findViewById(R.id.fragment_notes_list) != null) {
            mMenuDrawer.setDrawerIndicatorEnabled(false);
            ft.addToBackStack(null);
            if (mNotesList != null)
                ft.hide(mNotesList);
        }

        ft.commitAllowingStateLoss();
    }

    private void updateLastSeen(String timestamp) {
        getRestClientUtils().markNotificationsSeen(timestamp, new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.d(T.NOTIFS, String.format("Set last seen time %s", response));
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.d(T.NOTIFS, String.format("Could not set last seen time %s", error));
                    }
                }
        );
    }

    private class NoteClickListener implements NotificationsListFragment.OnNoteClickListener {
        @Override
        public void onClickNote(Note note){
            if (note == null)
                return;
            // open the latest version of this note just in case it has changed - this can
            // happen if the note was tapped from the list fragment after it was updated
            // by another fragment (such as NotificationCommentLikeFragment)
            Note updatedNote = WordPress.wpDB.getNoteById(StringUtils.stringToInt(note.getId()));
            openNote(updatedNote != null ? updatedNote : note);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        outState.putBoolean(KEY_INITIAL_UPDATE, mHasPerformedInitialUpdate);
        outState.remove(NOTE_ID_EXTRA);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mHasPerformedInitialUpdate) {
            mHasPerformedInitialUpdate = true;
            ReaderAuthActions.updateCookies(this);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = CommentDialogs.createCommentDialog(this, id);
        if (dialog != null)
            return dialog;
        return super.onCreateDialog(id);
    }

    /*
     * called from fragment when a link to a post is tapped - shows the post in a reader
     * detail fragment
     */
    @Override
    public void onPostClicked(Note note, int remoteBlogId, int postId) {
        ReaderPostDetailFragment readerFragment = ReaderPostDetailFragment.newInstance(remoteBlogId, postId);
        String tagForFragment = getString(R.string.fragment_tag_reader_post_detail);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.layout_fragment_container, readerFragment, tagForFragment)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
          .addToBackStack(tagForFragment)
          .commit();
    }
    /*
     * called from fragment when a link to a comment is tapped - shows the comment in the comment
     * detail fragment
     */
    @Override
    public void onCommentClicked(Note note, int remoteBlogId, long commentId) {
        CommentDetailFragment commentFragment = CommentDetailFragment.newInstance(note);
        String tagForFragment = getString(R.string.fragment_tag_comment_detail);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.layout_fragment_container, commentFragment, tagForFragment)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
          .addToBackStack(tagForFragment)
          .commit();
    }
}
