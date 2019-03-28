package de.blau.android;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Dummy class for legacy flavour
 * 
 * @author simon
 *
 */
public class Feedback  {

    private static final String DEBUG_TAG = "Feedback";
    
    /**
     * Start this Activity
     * 
     * @param context Android Context
     */
    public static void start(@NonNull Context context) {
        // dummy
    }

    /**
     * Start this Activity
     * 
     * @param context Android Context
     * @param repoUser github repository user
     * @param repoName githun repository name
     */
    public static void start(@NonNull Context context, @NonNull String repoUser, @NonNull String repoName) {
       // dummy
    }

}
