package org.wordpress.android.ui.reader;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.ui.ActionableEmptyView;
import org.wordpress.android.ui.reader.adapters.ReaderTagAdapter;
import org.wordpress.android.ui.reader.views.ReaderRecyclerView;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.WPActivityUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * fragment hosted by ReaderSubsActivity which shows followed tags
 */
public class ReaderTagFragment extends Fragment
        implements ReaderTagAdapter.TagDeletedListener, ReaderTagAdapter.TagAddedListener {
    private ReaderRecyclerView mRecyclerView;
    private ReaderTagAdapter mTagAdapter;

    private boolean mIsFirstDataLoaded;
    private final ReaderTagList mInitialReaderTagList = new ReaderTagList();

    static ReaderTagFragment newInstance() {
        AppLog.d(AppLog.T.READER, "reader tag list > newInstance");
        return new ReaderTagFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reader_fragment_list, container, false);
        mRecyclerView = view.findViewById(R.id.recycler_view);
        return view;
    }

    public boolean hasChangedSelectedTags() {
        final Set<String> initialTagsSlugs = new HashSet<>();
        for (final ReaderTag readerTag : mInitialReaderTagList) {
            initialTagsSlugs.add(readerTag.getTagSlug());
        }
        final List<ReaderTag> currentlySubscribedReaderTagList = getTagAdapter().getSubscribedItems();
        final Set<String> currentTagsSlugs = new HashSet<>();
        if (currentlySubscribedReaderTagList != null) {
            for (final ReaderTag readerTag : currentlySubscribedReaderTagList) {
                currentTagsSlugs.add(readerTag.getTagSlug());
            }
        }
        return !(initialTagsSlugs.equals(currentTagsSlugs));
    }

    private void checkEmptyView() {
        if (!isAdded() || getView() == null) {
            return;
        }

        ActionableEmptyView actionableEmptyView = getView().findViewById(R.id.actionable_empty_view);

        if (actionableEmptyView == null) {
            return;
        }

        actionableEmptyView.image.setVisibility(View.GONE);
        actionableEmptyView.title.setText(R.string.reader_no_followed_tags_text_title);
        actionableEmptyView.subtitle.setText(R.string.reader_empty_subscribed_tags_subtitle);
        actionableEmptyView.subtitle.setVisibility(View.VISIBLE);
        actionableEmptyView.setVisibility(hasTagAdapter() && getTagAdapter().isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRecyclerView.setAdapter(getTagAdapter());
        refresh();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mIsFirstDataLoaded = true;
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
            mTagAdapter.setTagAddedListener(this);
            mTagAdapter.setDataLoadedListener(isEmpty -> {
                checkEmptyView();
                if (mIsFirstDataLoaded) {
                    mIsFirstDataLoaded = false;
                    mInitialReaderTagList.clear();
                    if (mTagAdapter != null && mTagAdapter.getItems() != null) {
                        mInitialReaderTagList.addAll(mTagAdapter.getItems());
                    }
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

    @Override public void onTagAdded(@NonNull ReaderTag readerTag) {
        if (getActivity() instanceof ReaderTagAdapter.TagDeletedListener) {
            ((ReaderTagAdapter.TagAddedListener) getActivity()).onTagAdded(readerTag);
        }
    }
}
