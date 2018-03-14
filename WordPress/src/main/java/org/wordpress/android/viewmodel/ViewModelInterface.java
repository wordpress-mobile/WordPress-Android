package org.wordpress.android.viewmodel;

import android.os.Bundle;
import android.support.annotation.NonNull;

interface ViewModelInterface {
    boolean isStarted();
    void onStart();
    void readFromBundle(@NonNull Bundle savedInstanceState);
    void writeToBundle(@NonNull Bundle outState);
}
