package io.vespucci.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import io.vespucci.tasks.Task;
import io.vespucci.tasks.Todo;

public class TaskTest {

    /**
     * Check that sorting tasks by distance to a location works as intended
     */
    @Test
    public void sortByDistance() {
        InputStream input = getClass().getResourceAsStream("/test.todos");
        try {
            List<Todo> todos = Todo.parseTodos(input);
            Task.sortByDistance(todos, 8.3879595, 47.38827, false);
            Todo nearest = todos.get(0);
            assertEquals(8.3879595 * 1E7D, nearest.getLon(), 1);
            assertEquals(47.38827 * 1E7D, nearest.getLat(), 1);
            Task.sortByDistance(todos, 8.3879595, 47.38827, true);
            nearest = todos.get(0);
            assertEquals(8.3879595 * 1E7D, nearest.getLon(), 1);
            assertEquals(47.38827 * 1E7D, nearest.getLat(), 1);
            //
            nearest.setState(Task.State.SKIPPED);
            Task.sortByDistance(todos, 8.3879595, 47.38827, false);
            nearest = todos.get(0);
            assertEquals(8.3879595 * 1E7D, nearest.getLon(), 1);
            assertEquals(47.38827 * 1E7D, nearest.getLat(), 1);
            Task.sortByDistance(todos, 8.3879595, 47.38827, true);
            nearest = todos.get(0);
            assertNotEquals(8.3879595 * 1E7D, nearest.getLon(), 1);
            assertNotEquals(47.38827 * 1E7D, nearest.getLat(), 1);
            nearest = todos.get(todos.size() - 1);
            assertEquals(8.3879595 * 1E7D, nearest.getLon(), 1);
            assertEquals(47.38827 * 1E7D, nearest.getLat(), 1);
        } catch (NumberFormatException | IOException e) {
            fail(e.getMessage());
        }
    }
}