package org.wordpress.android.util.config

import org.mockito.kotlin.mock

class JPDeadlineConfigStub(
    val appConfig: AppConfig = mock(),
    val instance: JPDeadlineConfig = JPDeadlineConfig(appConfig),
)
