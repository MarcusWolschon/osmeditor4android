package de.blau.android.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.os.Looper;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class ExecutorTaskTest {

    private boolean cancelled         = false;
    private int     progress          = 0;
    private String  exceptionMessage;
    private boolean preExecuteDone;
    private int     postExecuteResult = 0;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        App.newLogic();
    }

    @Test
    public void cancelTest() {
        Logic logic = App.getLogic();
        ExecutorTask<Void, Void, Void> task = new ExecutorTask<Void, Void, Void>(logic.getExecutorService(), logic.getHandler()) {

            @Override
            protected Void doInBackground(Void input) throws Exception {
                while (!isCancelled()) {
                    sleep(100);
                }
                onCancelled();
                return null;
            }
        };

        task.setOnCancelledListener(() -> cancelled = true);

        task.execute();
        sleep(1000);
        assertTrue(task.isExecuting());
        assertFalse(task.isCancelled());
        task.cancel();
        sleep(2000);
        assertFalse(task.isExecuting());
        assertTrue(task.isCancelled());
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(cancelled);
    }

    @Test
    public void progressTest() {
        Logic logic = App.getLogic();
        ExecutorTask<Void, Integer, Void> task = new ExecutorTask<Void, Integer, Void>(logic.getExecutorService(), logic.getHandler()) {

            @Override
            protected Void doInBackground(Void input) throws Exception {
                publishProgress(1);
                return null;
            }

            @Override
            protected void onProgress(final Integer p) {
                progress = p;
            }
        };

        task.setOnProgressListener((Integer p) -> assertEquals(1, p.intValue()));

        task.execute();
        sleep(1000);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, progress);
    }

    @Test
    public void exceptionTest() {
        Logic logic = App.getLogic();
        ExecutorTask<Void, Void, Void> task = new ExecutorTask<Void, Void, Void>(logic.getExecutorService(), logic.getHandler()) {

            @Override
            protected Void doInBackground(Void input) throws Exception {
                throw new IOException("test");
            }

            @Override
            protected void onBackgroundError(Exception e) {
                exceptionMessage = e.getMessage();
            }
        };

        task.execute();
        sleep(1000);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("test", exceptionMessage);
    }

    @Test
    public void preExecutionTest() {
        Logic logic = App.getLogic();
        ExecutorTask<Void, Void, Void> task = new ExecutorTask<Void, Void, Void>(logic.getExecutorService(), logic.getHandler()) {
            @Override
            protected void onPreExecute() {
                preExecuteDone = true;
            }

            @Override
            protected Void doInBackground(Void input) throws Exception {
                return null;
            }

        };

        task.execute();
        sleep(1000);
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(preExecuteDone);
    }

    @Test
    public void postExecutionTest() {
        Logic logic = App.getLogic();
        ExecutorTask<Integer, Void, Integer> task = new ExecutorTask<Integer, Void, Integer>(logic.getExecutorService(), logic.getHandler()) {

            @Override
            protected Integer doInBackground(Integer input) throws Exception {
                return input;
            }

            @Override
            protected void onPostExecute(Integer o) {
                postExecuteResult = o;
            }
        };

        task.execute(123);
        sleep(1000);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(123, postExecuteResult);
        try {
            assertEquals(123, task.get().intValue());
        } catch (InterruptedException | ExecutionException e) { // NOSONAR
            fail(e.getMessage());
        }
    }

    /**
     * Test that the allocated ExecutorService is shutdown if we use the parameterless constructor
     * 
     * Uses Reflection to check the status
     */
    @Test
    public void parameterlessConstructorTest() {
        ExecutorTask<Integer, Void, Integer> task = new ExecutorTask<Integer, Void, Integer>() {

            @Override
            protected Integer doInBackground(Integer input) throws Exception {
                return input;
            }

            @Override
            protected void onPostExecute(Integer o) {
                postExecuteResult = o;
            }
        };

        task.execute(123);
        sleep(1000);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(123, postExecuteResult);
        try {
            assertEquals(123, task.get(1, TimeUnit.SECONDS).intValue());
        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
            fail(e.getMessage());
        }

        try {
            Field field = task.getClass().getSuperclass().getDeclaredField("executorService");
            field.setAccessible(true); // NOSONAR
            ExecutorService executorService = (ExecutorService) field.get(task);
            assertTrue(executorService.isShutdown());
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Sleep for duration
     * 
     * @param duration ms to sleep
     */
    private void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ex) { // NOSONAR
            // nothing
        }
    }
}