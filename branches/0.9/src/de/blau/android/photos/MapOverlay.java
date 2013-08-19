package de.blau.android.photos;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import de.blau.android.Application;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.osb.Bug;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.resources.Profile;
import de.blau.android.util.GeoMath;
import de.blau.android.views.IMapView;
import de.blau.android.views.overlay.OpenStreetMapViewOverlay;

/**
 * implement a geo-referenced photo overlay, code stolen from the OSM implementation
 * @author simon
 *
 */
public class MapOverlay extends OpenStreetMapViewOverlay {
	
	/** viewbox needs to be less wide than this for displaying bugs, just to avoid querying the whole world for bugs */ 
	private static final int TOLERANCE_MIN_VIEWBOX_WIDTH = 40000 * 32;
	
	/** Previously requested area. */
	private Rect prev;
	
	/** Current area. */
	private Rect cur;
	
	/** Map this is an overlay of. */
	private final Map map;
	
	/** Photos visible on the overlay. */
	private Collection<Photo> photos;
	
	/** have we already run a scan? */
	private boolean indexed = false;
	
	private final Drawable icon;
	
	/** Event handlers for the overlay. */
	private final Handler handler;
	
	/** Request to update the bugs for the current view.
	 * Ensure cur is set before invoking.
	 */
	private final Runnable getPhotos = new Runnable() {
		public void run() {
			new AsyncTask<Void, Integer, Collection<Photo>>() {
				
				@Override
				protected Collection<Photo> doInBackground(Void... params) {
					if (!cur.equals(prev)) { // attempt to suppress unnecessary invalidations
						PhotoIndex pi = new PhotoIndex(Application.mainActivity);
						if (!indexed) {
							publishProgress(0);
							pi.createOrUpdateIndex();
							publishProgress(1);
							indexed = true;
						}
						return pi.getPhotos(cur);
					}
					else return null;
				}
				
				@Override
				protected void onProgressUpdate(Integer ... progress) {
					if (progress[0] == 0)
						Toast.makeText(Application.mainActivity, R.string.toast_photo_indexing_started, Toast.LENGTH_SHORT).show();
					if (progress[0] == 1)
						Toast.makeText(Application.mainActivity, R.string.toast_photo_indexing_finished, Toast.LENGTH_SHORT).show();
				}
				
				@Override
				protected void onPostExecute(Collection<Photo> result) {
					if (!cur.equals(prev)) { // attempt to suppress unnecessary invalidations
						prev.set(cur);
						photos.clear();
						if (!result.isEmpty()) {
							photos.addAll(result);
							map.invalidate();
						}
					}
				}
				
			}.execute();
		}
	};
	
	public MapOverlay(final Map map, Server s) {
		this.map = map;
		prev = new Rect();
		cur = new Rect();
		photos = new ArrayList<Photo>();
		handler = new Handler();
		icon = Application.mainActivity.getResources().getDrawable(R.drawable.camera);
	}
	
	public boolean isReadyToDraw() {
		if (map.getPrefs().isPhotoLayerEnabled()) {
			return map.getOpenStreetMapTilesOverlay().isReadyToDraw();
		}
		return true;
	}
	
	@Override
	protected void onDraw(Canvas c, IMapView osmv) {
		if (map.getPrefs().isPhotoLayerEnabled()) {
			final Rect viewPort = c.getClipBounds();
			// the idea is to have the circles a bit bigger when zoomed in, not so
			// big when zoomed out
			final float radius = 1.0f + (float)osmv.getZoomLevel(viewPort) / 2.0f;
			BoundingBox bb = osmv.getViewBox();
			
			if ((bb.getWidth() > TOLERANCE_MIN_VIEWBOX_WIDTH) || (bb.getHeight() > TOLERANCE_MIN_VIEWBOX_WIDTH)) {
				return;
			}
			cur.set(bb.getLeft(), bb.getTop(), bb.getRight(), bb.getBottom());
			if (!cur.equals(prev)) {
				// map has moved/zoomed - need to refresh the bugs on display
				// don't flood OSB with requests - wait for 2s
				handler.removeCallbacks(getPhotos);
				handler.postDelayed(getPhotos, 500); // half a second delay
			}
			// draw all the bugs on the map as slightly transparent circles
			for (Photo p : photos) {
				if (bb.isIn(p.getLat(), p.getLon())) {
					int x = (int) GeoMath.lonE7ToX(viewPort.width() , bb, p.getLon());
					int y = (int) GeoMath.latE7ToY(viewPort.height(), bb, p.getLat());
					int w2 = icon.getIntrinsicWidth() / 2;
					int h2 = icon.getIntrinsicHeight() / 2;
					icon.setBounds(new Rect(x - w2, y - h2, x + w2, y + h2));
					icon.draw(c);
				}
			}
		}
	}

	@Override
	protected void onDrawFinished(Canvas c, IMapView osmv) {
		// do nothing
	}
	
	/**
	 * Given screen coordinates, find all nearby photos.
	 * @param x Screen X-coordinate.
	 * @param y Screen Y-coordinate.
	 * @param viewBox Map view box.
	 * @return List of bugs close to given location.
	 */
	public List<Photo> getClickedPhotos(final float x, final float y, final BoundingBox viewBox) {
		List<Photo> result = new ArrayList<Photo>();
		Log.d("photos.MapOverlay", "getClickedPhotos");	
		if (map.getPrefs().isPhotoLayerEnabled()) {
			final float tolerance = Profile.nodeToleranceValue;
			for (Photo p : photos) {
				int lat = p.getLat();
				int lon = p.getLon();
				float differenceX = Math.abs(GeoMath.lonE7ToX(map.getWidth(), viewBox, lon) - x);
				float differenceY = Math.abs(GeoMath.latE7ToY(map.getHeight(), viewBox, lat) - y);
				if ((differenceX <= tolerance) && (differenceY <= tolerance)) {
					if (Math.hypot(differenceX, differenceY) <= tolerance) {
						result.add(p);
					}
				}
			}
		}
		Log.d("photos.MapOverlay", "getClickedPhotos found " + result.size());
		return result;
	}
}
