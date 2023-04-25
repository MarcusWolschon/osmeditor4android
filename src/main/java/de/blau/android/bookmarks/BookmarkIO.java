package de.blau.android.bookmarks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
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
public class BookmarkIO {

    private static final String DEBUG_TAG = BookmarkIO.class.getSimpleName();

    private BookmarksStorage            currentBookmarkStorage;
    private ArrayList<BookmarksStorage> bookmarksStorage;
    public static final String          FILENAME     = "bookmarks.ser";
    private static final String         NEW_FILENAME = "bookmarks.geojson";

    SavingHelper<ArrayList<BookmarksStorage>> savingHelper;

    /**
     * BookmarkIO constructor
     */
    public BookmarkIO() {
        this.currentBookmarkStorage = new BookmarksStorage();
        this.bookmarksStorage = new ArrayList<>();
        this.savingHelper = new SavingHelper<>();
    }

    /**
     * Adds the text and viewbox to a list
     *
     * @param comment Bookmark name/comment
     * @param viewbox Map Viewbox
     */
    public void addDatatolist(@NonNull String comment, @NonNull ViewBox viewbox) {
        currentBookmarkStorage.set(comment, viewbox);
        bookmarksStorage.add(currentBookmarkStorage);
    }

    /**
     * (Over)Writes the bookmark file to storage.
     *
     * @param context the Android Context
     * @param bookmarksStorage Arraylist containing BookmarksStorage objects
     * @return true if successful
     */
    public boolean writeList(@NonNull Context context, @NonNull ArrayList<BookmarksStorage> bookmarksStorage) { // NOSONAR
        final List<Feature> features = new ArrayList<>();
        for (BookmarksStorage b : bookmarksStorage) {
            BoundingBox box = b.getViewBox();
            JsonObject properties = new JsonObject();
            properties.add(BookmarksStorage.NAME_FIELD, new JsonPrimitive(b.getComment()));
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
    public ArrayList<BookmarksStorage> readList(@NonNull Context context) { // NOSONAR
        final ArrayList<BookmarksStorage> jsonResult = new ArrayList<>();
        ExecutorTask<Void, Void, ArrayList<BookmarksStorage>> reader = new ExecutorTask<Void, Void, ArrayList<BookmarksStorage>>() {
            @Override
            protected ArrayList<BookmarksStorage> doInBackground(Void param) {
                try {
                    File infile = new File(FileUtil.getApplicationDirectory(context, Paths.DIRECTORY_PATH_OTHER), NEW_FILENAME);
                    try (FileInputStream fin = new FileInputStream(infile); Reader in = new InputStreamReader(fin, OsmXml.UTF_8);) { // NOSONAR
                        FeatureCollection fc = FeatureCollection.fromJson(FileUtil.readToString(in));
                        for (Feature f : fc.features()) {
                            com.mapbox.geojson.BoundingBox box = f.bbox();
                            BookmarksStorage bookmark = new BookmarksStorage(f.properties().get(BookmarksStorage.NAME_FIELD).getAsString(),
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
            bookmarksStorage = reader.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
            Log.e(DEBUG_TAG, "Read failed - " + NEW_FILENAME + " " + e.getMessage());
            bookmarksStorage = new ArrayList<>();
        }
        return bookmarksStorage;
    }

    /**
     * Utility for a saving bookmark
     *
     * @param context the Android context
     * @param comments Bookmark name/comment
     * @param viewBox map viewbox
     */
    public void writer(@NonNull Context context, @NonNull String comments, @NonNull ViewBox viewBox) {
        this.bookmarksStorage = readList(context);
        addDatatolist(comments, viewBox);
        writeList(context, this.bookmarksStorage);
    }

    /**
     * Migrate to geojson
     * 
     * @param context an Android Context
     */
    public void migrate(@NonNull Context context) {
        ArrayList<BookmarksStorage> storage = savingHelper.load(context, FILENAME, true);
        if (storage != null) {
            Log.i(DEBUG_TAG, "Migrating bookmark storage");
            if (!(writeList(context, storage) && context.deleteFile(FILENAME))) {
                Log.e(DEBUG_TAG, "Bookmark storage migration failed");
            }
        }
    }
}
