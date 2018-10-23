package org.wordpress.android.ui.history;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogContent;
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogController;
import org.wordpress.android.ui.history.HistoryListItem.Revision;
import org.wordpress.android.widgets.DiffView;

public class HistoryDetailFullScreenDialogFragment extends Fragment implements FullScreenDialogContent {
    protected FullScreenDialogController mDialogController;

    private Revision mRevision;

    public static final String EXTRA_REVISION = "EXTRA_REVISION";
    public static final String KEY_REVISION = "KEY_REVISION";

    public static Bundle newBundle(Revision revision) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_REVISION, revision);
        return bundle;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.history_detail_dialog_fragment, container, false);

        if (getArguments() != null) {
            mRevision = getArguments().getParcelable(EXTRA_REVISION);
        }

        return rootView;
    }

    @Override
    public void onViewCreated(final FullScreenDialogController controller) {
        mDialogController = controller;
    }

    @Override
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getView() != null) {
            ((DiffView) getView().findViewById(R.id.title)).showDiffs(mRevision.getTitleDiffs(), true);
            ((DiffView) getView().findViewById(R.id.content)).showDiffs(mRevision.getContentDiffs(), false);

            if (mRevision.getTotalAdditions() > 0) {
                TextView totalAdditionsView = getView().findViewById(R.id.diff_additions);
                totalAdditionsView.setText(String.valueOf(mRevision.getTotalAdditions()));
                totalAdditionsView.setVisibility(View.VISIBLE);
            }

            if (mRevision.getTotalDeletions() > 0) {
                TextView totalDeletionsView = getView().findViewById(R.id.diff_deletions);
                totalDeletionsView.setText(String.valueOf(mRevision.getTotalDeletions()));
                totalDeletionsView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public boolean onConfirmClicked(FullScreenDialogController controller) {
        Bundle result = new Bundle();
        result.putParcelable(KEY_REVISION, mRevision);
        controller.confirm(result);
        return true;
    }

    @Override
    public boolean onDismissClicked(FullScreenDialogController controller) {
        controller.dismiss();
        return true;
    }
}
