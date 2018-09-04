package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostType
import org.wordpress.android.fluxc.model.post.PostType.TypePage
import org.wordpress.android.fluxc.model.post.PostType.TypePortfolio
import org.wordpress.android.fluxc.model.post.PostType.TypePost

fun getResourceId(postType: PostType, pageResource: Int, postResource: Int): Int {
    return getValueForType(postType, pageResource, postResource)
}

fun getResourceId(postType: PostModel, pageResource: Int, postResource: Int): Int {
    return getValueForType(postType, pageResource, postResource)
}

fun getResourceId(postTypeModelValue: Int, pageResource: Int, postResource: Int): Int {
    return getValueForType(postTypeModelValue, postResource, pageResource)
}

fun <ValueType> getValueForType(postType: PostType, pageResource: ValueType, postResource: ValueType): ValueType {
    return getValueForType(postType.modelValue(), pageResource, postResource)
}

fun <ValueType> getValueForType(postModel: PostModel, pageResource: ValueType, postResource: ValueType): ValueType {
    return getValueForType(postModel.type, pageResource, postResource)
}

fun <ValueType> getValueForType(postTypeModelValue: Int, pageValue: ValueType, postValue: ValueType): ValueType {
    return when (postTypeModelValue) {
        TypePost.modelValue() ->
            postValue
        TypePage.modelValue() ->
            pageValue
        TypePortfolio.modelValue() ->
            throw IllegalStateException("TypePortfolio is not implemented yet")
        else ->
            throw IllegalStateException("Unknown post type:$postTypeModelValue")
    }
}
