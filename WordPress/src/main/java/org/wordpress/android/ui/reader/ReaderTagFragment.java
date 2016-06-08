package org.wordpress.android.ui.reader;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.adapters.ReaderTagAdapter;
import org.wordpress.android.ui.reader.views.ReaderRecyclerView;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.WPActivityUtils;

/*
 * fragment hosted by ReaderSubsActivity which shows followed tags
 */
public class ReaderTagFragment extends Fragment implements ReaderTagAdapter.TagDeletedListener {
    private ReaderRecyclerView mRecyclerView;
    private ReaderTagAdapter mTagAdapter;

    static ReaderTagFragment newInstance() {
        AppLog.d(AppLog.T.READER, "reader tag list > newInstance");
        return new ReaderTagFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reader_fragment_list, container, false);
        mRecyclerView = (ReaderRecyclerView) view.findViewById(R.id.recycler_view);
        return view;
    }

    private void checkEmptyView() {
        if (!isAdded()) return;

        TextView emptyView = (TextView) getView().findViewById(R.id.text_empty);
        if (emptyView != null) {
            boolean isEmpty = hasTagAdapter() && getTagAdapter().isEmpty();
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            if (isEmpty) {
                emptyView.setText(R.string.reader_empty_followed_tags);
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRecyclerView.setAdapter(getTagAdapter());
        refresh();
    }

    void refresh() {
        if (hasTagAdapter()) {
            AppLog.d(AppLog.T.READER, "reader subs > refreshing tag fragment");
            getTagAdapter().refresh();
        }
    }

    private ReaderTagAdapter getTagAdapter() {
        if (mTagAdapter == null) {
            Context context = WPActivityUtils.getThemedContext(getActivity());
            mTagAdapter = new ReaderTagAdapter(context);
            mTagAdapter.setTagDeletedListener(this);
            mTagAdapter.setDataLoadedListener(new ReaderInterfaces.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    checkEmptyView();
                }
            });
        }
        return mTagAdapter;
    }

    private boolean hasTagAdapter() {
        return (mTagAdapter != null);
    }

    /*
     * called from adapter when user removes a tag - note that the network request
     * has been made by the time this is called
     */
    @Override
    public void onTagDeleted(ReaderTag tag) {
        checkEmptyView();
        // let the host activity know about the change
        if (getActivity() instanceof ReaderTagAdapter.TagDeletedListener) {
            ((ReaderTagAdapter.TagDeletedListener) getActivity()).onTagDeleted(tag);
        }
    }
}
