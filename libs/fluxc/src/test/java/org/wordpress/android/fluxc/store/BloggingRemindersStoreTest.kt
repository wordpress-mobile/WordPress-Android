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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao.BloggingReminders
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class BloggingRemindersStoreTest {
    @Mock lateinit var bloggingRemindersDao: BloggingRemindersDao
    @Mock lateinit var mapper: BloggingRemindersMapper
    @Mock lateinit var siteStore: SiteStore
    private lateinit var store: BloggingRemindersStore
    private val siteId = 1
    private val secondSiteId = 2
    private val testHour = 10
    private val testMinute = 0

    @Before
    fun setUp() {
        store = BloggingRemindersStore(
            bloggingRemindersDao,
            mapper,
            siteStore,
            initCoroutineEngine(),
        )
    }

    @Test
    fun `maps all items emitted from dao`() = test {
        val dbEntity1 = BloggingReminders(
            siteId,
            monday = true,
            hour = testHour,
            minute = testMinute
        )
        val dbEntity2 = BloggingReminders(
            secondSiteId,
            monday = true,
            hour = testHour,
            minute = testMinute
        )
        val domainModel1 = BloggingRemindersModel(siteId, setOf(MONDAY))
        val domainModel2 = BloggingRemindersModel(secondSiteId, setOf(MONDAY))
        whenever(bloggingRemindersDao.getAll()).thenReturn(flowOf(listOf(dbEntity1, dbEntity2)))
        whenever(mapper.toDomainModel(dbEntity1)).thenReturn(domainModel1)
        whenever(mapper.toDomainModel(dbEntity2)).thenReturn(domainModel2)

        assertThat(store.getAll().single()).isEqualTo(listOf(domainModel1, domainModel2))
    }

    @Test
    fun `maps single item emitted from dao`() = test {
        val dbEntity = BloggingReminders(
            siteId,
            monday = true,
            hour = testHour,
            minute = testMinute,
            isPromptRemindersOptedIn = false,
        )
        val domainModel = BloggingRemindersModel(
            siteId,
            setOf(MONDAY),
            isPromptsCardEnabled = false
        )
        whenever(bloggingRemindersDao.liveGetBySiteId(siteId)).thenReturn(flowOf(dbEntity))
        whenever(mapper.toDomainModel(dbEntity)).thenReturn(domainModel)

        assertThat(store.bloggingRemindersModel(siteId).single()).isEqualTo(domainModel)
    }

    @Test
    fun `maps null value to empty model emitted from dao`() = test {
        whenever(bloggingRemindersDao.liveGetBySiteId(siteId)).thenReturn(flowOf(null))
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(
            SiteModel().apply { setIsPotentialBloggingSite(false) }
        )

        assertThat(store.bloggingRemindersModel(siteId).single()).isEqualTo(
            BloggingRemindersModel(
                siteId,
                isPromptsCardEnabled = false,
            )
        )
    }

    @Test
    fun `maps items stored to dao`() = test {
        val dbEntity = BloggingReminders(
            siteId,
            monday = true,
            hour = testHour,
            minute = testMinute
        )
        val domainModel = BloggingRemindersModel(siteId, setOf(MONDAY))
        whenever(mapper.toDatabaseModel(domainModel)).thenReturn(dbEntity)

        store.updateBloggingReminders(domainModel)

        verify(bloggingRemindersDao).insert(dbEntity)
    }

    @Test
    fun `has modified blogging reminders when DAO returns data`() = test {
        val dbEntity = BloggingReminders(
            siteId,
            monday = true,
            hour = testHour,
            minute = testMinute
        )
        whenever(bloggingRemindersDao.getBySiteId(siteId)).thenReturn(listOf(dbEntity))

        assertThat(store.hasModifiedBloggingReminders(siteId)).isTrue
    }

    @Test
    fun `does not have modified blogging reminders when DAO returns no data`() = test {
        whenever(bloggingRemindersDao.getBySiteId(siteId)).thenReturn(listOf())

        assertThat(store.hasModifiedBloggingReminders(siteId)).isFalse
    }
}
