package org.wordpress.android.login.widgets;

import android.content.Context;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.wordpress.android.login.R;
import org.wordpress.android.util.DisplayUtils;

public class WPBottomSheetDialog extends BottomSheetDialog {
    public WPBottomSheetDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    public void show() {
        super.show();

        // Limit width of bottom sheet on wide screens; non-zero width defined only for sw600dp.
        int dp = (int) getContext().getResources().getDimension(R.dimen.bottom_sheet_dialog_width);

        if (dp > 0) {
            int px = DisplayUtils.dpToPx(getContext(), dp);
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(getWindow().getAttributes());
            layoutParams.width = px;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            getWindow().setAttributes(layoutParams);
        }
    }
}
