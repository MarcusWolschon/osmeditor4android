package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import de.blau.android.App;

/**
 * Very simple cache for notification ids.
 * 
 * @author simon
 *
 */
public class NotificationCache implements Serializable {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, IssueAlert.class.getSimpleName().length());
    private static final String DEBUG_TAG = NotificationCache.class.getSimpleName().substring(0, TAG_LEN);

    private static final long serialVersionUID = 2L;
    private List<Integer>     cache;
    private int               size             = 5;

    /**
     * Construct a new cache getting the size from the preferences
     * 
     * @param ctx Android Context
     */
    public NotificationCache(@NonNull Context ctx) {
        init(App.getPreferences(ctx).getNotificationCacheSize());
    }

    /**
     * Initialize the cache
     * 
     * @param size the size of the cache
     */
    private void init(int size) {
        Log.d(DEBUG_TAG, "new notification cache size " + size);
        if (size < 1) {
            Log.e(DEBUG_TAG, "Cache size needs to be at least 1");
            size = 1;
        }
        cache = new ArrayList<>(size);
        this.size = size;
    }

    /**
     * Save notification id, canceling and removing the oldest notification if cache is full
     * 
     * @param manager a NotificationManager instance
     * @param id the id to save
     */
    synchronized void save(@NonNull NotificationManagerCompat manager, int id) {
        if (cache.size() >= size) {
            remove(manager);
        }
        cache.add(0, id);
    }

    /**
     * Remove notification id from cache and cancel it
     * 
     * @param manager a NotificationManager instance
     * @param id the id to remove
     */
    synchronized void remove(@NonNull NotificationManagerCompat manager, int id) {
        remove(id);
        manager.cancel(id); // cancel even if not found
    }

    /**
     * Remove notification if from cache
     * 
     * @param id the id of the notification to remove
     */
    synchronized void remove(int id) {
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i) == id) {
                cache.remove(i);
                break;
            }
        }
    }

    /**
     * Remove oldest notification from cache and cancel it
     * 
     * @param manager a NotificationManager instance
     */
    private synchronized void remove(@NonNull NotificationManagerCompat manager) {
        // remove notification
        int last = cache.size() - 1;
        if (last >= 0) {
            manager.cancel(cache.get(last));
            cache.remove(last);
        }
    }

    /**
     * Check if the cache is empty
     * 
     * @return true is empty
     */
    public boolean isEmpty() {
        return cache == null || cache.isEmpty();
    }

    /**
     * Reduce or expand cache size
     * 
     * @param ctx Android Context
     */
    public synchronized void trim(@NonNull Context ctx) {
        int prefSize = App.getPreferences(ctx).getNotificationCacheSize();
        Log.d(DEBUG_TAG, "trim " + prefSize + "/" + cache.size() + "/" + size);
        if (prefSize > this.size) {
            this.size = prefSize;
        } else if (prefSize < this.size) {
            NotificationManagerCompat manager = NotificationManagerCompat.from(ctx);
            for (int i = 0; i < (this.size - prefSize); i++) {
                remove(manager);
            }
            this.size = prefSize;
        }
    }
}
