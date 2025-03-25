package io.vespucci.exception;

import java.util.List;

import androidx.annotation.NonNull;

public class OsmParseException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 5410676963227425405L;

    /**
     * Construct a new exception
     * 
     * @param msg the error message
     */
    public OsmParseException(final String msg) {
        super(msg);
    }

    /**
     * Construct a new exception, using the messages from the provided exceptions
     * 
     * @param exceptions a List of Exceptions
     */
    public OsmParseException(@NonNull List<Exception> exceptions) {
        super(concatenateMessages(exceptions));
    }

    /**
     * Concatenate the messages from a list of exceptions
     * 
     * @param exceptions the List of Exceptions
     * @return the concatenate messages
     */
    @NonNull
    private static String concatenateMessages(@NonNull List<Exception> exceptions) {
        StringBuilder msg = new StringBuilder();
        for (Exception e : exceptions) {
            if (msg.length() > 0) {
                msg.append('\n');
            }
            msg.append(e.getMessage());
        }
        return msg.toString();
    }
}
