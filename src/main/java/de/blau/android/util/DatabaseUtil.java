package de.blau.android.util;

import java.io.File;
import java.io.FileReader;

public final class DatabaseUtil {

    private static final String SQLITE_MAGIC = "SQLite format 3\u0000";

    /**
     * Empty constructor to disable instantiating this class
     */
    private DatabaseUtil() {
    }

    /**
     * Check if this could be a valid SqlLite database
     * 
     * see https://stackoverflow.com/questions/39576646/android-check-if-a-file-is-a-valid-sqlite-database
     * 
     * @param dbPath path to the SQLite database file
     * @return true if there is a good chance that the path point to a valid DB
     */
    public static boolean isValidSQLite(String dbPath) {
        File file = new File(dbPath);

        if (!file.exists() || !file.canRead()) {
            return false;
        }
        FileReader fr = null;
        try {
            fr = new FileReader(file);
            char[] buffer = new char[16];
            if (fr.read(buffer, 0, 16) == 16) {
                String str = String.valueOf(buffer);
                return SQLITE_MAGIC.equals(str);
            }
            return false;
        } catch (Exception e) {
            return false;
        } finally {
            SavingHelper.close(fr);
        }
    }
}
