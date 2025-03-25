package io.vespucci.layer;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.PostAsyncActionHandler;
import io.vespucci.osm.BoundingBox;

public interface DownloadInterface {
    /**
     * Download data for a bounding box
     * 
     * @param context Android context
     * @param box the bounding box
     * @param handler handler to run after the download if not null
     */
    void downloadBox(@NonNull final Context context, @NonNull final BoundingBox box, @Nullable final PostAsyncActionHandler handler);
}
