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
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsUtils
import org.wordpress.android.fluxc.persistence.bloggingprompts.BloggingPromptsDao
import org.wordpress.android.fluxc.persistence.bloggingprompts.BloggingPromptsDao.BloggingPromptEntity
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class BloggingPromptsDaoTest {
    private lateinit var promptsDao: BloggingPromptsDao
    private lateinit var db: WPAndroidDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().context
        db = Room.inMemoryDatabaseBuilder(
            context, WPAndroidDatabase::class.java
        ).allowMainThreadQueries().build()
        promptsDao = db.bloggingPromptsDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `test prompt insert and update`(): Unit = runBlocking {
        // when
        var prompt = generateBloggingPrompt()
        promptsDao.insert(listOf(prompt))

        // then
        var observedProduct = promptsDao.getPrompt(localSideId, prompt.id).first().first()
        assertThat(observedProduct).isEqualTo(prompt)

        // when
        prompt = observedProduct.copy(text = "updated text")
        promptsDao.insert(listOf(prompt))

        // then
        observedProduct = promptsDao.getPrompt(localSideId, prompt.id).first().first()
        assertThat(observedProduct).isEqualTo(prompt)
    }

    @Test
    fun `test prompt insert and update with site Id`(): Unit = runBlocking {
        // when
        var promptEntity = generateBloggingPrompt()

        var prompt = promptEntity.toBloggingPrompt()
        promptsDao.insertForSite(localSideId, listOf(prompt))

        // then
        var observedProduct = promptsDao.getPrompt(localSideId, prompt.id).first().first()
        assertThat(observedProduct).isEqualTo(promptEntity)

        // when
        promptEntity = promptEntity.copy(text = "updated text")
        prompt = promptEntity.toBloggingPrompt()
        promptsDao.insertForSite(localSideId, listOf(prompt))

        // then
        observedProduct = promptsDao.getPrompt(localSideId, prompt.id).first().first()
        assertThat(observedProduct).isEqualTo(promptEntity)
    }

    @Test
    fun `getPromptForDate returns correct prompt based on the date`(): Unit = runBlocking {
        // when
        val prompt1 = generateBloggingPrompt().copy(id = 1, date = BloggingPromptsUtils.stringToDate("2022-05-01"))
            .toBloggingPrompt()
        val prompt2 = generateBloggingPrompt().copy(id = 2, date = BloggingPromptsUtils.stringToDate("2015-04-20"))
            .toBloggingPrompt()
        val prompt3 = generateBloggingPrompt().copy(id = 3, date = BloggingPromptsUtils.stringToDate("2015-03-20"))
            .toBloggingPrompt()

        promptsDao.insertForSite(localSideId, listOf(prompt1, prompt2, prompt3))

        // then
        val prompts = promptsDao.getPromptForDate(
            localSideId,
            BloggingPromptsUtils.stringToDate("2015-04-20")
        ).first()

        val specificPrompt = prompts.first()
        assertThat(specificPrompt).isNotNull
        assertThat(specificPrompt.toBloggingPrompt()).isEqualTo(prompt2)
    }

    @Test
    fun `getAllPrompts returns all prompts`(): Unit = runBlocking {
        // when
        val prompt1 = generateBloggingPrompt().copy(id = 1, date = BloggingPromptsUtils.stringToDate("2022-05-01"))
            .toBloggingPrompt()
        val prompt2 = generateBloggingPrompt().copy(id = 2, date = BloggingPromptsUtils.stringToDate("2015-04-20"))
            .toBloggingPrompt()
        val prompt3 = generateBloggingPrompt().copy(id = 3, date = BloggingPromptsUtils.stringToDate("2015-03-20"))
            .toBloggingPrompt()

        promptsDao.insertForSite(localSideId, listOf(prompt1, prompt2, prompt3))

        // then
        val prompts = promptsDao.getAllPrompts(
            localSideId
        ).first()

        assertThat(prompts).isNotNull
        assertThat(prompts.map { it.toBloggingPrompt() }).isEqualTo(listOf(prompt1, prompt2, prompt3))
    }


    @Test
    fun `clear removes all prompts`(): Unit = runBlocking {
        // when
        val prompt1 = generateBloggingPrompt().copy(id = 1, date = BloggingPromptsUtils.stringToDate("2022-05-01"))
            .toBloggingPrompt()
        val prompt2 = generateBloggingPrompt().copy(id = 2, date = BloggingPromptsUtils.stringToDate("2015-04-20"))
            .toBloggingPrompt()
        val prompt3 = generateBloggingPrompt().copy(id = 3, date = BloggingPromptsUtils.stringToDate("2015-03-20"))
            .toBloggingPrompt()

        promptsDao.insertForSite(localSideId, listOf(prompt1, prompt2, prompt3))

        // then
        promptsDao.clear()

        val prompts = promptsDao.getAllPrompts(
            localSideId
        ).first()

        assertThat(prompts).isNotNull
        assertThat(prompts).isEmpty()
    }

    @Test
    fun `BloggingPromptEntity correctly converts to BloggingPromptModel`(): Unit = runBlocking {
        val promptEntity = generateBloggingPrompt()
        val prompt = promptEntity.toBloggingPrompt()

        assertThat(promptEntity.id).isEqualTo(prompt.id)
        assertThat(promptEntity.text).isEqualTo(prompt.text)
        assertThat(promptEntity.title).isEqualTo(prompt.title)
        assertThat(promptEntity.content).isEqualTo(prompt.content)
        assertThat(promptEntity.date).isEqualTo(prompt.date)
        assertThat(promptEntity.isAnswered).isEqualTo(prompt.isAnswered)
        assertThat(promptEntity.respondentsCount).isEqualTo(prompt.respondentsCount)
        assertThat(promptEntity.respondentsAvatars).isEqualTo(prompt.respondentsAvatars)
    }

    @Test
    fun `BloggingPromptModel correctly converts to BloggingPromptEntity`(): Unit = runBlocking {
        val promptEntity = generateBloggingPrompt()
        val prompt = promptEntity.toBloggingPrompt()

        val convertedEntity = BloggingPromptEntity.from(localSideId, prompt)

        assertThat(promptEntity).isEqualTo(convertedEntity)
    }

    companion object {
        private const val localSideId = 1234

        private fun generateBloggingPrompt() = BloggingPromptEntity(
            id = 1,
            siteLocalId = localSideId,
            text = "Cast the movie of your life.",
            title = "Prompt Title",
            content = "content of the prompt",
            date = BloggingPromptsUtils.stringToDate("2015-01-12"),
            attribution = "dayone",
            isAnswered = false,
            respondentsCount = 5,
            respondentsAvatars = emptyList()
        )
    }
}
