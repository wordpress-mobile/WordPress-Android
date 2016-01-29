package org.wordpress.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;

/**
 * Used to store and transport dual pane content state.
 */
public class DualPaneContentState implements Parcelable {

    public final static String KEY = "dual_pane_fragment_state";

    private final static String ORIGINAL_INTENT_KEY = "original_intent";
    private final static String FRAGMENT_CLASS_KEY = "fragment_class";
    private final static String FRAGMENT_STATE_KEY = "fragment_state";

    private Bundle mState;

    public DualPaneContentState(Intent originalIntent, Class fragmentClass, Fragment.SavedState fragmentState) {
        //we are using bundle to transport both parcelable and serializable objects
        Bundle bundle = new Bundle();
        bundle.putParcelable(ORIGINAL_INTENT_KEY, originalIntent);
        bundle.putSerializable(FRAGMENT_CLASS_KEY, fragmentClass);
        bundle.putParcelable(FRAGMENT_STATE_KEY, fragmentState);

        mState = bundle;
    }

    public void setFragmentState(Fragment.SavedState fragmentState) {
        mState.remove(FRAGMENT_STATE_KEY);

        mState.putParcelable(FRAGMENT_STATE_KEY, fragmentState);
    }

    private Bundle getState() {
        return mState;
    }

    public Intent getOriginalIntent() {
        return getState().getParcelable(ORIGINAL_INTENT_KEY);
    }

    public Class getFragmentClass() {
        return (Class) getState().getSerializable(FRAGMENT_CLASS_KEY);
    }

    public Fragment.SavedState getFragmentState() {
        return getState().getParcelable(FRAGMENT_STATE_KEY);
    }

    public DualPaneContentState(Parcel in) {
        mState = in.readBundle(DualPaneContentState.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(mState);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<DualPaneContentState> CREATOR = new Parcelable.Creator<DualPaneContentState>() {
        @Override
        public DualPaneContentState createFromParcel(Parcel in) {
            return new DualPaneContentState(in);
        }

        @Override
        public DualPaneContentState[] newArray(int size) {
            return new DualPaneContentState[size];
        }
    };

    public boolean isActivityIntentAvailable() {
        return getOriginalIntent() != null;
    }
}
