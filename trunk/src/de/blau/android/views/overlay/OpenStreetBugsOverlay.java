package de.blau.android.views.overlay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.osb.Bug;
import de.blau.android.osb.Database;
import de.blau.android.osm.BoundingBox;
import de.blau.android.resources.Paints;
import de.blau.android.util.GeoMath;
import de.blau.android.views.IMapView;

public class OpenStreetBugsOverlay extends OpenStreetMapViewOverlay {
	
	/** Maximum closed age to display: 7 days. */
	private static final long MAX_CLOSED_AGE = 7 * 24 * 60 * 60 * 1000;
	
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
	
	/** Request to update the bugs for the current view.
	 * Ensure cur is set before invoking.
	 */
	private final Runnable getBugs = new Runnable() {
		public void run() {
			new AsyncTask<Void, Void, Collection<Bug>>() {
				@Override
				protected Collection<Bug> doInBackground(Void... params) {
					return Database.get(cur);
				}
				
				@Override
				protected void onPostExecute(Collection<Bug> result) {
					prev.set(cur);
					bugs.clear();
					long now = System.currentTimeMillis();
					for (Bug b : result) {
						// add open bugs or closed bugs younger than 7 days
						if (!b.isClosed() || (now - b.getMostRecentChange().getTime()) < MAX_CLOSED_AGE) {
							bugs.add(b);
						}
					}
					map.invalidate();
				}
				
			}.execute();
		}
	};
	
	public OpenStreetBugsOverlay(final Map map) {
		this.map = map;
		prev = new Rect();
		cur = new Rect();
		bugs = new ArrayList<Bug>();
		handler = new Handler();
		Resources r = map.getContext().getResources();
		openPaint = new Paint();
		openPaint.setColor(r.getColor(R.color.bug_open));
		openPaint.setAlpha(200);
		closedPaint = new Paint();
		closedPaint.setColor(r.getColor(R.color.bug_closed));
		closedPaint.setAlpha(200);
	}
	
	@Override
	protected void onDraw(Canvas c, IMapView osmv) {
		if (map.getPrefs().isOpenStreetBugsEnabled()) {
			final Rect viewPort = c.getClipBounds();
			// the idea is to have the circles a bit bigger when zoomed in, not so
			// big when zoomed out
			final float radius = 1.0f + (float)osmv.getZoomLevel(viewPort) / 2.0f;
			BoundingBox bb = osmv.getViewBox();
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
					float x = GeoMath.lonE7ToX(viewPort.width(), bb, b.getLon());
					float y = GeoMath.latE7ToY(viewPort.height(), bb, b.getLat());
					c.drawCircle(x, y, radius, b.isClosed() ? closedPaint : openPaint);
				}
			}
		}
	}

	@Override
	protected void onDrawFinished(Canvas c, IMapView osmv) {
		// do nothing
	}
	
	public List<Bug> getClickedBugs(final float x, final float y, final BoundingBox viewBox) {
		List<Bug> result = new ArrayList<Bug>();
		if (map.getPrefs().isOpenStreetBugsEnabled()) {
			final float tolerance = Paints.NODE_TOLERANCE_VALUE;
			for (Bug b : bugs) {
				int lat = b.getLat();
				int lon = b.getLon();
				float differenceX = Math.abs(GeoMath.lonE7ToX(map.getWidth(), viewBox, lon) - x);
				float differenceY = Math.abs(GeoMath.latE7ToY(map.getHeight(), viewBox, lat) - y);
				if ((differenceX <= tolerance) && (differenceY <= tolerance)) {
					if (Math.sqrt(Math.pow(differenceX, 2) + Math.pow(differenceY, 2)) <= tolerance) {
						result.add(b);
					}
				}
			}
			// For debugging the OSB editor when the OSB site is down:
			//result.add(new Bug(GeoMath.yToLatE7(map.getHeight(), viewBox, y), GeoMath.xToLonE7(map.getWidth(), viewBox, x), true));
		}
		return result;
	}
	
}
