package org.wordpress.android.ui.reader;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.adapters.ReaderTagAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderTagAdapter.TagActionListener;
import org.wordpress.android.util.AppLog;

/**
 * fragment hosted by ReaderTagActivity which shows either followed or recommended tags
 */
public class ReaderTagFragment extends SherlockFragment implements ReaderTagAdapter.TagActionListener {
    private ListView mListView;
    private ReaderTagAdapter mAdapter;
    private boolean mShowFollowedTags = true;
    private static final String ARG_SHOW_FOLLOWED = "showing_followed";

    static ReaderTagFragment newInstance(ReaderTag.ReaderTagType tagType) {
        AppLog.d(AppLog.T.READER, "reader tag list > newInstance");

        Bundle args = new Bundle();
        switch (tagType) {
            case SUBSCRIBED:
                args.putBoolean(ARG_SHOW_FOLLOWED, true);
                break;
            case RECOMMENDED:
                args.putBoolean(ARG_SHOW_FOLLOWED, false);
                break;
        }

        ReaderTagFragment fragment = new ReaderTagFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        if (args != null) {
            mShowFollowedTags = args.getBoolean(ARG_SHOW_FOLLOWED);
        }
    }

    ListView getListView() {
        return mListView;
    }

    void scrollToTag(String tagName) {
        int index = getTagAdapter().indexOfTagName(tagName);
        if (index > -1) {
            getListView().smoothScrollToPosition(index);
        }
    }

    protected void refreshTags(final String scrollToTagName) {
        if (!TextUtils.isEmpty(scrollToTagName)) {
            ReaderActions.DataLoadedListener dataListener = new ReaderActions.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    scrollToTag(scrollToTagName);
                }
            };
            getTagAdapter().refreshTags(dataListener);
        } else {
            getTagAdapter().refreshTags(null);
        }
    }

    private ReaderTag.ReaderTagType getTagType() {
        return (mShowFollowedTags ? ReaderTag.ReaderTagType.SUBSCRIBED : ReaderTag.ReaderTagType.RECOMMENDED);
    }

    private ReaderTagAdapter getTagAdapter() {
        if (mAdapter == null) {
            mAdapter = new ReaderTagAdapter(getActivity(), this);
            mAdapter.setTagType(getTagType());
        }
        return mAdapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.reader_fragment_tag_list, container, false);
        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setEmptyView(view.findViewById(R.id.text_empty));
        mListView.setAdapter(getTagAdapter());
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARG_SHOW_FOLLOWED, mShowFollowedTags);
    }

    @Override
    public void onTagAction(ReaderTagActions.TagAction action, String tagName) {
        if (getActivity() instanceof TagActionListener) {
            ((TagActionListener) getActivity()).onTagAction(action, tagName);
        }
    }
}
