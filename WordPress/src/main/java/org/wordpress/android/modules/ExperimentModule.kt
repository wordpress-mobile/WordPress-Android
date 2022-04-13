package org.wordpress.android.modules

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import org.wordpress.android.util.experiments.Experiment
import org.wordpress.android.util.experiments.SiteNameABExperiment

@Module
interface ExperimentModule {
    @Multibinds fun experiments(): Set<Experiment>

    // Copy and paste the line below to add a new experiment.
    // @Binds @IntoSet fun exampleExperiment(experiment: ExampleExperiment): Experiment

    @Binds @IntoSet fun siteNameABExperiment(experiment: SiteNameABExperiment): Experiment
}
