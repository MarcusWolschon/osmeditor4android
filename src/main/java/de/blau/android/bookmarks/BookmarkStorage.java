package de.blau.android.bookmarks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.contract.Paths;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmXml;
import de.blau.android.osm.ViewBox;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.FileUtil;
import de.blau.android.util.SavingHelper;

/**
 * Handles Bookmark reading/writing operations
 */
public class BookmarkStorage {

    private static final String DEBUG_TAG = BookmarkStorage.class.getSimpleName();

    private Bookmark            currentBookmark;
    private ArrayList<Bookmark> bookmarks;
    public static final String  FILENAME     = "bookmarks.ser";
    private static final String NEW_FILENAME = "bookmarks.geojson";

    SavingHelper<ArrayList<Bookmark>> savingHelper;

    /**
     * BookmarkIO constructor
     */
    public BookmarkStorage() {
        this.currentBookmark = new Bookmark();
        this.bookmarks = new ArrayList<>();
        this.savingHelper = new SavingHelper<>();
    }

    /**
     * Adds the text and viewbox to a list
     *
     * @param comment Bookmark name/comment
     * @param viewbox Map Viewbox
     */
    public void addDatatolist(@NonNull String comment, @NonNull ViewBox viewbox) {
        currentBookmark.set(comment, viewbox);
        bookmarks.add(currentBookmark);
    }

    /**
     * (Over)Writes the bookmark file to storage.
     *
     * @param context the Android Context
     * @param bookmarksStorage Arraylist containing BookmarksStorage objects
     * @return true if successful
     */
    public boolean writeList(@NonNull Context context, @NonNull ArrayList<Bookmark> bookmarksStorage) { // NOSONAR
        final List<Feature> features = new ArrayList<>();
        for (Bookmark b : bookmarksStorage) {
            BoundingBox box = b.getViewBox();
            JsonObject properties = new JsonObject();
            properties.add(Bookmark.NAME_FIELD, new JsonPrimitive(b.getComment()));
            features.add(Feature.fromGeometry(null, properties,
                    com.mapbox.geojson.BoundingBox.fromLngLats(box.getLeft() / 1E7D, box.getBottom() / 1E7D, box.getRight() / 1E7D, box.getTop() / 1E7D)));
        }
        ExecutorTask<Void, Void, Boolean> writer = new ExecutorTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void param) {
                try {
                    File outfile = new File(FileUtil.getApplicationDirectory(context, Paths.DIRECTORY_PATH_OTHER), NEW_FILENAME);
                    try (FileOutputStream fout = new FileOutputStream(outfile); OutputStream outputStream = new BufferedOutputStream(fout)) {
                        outputStream.write(FeatureCollection.fromFeatures(features).toJson().getBytes());
                    }
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "Save failed - " + NEW_FILENAME + " " + e.getMessage());
                }
                return true;
            }
        }.execute();
        try {
            return writer.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
            Log.e(DEBUG_TAG, "Save failed - " + NEW_FILENAME + " " + e.getMessage());
            return false;
        }
    }

    /**
     * Reads the bookmark file from storage
     *
     * @param context the Android context
     * @return Arraylist containing saved bookmarks
     */

    @NonNull
    public ArrayList<Bookmark> readList(@NonNull Context context) { // NOSONAR
        final ArrayList<Bookmark> jsonResult = new ArrayList<>();
        ExecutorTask<Void, Void, ArrayList<Bookmark>> reader = new ExecutorTask<Void, Void, ArrayList<Bookmark>>() {
            @Override
            protected ArrayList<Bookmark> doInBackground(Void param) {
                try {
                    File infile = new File(FileUtil.getApplicationDirectory(context, Paths.DIRECTORY_PATH_OTHER), NEW_FILENAME);
                    try (FileInputStream fin = new FileInputStream(infile); Reader in = new InputStreamReader(fin, Charset.forName(OsmXml.UTF_8));) { // NOSONAR
                        FeatureCollection fc = FeatureCollection.fromJson(FileUtil.readToString(in));
                        for (Feature f : fc.features()) {
                            com.mapbox.geojson.BoundingBox box = f.bbox();
                            Bookmark bookmark = new Bookmark(f.properties().get(Bookmark.NAME_FIELD).getAsString(),
                                    new ViewBox(box.west(), box.south(), box.east(), box.north()));
                            jsonResult.add(bookmark);
                        }
                    }
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "Read failed - " + NEW_FILENAME + " " + e.getMessage());
                }
                return jsonResult;
            }
        }.execute();
        try {
            bookmarks = reader.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
            Log.e(DEBUG_TAG, "Read failed - " + NEW_FILENAME + " " + e.getMessage());
            bookmarks = new ArrayList<>();
        }
        return bookmarks;
    }

    /**
     * Utility for a saving bookmark
     *
     * @param context the Android context
     * @param comments Bookmark name/comment
     * @param viewBox map viewbox
     */
    public void writer(@NonNull Context context, @NonNull String comments, @NonNull ViewBox viewBox) {
        this.bookmarks = readList(context);
        addDatatolist(comments, viewBox);
        writeList(context, this.bookmarks);
    }

    /**
     * Migrate to geojson
     * 
     * @param context an Android Context
     */
    public void migrate(@NonNull Context context) {
        ArrayList<Bookmark> storage = savingHelper.load(context, FILENAME, true);
        if (storage != null) {
            Log.i(DEBUG_TAG, "Migrating bookmark storage");
            if (!(writeList(context, storage) && context.deleteFile(FILENAME))) {
                Log.e(DEBUG_TAG, "Bookmark storage migration failed");
            }
        }
    }
}
