package de.blau.android.osb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;

import de.blau.android.Map;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.resources.Profile;
import de.blau.android.util.GeoMath;
import de.blau.android.views.IMapView;
import de.blau.android.views.overlay.OpenStreetMapViewOverlay;

public class MapOverlay extends OpenStreetMapViewOverlay {
	
	/** Maximum closed age to display: 7 days. */
	private static final long MAX_CLOSED_AGE = 7 * 24 * 60 * 60 * 1000;
	
	/** viewbox needs to be less wide than this for displaying bugs, just to avoid querying the whole world for bugs */ 
	private static final int TOLERANCE_MIN_VIEWBOX_WIDTH = 40000 * 32;
	
	/** Previously requested area. */
	private Rect prev;
	
	/** Current area. */
	private Rect cur;
	
	/** Paint for open bugs. */
	private final Paint openPaint;
	
	/** Paint for closed bugs. */
	private final Paint closedPaint;
	
	/** Map this is an overlay of. */
	private final Map map;
	
	/** Bugs visible on the overlay. */
	private Collection<Bug> bugs;
	
	/** Event handlers for the overlay. */
	private final Handler handler;
	
	private Server server;
	
	/** Request to update the bugs for the current view.
	 * Ensure cur is set before invoking.
	 */
	private final Runnable getBugs = new Runnable() {
		@Override
		public void run() {
			new AsyncTask<Void, Void, Collection<Bug>>() {
				@Override
				protected Collection<Bug> doInBackground(Void... params) {
					return server.getNotesForBox(cur,100);
				}
				
				@Override
				protected void onPostExecute(Collection<Bug> result) {
					if (result == null)
						return;
					prev.set(cur);
					bugs.clear();
					long now = System.currentTimeMillis();
					for (Bug b : result) {
						// add open bugs or closed bugs younger than 7 days
						if (!b.isClosed() || (now - b.getMostRecentChange().getTime()) < MAX_CLOSED_AGE) {
							bugs.add(b);
						}
					}

					if (!bugs.isEmpty()) {
						map.invalidate(); // if other overlay is going invalidate we shoudn't
					}
				}
				
			}.execute();
		}
	};
	
	public MapOverlay(final Map map, Server s) {
		this.map = map;
		server = s;
		prev = new Rect();
		cur = new Rect();
		bugs = new ArrayList<Bug>();
		handler = new Handler();
		openPaint = Profile.getCurrent(Profile.OPEN_NOTE).getPaint();
		closedPaint = Profile.getCurrent(Profile.CLOSED_NOTE).getPaint();
	}
	
	@Override
	public boolean isReadyToDraw() {
		if (map.getPrefs().isOpenStreetBugsEnabled()) {
			return map.getOpenStreetMapTilesOverlay().isReadyToDraw();
		}
		return true;
	}
	
	@Override
	protected void onDraw(Canvas c, IMapView osmv) {
		if (map.getPrefs().isOpenStreetBugsEnabled()) {
			final Rect viewPort = c.getClipBounds();
			// the idea is to have the circles a bit bigger when zoomed in, not so
			// big when zoomed out
			final float radius = 1.0f + osmv.getZoomLevel(viewPort) / 2.0f;
			BoundingBox bb = osmv.getViewBox();
			
			if ((bb.getWidth() > TOLERANCE_MIN_VIEWBOX_WIDTH) || (bb.getHeight() > TOLERANCE_MIN_VIEWBOX_WIDTH)) {
				return;
			}
			cur.set(bb.getLeft(), bb.getTop(), bb.getRight(), bb.getBottom());
			if (!cur.equals(prev)) {
				// map has moved/zoomed - need to refresh the bugs on display
				// don't flood OSB with requests - wait for 2s
				handler.removeCallbacks(getBugs);
				handler.postDelayed(getBugs, 2000);
			}
			// draw all the bugs on the map as slightly transparent circles
			for (Bug b : bugs) {
				if (bb.isIn(b.getLat(), b.getLon())) {
					float x = GeoMath.lonE7ToX(viewPort.width() , bb, b.getLon());
					float y = GeoMath.latE7ToY(viewPort.height(), viewPort.width(), bb, b.getLat());
					c.drawCircle(x, y, radius, b.isClosed() ? closedPaint : openPaint);
				}
			}
		}
	}

	@Override
	protected void onDrawFinished(Canvas c, IMapView osmv) {
		// do nothing
	}
	
	/**
	 * Given screen coordinates, find all nearby bugs.
	 * @param x Screen X-coordinate.
	 * @param y Screen Y-coordinate.
	 * @param viewBox Map view box.
	 * @return List of bugs close to given location.
	 */
	public List<Bug> getClickedBugs(final float x, final float y, final BoundingBox viewBox) {
		List<Bug> result = new ArrayList<Bug>();
		if (map.getPrefs().isOpenStreetBugsEnabled()) {
			final float tolerance = Profile.getCurrent().nodeToleranceValue;
			for (Bug b : bugs) {
				int lat = b.getLat();
				int lon = b.getLon();
				float differenceX = Math.abs(GeoMath.lonE7ToX(map.getWidth(), viewBox, lon) - x);
				float differenceY = Math.abs(GeoMath.latE7ToY(map.getHeight(), map.getWidth(), viewBox, lat) - y);
				if ((differenceX <= tolerance) && (differenceY <= tolerance)) {
					if (Math.hypot(differenceX, differenceY) <= tolerance) {
						result.add(b);
					}
				}
			}
			// For debugging the OSB editor when the OSB site is down:
			//result.add(new Bug(GeoMath.yToLatE7(map.getHeight(), viewBox, y), GeoMath.xToLonE7(map.getWidth(), viewBox, x), true));
		}
		return result;
	}
	
	/**
	 * Add a bug to the overlay. Intended for when a bug is added to the map. The map will
	 * need to be invalidated for the change to be shown.
	 * @param bug New bug.
	 */
	public void addBug(final Bug bug) {
		bugs.add(bug);
	}
	
}
