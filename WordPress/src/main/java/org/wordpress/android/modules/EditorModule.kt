package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.posts.editor.EditorPhotoPicker
import org.wordpress.android.ui.posts.editor.StorePostViewModel
import org.wordpress.android.ui.posts.editor.events.PostEventHandler
import org.wordpress.android.ui.posts.editor.media.EditorMediaEventHandler
import org.wordpress.android.ui.posts.editor.media.EditorMediaManager
import org.wordpress.android.ui.posts.editor.publishing.PostPublishingManager
import org.wordpress.android.ui.posts.editor.publishing.PostValidationService
import org.wordpress.android.ui.posts.editor.state.PostStateManager
import org.wordpress.android.ui.posts.editor.state.EditorStateManager
import org.wordpress.android.ui.posts.editor.fragment.EditorFragmentManager
import org.wordpress.android.ui.posts.editor.config.EditorConfigurationManager
import javax.inject.Singleton

/**
 * Dagger module providing editor-related dependencies.
 * This module supports the refactored editor architecture.
 * 
 * Updated for Phase 2: Publishing Logic Extraction
 */
@Module
@InstallIn(SingletonComponent::class)
object EditorModule {
    
    // ========================================
    // Phase 1: Media Management Components
    // ========================================
    
    @Provides
    @Singleton
    fun provideEditorMediaManager(
        mediaStore: MediaStore,
        editorPhotoPicker: EditorPhotoPicker
    ): EditorMediaManager {
        return EditorMediaManager(mediaStore, editorPhotoPicker)
    }
    
    @Provides
    @Singleton
    fun provideEditorMediaEventHandler(
        mediaManager: EditorMediaManager
    ): EditorMediaEventHandler {
        return EditorMediaEventHandler(mediaManager)
    }
    
    // ========================================
    // Phase 2: Publishing Management Components
    // ========================================
    
    @Provides
    @Singleton
    fun providePostValidationService(
        mediaStore: MediaStore
    ): PostValidationService {
        return PostValidationService(mediaStore)
    }
    
    @Provides
    @Singleton
    fun providePostPublishingManager(
        accountStore: AccountStore,
        postStore: PostStore,
        uploadStore: UploadStore,
        storePostViewModel: StorePostViewModel,
        postValidationService: PostValidationService
    ): PostPublishingManager {
        return PostPublishingManager(
            accountStore,
            postStore,
            uploadStore,
            storePostViewModel,
            postValidationService
        )
    }
    
    @Provides
    @Singleton
    fun providePostStateManager(
        storePostViewModel: StorePostViewModel
    ): PostStateManager {
        return PostStateManager(storePostViewModel)
    }
    
    @Provides
    @Singleton
    fun providePostEventHandler(): PostEventHandler {
        return PostEventHandler()
    }
    
    // ========================================
    // Phase 3: Editor State Management Components
    // ========================================
    
    @Provides
    @Singleton
    fun provideEditorStateManager(): EditorStateManager {
        return EditorStateManager()
    }
    
    @Provides
    @Singleton
    fun provideEditorFragmentManager(): EditorFragmentManager {
        return EditorFragmentManager()
    }
    
    @Provides
    @Singleton
    fun provideEditorConfigurationManager(): EditorConfigurationManager {
        return EditorConfigurationManager()
    }
}
