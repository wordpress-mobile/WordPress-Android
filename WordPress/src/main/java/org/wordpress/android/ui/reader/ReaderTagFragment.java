package org.wordpress.android.ui.reader;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.reader.actions.ReaderTagActions.TagAction;
import org.wordpress.android.ui.reader.adapters.ReaderTagAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderTagAdapter.TagActionListener;
import org.wordpress.android.util.AppLog;

/*
 * fragment hosted by ReaderSubsActivity which shows either followed or popular tags
 */
public class ReaderTagFragment extends Fragment implements ReaderTagAdapter.TagActionListener {
    private ListView mListView;
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
        mListView = (ListView) view.findViewById(android.R.id.list);

        final TextView emptyView = (TextView)view.findViewById(R.id.text_empty);
        switch (getTagType()) {
            case FOLLOWED:
                emptyView.setText(R.string.reader_empty_followed_tags);
                break;
            case RECOMMENDED:
                emptyView.setText(R.string.reader_empty_popular_tags);
                break;
        }

        mListView.setEmptyView(view.findViewById(R.id.text_empty));

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView.setAdapter(getTagAdapter());
        getTagAdapter().refresh();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(ARG_TAG_TYPE, getTagType());
        super.onSaveInstanceState(outState);
    }

    private void scrollToTagName(String tagName) {
        int index = getTagAdapter().indexOfTagName(tagName);
        if (index > -1) {
            mListView.smoothScrollToPosition(index);
        }
    }

    void refresh() {
        refresh(null);
    }
    void refresh(final String scrollToTagName) {
        if (!hasTagAdapter()) {
            return;
        }
        if (!TextUtils.isEmpty(scrollToTagName)) {
            ReaderInterfaces.DataLoadedListener dataListener = new ReaderInterfaces.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    scrollToTagName(scrollToTagName);
                }
            };
            getTagAdapter().refresh(dataListener);
        } else {
            getTagAdapter().refresh(null);
        }
    }

    ReaderTagType getTagType() {
        return mTagType;
    }

    private ReaderTagAdapter getTagAdapter() {
        if (mTagAdapter == null) {
            mTagAdapter = new ReaderTagAdapter(getActivity(), getTagType(), this);
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
        final boolean animateRemoval;
        switch (action) {
            case ADD:
                // animate tag's removal if added from recommended tags
                animateRemoval = (getTagType() == ReaderTagType.RECOMMENDED);
                break;
            case DELETE:
                // animate tag's removal if deleted from followed tags
                animateRemoval = (getTagType() == ReaderTagType.FOLLOWED);
                break;
            default:
                animateRemoval = false;
                break;
        }

        int index = getTagAdapter().indexOfTagName(tag.getTagName());
        if (animateRemoval && index > -1) {
            Animation.AnimationListener aniListener = new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) { }
                @Override
                public void onAnimationRepeat(Animation animation) { }
                @Override
                public void onAnimationEnd(Animation animation) {
                    refresh();
                }
            };
            ReaderAnim.AnimateListItemStyle animStyle =
                    (action == TagAction.ADD ? ReaderAnim.AnimateListItemStyle.ADD : ReaderAnim.AnimateListItemStyle.REMOVE);
            ReaderAnim.animateListItem(mListView, index, animStyle, aniListener);
        } else {
            refresh();
        }

        // let the host activity know about the change
        if (getActivity() instanceof TagActionListener) {
            ((TagActionListener) getActivity()).onTagAction(tag, action);
        }
    }
}
