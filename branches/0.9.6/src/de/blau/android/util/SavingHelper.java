package de.blau.android.util;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.acra.ACRA;

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
	 * Original version was running out of stack, fixed by moving to a thread
	 * 
	 * @param filename filename of the save file
	 * @param object object to save
	 * @param compress true if the output should be gzip-compressed, false if it should be written without compression
	 * @return true if successful, false if saving failed for some reason
	 */
	public synchronized boolean save(String filename, T object, boolean compress) {
		
		try
		{
			Log.d("SavingHelper", "preparing to save " + filename);
			SaveThread r = new SaveThread(filename, object, compress);
			Thread t = new Thread(null, r, "SaveThread", 200000);
			t.start();
			t.join(60000); // wait max 60 s for thread to finish TODO this needs to be done differently given this limits the size of the file that can be saved
			Log.d("SavingHelper", "save thread finished");
			return r.getResult();
		} catch (Exception e) {
			ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
			ACRA.getErrorReporter().handleException(e); // serious error report if we has crashed
			return false;
		}
	}
		
	public class SaveThread implements Runnable {
		
		String filename;
		T	object;
		boolean compress;
		boolean result = false;
		
		SaveThread(String fn, T obj, boolean c) {
			filename = fn;
			object = obj;
			compress = c;
			
		}
		
		public boolean getResult() {
			return result;
		}
		
		@Override
		public void run() {

        	OutputStream out = null;
        	ObjectOutputStream objectOut = null;
        	try {
        		Log.i("SavingHelper", "saving  " + filename);
        		Context context = Application.mainActivity.getApplicationContext();
        		String tempFilename = filename + "." + System.currentTimeMillis();
        		out = context.openFileOutput(tempFilename, Context.MODE_PRIVATE);
        		if (compress) {
        			out = new GZIPOutputStream(out);
        		}
        		objectOut = new ObjectOutputStream(out);
        		objectOut.writeObject(object);
        		rename(context, filename, filename + ".backup"); // don't overwrite last saved state
        		rename(context, tempFilename, filename); 		 // rename to expected name 
        		Log.i("SavingHelper", "saved " + filename + " successfully");
        		result = true;
        	} catch (Exception e) {
        		Log.e("SavingHelper", "failed to save "+filename, e);
        		ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
        		ACRA.getErrorReporter().handleException(e); // serious error report if we has crashed
        		result = false;
        	} catch (Error e) {
        		Log.e("SavingHelper", "failed to save "+filename, e);
        		ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
        		ACRA.getErrorReporter().handleException(e); // serious error report if we has crashed
        		result = false;
        	} finally {
        		SavingHelper.close(objectOut);
        		SavingHelper.close(out);
        	}
        }
	}
	
	/**
	 * Loads and deserializes a single object from the given file
	 * Original version was running out of stack, fixed by moving to a thread
	 *  
	 * @param filename filename of the save file
	 * @param compressed true if the output is gzip-compressed, false if it is uncompressed
	 * @return the deserialized object if successful, null if loading/deserialization/casting failed
	 */
	public synchronized T load(String filename, boolean compressed) {
		try
		{
			Log.d("SavingHelper", "preparing to load " + filename);
			LoadThread r = new LoadThread(filename, compressed);
			Thread t = new Thread(null, r, "LoadThread", 200000);
			t.start();
			t.join(60000); // wait max 60 s for thread to finish TODO this needs to be done differently given this limits the size of the file that can be loaded
			Log.d("SavingHelper", "load thread finished");
			return r.getResult();
		} catch (Exception e) {
			ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
			ACRA.getErrorReporter().handleException(e); // serious error report if we has crashed
			return null;
		}
	}
	
	public class LoadThread implements Runnable {
		
		String filename;
		boolean compressed;
		T result;
		
		LoadThread(String fn,  boolean c) {
			filename = fn;
			compressed = c;
		}
		
		public T getResult() {
			return result;
		}
		
		@Override
		public void run() {

			InputStream in = null;
			ObjectInputStream objectIn = null;
			try {
				Log.d("SavingHelper", "loading  " + filename);
				Context context = Application.mainActivity.getApplicationContext();
				in = context.openFileInput(filename);
				if (compressed) {
					in = new GZIPInputStream(in);
				}
				objectIn = new ObjectInputStream(in);
				@SuppressWarnings("unchecked") // casting exceptions are caught by the exception handler
				T object = (T) objectIn.readObject();
				Log.d("SavingHelper", "loaded " + filename + " successfully");
				result = object;
			} catch (Exception e) {
				Log.e("SavingHelper", "failed to load " + filename, e);
				result = null;
				if (e instanceof InvalidClassException) { // serial id mismatch, will typically happen on upgrades
					// do nothing 
				} else {
					ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
					ACRA.getErrorReporter().handleException(e); //
				}
			} catch (Error e) {
				Log.e("SavingHelper", "failed to load " + filename, e);
				result = null;
				ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
				ACRA.getErrorReporter().handleException(e); // 
			} finally {
				SavingHelper.close(objectIn);
				SavingHelper.close(in);
			}
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
				Log.e("SavingHelper", "Problem closing", e);
			}
		}
	}
	
	/**
	 * Rename existing file in same directory if target file exists, delete
	 * Code nicked from http://stackoverflow.com/users/325442/mr-bungle
	 * @param context
	 * @param originalFileName
	 * @param newFileName
	 */
	static void rename(Context context, String originalFileName, String newFileName) {
	    File originalFile = context.getFileStreamPath(originalFileName);
	    if (originalFile.exists()) {
	    	Log.d("SavingHelper", "renaming " + originalFileName + " size " + originalFile.length() + " to " + newFileName);
	    	File newFile = new File(originalFile.getParent(), newFileName);
	    	if (newFile.exists()) {
	    		context.deleteFile(newFileName);        
	    	}
	    	originalFile.renameTo(newFile);
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
				String filename = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss", Locale.US).format(new Date())+"."+exportable.exportExtension();
				File outfile = new File(outdir, filename);
				OutputStream outputStream = null;
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
			
			@Override
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
