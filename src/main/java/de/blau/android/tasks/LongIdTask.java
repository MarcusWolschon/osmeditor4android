package de.blau.android.tasks;

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
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }
}
