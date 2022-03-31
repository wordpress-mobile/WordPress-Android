package org.wordpress.android.models.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.models.bloggingprompts.BloggingPrompt
import javax.inject.Inject

class GetBloggingPromptUseCase @Inject constructor() {
    // TODO fetch from Store when implementation is ready
    fun execute(): Flow<BloggingPrompt> = flow {
        emit(
                BloggingPrompt(
                        text = "This is a blogging prompt!",
                        numberOfAnswers = 7,
                        template = ""
                )
        )
    }
}
