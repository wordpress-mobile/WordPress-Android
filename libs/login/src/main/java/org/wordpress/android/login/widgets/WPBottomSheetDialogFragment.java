package org.wordpress.android.login.widgets;

import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

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
        return new BottomSheetDialog(requireContext(), getTheme());
    }

    @Override
    public void onResume() {
        super.onResume();

        Dialog dialog = getDialog();
        if (dialog != null) {
            restrictMaxWidthForDialog(dialog);
        }
    }

    private void restrictMaxWidthForDialog(@NonNull Dialog dialog) {
        Resources resources = dialog.getContext().getResources();
        int dp = (int) resources.getDimension(R.dimen.bottom_sheet_dialog_width);
        // Limit width of bottom sheet on wide screens; non-zero width defined only for large qualifier.
        if (dp > 0) {
            FrameLayout bottomSheetLayout =
                    dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            CoordinatorLayout.LayoutParams coordinatorLayoutParams =
                    (CoordinatorLayout.LayoutParams) bottomSheetLayout.getLayoutParams();
            coordinatorLayoutParams.width = dp;
            bottomSheetLayout.setLayoutParams(coordinatorLayoutParams);

            CoordinatorLayout coordinatorLayout = (CoordinatorLayout) bottomSheetLayout.getParent();
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) coordinatorLayout.getLayoutParams();
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
            coordinatorLayout.setLayoutParams(layoutParams);
        }
    }
}
