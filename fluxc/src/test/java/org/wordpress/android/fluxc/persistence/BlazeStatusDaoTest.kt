package org.wordpress.android.fluxc.persistence

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.persistence.blaze.BlazeStatusDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeStatusDao.BlazeStatus
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class BlazeStatusDaoTest {
    private lateinit var dao: BlazeStatusDao
    private lateinit var db: WPAndroidDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().context
        db = Room.inMemoryDatabaseBuilder(
            context, WPAndroidDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.blazeStatusDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `when insert followed by update, then valid status is returned`(): Unit = runBlocking {
        // when
        var blazeStatus = generateBlazeStatus(isEligible = true)
        dao.insert(blazeStatus)

        // then
        var observedStatus = dao.getBlazeStatus(defaultSiteId).first().first()
        assertThat(observedStatus).isEqualTo(blazeStatus)

        // when
        blazeStatus = blazeStatus.copy(isEligible = false)
        dao.insert(blazeStatus)

        // then
        observedStatus = dao.getBlazeStatus(defaultSiteId).first().first()
        assertThat(observedStatus).isEqualTo(blazeStatus)
    }

    @Test
    fun `when clear is requested, then all rows are deleted`(): Unit = runBlocking {
        // when
        val status1 = generateBlazeStatus(siteId = 1, isEligible = true)
        dao.insert(status1)

        val status2 = generateBlazeStatus(siteId = 2, isEligible = true)
        dao.insert(status2)

        val status3 = generateBlazeStatus(siteId = 3, isEligible = true)
        dao.insert(status3)

        // then
       dao.clear()

        // when

        assertEmptyResult(dao.getBlazeStatus(1).first())
        assertEmptyResult(dao.getBlazeStatus(2).first())
        assertEmptyResult(dao.getBlazeStatus(3).first())
    }

    @Test
    fun `given site not in the db, when request is made, empty list is returned`(): Unit = runBlocking {
        // when
        val emptyList = emptyList<BlazeStatus>()

        // then
        val observedStatus = dao.getBlazeStatus(defaultSiteId).first()

        // when
        assertThat(observedStatus).isEqualTo(emptyList)
    }


    private fun assertEmptyResult(blazeStatus: List<BlazeStatus>?) {
        assertThat(blazeStatus).isNotNull
        assertThat(blazeStatus).isEmpty()
    }

    private fun generateBlazeStatus(siteId: Long = defaultSiteId, isEligible: Boolean) = BlazeStatus(
        siteId = siteId,
        isEligible = isEligible
    )

    companion object {
        private const val defaultSiteId = 1234L
    }
}