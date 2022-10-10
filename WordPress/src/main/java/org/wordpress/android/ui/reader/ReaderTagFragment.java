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
import org.wordpress.android.ui.ActionableEmptyView;
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reader_fragment_list, container, false);
        mRecyclerView = view.findViewById(R.id.recycler_view);
        return view;
    }

    private void checkEmptyView() {
        if (!isAdded() || getView() == null) {
            return;
        }

        ActionableEmptyView actionableEmptyView = getView().findViewById(R.id.actionable_empty_view);

        if (actionableEmptyView == null) {
            return;
        }

        actionableEmptyView.image.setImageResource(R.drawable.img_illustration_empty_results_216dp);
        actionableEmptyView.image.setVisibility(View.VISIBLE);
        actionableEmptyView.title.setText(R.string.reader_empty_followed_tags_title);
        actionableEmptyView.subtitle.setText(R.string.reader_empty_followed_tags_subtitle);
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
