package org.wordpress.android.ui.quickstart;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.store.QuickStartStore;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType;
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogContent;
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogController;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.quickstart.QuickStartAdapter.OnQuickStartAdapterActionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import static org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE;
import static org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW;

public class QuickStartFullScreenDialogFragment extends Fragment implements FullScreenDialogContent,
        OnQuickStartAdapterActionListener {
    private FullScreenDialogController mDialogController;
    private QuickStartAdapter mQuickStartAdapter;

    public static final String KEY_COMPLETED_TASKS_LIST_EXPANDED = "completed_tasks_list_expanded";
    public static final String EXTRA_TYPE = "EXTRA_TYPE";
    public static final String RESULT_TASK = "RESULT_TASK";

    private QuickStartTaskType mTasksType = CUSTOMIZE;

    @Inject protected QuickStartStore mQuickStartStore;

    public static Bundle newBundle(QuickStartTaskType type) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_TYPE, type);
        return bundle;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) requireActivity().getApplication()).component().inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.quick_start_dialog_fragment, container, false);

        if (getArguments() != null) {
            mTasksType = (QuickStartTaskType) getArguments().getSerializable(EXTRA_TYPE);
        }

        RecyclerView listUncompleted = rootView.findViewById(R.id.list_uncompleted);
        RecyclerView listCompleted = rootView.findViewById(R.id.list_completed);
        List<QuickStartTask> tasksUncompleted = new ArrayList<>();
        List<QuickStartTask> tasksCompleted = new ArrayList<>();
        int site = AppPrefs.getSelectedSite();

        switch (Objects.requireNonNull(mTasksType)) {
            case CUSTOMIZE:
                tasksUncompleted.addAll(mQuickStartStore.getUncompletedTasksByType(site, CUSTOMIZE));
                tasksCompleted.addAll(mQuickStartStore.getCompletedTasksByType(site, CUSTOMIZE));
                break;
            case GROW:
                tasksUncompleted.addAll(mQuickStartStore.getUncompletedTasksByType(site, GROW));
                tasksCompleted.addAll(mQuickStartStore.getCompletedTasksByType(site, GROW));
                break;
            case UNKNOWN:
                tasksUncompleted.addAll(mQuickStartStore.getUncompletedTasksByType(site, CUSTOMIZE));
                tasksCompleted.addAll(mQuickStartStore.getCompletedTasksByType(site, CUSTOMIZE));
                break;
        }

        boolean isCompletedTasksListExpanded = savedInstanceState != null
                                               && savedInstanceState.getBoolean(KEY_COMPLETED_TASKS_LIST_EXPANDED);

        QuickStartAdapter quickStartAdapter = new QuickStartAdapter(
                requireContext(),
                tasksUncompleted,
                new ArrayList<QuickStartTask>(),
                isCompletedTasksListExpanded);
        quickStartAdapter.setOnTaskTappedListener(QuickStartFullScreenDialogFragment.this);
        listUncompleted.setLayoutManager(new LinearLayoutManager(requireContext()));
        listUncompleted.setAdapter(quickStartAdapter);

        mQuickStartAdapter = new QuickStartAdapter(
                requireContext(),
                new ArrayList<QuickStartTask>(),
                tasksCompleted,
                isCompletedTasksListExpanded);
        mQuickStartAdapter.setOnTaskTappedListener(QuickStartFullScreenDialogFragment.this);
        listCompleted.setLayoutManager(new LinearLayoutManager(requireContext()));
        listCompleted.setAdapter(mQuickStartAdapter);

        return rootView;
    }

    @Override
    public void onViewCreated(final FullScreenDialogController controller) {
        mDialogController = controller;
    }

    @Override
    public boolean onConfirmClicked(FullScreenDialogController controller) {
        return true;
    }

    @Override
    public boolean onDismissClicked(FullScreenDialogController controller) {
        controller.dismiss();
        return true;
    }

    @Override
    public void onTaskTapped(QuickStartTask task) {
        Bundle result = new Bundle();
        result.putSerializable(RESULT_TASK, task);
        mDialogController.confirm(result);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mQuickStartAdapter != null) {
            outState.putBoolean(KEY_COMPLETED_TASKS_LIST_EXPANDED, mQuickStartAdapter.isCompletedTasksListExpanded());
        }
    }

    @Override
    public void onSkipTaskTapped(QuickStartTask task) {
        // TODO: Quick Start - Add analytics for skipping task.
        mQuickStartStore.setDoneTask(AppPrefs.getSelectedSite(), task, true);
        if (mQuickStartAdapter != null) {
            int site = AppPrefs.getSelectedSite();
            mQuickStartAdapter.updateContent(
                    mQuickStartStore.getUncompletedTasksByType(site, mTasksType),
                    mQuickStartStore.getCompletedTasksByType(site, mTasksType),
                    task);
        }
    }
}
