package org.wordpress.android.fluxc.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.FRIDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.MONDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.SATURDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.SUNDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.THURSDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.TUESDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.WEDNESDAY
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao.BloggingReminders

class BloggingRemindersMapperTest {
    private val mapper = BloggingRemindersMapper()
    private val testSiteId = 1
    private val testHour = 10
    private val testMinute = 0

    @Test
    fun `model mapped to database with all days selected`() {
        val enabledDays = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)
        val fullModel = BloggingRemindersModel(
                testSiteId,
                enabledDays,
                testHour,
                testMinute
        )

        val databaseModel = mapper.toDatabaseModel(fullModel)

        databaseModel.assertDays(enabledDays)
    }

    @Test
    fun `model mapped to database with one day selected`() {
        for (day in Day.values()) {
            val enabledDays = setOf(day)
            val fullModel = BloggingRemindersModel(
                    testSiteId,
                    enabledDays
            )

            val databaseModel = mapper.toDatabaseModel(fullModel)

            databaseModel.assertDays(enabledDays)
        }
    }

    @Test
    fun `model mapped from database with all days selected`() {
        val fullModel = BloggingReminders(
                localSiteId = testSiteId,
                monday = true,
                tuesday = true,
                wednesday = true,
                thursday = true,
                friday = true,
                saturday = true,
                sunday = true,
                hour = testHour,
                minute = testMinute
        )

        val domainModel = mapper.toDomainModel(fullModel)

        domainModel.assertDays(fullModel)
    }

    @Test
    fun `model mapped from database with one day selected`() {
        val fullModel = BloggingReminders(
                localSiteId = testSiteId,
                sunday = true,
                hour = testHour,
                minute = testMinute
        )

        val domainModel = mapper.toDomainModel(fullModel)

        domainModel.assertDays(fullModel)
    }

    private fun BloggingReminders.assertDays(enabledDays: Set<Day>) {
        assertThat(this.localSiteId).isEqualTo(testSiteId)
        assertThat(this.monday).isEqualTo(enabledDays.contains(MONDAY))
        assertThat(this.tuesday).isEqualTo(enabledDays.contains(TUESDAY))
        assertThat(this.wednesday).isEqualTo(enabledDays.contains(WEDNESDAY))
        assertThat(this.thursday).isEqualTo(enabledDays.contains(THURSDAY))
        assertThat(this.friday).isEqualTo(enabledDays.contains(FRIDAY))
        assertThat(this.saturday).isEqualTo(enabledDays.contains(SATURDAY))
        assertThat(this.sunday).isEqualTo(enabledDays.contains(SUNDAY))
        assertThat(this.hour).isEqualTo(testHour)
        assertThat(this.minute).isEqualTo(testMinute)
    }

    private fun BloggingRemindersModel.assertDays(databaseModel: BloggingReminders) {
        assertThat(this.siteId).isEqualTo(testSiteId)
        assertThat(this.enabledDays.contains(MONDAY)).isEqualTo(databaseModel.monday)
        assertThat(this.enabledDays.contains(TUESDAY)).isEqualTo(databaseModel.tuesday)
        assertThat(this.enabledDays.contains(WEDNESDAY)).isEqualTo(databaseModel.wednesday)
        assertThat(this.enabledDays.contains(THURSDAY)).isEqualTo(databaseModel.thursday)
        assertThat(this.enabledDays.contains(FRIDAY)).isEqualTo(databaseModel.friday)
        assertThat(this.enabledDays.contains(SATURDAY)).isEqualTo(databaseModel.saturday)
        assertThat(this.enabledDays.contains(SUNDAY)).isEqualTo(databaseModel.sunday)
        assertThat(this.hour).isEqualTo(testHour)
        assertThat(this.minute).isEqualTo(testMinute)
    }
}
