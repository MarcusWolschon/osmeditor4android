package de.blau.android.util;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import de.blau.android.Application;


import android.content.Context;
import android.util.Log;

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
	 * @return true if successful, false if saving failed for some reason
	 */
	public synchronized boolean save(String filename, T object) {
		OutputStream out = null;
		ObjectOutputStream objectOut = null;
		try {
			Context context = Application.mainActivity.getApplicationContext();
			out = context.openFileOutput(filename, Context.MODE_PRIVATE);
			objectOut = new ObjectOutputStream(out);
			objectOut.writeObject(object);
			Log.i("SavingHelper", "saved " + filename + " successfully");
			return true;
		} catch (Exception e) {
			Log.e("SavingHelper", "failed to save "+filename, e);
			return false;
		} finally {
			SavingHelper.close(objectOut);
			SavingHelper.close(out);
		}
	}
	
	/**
	 * Loads and deserializes a single object from the given file
	 * 
	 * @param filename filename of the save file
	 * @return the deserialized object if successful, null if loading/deserialization/casting failed
	 */
	public synchronized T load(String filename) {
		FileInputStream in = null;
		ObjectInputStream objectIn = null;
		try {
			Context context = Application.mainActivity.getApplicationContext();
			in = context.openFileInput(filename);
			objectIn = new ObjectInputStream(in);
			@SuppressWarnings("unchecked") // casting exceptions are caught by the exception handler
			T object = (T) objectIn.readObject();
			Log.i("SavingHelper", "loaded " + filename + " successfully");
			return object;
		} catch (Exception e) {
			Log.e("SavingHelper", "failed to load " + filename, e);
			return null;
		} finally {
			SavingHelper.close(in);
			SavingHelper.close(objectIn);
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
