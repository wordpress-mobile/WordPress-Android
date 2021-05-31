package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.BloggingRemindersMapper
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BloggingRemindersStore
@Inject constructor(
    private val bloggingRemindersDao: BloggingRemindersDao,
    private val mapper: BloggingRemindersMapper
) {
    fun bloggingRemindersModel(siteId: Int): Flow<BloggingRemindersModel> {
        return bloggingRemindersDao.getBySiteId(siteId).map {
            it?.let { dbModel -> mapper.toDomainModel(dbModel) } ?: BloggingRemindersModel(siteId)
        }
    }

    fun updateBloggingReminders(model: BloggingRemindersModel) {
        bloggingRemindersDao.insert(mapper.toDatabaseModel(model))
    }
}
