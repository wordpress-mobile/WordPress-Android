package org.wordpress.android.modules

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import org.wordpress.android.util.experiments.Experiment

@InstallIn(SingletonComponent::class)
@Module
interface ExperimentModule {
    @Multibinds
    fun experiments(): Set<Experiment>

    // Copy and paste the line below to add a new experiment.
    // @Binds @IntoSet fun exampleExperiment(experiment: ExampleExperiment): Experiment
}
