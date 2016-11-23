package org.wordpress.android.ui.stats.exceptions;

import java.io.Serializable;

public class StatsError extends Exception implements Serializable {
    public StatsError(String errorMessage) {
        super(errorMessage);
    }
}
