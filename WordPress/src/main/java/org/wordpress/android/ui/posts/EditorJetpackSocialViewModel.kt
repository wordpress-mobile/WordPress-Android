package org.wordpress.android.ui.posts

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.datasets.wrappers.PublicizeTableWrapper
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.posts.social.PostSocialSharingModelMapper
import org.wordpress.android.usecase.social.GetJetpackSocialShareLimitStatusUseCase
import org.wordpress.android.usecase.social.GetJetpackSocialShareMessageUseCase
import org.wordpress.android.usecase.social.GetPublicizeConnectionsForUserUseCase
import org.wordpress.android.util.config.JetpackSocialFeatureConfig
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

// TODO remove this annotation after class implementation
@Suppress("unused")
class EditorJetpackSocialViewModel @Inject constructor(
    private val jetpackSocialFeatureConfig: JetpackSocialFeatureConfig,
    private val accountStore: AccountStore,
    private val getPublicizeConnectionsForUserUseCase: GetPublicizeConnectionsForUserUseCase,
    private val getJetpackSocialShareMessageUseCase: GetJetpackSocialShareMessageUseCase,
    private val getJetpackSocialShareLimitStatusUseCase: GetJetpackSocialShareLimitStatusUseCase,
    private val jetpackUiStateMapper: EditPostPublishSettingsJetpackSocialUiStateMapper,
    private val postSocialSharingModelMapper: PostSocialSharingModelMapper,
    private val publicizeTableWrapper: PublicizeTableWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) : ScopedViewModel(bgDispatcher) {
    // TODO add common LiveData and functions that are used in the Post Editor several places, related to JP Social
    private lateinit var siteModel: SiteModel
    private lateinit var editPostRepository: EditPostRepository

    private val currentPost: PostImmutableModel?
        get() = editPostRepository.getPost()

    fun start(siteModel: SiteModel, editPostRepository: EditPostRepository) {
        this.siteModel = siteModel
        this.editPostRepository = editPostRepository
    }
}
