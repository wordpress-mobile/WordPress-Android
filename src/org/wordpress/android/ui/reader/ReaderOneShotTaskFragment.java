package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.util.AppLog;

/**
 * worker fragment to safely perform reader-related tasks from within activities without
 * the side effects of configuration changes - activities with use this fragment MUST
 * implement ReaderTaskCallbacks
 *
 * http://www.androiddesignpatterns.com/2013/04/retaining-objects-across-config-changes.html
 *
 * note that this can easily be expanded to perform additional tasks
 */
public class ReaderOneShotTaskFragment extends Fragment {

    private static final String ARG_TASK_TYPE = "task_type";
    static enum ReaderTaskType { UPDATE_TAGS }

    static enum ReaderTaskResult {
        HAS_CHANGES,
        NO_CHANGES,
        FAILED;

        static ReaderTaskResult fromUpdateResult(UpdateResult updateResult) {
            switch (updateResult) {
                case CHANGED:
                    return ReaderTaskResult.HAS_CHANGES;
                case FAILED:
                    return ReaderTaskResult.FAILED;
                default:
                    return ReaderTaskResult.NO_CHANGES;
            }
        }
    }

    static interface ReaderTaskCallbacks {
        void onPreExecuteTask(ReaderTaskType task);
        void onPostExecuteTask(ReaderTaskType task, ReaderTaskResult taskResult);
    }

    private ReaderTaskCallbacks mCallbacks;
    private ReaderTaskType mTaskType;

    static ReaderOneShotTaskFragment newInstance(ReaderTaskType taskType) {
        if (taskType == null) {
            throw new IllegalArgumentException("task type cannot be null");

        }

        AppLog.d(AppLog.T.READER, "reader task fragment > newInstance " + taskType.toString());

        Bundle args = new Bundle();
        args.putSerializable(ARG_TASK_TYPE, taskType);

        ReaderOneShotTaskFragment fragment = new ReaderOneShotTaskFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        if (args != null && args.containsKey(ARG_TASK_TYPE)) {
            mTaskType = (ReaderTaskType) args.getSerializable(ARG_TASK_TYPE);
        }
    }

    /**
     * hold a reference to the parent Activity so we can report the
     * task's current progress and results. The Android framework
     * will pass us a reference to the newly created Activity after
     * each configuration change.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (ReaderTaskCallbacks) activity;
    }

    /**
     * this method will only be called once when the retained
     * fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // retain this fragment across configuration changes.
        setRetainInstance(true);

        // execute the task.
        if (mTaskType != null) {
            if (mCallbacks != null) {
                mCallbacks.onPreExecuteTask(mTaskType);
            }

            switch (mTaskType) {
                case UPDATE_TAGS:
                    updateTags();
                    break;
            }
        }
    }

    /**
     * set the callback to null so we don't accidentally leak the
     * Activity instance.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }


    private void updateTags() {
        ReaderActions.UpdateResultListener listener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(UpdateResult result) {
                if (mCallbacks != null) {
                    mCallbacks.onPostExecuteTask(
                            ReaderTaskType.UPDATE_TAGS,
                            ReaderTaskResult.fromUpdateResult(result));
                }
            }
        };

        ReaderTagActions.updateTags(listener);
    }
}
