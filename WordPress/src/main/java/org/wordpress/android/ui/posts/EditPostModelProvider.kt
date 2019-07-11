package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostLocation
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.UNKNOWN
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditPostModelProvider
@Inject constructor() {
    var postModel: PostModel? = null
        private set

    private val _livePostModel = MutableLiveData<PostModel>()
    val livePostModel: LiveData<PostModel> = _livePostModel

    fun hasPost(): Boolean = postModel != null

    fun set(postModel: PostModel) {
        this.postModel = postModel
        _livePostModel.postValue(postModel)
    }

    fun setStatus(postStatus: PostStatus) {
        postModel?.let {
            it.status = postStatus.toString()
            _livePostModel.postValue(it)
        }
    }

    fun getStatus(): PostStatus {
        return postModel?.let { PostStatus.fromPost(postModel) } ?: UNKNOWN
    }

    fun setTitle(title: String?) {
        if (title != null) {
            postModel?.let {
                it.title = title
                _livePostModel.postValue(it)
            }
        }
    }

    fun setContent(content: String?) {
        if (content != null) {
            postModel?.let {
                it.content = content
                _livePostModel.postValue(it)
            }
        }
    }

    fun setIsLocallyChanged(isLocallyChanged: Boolean) {
        postModel?.let {
            it.setIsLocallyChanged(isLocallyChanged)
            _livePostModel.postValue(it)
        }
    }

    fun setDateLocallyChanged(mDateLocallyChanged: String) {
        postModel?.let {
            it.dateLocallyChanged = mDateLocallyChanged
            _livePostModel.postValue(it)
        }
    }

    fun setDateCreated(mDateCreated: String) {
        postModel?.let {
            it.dateCreated = mDateCreated
            _livePostModel.postValue(it)
        }
    }

    fun setFeaturedImageId(featuredImageId: Long) {
        postModel?.let {
            it.featuredImageId = featuredImageId
            _livePostModel.postValue(it)
        }
    }

    fun setPostFormat(postFormat: String) {
        postModel?.let {
            it.postFormat = postFormat
            _livePostModel.postValue(it)
        }
    }

    fun setExcerpt(excerpt: String) {
        postModel?.let {
            it.excerpt = excerpt
            _livePostModel.postValue(it)
        }
    }

    fun setSlug(slug: String) {
        postModel?.let {
            it.slug = slug
            _livePostModel.postValue(it)
        }
    }

    fun setPassword(password: String) {
        postModel?.let {
            it.password = password
            _livePostModel.postValue(it)
        }
    }

    fun setCategoryIdList(categoryIdList: List<Long>) {
        postModel?.let {
            it.categoryIdList = categoryIdList
            _livePostModel.postValue(it)
        }
    }

    fun setTagNameList(tagNameList: List<String>?) {
        postModel?.let {
            it.setTagNameList(tagNameList)
            _livePostModel.postValue(it)
        }
    }

    fun setLocation(postLocation: PostLocation) {
        postModel?.let {
            it.setLocation(postLocation.latitude, postLocation.longitude)
            _livePostModel.postValue(it)
        }
    }

    fun clearLocation() {
        postModel?.let {
            it.clearLocation()
            _livePostModel.postValue(it)
        }
    }

    fun updatePublishDateIfShouldBePublishedImmediately(): Boolean {
        postModel?.let {
            if (PostUtils.updatePublishDateIfShouldBePublishedImmediately(it)) {
                _livePostModel.postValue(it)
                return true
            }
        }
        return false
    }

    fun updatePostTitleIfDifferent(title: String): Boolean {
        postModel?.let {
            if (PostUtils.updatePostTitleIfDifferent(it, title)) {
                _livePostModel.postValue(it)
                return true
            }
        }
        return false
    }

    fun updatePostContentIfDifferent(content: String): Boolean {
        postModel?.let {
            if (PostUtils.updatePostContentIfDifferent(it, content)) {
                _livePostModel.postValue(it)
                return true
            }
        }
        return false
    }
}
