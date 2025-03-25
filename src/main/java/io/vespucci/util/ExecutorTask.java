package io.vespucci.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Based on https://stackoverflow.com/questions/69217817/best-replacement-for-async-task
 *
 * @param <I> input value
 * @param <P> progress value
 * @param <O> returned value
 */
public abstract class ExecutorTask<I, P, O> {
    private boolean               cancelled = false;
    private Future<O>             outputFuture;
    private final ExecutorService executorService;
    private final Handler         handler;
    private final boolean         shutdown;

    /**
     * Create a new instance with a single thread executor
     * 
     * Note the executor with be shutdown once the task has completed.
     */
    protected ExecutorTask() {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.handler = new Handler(Looper.getMainLooper());
        shutdown = true;
    }

    /**
     * Create a new instance
     * 
     * @param executorService ExecutorService to use
     * @param handler Handler to use
     */
    protected ExecutorTask(@NonNull ExecutorService executorService, @NonNull Handler handler) {
        this.executorService = executorService;
        this.handler = handler;
        shutdown = false;
    }

    /**
     * Starts execution of the task
     * 
     * @return this ExecutorTask
     */
    public ExecutorTask<I, P, O> execute() {
        return execute(null);
    }

    /**
     * Starts execution of the task
     * 
     * @param input Data you want to work with in the background
     * @return this ExecutorTask
     */
    public ExecutorTask<I, P, O> execute(final I input) {
        onPreExecute();

        outputFuture = executorService.submit(() -> {
            try {
                final O output = doInBackground(input);
                handler.post(() -> onPostExecute(output));
                return output;
            } catch (Exception e) {
                handler.post(() -> onBackgroundError(e));
                throw e;
            } finally {
                if (shutdown) {
                    executorService.shutdown();
                }
            }
        });

        return this;
    }

    /**
     * Get the task result
     * 
     * @return O
     * @throws InterruptedException if the execution was interrupted
     * @throws ExecutionException other execution issue
     */
    public O get() throws InterruptedException, ExecutionException {
        if (outputFuture == null) {
            throw new IllegalStateException("future is null");
        } else {
            return outputFuture.get();
        }
    }

    /**
     * Get the task result
     * 
     * @param timeout timeout
     * @param timeUnit unit of the timeout
     * @return O
     * @throws InterruptedException if the execution was interrupted
     * @throws ExecutionException other execution issue
     * @throws TimeoutException if timed out
     */
    public O get(long timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        if (outputFuture == null) {
            throw new IllegalStateException("future is null");
        } else {
            return outputFuture.get(timeout, timeUnit);
        }
    }

    /**
     * Call to publish progress from background
     * 
     * @param progress Progress made
     */
    protected void publishProgress(final P progress) {
        handler.post(() -> {
            onProgress(progress);

            if (onProgressListener != null) {
                onProgressListener.onProgress(progress);
            }
        });
    }

    /**
     * Called when progress is updated
     * 
     * @param progress P indicating the progress made
     */
    protected void onProgress(final P progress) {

    }

    /**
     * Call to cancel background work
     */
    public void cancel() {
        cancelled = true;
    }

    /**
     *
     * @return Returns true if the background work should be cancelled
     */
    protected boolean isCancelled() {
        return cancelled;
    }

    /**
     * Call this method after cancelling background work
     */
    protected void onCancelled() {
        handler.post(() -> {
            if (onCancelledListener != null) {
                onCancelledListener.onCancelled();
            }
        });
    }

    /**
     * Work which you want to be done on UI thread before {@link #doInBackground(Object)}
     */
    protected void onPreExecute() {

    }

    /**
     * Work on background
     * 
     * @param input Input data
     * @return Output data
     * @throws Exception Any uncaught exception which occurred while working in background. If any occurs,
     *             {@link #onBackgroundError(Exception)} will be executed (on the UI thread)
     */
    protected abstract O doInBackground(I input) throws Exception; // NOSONAR

    /**
     * Work which you want to be done on UI thread after {@link #doInBackground(Object)}
     * 
     * @param output Output data from {@link #doInBackground(Object)}
     */
    protected void onPostExecute(O output) {

    }

    /**
     * Triggered on UI thread if any uncaught exception occurred while working in background
     * 
     * @param e Exception
     * @see #doInBackground(Object)
     */
    protected void onBackgroundError(Exception e) {

    }

    private OnProgressListener<P> onProgressListener;

    public interface OnProgressListener<P> {
        /**
         * Called in an OnProgressListener when progress is updated
         * 
         * @param progress a value of P indicating the progress
         */
        void onProgress(P progress);
    }

    /**
     * Set an OnProgressListener
     * 
     * @param onProgressListener the listener
     */
    public void setOnProgressListener(@Nullable OnProgressListener<P> onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

    private OnCancelledListener onCancelledListener;

    public interface OnCancelledListener {
        /**
         * Called when execution has been cancelled
         */
        void onCancelled();
    }

    /**
     * Set an OnCancelledListener
     * 
     * @param onCancelledListener the listener
     */
    public void setOnCancelledListener(@Nullable OnCancelledListener onCancelledListener) {
        this.onCancelledListener = onCancelledListener;
    }

    /**
     * Check that task has started execution and is neither cancelled or done
     * 
     * @return true if the task has started execution and is neither cancelled or done
     */
    public boolean isExecuting() {
        return outputFuture != null && !outputFuture.isCancelled() && !outputFuture.isDone();
    }
}
