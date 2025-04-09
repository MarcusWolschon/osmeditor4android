package de.blau.android.photos;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.acra.ACRA;
import org.xmlpull.v1.XmlPullParserException;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.MimeTypes;
import de.blau.android.contract.Paths;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.ContentResolverUtil;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Util;
import de.blau.android.util.rtree.RTree;

/**
 * Scan the system for geo ref photos and store is in a DB
 * 
 * @author Simon
 *
 */
public class PhotoIndex extends SQLiteOpenHelper {

    private static final int   DATA_VERSION = 7;
    public static final String DB_NAME      = PhotoIndex.class.getSimpleName();

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PhotoIndex.class.getSimpleName().length());
    private static final String DEBUG_TAG = PhotoIndex.class.getSimpleName().substring(0, TAG_LEN);

    private static final String NOVESPUCCI = ".novespucci";

    private static final String MEDIA_STORE = "MediaStore";
    private static final String OSMTRACKER  = "osmtracker";
    private static final String DCIM        = "DCIM";

    private static final String SOURCES_TABLE      = "directories";
    private static final String PHOTOS_TABLE       = "photos";
    private static final String NAME_COLUMN        = "name";
    private static final String SOURCE_COLUMN      = "source";
    private static final String LAT_COLUMN         = "lat";
    private static final String LON_COLUMN         = "lon";
    private static final String DIRECTION_COLUMN   = "direction";
    private static final String ORIENTATION_COLUMN = "orientation";
    private static final String URI_COLUMN         = "dir";        // historically this was the dir
    private static final String URI_WHERE          = "dir = ?";
    private static final String LAST_SCAN_COLUMN   = "last_scan";
    private static final String TAG_COLUMN         = "tag";

    private static final String INSERT_INTO = "INSERT INTO ";
    private static final String ALTER_TABLE = "ALTER TABLE ";
    private static final String DELETE_FROM = "DELETE FROM ";

    private static final int LAT_INDEX         = 0;
    private static final int LON_INDEX         = 1;
    private static final int DIRECTION_INDEX   = 2;
    private static final int DIR_INDEX         = 3;
    private static final int NAME_INDEX        = 4;
    private static final int ORIENTATION_INDEX = 5;

    private final Context context;

    /**
     * Provide access to the on disk Photo index
     * 
     * @param context an Android Context
     */
    public PhotoIndex(@NonNull Context context) {
        super(context, DEBUG_TAG, null, DATA_VERSION);
        this.context = context;
    }

    @Override
    public synchronized void onCreate(SQLiteDatabase db) {
        Log.d(DEBUG_TAG, "Creating photo index DB");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + PHOTOS_TABLE
                + " (lat int, lon int, direction int DEFAULT NULL, dir VARCHAR, name VARCHAR, source VARCHAR DEFAULT NULL, orientation int DEFAULT 0);");
        db.execSQL("CREATE INDEX latidx ON " + PHOTOS_TABLE + " (lat)");
        db.execSQL("CREATE INDEX lonidx ON " + PHOTOS_TABLE + " (lon)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + SOURCES_TABLE + " (dir VARCHAR, last_scan int8, tag VARCHAR DEFAULT NULL);");
        initSource(db, DCIM, null);
        initSource(db, Paths.DIRECTORY_PATH_VESPUCCI, null);
        initSource(db, OSMTRACKER, null);
        initSource(db, MEDIA_STORE, "");
    }

    /**
     * Initialize a source entry
     *
     * @param db a writable database
     * @param source the source to init
     * @param tag the initial tag
     */
    private void initSource(@NonNull SQLiteDatabase db, @NonNull String source, @Nullable String tag) {
        db.execSQL(INSERT_INTO + SOURCES_TABLE + " VALUES ('" + source + "', 0, " + (tag == null ? "NULL" : "''") + ");");
    }

    @Override
    public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(DEBUG_TAG, "Upgrading photo index DB");
        if (oldVersion <= 2) {
            db.execSQL(ALTER_TABLE + PHOTOS_TABLE + " ADD direction int DEFAULT NULL");
        }
        if (oldVersion <= 4) {
            db.execSQL(DELETE_FROM + PHOTOS_TABLE); // this should force a complete reindex
        }
        if (oldVersion <= 5) {
            db.execSQL(ALTER_TABLE + PHOTOS_TABLE + " ADD source VARCHAR DEFAULT NULL");
            db.execSQL(ALTER_TABLE + SOURCES_TABLE + " ADD tag VARCHAR DEFAULT NULL");
            initSource(db, MEDIA_STORE, "");
            db.execSQL(DELETE_FROM + PHOTOS_TABLE);
        }
        if (oldVersion <= 6) {
            db.execSQL(ALTER_TABLE + PHOTOS_TABLE + " ADD orientation int DEFAULT 0");
            db.execSQL(DELETE_FROM + PHOTOS_TABLE);
            updateSources(db, MEDIA_STORE, null, System.currentTimeMillis());
        }
    }

    @Override
    public synchronized void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(DEBUG_TAG, "Recreate from scratch");
        db.execSQL("DROP TABLE " + PHOTOS_TABLE);
        db.execSQL("DROP TABLE " + SOURCES_TABLE);
        onCreate(db);
    }

    /**
     * Create or update the index of images on the device
     */
    public synchronized void createOrUpdateIndex() {
        Log.d(DEBUG_TAG, "starting scan");
        indexDirectories();
        Logic logic = App.getLogic();
        Preferences prefs = logic != null ? logic.getPrefs() : null;
        if (prefs != null) {
            final boolean accessMediaLocation = Util.permissionGranted(context, Manifest.permission.ACCESS_MEDIA_LOCATION);
            Log.d(DEBUG_TAG, "ACCESS_MEDIA_LOCATION permission " + accessMediaLocation);
            if (prefs.scanMediaStore() && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || accessMediaLocation)) {
                indexMediaStore();
            } else {
                // delete scanned photos from index
                try (SQLiteDatabase db = getWritableDatabase()) {
                    db.delete(PHOTOS_TABLE, SOURCE_COLUMN + "= ?", new String[] { MEDIA_STORE });
                    updateSources(db, MEDIA_STORE, "", 0);
                }
            }
        }
    }

    /**
     * Remove all information from the index
     */
    public synchronized void resetIndex() {
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.execSQL(DELETE_FROM + PHOTOS_TABLE);
            db.execSQL(DELETE_FROM + SOURCES_TABLE);
        }
    }

    /**
     * Index photos from the MediaStore
     */
    public void indexMediaStore() {
        Log.d(DEBUG_TAG, "scanning MediaStore");
        try (SQLiteDatabase db = getWritableDatabase()) {
            final String mediaStoreVersion = MediaStore.getVersion(context);
            if (mediaStoreVersion.equals(getTag(db, MEDIA_STORE))) {
                Log.d(DEBUG_TAG, "MediaStore unchanged");
                return;
            }
            db.delete(PHOTOS_TABLE, SOURCE_COLUMN + " = ?", new String[] { MEDIA_STORE });
            String[] projection = new String[] { BaseColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaColumns.MIME_TYPE };
            try (Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                    MediaColumns.MIME_TYPE + " = ? OR " + MediaColumns.MIME_TYPE + " = ?", new String[] { MimeTypes.JPEG, MimeTypes.HEIC }, null)) {
                Log.d(DEBUG_TAG, "Media store contains " + cursor.getCount() + " entries");
                // Cache column indices.
                int idColumn = cursor.getColumnIndexOrThrow(BaseColumns._ID);
                int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                int mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
                while (cursor.moveToNext()) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && MimeTypes.HEIC.equals(cursor.getString(mimeTypeColumn))) {
                        // skip HEIC images is we are on an old Android version
                        continue;
                    }
                    Uri photoUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getString(idColumn));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        photoUri = MediaStore.setRequireOriginal(photoUri);
                    }
                    if (!isIndexed(db, photoUri)) {
                        String path = ContentResolverUtil.getDataColumn(context, photoUri, null, null);
                        if (path == null || !isIndexed(db, path)) {
                            addPhoto(context, db, photoUri, cursor.getString(displayNameColumn));
                        }
                    }
                }
            }
            updateSources(db, MEDIA_STORE, mediaStoreVersion, System.currentTimeMillis());
        }
    }

    /**
     * Update a source record
     * 
     * @param db a writable database
     * @param source the source to update
     * @param tag the tag
     * @param lastScan the time of last scan
     */
    private void updateSources(@NonNull SQLiteDatabase db, @NonNull String source, @Nullable String tag, long lastScan) {
        Log.d(DEBUG_TAG, "updating " + source + " to scan " + lastScan + " tag " + tag);
        ContentValues lastVersion = new ContentValues();
        lastVersion.put(TAG_COLUMN, tag);
        lastVersion.put(LAST_SCAN_COLUMN, lastScan);
        db.update(SOURCES_TABLE, lastVersion, URI_WHERE, new String[] { source });
    }

    /**
     * Index any photos found in interesting directories
     * 
     * On Android 11 and later this will only find images that are owned by the app
     */
    private void indexDirectories() {
        Log.d(DEBUG_TAG, "scanning directories");
        List<String> mountPoints = getMountPoints(Environment.getExternalStorageDirectory()); // NOSONAR

        try (SQLiteDatabase db = getWritableDatabase();
                Cursor dbresult = db.query(SOURCES_TABLE, new String[] { URI_COLUMN, LAST_SCAN_COLUMN, TAG_COLUMN }, TAG_COLUMN + " is NULL", null, null, null,
                        null, null)) {
            int dirCount = dbresult.getCount();
            dbresult.moveToFirst();
            // loop over the directories configured
            for (int i = 0; i < dirCount; i++) {
                String dir = dbresult.getString(0);
                long lastScan = dbresult.getLong(1);
                Log.d(DEBUG_TAG, dbresult.getString(0) + " " + dbresult.getLong(1));
                // loop over all possible mount points
                for (String m : mountPoints) {
                    File indir = new File(m, dir);
                    Log.d(DEBUG_TAG, "Scanning directory " + indir.getAbsolutePath());
                    if (!indir.exists()) {
                        Log.d(DEBUG_TAG, "Directory " + indir.getAbsolutePath() + " doesn't exist");
                        // remove all entries for this directory
                        db.delete(PHOTOS_TABLE, URI_WHERE, new String[] { indir.getAbsolutePath() });
                        db.delete(PHOTOS_TABLE, "dir LIKE ?", new String[] { indir.getAbsolutePath() + "/%" });
                        continue;
                    }
                    try (Cursor dbresult2 = db.query(PHOTOS_TABLE, new String[] { "distinct dir" }, "dir LIKE '" + indir.getAbsolutePath() + "%'", null, null,
                            null, null, null)) {
                        int dirCount2 = dbresult2.getCount();
                        dbresult2.moveToFirst();
                        for (int j = 0; j < dirCount2; j++) {
                            String photo = dbresult2.getString(0);
                            if (!new File(photo).exists()) {
                                Log.d(DEBUG_TAG, "Deleting entries for gone photo " + photo);
                                db.delete(PHOTOS_TABLE, URI_WHERE, new String[] { photo });
                            }
                            dbresult2.moveToNext();
                        }
                        scanDir(db, indir.getAbsolutePath(), lastScan);
                        updateSources(db, indir.getName(), null, System.currentTimeMillis());
                    }
                }
                dbresult.moveToNext();
            }
        } catch (SQLiteException ex) {
            // Don't crash just report
            ACRAHelper.nocrashReport(ex, ex.getMessage());
        }
    }

    /**
     * Determine at least a few of the possible mount points
     * 
     * @param extStorage location of external storage
     * @return list of directories
     */
    private List<String> getMountPoints(File extStorage) {
        List<String> mountPoints = new ArrayList<>();
        mountPoints.add(extStorage.getAbsolutePath());
        mountPoints.add(extStorage.getAbsolutePath() + Paths.DIRECTORY_PATH_EXTERNAL_SD_CARD);
        mountPoints.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()); // NOSONAR
        File storageDir = new File(Paths.DIRECTORY_PATH_STORAGE);
        File[] list = storageDir.listFiles();
        if (list != null) {
            for (File f : list) {
                if (f.exists() && f.isDirectory() && !extStorage.getAbsolutePath().equals(f.getAbsolutePath())) {
                    Log.d(DEBUG_TAG, "Adding mount point " + f.getAbsolutePath());
                    mountPoints.add(f.getAbsolutePath());
                }
            }
        }
        return mountPoints;
    }

    /**
     * Recursively scan directories and add images to index
     * 
     * @param db database containing the index
     * @param dir directory we are starting with
     * @param lastScan date we last scanned this directory tree
     */
    private void scanDir(@NonNull SQLiteDatabase db, @NonNull String dir, long lastScan) {
        File indir = new File(dir);
        boolean needsReindex = false;
        if (indir.lastModified() >= lastScan) { // directory was modified
            // remove all entries
            Log.d(DEBUG_TAG, "deleteing refs for reindex");
            try {
                db.delete(PHOTOS_TABLE, URI_WHERE, new String[] { indir.getAbsolutePath() });
            } catch (SQLiteException sqex) {
                Log.d(DEBUG_TAG, sqex.toString());
                ACRAHelper.nocrashReport(sqex, sqex.getMessage());
            }
            needsReindex = true;
        }
        // now process
        File[] list = indir.listFiles();
        if (list == null || dontIndex(list)) {
            return;
        }
        for (File f : list) {
            if (f.isDirectory()) {
                // recursive decent
                scanDir(db, f.getAbsolutePath(), lastScan);
            }
            if (!needsReindex) {
                continue;
            }
            String extension = getFileExtension(f.getName().toLowerCase(Locale.US));
            if (FileExtensions.JPG.equals(extension) || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && FileExtensions.HEIC.equals(extension))) {
                addPhoto(db, indir, f);
            }
        }
    }

    /**
     * Check if the list of File contains a .novespucci file
     * 
     * @param true if the file is found
     */
    private boolean dontIndex(File[] list) {
        // check if we shouldn't process this directory, not the most efficient way likely
        for (File f : list) {
            if (NOVESPUCCI.equals(f.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the file extension if any
     * 
     * @param fileName the full file name
     * @return the extension of the the file name if there is none
     */
    private String getFileExtension(@NonNull String fileName) {
        String[] parts = fileName.split("\\.");
        return parts[parts.length - 1];
    }

    /**
     * Add image to index
     * 
     * @param f the image file
     * @return a Photo object or null
     */
    @Nullable
    public synchronized Photo addPhoto(@NonNull File f) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            Photo p = addPhoto(db, f.getParentFile(), f);
            addToIndex(p);
            return p;
        }
    }

    /**
     * Add image to index
     * 
     * @param p the Photo object
     */
    public synchronized void addPhoto(@NonNull Photo p) {
        SQLiteDatabase db = getWritableDatabase();
        insertPhoto(db, p, p.getRef(), null);
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
            insertPhoto(db, p, f.getName(), null);
            return p;
        } catch (NumberFormatException | IOException e) {
            // ignore silently, broken pictures are not our business
        }
        return null;
    }

    /**
     * Add image to index
     * 
     * @param context an Android Context
     * @param uri the Uri for the photo
     * @param displayName a name for the photo for display purposes
     * @return a Photo object
     */
    @Nullable
    public synchronized Photo addPhoto(@NonNull Context context, @NonNull Uri uri, @Nullable String displayName) {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            return addPhoto(context, db, uri, displayName != null ? displayName : uri.getLastPathSegment());
        } finally {
            SavingHelper.close(db);
        }
    }

    /**
     * Add image to index
     * 
     * @param context an Android Context
     * @param db database containing the index
     * @param uri the Uri for the photo
     * @param displayName a name for the photo for display purposes
     * @return a Photo object
     */
    @Nullable
    private Photo addPhoto(@NonNull Context context, @NonNull SQLiteDatabase db, @NonNull Uri uri, @NonNull String displayName) {
        try {
            Photo p = new Photo(context, uri, displayName);
            insertPhoto(db, p, displayName, MEDIA_STORE);
            return p;
        } catch (NumberFormatException | IOException e) {
            // ignore silently, broken pictures are not our business
        }
        return null;
    }

    /**
     * Check if we have already indexed the photo
     * 
     * @param uri the Uri for the photo
     * @return true if already present
     */
    public boolean isIndexed(@NonNull Uri uri) {
        SQLiteDatabase db = null;
        try {
            db = getReadableDatabase();
            return isIndexed(db, uri);
        } finally {
            SavingHelper.close(db);
        }
    }

    /**
     * Check if we have already indexed the photo
     * 
     * @param uri the Uri as a String for the photo
     * @return true if already present
     */
    public boolean isIndexed(@NonNull String uri) {
        SQLiteDatabase db = null;
        try {
            db = getReadableDatabase();
            return isIndexed(db, uri);
        } finally {
            SavingHelper.close(db);
        }
    }

    /**
     * Check if we have already indexed the photo
     * 
     * @param db a readable database
     * @param uri the Uri for the photo
     * @return true if already present
     */
    private boolean isIndexed(@NonNull SQLiteDatabase db, @NonNull Uri uri) {
        return isIndexed(db, uri.toString());
    }

    /**
     * Check if we have already indexed the photo
     * 
     * @param db a readable database
     * @param uriString the Uri for the photo
     * @return true if already present
     */
    private boolean isIndexed(@NonNull SQLiteDatabase db, @NonNull String uriString) {
        try (Cursor dbresult = db.query(PHOTOS_TABLE, new String[] { URI_COLUMN }, URI_COLUMN + "  = ?", new String[] { uriString }, null, null, null, null)) {
            return dbresult.getCount() > 0;
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, ex.getMessage());
            return true;
        }
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
     * @param source an indication of where the photo was found on the device
     */
    private void insertPhoto(@NonNull SQLiteDatabase db, @NonNull Photo photo, @NonNull String name, @Nullable String source) {
        try {
            ContentValues values = new ContentValues();
            values.put(LAT_COLUMN, photo.getLat());
            values.put(LON_COLUMN, photo.getLon());
            if (photo.hasDirection()) {
                values.put(DIRECTION_COLUMN, photo.getDirection());
            }
            values.put(URI_COLUMN, photo.getRef());
            values.put(NAME_COLUMN, name);
            if (source != null) {
                values.put(SOURCE_COLUMN, source);
            }
            values.put(ORIENTATION_COLUMN, photo.getOrientation());
            db.insert(PHOTOS_TABLE, null, values);
        } catch (SQLiteException sqex) {
            Log.d(DEBUG_TAG, sqex.toString());
            ACRAHelper.nocrashReport(sqex, sqex.getMessage());
        } catch (NumberFormatException bfex) {
            // ignore silently, broken pictures are not our business
        } catch (Exception ex) {
            Log.d(DEBUG_TAG, ex.toString());
            ACRAHelper.nocrashReport(ex, ex.getMessage());
        } // ignore
    }

    /**
     * Get the tag value for a source
     * 
     * @param db the database
     * @param source the source
     * @return the tag value or null
     */
    @Nullable
    private String getTag(@NonNull SQLiteDatabase db, @NonNull String source) {
        try (Cursor dbresult = db.query(SOURCES_TABLE, new String[] { TAG_COLUMN, URI_COLUMN }, URI_WHERE, new String[] { source }, null, null, null, null)) {
            if (dbresult.getCount() >= 1) {
                dbresult.moveToFirst();
                return dbresult.getString(0);
            }
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, ex.getMessage());
        }
        return null;
    }

    /**
     * Try to remove an entry from both the in memory and the on device index
     * 
     * As this might not be something that we have actually indexed, fail gracefully.
     * 
     * @param context an Android Context
     * @param uri the uri
     * @return true if successful
     */
    public boolean deletePhoto(@NonNull Context context, @NonNull Uri uri) {
        return deletePhoto(context, uri.toString());
    }

    /**
     * Try to remove an entry from both the in memory and the on device index
     * 
     * As this might not be something that we have actually indexed, fail gracefully.
     * 
     * @param context an Android Context
     * @param uriString the uri or path as a String
     * @return true if successful
     */
    public boolean deletePhoto(@NonNull Context context, @NonNull String uriString) {
        Log.d(DEBUG_TAG, "deletePhoto " + uriString);
        try (SQLiteDatabase db = getWritableDatabase();
                Cursor dbresult = db.query(PHOTOS_TABLE, new String[] { URI_COLUMN, LON_COLUMN, LAT_COLUMN }, URI_WHERE, new String[] { uriString }, null, null,
                        null, null)) {
            if (dbresult.getCount() > 0) {
                RTree<Photo> index = App.getPhotoIndex();
                if (index != null) {
                    dbresult.moveToFirst();
                    Collection<Photo> existing = getPhotosFromIndex(index, new BoundingBox(dbresult.getInt(1), dbresult.getInt(2)));
                    boolean removed = false;
                    for (Photo p : existing) {
                        if (p.getRef().equals(uriString) && index.remove(p)) {
                            removed = true;
                            break;
                        }
                    }
                    if (!removed) {
                        Log.e(DEBUG_TAG, "deletePhoto uri not removed from RTree");
                    }
                }
                return db.delete(PHOTOS_TABLE, URI_WHERE, new String[] { uriString }) > 0;
            }
            Log.e(DEBUG_TAG, "deletePhoto uri not found in database");
            return false;
        }
    }

    /**
     * Return all photographs in a given bounding box, if necessary fill in-memory index first
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
            Cursor dbresult = db.query(PHOTOS_TABLE, new String[] { LAT_COLUMN, LON_COLUMN, DIRECTION_COLUMN, URI_COLUMN, NAME_COLUMN, ORIENTATION_COLUMN },
                    null, null, null, null, null, null);
            int photoCount = dbresult.getCount();
            dbresult.moveToFirst();
            Log.i(DEBUG_TAG, "Query returned " + photoCount + " photos");
            //
            for (int i = 0; i < photoCount; i++) {
                String name = dbresult.getString(NAME_INDEX);
                String dir = dbresult.getString(DIR_INDEX);
                Photo newPhoto;
                if (dbresult.isNull(DIRECTION_INDEX)) { // no direction
                    newPhoto = new Photo(dbresult.getInt(LAT_INDEX), dbresult.getInt(LON_INDEX), dir, name, dbresult.getInt(ORIENTATION_INDEX));
                } else {
                    newPhoto = new Photo(dbresult.getInt(LAT_INDEX), dbresult.getInt(LON_INDEX), dbresult.getInt(DIRECTION_INDEX), dir, name,
                            dbresult.getInt(ORIENTATION_INDEX));
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
        return queryResult;
    }

    /**
     * Add an image to the media store
     * 
     * @param filePath the path of the image
     */
    public static void addImageToMediaStore(@NonNull ContentResolver cr, String filePath) {
        new ExecutorTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void v) throws NumberFormatException, XmlPullParserException, IOException {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis());
                values.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis());
                values.put(MediaStore.MediaColumns.DATA, filePath);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, (String) null);
                    values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                    Uri uri = cr.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), values);
                    values.clear();
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                    cr.update(uri, values, null, null);
                } else {
                    cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                }
                return null;
            }
        }.execute();
    }
}
