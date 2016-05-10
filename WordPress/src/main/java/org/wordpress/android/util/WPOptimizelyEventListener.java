package org.wordpress.android.util;

import android.os.Bundle;

import com.optimizely.Optimizely;
import com.optimizely.integration.OptimizelyEventListener;
import com.optimizely.integration.OptimizelyExperimentData;

import org.wordpress.android.analytics.AnalyticsTracker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WPOptimizelyEventListener implements OptimizelyEventListener {
    public static final String ABTEST_NAME = "abtest_name";
    public static final String ABTEST_VARIATION = "abtest_variation";

    @Override
    public void onOptimizelyStarted() {

    }

    @Override
    public void onOptimizelyFailedToStart(String s) {

    }

    @Override
    public void onOptimizelyExperimentVisited(OptimizelyExperimentData optimizelyExperimentData) {

    }

    @Override
    public void onOptimizelyExperimentViewed(OptimizelyExperimentData optimizelyExperimentData) {
        Map<String, OptimizelyExperimentData> visitedExperiments = Optimizely.getVisitedExperiments();

        for (String experimentId : visitedExperiments.keySet()) {
            OptimizelyExperimentData experiment = visitedExperiments.get(experimentId);
            HashMap<String, String> abTestProperties = new HashMap<>();
            abTestProperties.put(ABTEST_NAME, experiment.experimentName);
            abTestProperties.put(ABTEST_VARIATION, experiment.variationName);
            AnalyticsTracker.track(AnalyticsTracker.Stat.ABTEST_START, abTestProperties);
        }
    }

    @Override
    public void onOptimizelyEditorEnabled() {

    }

    @Override
    public void onOptimizelyDataFileLoaded() {

    }

    @Override
    public void onGoalTriggered(String s, List<OptimizelyExperimentData> list) {

    }

    @Override
    public void onMessage(String s, String s1, Bundle bundle) {

    }
}
