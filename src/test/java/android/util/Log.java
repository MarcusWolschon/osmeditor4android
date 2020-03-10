package android.util;

/**
 * Mock Log class for unit testing
 *
 */
public class Log {
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
}
