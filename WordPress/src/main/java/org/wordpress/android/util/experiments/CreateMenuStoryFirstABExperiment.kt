package org.wordpress.android.util.experiments

import javax.inject.Inject

class CreateMenuStoryFirstABExperiment
@Inject constructor(
    exPlat: ExPlat
) : Experiment(
        name = "wpandroid_create_menu_story_first",
        exPlat
)
