package de.blau.android.util;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.content.Context;
import android.util.Log;
import de.blau.android.Application;

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
	
}
