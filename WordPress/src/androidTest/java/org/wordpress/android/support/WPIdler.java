package org.wordpress.android.support;

import android.support.test.espresso.IdlingResource;
import static junit.framework.Assert.fail;

public abstract class WPIdler implements IdlingResource {
    protected ResourceCallback mResourceCallback;
    protected Boolean mConditionWasMet = false;

    private Integer mNumberOfTries = 100;
    private Integer mRetryInterval = 100;

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public final boolean isIdleNow() {
        if (mConditionWasMet) {
            return true;
        }

        boolean isConditionMet = checkCondition();

        if (isConditionMet) {
            mConditionWasMet = true;
            mResourceCallback.onTransitionToIdle();
        }

        return isConditionMet;
    }

    public abstract boolean checkCondition();

    public void idleUntilReady() {
        idleUntilReady(true);
    }

    public void idleUntilReady(boolean failIfUnsatisfied) {
        Integer tries = 0;

        while (!checkCondition() && ++tries < mNumberOfTries) {
            try {
                Thread.sleep(mRetryInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (tries == mNumberOfTries && failIfUnsatisfied) {
            fail("Unable to continue – expectation wasn't satisfied quickly enough");
        }
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        mResourceCallback = resourceCallback;
    }
}
