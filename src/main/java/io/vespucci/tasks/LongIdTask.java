package io.vespucci.tasks;

import java.util.Objects;

public abstract class LongIdTask extends Task {

    private static final long serialVersionUID = 1L;

    /** Task ID */
    protected long id;

    /**
     * Get the task ID.
     * 
     * @return The task ID.
     */
    public long getId() {
        return id;
    }

    @Override
    public int hashCode() { // NOSONAR
        return Objects.hash(id);
    }
}
