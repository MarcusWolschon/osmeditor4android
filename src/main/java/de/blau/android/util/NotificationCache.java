package de.blau.android.util;

import java.io.Serializable;
import java.util.ArrayList;

import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import de.blau.android.prefs.Preferences;

/**
 * Very simple cache for notification ids.
 * @author simon
 *
 */
public class NotificationCache implements Serializable {
	private static final String DEBUG_TAG = "NotificationCache";
	private static final long serialVersionUID = 1L;
	private ArrayList<Integer> cache;
	private int size = 5;
	
	public NotificationCache(Context ctx) {
		Preferences prefs = new Preferences(ctx);
		init(prefs.getNotificationCacheSize());
	}
	
	public NotificationCache(int size) {
		init(size);
	}
	
	private void init(int size) {
		Log.d(DEBUG_TAG, "new notification cache size " + size);
		if (size <=1) {
			throw new IllegalArgumentException("Cache size needs to be at least 1");
		}
		cache = new ArrayList<Integer>(size);
		this.size = size;
	}
	
	/**
	 * Save notification id, canceling and removing the oldest notification if cache is full
	 * @param mNotificationManager
	 * @param id
	 */
	synchronized void save(NotificationManager mNotificationManager, int id) {
		// Log.d(DEBUG_TAG, "saving " + id + " " + cache.size() + " of " + size);
		if (cache.size() >= size) {
			remove(mNotificationManager);
		}
		cache.add(0,id);
	}
	
	/**
	 * Remove id from cache and cancel it
	 * @param manager
	 * @param id
	 */
	synchronized void remove(NotificationManager manager, int id) {
		for (int i=0;i < cache.size();i++) {
			if (cache.get(i)==id) {
				cache.remove(i);
				break;
			}
		}
		manager.cancel(id); // cancel even if not found
	}
	
	/**
	 * Remove id from cache
	 * @param id
	 */
	synchronized void remove(int id) {
		for (int i=0;i < cache.size();i++) {
			if (cache.get(i)==id) {
				cache.remove(i);
				break;
			}
		}
	}
	
	/**
	 * Remove oldest notification from cache and cancel it
	 */
	private synchronized void remove(NotificationManager manager) {
		// remove notification
		int last = cache.size() - 1;
		if (last >= 0) {
			// Log.d(DEBUG_TAG, "removing oldest alert " + cache.get(last));
			manager.cancel(cache.get(last));
			cache.remove(last);
		}
	}
	
	/**
	 * 
	 */
	public boolean isEmpty() {
		return cache == null || cache.size() == 0;
	}

	/**
	 * Reduce or expand cache size
	 */
	public synchronized void trim(Context ctx) {
		
		Preferences prefs = new Preferences(ctx);
		int prefSize = prefs.getNotificationCacheSize();
		Log.d(DEBUG_TAG, "trim " + prefSize + "/" + cache.size() + "/" + size);
		if (prefSize > this.size) {
			this.size = prefSize;
		} else if (prefSize < this.size) {
			NotificationManager manager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
			for (int i=0;i<(this.size-prefSize);i++) {
				remove(manager);
			}
			this.size = prefSize;
		}
		
	}
}
