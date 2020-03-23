package de.blau.android.photos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.acra.ACRA;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.contract.Paths;
import de.blau.android.osm.BoundingBox;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.rtree.RTree;

/**
 * Scan the system for geo ref photos and store is in a DB
 * 
 * @author Simon
 *
 */
public class PhotoIndex extends SQLiteOpenHelper {

    private static final int    DATA_VERSION = 5;
    private static final String LOGTAG       = "PhotoIndex";

    private static final String DIRECTORIES_TABLE = "directories";
    private static final String PHOTOS_TABLE      = "photos";
    private static final String NAME_COLUMN       = "name";
    private static final String LAT_COLUMN        = "lat";
    private static final String LON_COLUMN        = "lon";
    private static final String DIRECTION_COLUMN  = "direction";
    private static final String DIR_COLUMN        = "dir";
    private static final String DIR_WHERE         = "dir = ?";

    private static final String INSERT_INTO = "INSERT INTO ";

    /**
     * Provide access to the on disk Photo index
     * 
     * @param context an Android Context
     */
    public PhotoIndex(@NonNull Context context) {
        super(context, "PhotoIndex", null, DATA_VERSION);
    }

    @Override
    public synchronized void onCreate(SQLiteDatabase db) {
        Log.d(LOGTAG, "Creating photo index DB");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + PHOTOS_TABLE + " (lat int, lon int, direction int DEFAULT NULL, dir VARCHAR, name VARCHAR);");
        db.execSQL("CREATE INDEX latidx ON " + PHOTOS_TABLE + " (lat)");
        db.execSQL("CREATE INDEX lonidx ON " + PHOTOS_TABLE + " (lon)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + DIRECTORIES_TABLE + " (dir VARCHAR, last_scan int8);");
        db.execSQL(INSERT_INTO + DIRECTORIES_TABLE + " VALUES ('DCIM', 0);");
        db.execSQL(INSERT_INTO + DIRECTORIES_TABLE + " VALUES ('Vespucci', 0);");
        db.execSQL(INSERT_INTO + DIRECTORIES_TABLE + " VALUES ('osmtracker', 0);");
    }

    @Override
    public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOGTAG, "Upgrading photo index DB");
        if (oldVersion <= 2) {
            db.execSQL("ALTER TABLE " + PHOTOS_TABLE + " ADD direction int DEFAULT NULL");
        }
        if (oldVersion <= 4) {
            db.execSQL("DELETE FROM " + PHOTOS_TABLE); // this should force a complete reindex
        }
    }

    @Override
    public synchronized void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOGTAG, "Recreate from scratch");
        db.execSQL("DROP TABLE " + PHOTOS_TABLE);
        db.execSQL("DROP TABLE " + DIRECTORIES_TABLE);
        onCreate(db);
    }

    /**
     * Create or update the index of images on the device
     */
    public synchronized void createOrUpdateIndex() {
        Log.d(LOGTAG, "starting scan");
        // determine at least a few of the possible mount points
        File sdcard = Environment.getExternalStorageDirectory();
        ArrayList<String> mountPoints = new ArrayList<>();
        mountPoints.add(sdcard.getAbsolutePath());
        mountPoints.add(sdcard.getAbsolutePath() + Paths.DIRECTORY_PATH_EXTERNAL_SD_CARD);
        File storageDir = new File(Paths.DIRECTORY_PATH_STORAGE);
        File[] list = storageDir.listFiles();
        if (list != null) {
            for (File f : list) {
                if (f.exists() && f.isDirectory() && !sdcard.getAbsolutePath().equals(f.getAbsolutePath())) {
                    Log.d(LOGTAG, "Adding mount point " + f.getAbsolutePath());
                    mountPoints.add(f.getAbsolutePath());
                }
            }
        }

        try {
            SQLiteDatabase db = getWritableDatabase();
            Cursor dbresult = db.query(DIRECTORIES_TABLE, new String[] { DIR_COLUMN, "last_scan" }, null, null, null, null, null, null);
            int dirCount = dbresult.getCount();
            dbresult.moveToFirst();
            // loop over the directories configured
            for (int i = 0; i < dirCount; i++) {
                String dir = dbresult.getString(0);
                long lastScan = dbresult.getLong(1);
                Log.d(LOGTAG, dbresult.getString(0) + " " + dbresult.getLong(1));
                // loop over all possible mount points
                for (String m : mountPoints) {
                    File indir = new File(m + "/" + dir);
                    Log.d(LOGTAG, "Scanning directory " + indir.getAbsolutePath());
                    if (indir.exists()) {
                        Cursor dbresult2 = db.query(PHOTOS_TABLE, new String[] { "distinct dir" }, "dir LIKE '" + indir.getAbsolutePath() + "%'", null, null,
                                null, null, null);
                        int dirCount2 = dbresult2.getCount();
                        dbresult2.moveToFirst();
                        for (int j = 0; j < dirCount2; j++) {
                            String dir2 = dbresult2.getString(0);
                            Log.d(LOGTAG, "Checking dir " + dir2);
                            File pDir = new File(dir2);
                            if (!pDir.exists()) {
                                Log.d(LOGTAG, "Deleting entries for gone dir " + dir2);
                                db.delete(PHOTOS_TABLE, DIR_WHERE, new String[] { dir2 });
                            }
                            dbresult2.moveToNext();
                        }
                        dbresult2.close();
                        scanDir(db, indir.getAbsolutePath(), lastScan);
                        ContentValues values = new ContentValues();
                        Log.d(LOGTAG, "updating last scan for " + indir.getName() + " to " + System.currentTimeMillis());
                        values.put("last_scan", System.currentTimeMillis());
                        db.update(DIRECTORIES_TABLE, values, DIR_WHERE, new String[] { indir.getName() });
                    } else {
                        Log.d(LOGTAG, "Directory " + indir.getAbsolutePath() + " doesn't exist");
                        // remove all entries for this directory
                        db.delete(PHOTOS_TABLE, DIR_WHERE, new String[] { indir.getAbsolutePath() });
                        db.delete(PHOTOS_TABLE, "dir LIKE ?", new String[] { indir.getAbsolutePath() + "/%" });
                    }
                }
                dbresult.moveToNext();
            }
            dbresult.close();
            db.close();
        } catch (SQLiteException ex) {
            // Don't crash just report
            ACRAHelper.nocrashReport(ex, ex.getMessage());
        }
    }

    /**
     * Recursively scan directories and add images to index
     * 
     * @param db database containing the index
     * @param dir directory we are starting with
     * @param lastScan date we last scanned this directory tree
     */
    private void scanDir(@NonNull SQLiteDatabase db, @NonNull String dir, long lastScan) {
        // Log.d(LOGTAG,"directory " + dir + " last Scan " + (new Date(lastScan)).toString());
        File indir = new File(dir);
        boolean needsReindex = false;
        if (indir != null) {
            if (indir.lastModified() >= lastScan) { // directory was modified
                // remove all entries
                Log.d(LOGTAG, "deleteing refs for reindex");
                try {
                    db.delete(PHOTOS_TABLE, DIR_WHERE, new String[] { indir.getAbsolutePath() });
                } catch (SQLiteException sqex) {
                    Log.d(LOGTAG, sqex.toString());
                    ACRAHelper.nocrashReport(sqex, sqex.getMessage());
                }
                needsReindex = true;
            }
            // now process
            File[] list = indir.listFiles();
            if (list == null) {
                return;
            }
            // check if we shouldn't process this directory, not the most efficient way likely
            for (File f : list) {
                if (f.getName().equals(".novespucci")) {
                    return;
                }
            }
            for (File f : list) {
                if (f.isDirectory()) {
                    // recursive decent
                    scanDir(db, f.getAbsolutePath(), lastScan);
                }
                if (needsReindex && f.getName().toLowerCase(Locale.US).endsWith(Paths.FILE_EXTENSION_IMAGE)) {
                    addPhoto(db, indir, f);
                }
            }
        }
    }

    /**
     * Add image to index
     * 
     * @param f the image file
     */
    public synchronized void addPhoto(@NonNull File f) {
        SQLiteDatabase db = getWritableDatabase();
        // Log.i(LOGTAG,"Adding entry in " + f.getParent());
        Photo p = addPhoto(db, f.getParentFile(), f);
        db.close();
        addToIndex(p);
    }

    /**
     * Add image to index
     * 
     * @param p the Photo object
     */
    public synchronized void addPhoto(@NonNull Photo p) {
        SQLiteDatabase db = getWritableDatabase();
        insertPhoto(db, p, p.getRef());
        db.close();
        addToIndex(p);
    }

    /**
     * Add image to index
     * 
     * @param db database containing the index
     * @param dir directory the image is in
     * @param f the image file
     * @return a Photo object
     */
    @Nullable
    private Photo addPhoto(@NonNull SQLiteDatabase db, @NonNull File dir, @NonNull File f) {
        try {
            Photo p = new Photo(dir, f);
            insertPhoto(db, p, f.getName());
            return p;
        } catch (NumberFormatException | IOException e) {
            // ignore silently, broken pictures are not our business
        }
        return null;
    }

    /**
     * Add the photo to the in memory index
     * 
     * @param p the Photo
     */
    public static void addToIndex(@Nullable Photo p) {
        RTree<Photo> index = App.getPhotoIndex();
        if (p != null && index != null) { // if nothing is in the index the complete DB including this photo will be
                                          // added
            index.insert(p);
        }
    }

    /**
     * Insert a photo in to the on device index
     * 
     * @param db database containing the index
     * @param photo the photo object
     * @param name a short identifying name
     */
    private void insertPhoto(@NonNull SQLiteDatabase db, @NonNull Photo photo, @NonNull String name) {
        // Log.i(LOGTAG,"Adding entry for " + dir.getName() + " " + f.getName() + " abs " + dir.getAbsolutePath());
        try {
            ContentValues values = new ContentValues();
            values.put(LAT_COLUMN, photo.getLat());
            values.put(LON_COLUMN, photo.getLon());
            // Log.i(LOGTAG,"Lat: " + p.getLat() + " " + p.getLon());
            if (photo.hasDirection()) {
                values.put(DIRECTION_COLUMN, photo.getDirection());
            }
            values.put(DIR_COLUMN, photo.getRef());
            values.put(NAME_COLUMN, name);
            db.insert(PHOTOS_TABLE, null, values);
        } catch (SQLiteException sqex) {
            Log.d(LOGTAG, sqex.toString());
            ACRAHelper.nocrashReport(sqex, sqex.getMessage());
        } catch (NumberFormatException bfex) {
            // ignore silently, broken pictures are not our business
        } catch (Exception ex) {
            Log.d(LOGTAG, ex.toString());
            ACRAHelper.nocrashReport(ex, ex.getMessage());
        } // ignore
    }

    /**
     * Try to remove an entry from both the in memory as the on device index
     * 
     * As this might not be something that we have actually indexed, fail gracefully
     * 
     * @param context an Android Context
     * @param uri the uri
     * @return true if successful
     */
    public boolean deletePhoto(@NonNull Context context, @NonNull Uri uri) {

        try {
            Photo photo = new Photo(context, uri);
            RTree<Photo> index = App.getPhotoIndex();
            if (index != null) {
                // check if this is an existing indexed photo
                Collection<Photo> existing = getPhotosFromIndex(index, photo.getBounds());
                String name = uri.getLastPathSegment();
                if (name != null) {
                    for (Photo p : existing) {
                        Uri dbUri = p.getRefUri(context);
                        String dbName = dbUri.getLastPathSegment();
                        if (name.equals(dbName)) {
                            index.remove(p); // NOSONAR
                            SQLiteDatabase db = getWritableDatabase();
                            String path = p.getRef();
                            path = path.substring(0, path.lastIndexOf('/'));
                            int rows = db.delete(PHOTOS_TABLE, DIR_WHERE + " AND name = ?", new String[] { path, dbName });
                            return rows == 1;
                        }
                    }
                }
            }
        } catch (NumberFormatException | IOException e) {
            Log.e(LOGTAG, "Exception " + e.getMessage());
        }
        Log.e(LOGTAG, "Unable to remove " + uri.toString());
        return false;
    }

    /**
     * Return all photographs in a given bounding box If necessary fill in-memory index first
     * 
     * @param box the BoundingBox we are interested in
     * @return a Collection of the Photos in the BoundingBox
     */
    @NonNull
    public List<Photo> getPhotos(@NonNull BoundingBox box) {
        RTree<Photo> index = App.getPhotoIndex();
        if (index == null) {
            return new ArrayList<>();
        }

        return getPhotosFromIndex(index, box);
    }

    /**
     * Create the in-memory index from the on device database
     * 
     * @param index the current in memory index or null
     */
    public synchronized void fill(@Nullable RTree<Photo> index) {
        if (index == null) {
            App.resetPhotoIndex(); // allocate r-tree
            index = App.getPhotoIndex();
        }
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor dbresult = db.query(PHOTOS_TABLE, new String[] { LAT_COLUMN, LON_COLUMN, DIRECTION_COLUMN, DIR_COLUMN, NAME_COLUMN }, null, null, null, null,
                    null, null);
            int photoCount = dbresult.getCount();
            dbresult.moveToFirst();
            Log.i(LOGTAG, "Query returned " + photoCount + " photos");
            //
            for (int i = 0; i < photoCount; i++) {
                String name = dbresult.getString(4);
                String dir = dbresult.getString(3);
                Photo newPhoto;
                if (dbresult.isNull(2)) { // no direction
                    newPhoto = new Photo(dbresult.getInt(0), dbresult.getInt(1), dir, name);
                } else {
                    newPhoto = new Photo(dbresult.getInt(0), dbresult.getInt(1), dbresult.getInt(2), dir, name);
                }
                if (!index.contains(newPhoto)) {
                    index.insert(newPhoto);
                }
                dbresult.moveToNext();
            }
            dbresult.close();
            db.close();
        } catch (SQLiteException ex) {
            // shoudn't happen (getReadableDatabase failed), simply report for now
            ACRA.getErrorReporter().handleException(ex);
        }
    }

    /**
     * Query the in memory index for Photos
     * 
     * @param index the index
     * @param box the BoundingBox we are interested in
     * @return a List of Photos
     */
    @NonNull
    private List<Photo> getPhotosFromIndex(@NonNull RTree<Photo> index, @NonNull BoundingBox box) {
        List<Photo> queryResult = new ArrayList<>();
        index.query(queryResult, box.getBounds());
        Log.d(LOGTAG, "result count " + queryResult.size());
        return queryResult;
    }
}
