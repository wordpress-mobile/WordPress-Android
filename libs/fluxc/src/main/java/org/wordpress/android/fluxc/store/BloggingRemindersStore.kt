package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.BloggingRemindersMapper
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.persistence.BloggingRemindersRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BloggingRemindersStore
@Inject constructor(private val repository: BloggingRemindersRepository, private val mapper: BloggingRemindersMapper) {
    fun bloggingRemindersModel(siteId: Int): Flow<BloggingRemindersModel> {
        return repository.bloggingRemindersModel(siteId).map(mapper::toDomainModel)
    }

    fun updateBloggingReminders(model: BloggingRemindersModel) {
        repository.updateBloggingReminders(mapper.toDatabaseModel(model))
    }
}
