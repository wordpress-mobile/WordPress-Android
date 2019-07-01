package org.wordpress.android.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.wordpress.android.R;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.CollapseFullScreenDialogContent;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.CollapseFullScreenDialogController;
import org.wordpress.android.widgets.SuggestionAutoCompleteText;

public class CommentFullScreenDialogFragment extends Fragment implements CollapseFullScreenDialogContent {
    public static final String RESULT_REPLY = "RESULT_REPLY";
    public static final String TAG = CommentFullScreenDialogFragment.class.getSimpleName();

    private static final String EXTRA_REPLY = "EXTRA_REPLY";

    private CollapseFullScreenDialogController mDialogController;
    private SuggestionAutoCompleteText mReply;

    public static Bundle newBundle(String reply) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_REPLY, reply);
        return bundle;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.comment_dialog_fragment, container, false);
        mReply = layout.findViewById(R.id.edit_comment_expand);
        mReply.requestFocus();
        mReply.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mDialogController.setConfirmEnabled(s.length() > 0);
            }
        });

        if (getArguments() != null) {
            mReply.setText(getArguments().getString(EXTRA_REPLY));
        }

        return layout;
    }

    @Override
    public boolean onCollapseClicked(CollapseFullScreenDialogController controller) {
        Bundle result = new Bundle();
        result.putString(RESULT_REPLY, mReply.getText().toString());
        controller.collapse(result);
        return true;
    }

    @Override
    public boolean onConfirmClicked(CollapseFullScreenDialogController controller) {
        Bundle result = new Bundle();
        result.putString(RESULT_REPLY, mReply.getText().toString());
        controller.confirm(result);
        return true;
    }

    @Override
    public void onViewCreated(final CollapseFullScreenDialogController controller) {
        mDialogController = controller;
    }
}
