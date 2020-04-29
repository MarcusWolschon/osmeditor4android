package android.util;

/**
 * Mock Log class for unit testing
 *
 */
public class Log {

    public final static int DEBUG = 3;
    public final static int INFO  = 4;
    public final static int WARN  = 5;
    public final static int ERROR = 6;

    /**
     * Debug log message
     * 
     * @param tag log tag
     * @param msg message
     * @return not documented
     */
    public static int d(String tag, String msg) {
        System.out.println("DEBUG: " + tag + ": " + msg);
        return 0;
    }

    /**
     * Debug log message
     * 
     * @param tag log tag
     * @param msg message
     * @param e Throwable that caused the error
     * @return not documented
     */
    public static int d(String tag, String msg, Throwable e) {
        System.out.println("DEBUG: " + tag + ": " + msg + " " + e.getMessage());
        return 0;
    }

    /**
     * Information log message
     * 
     * @param tag log tag
     * @param msg message
     * @return not documented
     */
    public static int i(String tag, String msg) {
        System.out.println("INFO: " + tag + ": " + msg);
        return 0;
    }

    /**
     * Warning log message
     * 
     * @param tag log tag
     * @param msg message
     * @return not documented
     */
    public static int w(String tag, String msg) {
        System.out.println("WARN: " + tag + ": " + msg);
        return 0;
    }

    /**
     * Warning log message
     * 
     * @param tag log tag
     * @param msg message
     * @param e Throwable that caused the error
     * @return not documented
     */
    public static int w(String tag, String msg, Throwable e) {
        System.out.println("WARN: " + tag + ": " + msg + " " + e.getMessage());
        return 0;
    }

    /**
     * Error log message
     * 
     * @param tag log tag
     * @param msg message
     * @return not documented
     */
    public static int e(String tag, String msg) {
        System.out.println("ERROR: " + tag + ": " + msg);
        return 0;
    }

    /**
     * Error log message
     * 
     * @param tag log tag
     * @param msg message
     * @param e Throwable that caused the error
     * @return not documented
     */
    public static int e(String tag, String msg, Throwable e) {
        System.out.println("ERROR: " + tag + ": " + msg + " " + e.getMessage());
        return 0;
    }

    /**
     * Check if the the tag can be logged
     * 
     * @param tag tag
     * @param logLevel log level
     * @return always true
     */
    public static boolean isLoggable(String tag, int logLevel) {
        return true;
    }
}
