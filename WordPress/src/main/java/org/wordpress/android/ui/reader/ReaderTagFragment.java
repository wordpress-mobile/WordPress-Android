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
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.reader.actions.ReaderTagActions.TagAction;
import org.wordpress.android.ui.reader.adapters.ReaderTagAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderTagAdapter.TagActionListener;
import org.wordpress.android.ui.reader.views.ReaderRecyclerView;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.WPActivityUtils;

/*
 * fragment hosted by ReaderSubsActivity which shows either followed or popular tags
 */
public class ReaderTagFragment extends Fragment implements ReaderTagAdapter.TagActionListener {
    private ReaderRecyclerView mRecyclerView;
    private ReaderTagAdapter mTagAdapter;
    private ReaderTagType mTagType;
    private static final String ARG_TAG_TYPE = "tag_type";

    static ReaderTagFragment newInstance(ReaderTagType tagType) {
        AppLog.d(AppLog.T.READER, "reader tag list > newInstance");

        Bundle args = new Bundle();
        args.putSerializable(ARG_TAG_TYPE, tagType);
        ReaderTagFragment fragment = new ReaderTagFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        restoreState(args);
    }

    private void restoreState(Bundle args) {
        if (args == null) {
            return;
        }
        if (args.containsKey(ARG_TAG_TYPE)) {
            mTagType = (ReaderTagType) args.getSerializable(ARG_TAG_TYPE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            AppLog.d(AppLog.T.READER, "reader tag fragment > restoring instance state");
            restoreState(savedInstanceState);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.reader_fragment_list, container, false);
        final Context context = container.getContext();

        mRecyclerView = (ReaderRecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.addItemDecoration(new ReaderRecyclerView.ReaderItemDecoration(0, DisplayUtils.dpToPx(context, 1)));

        return view;
    }

    void showEmptyView(boolean show) {
        if (!isAdded()) {
            return;
        }
        TextView emptyView = (TextView) getView().findViewById(R.id.text_empty);
        if (show) {
            switch (getTagType()) {
                case FOLLOWED:
                    emptyView.setText(R.string.reader_empty_followed_tags);
                    break;
                case RECOMMENDED:
                    emptyView.setText(R.string.reader_empty_popular_tags);
                    break;
            }
        }
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRecyclerView.setAdapter(getTagAdapter());
        getTagAdapter().refresh();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(ARG_TAG_TYPE, getTagType());
        super.onSaveInstanceState(outState);
    }

    void refresh() {
        if (hasTagAdapter()) {
            getTagAdapter().refresh();
        }
    }

    ReaderTagType getTagType() {
        return mTagType;
    }

    private ReaderTagAdapter getTagAdapter() {
        if (mTagAdapter == null) {
            Context context = WPActivityUtils.getThemedContext(getActivity());
            mTagAdapter = new ReaderTagAdapter(context, getTagType());
            mTagAdapter.setTagActionListener(this);
            mTagAdapter.setDataLoadedListener(new ReaderInterfaces.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    showEmptyView(isEmpty);
                }
            });
        }
        return mTagAdapter;
    }

    private boolean hasTagAdapter() {
        return (mTagAdapter != null);
    }

    /*
     * called from adapter when user adds/removes a tag - note that the network request
     * has been made by the time this is called
     */
    @Override
    public void onTagAction(ReaderTag tag, TagAction action) {
        // let the host activity know about the change
        if (getActivity() instanceof TagActionListener) {
            ((TagActionListener) getActivity()).onTagAction(tag, action);
        }
    }
}
