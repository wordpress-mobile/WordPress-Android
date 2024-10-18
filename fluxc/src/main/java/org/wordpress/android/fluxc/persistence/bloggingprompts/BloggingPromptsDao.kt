package org.wordpress.android.fluxc.persistence.bloggingprompts

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.persistence.coverters.BloggingPromptDateConverter
import java.util.Date

@Dao
@TypeConverters(BloggingPromptDateConverter::class)
abstract class BloggingPromptsDao {
    @Query("SELECT * FROM BloggingPrompts WHERE id = :promptId AND siteLocalId = :siteLocalId")
    abstract fun getPrompt(siteLocalId: Int, promptId: Int): Flow<List<BloggingPromptEntity>>

    @Query("SELECT * FROM BloggingPrompts WHERE date = :date AND siteLocalId = :siteLocalId")
    @TypeConverters(BloggingPromptDateConverter::class)
    abstract fun getPromptForDate(siteLocalId: Int, date: Date): Flow<List<BloggingPromptEntity>>

    @Query("SELECT * FROM BloggingPrompts WHERE siteLocalId = :siteLocalId")
    abstract fun getAllPrompts(siteLocalId: Int): Flow<List<BloggingPromptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(prompts: List<BloggingPromptEntity>)

    suspend fun insertForSite(siteLocalId: Int, prompts: List<BloggingPromptModel>) {
        insert(prompts.map { BloggingPromptEntity.from(siteLocalId, it) })
    }

    @Query("DELETE FROM BloggingPrompts")
    abstract fun clear()

    @Entity(
        tableName = "BloggingPrompts",
        primaryKeys = ["date"]
    )
    @TypeConverters(BloggingPromptDateConverter::class)
    data class BloggingPromptEntity(
        val id: Int,
        val siteLocalId: Int,
        val text: String,
        val date: Date,
        val isAnswered: Boolean,
        val respondentsCount: Int,
        val attribution: String,
        val respondentsAvatars: List<String>,
        val answeredLink: String,
        val bloganuaryId: String? = null,
    ) {
        fun toBloggingPrompt() = BloggingPromptModel(
            id = id,
            text = text,
            date = date,
            isAnswered = isAnswered,
            attribution = attribution,
            respondentsCount = respondentsCount,
            respondentsAvatarUrls = respondentsAvatars,
            answeredLink = answeredLink,
            bloganuaryId = bloganuaryId,
        )

        companion object {
            fun from(
                siteLocalId: Int,
                prompt: BloggingPromptModel
            ) = BloggingPromptEntity(
                id = prompt.id,
                siteLocalId = siteLocalId,
                text = prompt.text,
                date = prompt.date,
                isAnswered = prompt.isAnswered,
                respondentsCount = prompt.respondentsCount,
                attribution = prompt.attribution,
                respondentsAvatars = prompt.respondentsAvatarUrls,
                answeredLink = prompt.answeredLink,
                bloganuaryId = prompt.bloganuaryId,
            )
        }
    }
}
