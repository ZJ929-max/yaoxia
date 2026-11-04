package com.opensandbox.dhizuku.core;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 唯一的线程边界：耗时工作都在单线程池里跑，结果统一回到主线程。
 * 调用方（界面）不需要知道 Executor / Handler 的存在。
 */
public final class AsyncTaskRunner {

    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AsyncTaskRunner(String threadName) {
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        });
    }

    public <T> void execute(Callable<Outcome<T>> work, Consumer<Outcome<T>> onSuccessOrFailure) {
        executor.execute(() -> {
            Outcome<T> result;
            try {
                result = work.call();
            } catch (Throwable throwable) {
                result = Outcome.fail(throwable);
            }
            final Outcome<T> delivered = result;
            mainHandler.post(() -> onSuccessOrFailure.accept(delivered));
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
