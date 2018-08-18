package org.wordpress.android.ui.screenshots.support;

import java.util.function.Supplier;

public class SupplierIdler extends WPScreenshotIdler {
    private Supplier<Boolean> mSupplier;

    public SupplierIdler(Supplier<Boolean> supplier) {
        mSupplier = supplier;
    }

    @Override
    public boolean checkCondition() {
        return mSupplier.get();
    }
}
