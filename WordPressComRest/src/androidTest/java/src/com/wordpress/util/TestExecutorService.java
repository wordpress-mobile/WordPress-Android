package com.wordpress.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Future;
import java.util.concurrent.BlockingQueue;

public class TestExecutorService extends ThreadPoolExecutor {
    public TestExecutorService(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        FutureTask futureTask = new FutureTask(runnable, null);
        futureTask.run(); // or queue this future to be run when you want it to be run
        return futureTask;
    }
}
