package org.wordpress.android.ui.notifications;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.PullToRefreshHelper;
import org.wordpress.android.ui.PullToRefreshHelper.RefreshListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;

public class NotificationsListFragment extends ListFragment implements NotesAdapter.DataLoadedListener {
    private static final int LOAD_MORE_WITHIN_X_ROWS = 5;
    private NoteProvider mNoteProvider;
    private NotesAdapter mNotesAdapter;
    private OnNoteClickListener mNoteClickListener;
    private View mProgressFooterView;
    private boolean mAllNotesLoaded;
    private PullToRefreshHelper mPullToRefreshHelper;

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
        public void onRequestMoreNotifications();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.empty_listview, container, false);
    }

    public void animateRefresh(boolean refresh) {
        mPullToRefreshHelper.setRefreshing(refresh);
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
        setListAdapter(getNotesAdapter());

        // Set empty text if no notifications
        TextView textview = (TextView) listView.getEmptyView();
        if (textview != null) {
            textview.setText(getText(R.string.notifications_empty_list));
        }
        initPullToRefreshHelper();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        boolean isRefreshing = mPullToRefreshHelper.isRefreshing();
        super.onConfigurationChanged(newConfig);
        // Pull to refresh layout is destroyed onDetachedFromWindow,
        // so we have to re-init the layout, via the helper here
        initPullToRefreshHelper();
        mPullToRefreshHelper.setRefreshing(isRefreshing);
    }

    private void initPullToRefreshHelper() {
        mPullToRefreshHelper = new PullToRefreshHelper(getActivity(),
                (PullToRefreshLayout) getActivity().findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted(View view) {
                        if (getActivity() == null || !NetworkUtils.checkConnection(getActivity())) {
                            mPullToRefreshHelper.setRefreshing(false);
                            return;
                        }
                        if (getActivity() instanceof NotificationsActivity) {
                            ((NotificationsActivity) getActivity()).refreshNotes();
                        }
                    }
                }, TextView.class);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Note note = getNotesAdapter().getItem(position);
        l.setItemChecked(position, true);
        if (note != null && !note.isPlaceholder() && mNoteClickListener != null) {
            mNoteClickListener.onClickNote(note);
        }
    }

    public void setNoteSelected(Note note, boolean scrollToNote) {
        if (!hasActivity() || getView() == null) {
            AppLog.w(T.NOTIFS, "notifications list, note selected when fragment is invalid");
            return;
        }
        int position = getNotesAdapter().indexOfNote(note);
        if (position >= 0) {
            getListView().setItemChecked(position, true);
            if (scrollToNote) {
                getListView().setSelection(position);
            }
        }
    }

    NotesAdapter getNotesAdapter() {
        if (mNotesAdapter == null) {
            mNotesAdapter = new NotesAdapter(getActivity(), this);
        }
        return mNotesAdapter;
    }

    boolean hasAdapter() {
        return (mNotesAdapter != null);
    }

    /*
     * update the passed note in the adapter
     */
    protected void updateNote(Note note) {
        if (hasActivity() && hasAdapter())
            getNotesAdapter().updateNote(note);
    }

    /*
     * called by NotesAdapter after loading notes
     */
    @Override
    public void onDataLoaded(boolean isEmpty) {
        hideProgressFooter();
    }

    public void setNoteProvider(NoteProvider provider) {
        mNoteProvider = provider;
    }

    public void setOnNoteClickListener(OnNoteClickListener listener) {
        mNoteClickListener = listener;
    }

    private void requestMoreNotifications() {
        if (getView() == null) {
            AppLog.w(T.NOTIFS, "requestMoreNotifications called before view exists");
            return;
        }

        if (!hasActivity()) {
            AppLog.w(T.NOTIFS, "requestMoreNotifications called without activity");
            return;
        }

        if (mNoteProvider != null && mNoteProvider.canRequestMore()) {
            showProgressFooter();
            mNoteProvider.onRequestMoreNotifications();
        }
    }

    void setAllNotesLoaded(boolean allNotesLoaded) {
        mAllNotesLoaded = allNotesLoaded;
    }

    private boolean hasActivity() {
        return (getActivity() != null && !isRemoving());
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
            if (visibleItemCount == 0 || totalItemCount == 0)
                return;

            // skip if all notes are loaded or notes are currently being added to the adapter
            if (mAllNotesLoaded || getNotesAdapter().isAddingNotes())
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