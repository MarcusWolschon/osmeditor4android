package de.blau.android.tasks;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.dialogs.TextLineDialog;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.Snack;

public class MapRouletteApiKey {

    private static final String DEBUG_TAG = "MapRouletteApiKey";

    /**
     * Private constructor
     */
    private MapRouletteApiKey() {
        // empty
    }
    /**
     * Set a MapRoulette API key by asking the user for it
     * 
     * If the key is empty it will ask the user if MapRoulette should be disabled
     * 
     * @param activity the calling activity (should be Main)
     * @param server the current Server object
     * @param retrieveKey if the dialog should be pre-filled with the current key
     */
    public static void set(@NonNull final Activity activity, @NonNull final Server server, final boolean retrieveKey) {

        final String apiKey = retrieveKey ? get(server) : null;

        final AppCompatDialog dialog = TextLineDialog.get(activity, R.string.maproulette_task_set_apikey, apiKey, new TextLineDialog.TextLineInterface() {
            @Override
            public void processLine(EditText input) {

                final String newApiKey = input.getText().toString().trim();
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            if (newApiKey.length() > 0) {
                                server.setUserPreference(TransferTasks.MAPROULETTE_APIKEY_V2, newApiKey);
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Snack.toastTopInfo(activity, R.string.maproulette_task_apikey_set);
                                    }
                                });
                            } else {
                                if (apiKey != null) {
                                    try {
                                        server.deleteUserPreference(TransferTasks.MAPROULETTE_APIKEY_V2);
                                    } catch (OsmException oex) {
                                        Log.e(DEBUG_TAG, "Unable to delete maproulette key " + oex.getMessage());
                                    }
                                }
                                if (activity instanceof Main) {
                                    Preferences prefs = ((Main) activity).getMap().getPrefs();
                                    final Set<String> bugFilter = prefs.taskFilter();
                                    if (bugFilter.contains(activity.getString(R.string.bugfilter_maproulette))) {
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Snack.barError(activity, R.string.maproulette_task_no_apikey, R.string.maproulette_task_disable,
                                                        new OnClickListener() {
                                                            @Override
                                                            public void onClick(View v) {
                                                                bugFilter.remove(activity.getString(R.string.bugfilter_maproulette));
                                                                prefs.setTaskFilter(bugFilter);
                                                                TaskStorage taskStorage = App.getTaskStorage();
                                                                final List<Task> queryResult = App.getTaskStorage().getTasks();
                                                                for (Task t : queryResult) {
                                                                    if (t instanceof MapRouletteTask) {
                                                                        taskStorage.delete(t);
                                                                    }
                                                                }
                                                                ((Main) activity).getMap().invalidate();
                                                            }
                                                        });
                                            }
                                        });
                                    }
                                }
                            }
                        } catch (OsmException oex) {
                            Log.e(DEBUG_TAG, "Unable to set maproulette key " + oex.getMessage());
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Snack.toastTopError(activity, R.string.maproulette_task_apikey_not_set);
                                }
                            });
                        }
                        return null;
                    }
                }.execute();

            }
        });
        dialog.show();
    }

    /**
     * Get the current MapRoulette API key
     * 
     * @param server the current Server object
     * @return the key or null
     */
    @Nullable
    public static String get(@NonNull final Server server) {
        AsyncTask<Void, Void, String> getKey = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                return server.getUserPreferences().get(TransferTasks.MAPROULETTE_APIKEY_V2);
            }
        };

        try {
            getKey.execute();
            return getKey.get(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR cancel does interrupt
            getKey.cancel(true);
            return null;
        }
    }
}
