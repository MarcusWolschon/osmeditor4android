package de.blau.android.util;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import de.blau.android.Application;
import de.blau.android.R;

/**
 * Helper class for loading and saving individual serializable objects to files.
 * Does all error handling, stream opening etc.
 *
 * @param <T> The type of the saved objects
 */
public class SavingHelper<T extends Serializable> {
	
	/**
	 * Serializes the given object and writes it to a private file with the given name
	 * 
	 * @param filename filename of the save file
	 * @param object object to save
	 * @param compress true if the output should be gzip-compressed, false if it should be written without compression
	 * @return true if successful, false if saving failed for some reason
	 */
	public synchronized boolean save(String filename, T object, boolean compress) {
		OutputStream out = null;
		GZIPOutputStream gzout = null;
		ObjectOutputStream objectOut = null;
		try {
			Context context = Application.mainActivity.getApplicationContext();
			out = context.openFileOutput(filename, Context.MODE_PRIVATE);
			if (compress) {
				gzout = new GZIPOutputStream(out);
				objectOut = new ObjectOutputStream(gzout);
			} else {
				objectOut = new ObjectOutputStream(out);
			}
			objectOut.writeObject(object);
			Log.i("SavingHelper", "saved " + filename + " successfully");
			return true;
		} catch (Exception e) {
			Log.e("SavingHelper", "failed to save "+filename, e);
			return false;
		} finally {
			SavingHelper.close(objectOut);
			SavingHelper.close(gzout);
			SavingHelper.close(out);
		}
	}
	
	/**
	 * Loads and deserializes a single object from the given file
	 * 
	 * @param filename filename of the save file
	 * @param compressed true if the output is gzip-compressed, false if it is uncompressed
	 * @return the deserialized object if successful, null if loading/deserialization/casting failed
	 */
	public synchronized T load(String filename, boolean compressed) {
		FileInputStream in = null;
		GZIPInputStream gzin = null;
		ObjectInputStream objectIn = null;
		try {
			Context context = Application.mainActivity.getApplicationContext();
			in = context.openFileInput(filename);
			if (compressed) {
				gzin = new GZIPInputStream(in);
				objectIn = new ObjectInputStream(gzin);
			} else {
				objectIn = new ObjectInputStream(in);
			}
			@SuppressWarnings("unchecked") // casting exceptions are caught by the exception handler
			T object = (T) objectIn.readObject();
			Log.i("SavingHelper", "loaded " + filename + " successfully");
			return object;
		} catch (Exception e) {
			Log.e("SavingHelper", "failed to load " + filename, e);
			return null;
		} finally {
			SavingHelper.close(objectIn);
			SavingHelper.close(gzin);
			SavingHelper.close(in);
		}
	}

	/**
	 * Convenience function - closes the given stream (can be any Closable), catching and logging exceptions
	 * @param stream a Closeable to close
	 */
	static public void close(final Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				Log.e("Vespucci", "Problem closing", e);
			}
		}
	}

	/**
	 * Exports an Exportable asynchronously, displaying a toast on success or failure
	 * @param ctx context for the toast
	 * @param exportable the exportable to run
	 */
	public static void asyncExport(final Context ctx, final Exportable exportable) {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				File sdcard = Environment.getExternalStorageDirectory();
				File outdir = new File(sdcard, "Vespucci");
				outdir.mkdir(); // ensure directory exists;
				String filename = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss").format(new Date())+"."+exportable.exportExtension();
				File outfile = new File(outdir, filename);
				BufferedOutputStream outputStream = null;
				try {
					outputStream = new BufferedOutputStream(new FileOutputStream(outfile));
					exportable.export(outputStream);
				} catch (Exception e) {
					Log.e("SavingHelper", "Export failed - " + filename);
					return null;
				} finally {
					SavingHelper.close(outputStream);
				}
				// workaround for android bug - make sure export file shows up via MTP
				triggerMediaScanner(ctx, outfile);
				return filename;
			}
			
			protected void onPostExecute(String result) {
				if (result == null) {
					Toast.makeText(ctx, R.string.toast_export_failed, Toast.LENGTH_SHORT).show();
				} else {
					Log.i("SavingHelper", "Successful export to " + result);
					String text = ctx.getResources().getString(R.string.toast_export_success, result);
					Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show();
				}
			};
		}.execute();
	}
	
	/**
	 * Trigger the media scanner to ensure files show up in MTP.
	 * @param context a context to use for communication with the media scanner
	 * @param scanfile directory or file to scan
	 */
	@TargetApi(11)
	public static void triggerMediaScanner(Context context, File scanfile) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) return; // API 11 - lower versions do not have MTP
		try {
			String path = scanfile.getCanonicalPath();
			Log.i("SavingHelper", "Triggering media scan for " + path);
			MediaScannerConnection.scanFile(context, new String[] {path}, null, new OnScanCompletedListener() {
				@Override
				public void onScanCompleted(String path, Uri uri) {
					Log.i("SavingHelper", "Media scan completed for " + path + " URI " + uri);
				}				
			});
		} catch (Exception e) {
			Log.e("SavingHelper", "Exception when triggering media scanner", e);
		}
	}


	public static interface Exportable {
		/** Exports some data to an OutputStream */
		public void export(OutputStream outputStream) throws Exception;
		
		/** @returns the extension to be used for exports */
		public String exportExtension();
	}
	
}
