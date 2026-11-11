package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class ClockModule {
    @Provides
    @Singleton
    fun provideClock(): Clock {
        return SystemDefaultZoneClock
    }
}

/**
 * The system clock in whatever the default time zone is *now*, read on every call.
 *
 * [Clock.systemDefaultZone] captures the zone when it is created, and this clock is a process-wide
 * singleton, so a process that survives a time zone change (travel, automatic zone) would keep computing
 * dates in the old zone — off by a day for "today". `LocalDate.now()` doesn't have that problem because
 * it builds a fresh clock per call, and Android resets the default zone in running processes when the
 * device zone changes. This keeps that behaviour for code that injects the clock.
 */
private object SystemDefaultZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()

    override fun millis(): Long = System.currentTimeMillis()
}
