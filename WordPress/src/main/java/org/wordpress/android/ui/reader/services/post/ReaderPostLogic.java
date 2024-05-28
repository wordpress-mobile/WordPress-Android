package org.wordpress.android.ui.reader.services.post;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.ReaderEvents;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.repository.ReaderPostRepository;
import org.wordpress.android.ui.reader.services.ServiceCompletionListener;
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.UpdateAction;

public class ReaderPostLogic {
    @NonNull
    private final ServiceCompletionListener mCompletionListener;
    @NonNull
    private final ReaderPostRepository mReaderPostRepository;
    private Object mListenerCompanion;

    public ReaderPostLogic(@NonNull final ServiceCompletionListener listener,
                           @NonNull final ReaderPostRepository readerPostRepository) {
        mCompletionListener = listener;
        mReaderPostRepository = readerPostRepository;
    }

    public void performTask(Object companion, UpdateAction action,
                            ReaderTag tag, long blogId, long feedId) {
        mListenerCompanion = companion;

        EventBus.getDefault().post(new ReaderEvents.UpdatePostsStarted(action, tag));

        if (tag != null) {
            updatePostsWithTag(tag, action);
        } else if (blogId > -1) {
            updatePostsInBlog(blogId, action);
        } else if (feedId > -1) {
            updatePostsInFeed(feedId, action);
        }
    }

    private void updatePostsWithTag(final ReaderTag tag, final UpdateAction action) {
        mReaderPostRepository.requestPostsWithTag(
                tag,
                action,
                new ReaderActions.UpdateResultListener() {
                    @Override
                    public void onUpdateResult(ReaderActions.UpdateResult result) {
                        EventBus.getDefault().post(new ReaderEvents.UpdatePostsEnded(tag, result, action));
                        mCompletionListener.onCompleted(mListenerCompanion);
                    }
                });
    }

    private void updatePostsInBlog(long blogId, final UpdateAction action) {
        ReaderActions.UpdateResultListener listener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                EventBus.getDefault().post(new ReaderEvents.UpdatePostsEnded(result, action));
                mCompletionListener.onCompleted(mListenerCompanion);
            }
        };
        mReaderPostRepository.requestPostsForBlog(blogId, action, listener);
    }

    private void updatePostsInFeed(long feedId, final UpdateAction action) {
        ReaderActions.UpdateResultListener listener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                EventBus.getDefault().post(new ReaderEvents.UpdatePostsEnded(result, action));
                mCompletionListener.onCompleted(mListenerCompanion);
            }
        };
        mReaderPostRepository.requestPostsForFeed(feedId, action, listener);
    }
}
