package org.wordpress.android.fluxc.persistance

import app.cash.turbine.test
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.persistence.BloggingRemindersRepository
import org.wordpress.android.fluxc.persistence.BloggingRemindersSqlUtils
import org.wordpress.android.fluxc.persistence.BloggingRemindersSqlUtils.BloggingReminders
import org.wordpress.android.fluxc.test
import kotlin.time.ExperimentalTime

@RunWith(MockitoJUnitRunner::class)
class BloggingRemindersRepositoryTest {
    @Mock lateinit var sqlUtils: BloggingRemindersSqlUtils
    private lateinit var repository: BloggingRemindersRepository
    private val siteId = 1

    @Test
    fun `initialized blogging reminders from database on start`() = test {
        val model = BloggingReminders(localSiteId = siteId, monday = true)
        whenever(sqlUtils.getBloggingReminders()).thenReturn(listOf(model))
        repository = BloggingRemindersRepository(sqlUtils)

        val result = repository.bloggingRemindersModel(siteId).first()

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns empty model when no data for given site`() = test {
        whenever(sqlUtils.getBloggingReminders()).thenReturn(listOf())
        repository = BloggingRemindersRepository(sqlUtils)

        val result = repository.bloggingRemindersModel(siteId).first()

        assertThat(result.localSiteId).isEqualTo(siteId)
        assertThat(result.monday).isFalse()
        assertThat(result.tuesday).isFalse()
        assertThat(result.wednesday).isFalse()
        assertThat(result.thursday).isFalse()
        assertThat(result.friday).isFalse()
        assertThat(result.saturday).isFalse()
        assertThat(result.sunday).isFalse()
    }

    @ExperimentalTime
    @Test
    fun `updates blogging reminders from database on insert`() = test {
        val model = BloggingReminders(localSiteId = siteId, monday = true)
        val updatedModel = BloggingReminders(localSiteId = siteId, tuesday = true)
        whenever(sqlUtils.getBloggingReminders()).thenReturn(listOf(model), listOf(updatedModel))
        repository = BloggingRemindersRepository(sqlUtils)

        repository.bloggingRemindersModel(siteId).test {
            assertThat(expectItem()).isEqualTo(model)
            repository.updateBloggingReminders(updatedModel)
            assertThat(expectItem()).isEqualTo(updatedModel)
        }
    }

    @Test
    fun `update reminders updates database`() {
        repository = BloggingRemindersRepository(sqlUtils)

        val model = BloggingReminders(localSiteId = siteId, monday = true)

        repository.updateBloggingReminders(model)

        verify(sqlUtils).replaceBloggingReminder(model)
    }
}

