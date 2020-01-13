package org.wordpress.android.login.widgets;

import android.app.Dialog;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.wordpress.android.login.R;

public class WPBottomSheetDialogFragment extends BottomSheetDialogFragment {
    @Override
    public int getTheme() {
        return R.style.LoginTheme_BottomSheetDialogStyle;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setRetainInstance(true);
        return new BottomSheetDialog(requireContext(), getTheme());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getDialog() != null) {
            // Limit width of bottom sheet on wide screens; non-zero width defined only for large qualifier.
            int dp = (int) getDialog().getContext().getResources().getDimension(R.dimen.bottom_sheet_dialog_width);

            if (dp > 0) {
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(getDialog().getWindow() != null ? getDialog().getWindow().getAttributes() : null);
                layoutParams.width = dp;
                layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
                getDialog().getWindow().setAttributes(layoutParams);
            }
        }
    }
}
