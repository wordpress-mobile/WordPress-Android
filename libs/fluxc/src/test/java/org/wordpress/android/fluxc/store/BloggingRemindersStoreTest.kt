package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.BloggingRemindersMapper
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.MONDAY
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao.BloggingReminders
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class BloggingRemindersStoreTest {
    @Mock lateinit var bloggingRemindersDao: BloggingRemindersDao
    @Mock lateinit var mapper: BloggingRemindersMapper
    private lateinit var store: BloggingRemindersStore
    private val siteId = 1
    private val testHour = 10
    private val testMinute = 0

    @Before
    fun setUp() {
        store = BloggingRemindersStore(bloggingRemindersDao, mapper, initCoroutineEngine())
    }

    @Test
    fun `maps items emitted from dao`() = test {
        val dbEntity = BloggingReminders(siteId, monday = true, hour = testHour, minute = testMinute)
        val domainModel = BloggingRemindersModel(siteId, setOf(MONDAY))
        whenever(bloggingRemindersDao.liveGetBySiteId(siteId)).thenReturn(flowOf(dbEntity))
        whenever(mapper.toDomainModel(dbEntity)).thenReturn(domainModel)

        assertThat(store.bloggingRemindersModel(siteId).single()).isEqualTo(domainModel)
    }

    @Test
    fun `maps null value to empty model emitted from dao`() = test {
        whenever(bloggingRemindersDao.liveGetBySiteId(siteId)).thenReturn(flowOf(null))

        assertThat(store.bloggingRemindersModel(siteId).single()).isEqualTo(BloggingRemindersModel(siteId))
    }

    @Test
    fun `maps items stored to dao`() = test {
        val dbEntity = BloggingReminders(siteId, monday = true, hour = testHour, minute = testMinute)
        val domainModel = BloggingRemindersModel(siteId, setOf(MONDAY))
        whenever(mapper.toDatabaseModel(domainModel)).thenReturn(dbEntity)

        store.updateBloggingReminders(domainModel)

        verify(bloggingRemindersDao).insert(dbEntity)
    }

    @Test
    fun `has modified blogging reminders when DAO returns data`() = test {
        val dbEntity = BloggingReminders(siteId, monday = true, hour = testHour, minute = testMinute)
        whenever(bloggingRemindersDao.getBySiteId(siteId)).thenReturn(listOf(dbEntity))

        assertThat(store.hasModifiedBloggingReminders(siteId)).isTrue()
    }

    @Test
    fun `does not have modified blogging reminders when DAO returns no data`() = test {
        whenever(bloggingRemindersDao.getBySiteId(siteId)).thenReturn(listOf())

        assertThat(store.hasModifiedBloggingReminders(siteId)).isFalse()
    }
}
