package de.blau.android.photos;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import org.acra.ACRA;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;
import de.blau.android.contract.Paths;

/**
 * Scan the system for geo ref photos and store is in a DB
 * @author simon
 *
 */
public class PhotoIndex extends SQLiteOpenHelper {
	
	private final static int DATA_VERSION = 3;
	private final static String LOGTAG = "PhotoIndex";
	private final Context ctx;

	
	class JpgFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(Paths.FILE_EXTENSION_IMAGE);
		}
	}
	
	public PhotoIndex(Context context) {
		super(context, "PhotoIndex", null, DATA_VERSION);
		ctx = context;
	}
	
	@Override
	public synchronized void onCreate(SQLiteDatabase db) {
		Log.d(LOGTAG, "Creating photo index DB");
		db.execSQL("CREATE TABLE IF NOT EXISTS photos (lat int, lon int, direction int DEFAULT NULL, dir VARCHAR, name VARCHAR);");
		db.execSQL("CREATE INDEX latidx ON photos (lat)");
		db.execSQL("CREATE INDEX lonidx ON photos (lon)");
		db.execSQL("CREATE TABLE IF NOT EXISTS directories  (dir VARCHAR, last_scan int8);");
		db.execSQL("INSERT INTO directories VALUES ('DCIM', 0);");
		db.execSQL("INSERT INTO directories VALUES ('Vespucci', 0);");
		db.execSQL("INSERT INTO directories VALUES ('osmtracker', 0);");
	}

	@Override
	public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(LOGTAG, "Upgrading photo index DB");
		if (oldVersion <= 2) {
			db.execSQL("ALTER TABLE photos ADD direction int DEFAULT NULL");
		}
	}
	
	public synchronized void createOrUpdateIndex()
	{
		Log.d(LOGTAG,"starting scan");
		// determine at least a few of the possible mount points
		File sdcard = Environment.getExternalStorageDirectory();
		ArrayList<String> mountPoints = new ArrayList<String>();
		mountPoints.add(sdcard.getAbsolutePath());
		mountPoints.add(sdcard.getAbsolutePath() + Paths.DIRECTORY_PATH_EXTERNAL_SD_CARD);
		File storageDir = new File(Paths.DIRECTORY_PATH_STORAGE);
		File[] list = storageDir.listFiles();
		if (list != null) {
			for (File f:list) {
				if (f.exists() && f.isDirectory() && !sdcard.getAbsolutePath().equals(f.getAbsolutePath())) {
					Log.d(LOGTAG, "Adding mount point " + f.getAbsolutePath());
					mountPoints.add(f.getAbsolutePath());
				}
			}
		}
		
		try {
			SQLiteDatabase db = getWritableDatabase();
			Cursor dbresult = db.query(
					"directories",
					new String[] {"dir", "last_scan"},
					null, null, null, null, null, null);
			int dirCount = dbresult.getCount();
			dbresult.moveToFirst();
			// loop over the directories configured
			for (int i = 0; i < dirCount; i++) {
				String dir = dbresult.getString(0);
				long lastScan = dbresult.getLong(1);
				Log.d(LOGTAG, dbresult.getString(0) + " " + dbresult.getLong(1));
				// loop over all possible mount points
				for (String m:mountPoints) {
					File indir = new File(m + "/" + dir);
					Log.d(LOGTAG, "Scanning directory " + indir.getAbsolutePath());
					if (indir.exists()) {			
						Cursor dbresult2 = db.query(
								"photos",
								new String[] {"distinct dir"},
								"dir LIKE '" + indir.getAbsolutePath() + "%'", 
								null, null, null, null, null);
						int dirCount2 = dbresult2.getCount();
						dbresult2.moveToFirst();
						for (int j = 0; j < dirCount2; j++) {
							String dir2 = dbresult2.getString(0);
							Log.d(LOGTAG, "Checking dir " + dir2);
							File pDir = new File(dir2);
							if (!pDir.exists()) {
								Log.d(LOGTAG, "Deleting entries for gone dir " + dir2);
								db.delete("photos","dir = ?", new String[] {dir2});
							}
							dbresult2.moveToNext();
						}
						dbresult2.close();
						scanDir(db, indir.getAbsolutePath(), lastScan);
						ContentValues values = new ContentValues();
						Log.d(LOGTAG,"updating last scan for " + indir.getName() + " to " + System.currentTimeMillis());
						values.put("last_scan", System.currentTimeMillis());	
						db.update("directories", values, "dir = ?", new String[] {indir.getName()});
					} else {
						Log.d(LOGTAG, "Directory " + indir.getAbsolutePath() + " doesn't exist");
						// remove all entries for this directory
						db.delete("photos","dir = ?", new String[] {indir.getAbsolutePath()});
						db.delete("photos","dir LIKE ?", new String[] {indir.getAbsolutePath() + "/%"});
					}
				}
				dbresult.moveToNext();
			}
			dbresult.close();
			db.close();
		} catch (SQLiteException ex) {
			// Don't crash just report
			ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
			ACRA.getErrorReporter().handleException(ex);
		}
	}
	
	private void scanDir(SQLiteDatabase db, String dir, long lastScan) {
		
		Log.d(LOGTAG,"directory " + dir + " last Scan " + (new Date(lastScan)).toString());
		File indir = new File(dir);
		boolean needsReindex = false;
		if (indir != null) {
			if (indir.lastModified() >= lastScan) { // directory was modified
				// remove all entries
				Log.d(LOGTAG,"deleteing refs for reindex");
				try {
					db.delete("photos","dir = ?", new String[] { indir.getAbsolutePath()});
				} catch (SQLiteException sqex) { 
					Log.d(LOGTAG, sqex.toString()); 
					ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
					ACRA.getErrorReporter().handleException(sqex);
				}
				needsReindex = true;
			}
			// now process 
			File[] list = indir.listFiles();
			if (list == null) {
				return;
			}
			// check if we shouldn't process this directory, not the most efficient way likely
			for (File f:list) {
				if (f.getName().equals(".novespucci")) {
					return;
				}
			}
			for (File f:list) {
				if (f.isDirectory()) {
					//recursive decent
					scanDir(db, f.getAbsolutePath(), lastScan);
				}
				if (needsReindex && f.getName()
						.toLowerCase(Locale.US)
						.endsWith(Paths.FILE_EXTENSION_IMAGE)) {
					addPhoto(db, indir, f);
				}
			}
		}
	}
	
	public void addPhoto(File f) {
		SQLiteDatabase db = getWritableDatabase();
		// Log.i(LOGTAG,"Adding entry in " + f.getParent());
		addPhoto(db, f.getParentFile(), f);
		db.close();
	}
	
	public void addPhoto(SQLiteDatabase db, File dir, File f) {
		// Log.i(LOGTAG,"Adding entry from " + f.getName());
		try {	
			Photo p = new Photo(f);
			ContentValues values = new ContentValues();
			values.put("lat", p.getLat());
			values.put("lon", p.getLon());
			// Log.i(LOGTAG,"Lat: " + p.getLat() + " " + p.getLon());
			if (p.hasDirection()) {
				values.put("direction", p.getDirection());
			}
			values.put("dir", dir.getAbsolutePath());
			values.put("name", f.getName());
			db.insert("photos", null, values); 
		} catch (SQLiteException sqex) { 
			Log.d(LOGTAG, sqex.toString());
			ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
			ACRA.getErrorReporter().handleException(sqex);
		} catch (IOException ioex) {
			// ignore silently broken pictures are not our business
			// Log.d(LOGTAG, ioex.toString());
		} catch (Exception ex) {
			Log.d(LOGTAG, ex.toString());
			ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
			ACRA.getErrorReporter().handleException(ex);
		} // ignore
	}

	/**
	 * Return all photographs in a given bounding box
	 * @param cur
	 * @return
	 */
	public Collection<Photo> getPhotos(Rect cur) {
		Collection<Photo> result = new ArrayList<Photo>();
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor dbresult = db.query(
					"photos",
					new String[] {"lat", "lon", "direction", "dir", "name"},
					"lat >= " + cur.bottom + " AND lat <= " + cur.top + " AND lon >= " + cur.left + " AND lon <= " + cur.right, 
					null, null, null, null, null);
			int photoCount = dbresult.getCount();
			dbresult.moveToFirst();
			// loop over the directories configured
			for (int i = 0; i < photoCount; i++) {
				if (dbresult.isNull(2) ) { // no direction
					result.add(new Photo(dbresult.getInt(0), dbresult.getInt(1), dbresult.getString(3) + "/" + dbresult.getString(4)));
				} else {
					result.add(new Photo(dbresult.getInt(0), dbresult.getInt(1), dbresult.getInt(2), dbresult.getString(3) + "/" + dbresult.getString(4)));
				}
				dbresult.moveToNext();
			}
			dbresult.close();
			db.close();
		} catch (SQLiteException ex) {
			// shoudn't happen (getReadableDatabase failed), simply report for now
			ACRA.getErrorReporter().handleException(ex);
		}
		Log.i(LOGTAG,"Found " + result.size() + " photos");
		return result;
	}
}
