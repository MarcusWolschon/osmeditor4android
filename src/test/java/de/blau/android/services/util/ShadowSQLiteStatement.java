package de.blau.android.services.util;

import java.io.IOException;
import java.io.InputStream;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import android.database.sqlite.SQLiteStatement;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;

@Implements(value = SQLiteStatement.class)
public class ShadowSQLiteStatement {

    /**
     * Mock implementation that returns a ParcelFileDescriptor for a sample tile
     * 
     * @return a ParcelFileDescriptor for a sample tile
     */
    @Implementation
    public ParcelFileDescriptor simpleQueryForBlobFileDescriptor() {
        try (InputStream is = ShadowSQLiteStatement.class.getResourceAsStream("/340.png")) {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            AutoCloseOutputStream os = new AutoCloseOutputStream(pipe[1]);
            int len;
            while ((len = is.read()) >= 0) {
                os.write(len);
            }
            os.flush();
            os.close();
            return pipe[0];
        } catch (IOException ioex) {
            // ignore
        }
        return null;
    }

    /**
     * Mock implementation that does nothing
     * 
     * @param index index of value in statement
     * @param value the value to bind to it
     */
    @Implementation
    public void bindLong(int index, long value) {
        // dummy
    }

    /**
     * Mock implementation that does nothing
     * 
     * @param index index of value in statement
     * @param value the value to bind to it
     */
    @Implementation
    public void bindString(int index, String value) {
        // dummy
    }
}
